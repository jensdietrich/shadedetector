package nz.ac.wgtn.shadedetector;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main API to search artifacts implementing a named class in Maven using the REST API.
 * @author jens dietrich
 */
public class ArtifactSearch {

    private static Logger LOGGER = LoggerFactory.getLogger(ArtifactSearch.class);

    static final String CACHE_BY_CLASSNAME_NAME = "artifacts-using-classes";
    static final String CACHE_ARTIFACT_VERSIONS_NAME = "artifacts-versions";

    static File CACHE_BY_CLASSNAME = Cache.getCache(CACHE_BY_CLASSNAME_NAME);
    static File CACHE_ARTIFACT_VERSIONS = Cache.getCache(CACHE_ARTIFACT_VERSIONS_NAME);

    // https://search.maven.org/solrsearch/select?q=c:junit&rows=20&wt=json
    public static final String SEARCH_URL = "https://search.maven.org/solrsearch/select";
    public static final int ROWS_PER_BATCH = 200;  // maximum honoured, for larger numbers the API defaults back to 20
    public static final int BATCHES = 5; // will result in 1k results

    public static void main (String[] args) throws ArtifactSearchException, IOException {
        // for testing only -- query by class name
//        File commonsFolder = new File("src/test/resources/commons-collections4-4.0");
//
//        List<String> classList = Utils.loadClassListFromFile("classlists/ysoserial/commons-collections4-4.0/CommonsCollections2.list");
//        ClassSelector classSelector = new SelectClassesFromList(classList);
//
//        Map<String,ArtifactSearchResponse> results = findShadingArtifacts(Utils.listSourcecodeFilesInFolder(commonsFolder),classSelector,3,BATCHES,ROWS_PER_BATCH);
//
//        LOGGER.info("Query results obtained: {}",results.size());
//
//        List<Artifact> occurInAny = ArtifactSearchResultConsolidationStrategy.Merge.consolidate(results);
//        LOGGER.info("Artifacts occurring in any results: {}",occurInAny.size());
//
//        List<Artifact> occurInAll = ArtifactSearchResultConsolidationStrategy.Intersect.consolidate(results);
//        LOGGER.info("Artifacts occurring in all results: {}",occurInAll.size());
//
//        List<Artifact> occurInMultiple = ArtifactSearchResultConsolidationStrategy.MultipleOccurrences.consolidate(results);
//        LOGGER.info("Artifacts occurring in more than one results: {}",occurInMultiple.size());

        // for testing only -- query versions

        ArtifactSearchResponse results = findVersions("org.apache.commons","commons-collections4");
        LOGGER.info("Versions found: {}",results.getBody().getArtifacts().size());


    }

    /**
     * Find candidate artifacts based on class names they share with the PoV project.
     * @return a mapping of class names to the artifact search responses they generated
     */
    static Map<String,ArtifactSearchResponse> findShadingArtifacts (Path projectSources, ClassSelector classSelector, Predicate<String> classNamePredicate, Predicate<String> gavPredicate, int maxClassesUsedForSearch, int batchCount, int maxResultsInEachBatch) {
        List<String> classNamesSelectedForSearch = classSelector.selectForSearch(projectSources);
        List<String> cappedClassNamesSelectedForSearch = classNamesSelectedForSearch.stream()
                .filter(classNamePredicate)
                .limit(maxClassesUsedForSearch)
                .collect(Collectors.toList());
        Map<String,ArtifactSearchResponse> responses = new HashMap<>();
        for (String className:cappedClassNamesSelectedForSearch) {
            LOGGER.info("querying for uses of class " + className);
            try {
                responses.put(className, findShadingArtifacts(className, gavPredicate, batchCount, maxResultsInEachBatch));
            }
            catch (ArtifactSearchException x) {
                LOGGER.error("artifact search for class " + className + " has failed",x);
            }
        }
        return responses;

    }

    static ArtifactSearchResponse findShadingArtifacts (String className, Predicate<String> gavPredicate, int batchCount, int maxResultsInEachBatch) throws ArtifactSearchException {
        List<File> cachedResultFiles = getCachedOrFetchByClass(className,batchCount,maxResultsInEachBatch);
        List<ArtifactSearchResponse> results = cachedResultFiles.stream()
                .map(f -> parse(f))
                .map(response -> ArtifactSearchResponseMerger.filterArtifacts(response, gavPredicate))
                .collect(Collectors.toList());
        ArtifactSearchResponse result = ArtifactSearchResponseMerger.merge(results);
        LOGGER.info("\t{} artifacts found with a class named \"{}\"",result.getBody().getArtifacts().size(),className);
        return result;
    }

    static ArtifactSearchResponse findVersions (String groupName, String artifactName) throws ArtifactSearchException {
        List<File> cachedResultFiles = getCachedOrFetchByGroupAndArtifactId(groupName, artifactName, ROWS_PER_BATCH);
        List<ArtifactSearchResponse> results = cachedResultFiles.stream().map(f -> parse(f)).collect(Collectors.toList());
        ArtifactSearchResponse result = ArtifactSearchResponseMerger.merge(results);
        LOGGER.info("\t{} versions found of \"{}:{}",result.getBody().getArtifacts().size(),groupName,artifactName);
        return result;
    }

    private static List<File> getCachedOrFetchByClass(String className, int batchCount, int maxResultsInEachBatch) throws ArtifactSearchException {
        Optional<List<File>> alreadyCached = getCachedByClassName(className);

        if (alreadyCached.isPresent()) {
            LOGGER.info("using cached data from " + alreadyCached.get().stream().map(f -> f.getAbsolutePath()).collect(Collectors.joining(", ")));
            return alreadyCached.get();
        }
        else {
            List<File> newFiles = new ArrayList<>();
            try {
                // Download data to a temp dir, then rename when it is complete.
                Path tempDir = Files.createTempDirectory(CACHE_BY_CLASSNAME.toPath(), "tmp.");
                Path finalDir = CACHE_BY_CLASSNAME.toPath().resolve(className);
                LOGGER.debug("\tfetching to temp dir {}", tempDir);

                for (int i=0;i<batchCount;i++) {
                    LOGGER.info("\tfetching batch {}/{}",i+1,batchCount);

                    HttpUrl.Builder urlBuilder = HttpUrl.parse(SEARCH_URL).newBuilder();
                    urlBuilder.addQueryParameter("q", "c:" + className);
                    urlBuilder.addQueryParameter("wt", "json");
                    urlBuilder.addQueryParameter("rows", "" + maxResultsInEachBatch);
                    urlBuilder.addQueryParameter("start",""+((maxResultsInEachBatch*i)+1));

                    String url = urlBuilder.build().toString();
                    String basename = className+'-'+(i+1)+".json";
                    File cachedElementTemp = new File(tempDir.toFile(),basename);
                    File cachedElementFinal = new File(finalDir.toFile(),basename);
                    try {
                        MvnRestAPIClient.fetchCharData(url,cachedElementTemp.toPath());
                        newFiles.add(cachedElementFinal);
                    } catch (IOException x) {
                        throw new ArtifactSearchException("fetch of batch " + (i+1) + " failed", x);   // Temp dir will remain
                    }
                }

                Files.move(tempDir, finalDir);  // Should work since source and target are on same FileStore
                LOGGER.debug("renamed temp dir {} to {} successfully", tempDir, finalDir);
            } catch (IOException x) {
                throw new ArtifactSearchException("creating or renaming temp dir failed", x);
            }

            return newFiles;
        }
    }


    private static List<File> getCachedOrFetchByGroupAndArtifactId(String groupId,String artifactId, int maxResultsInEachBatch) throws ArtifactSearchException {
        List<File> cached = getCachedByGroupAndArtifactId(groupId,artifactId);
        if (!cached.isEmpty()) {
            LOGGER.info("using cached data from " + cached.stream().map(f -> f.getAbsolutePath()).collect(Collectors.joining(", ")));
        }
        else {
            OkHttpClient client = new OkHttpClient();
            cached = new ArrayList<>();
            int batchCount = -1;    // -1 means we don't know how many batches there will be yet
            for (int i = 0; batchCount == -1 || i < batchCount; i++) {
                LOGGER.info("\tfetching batch {}/{}",i+1,batchCount);

                // https://search.maven.org/solrsearch/select?q=g:com.google.inject+AND+a:guice&core=gav&rows=20&wt=json
                // from https://central.sonatype.org/search/rest-api-guide/
                HttpUrl.Builder urlBuilder = HttpUrl.parse(SEARCH_URL).newBuilder();
                urlBuilder.addQueryParameter("q", "g:" + groupId + " AND " + "a:" + artifactId);
                urlBuilder.addQueryParameter("core","gav");
                urlBuilder.addQueryParameter("wt", "json");
                urlBuilder.addQueryParameter("rows", "" + maxResultsInEachBatch);
                urlBuilder.addQueryParameter("start",""+((maxResultsInEachBatch*i)+1));

                String url = urlBuilder.build().toString();
                LOGGER.info("\tsearch url: " + url);
                Request request = new Request.Builder().url(url).build();

                Call call = client.newCall(request);
                Response response = null;
                try {
                    response = call.execute();
                } catch (IOException x) {
                    throw new ArtifactSearchException(x);
                }

                int responseCode = response.code();
                LOGGER.info("\tresponse code is: {}",responseCode);

                if (responseCode == 200) {
                    File cache = new File(CACHE_ARTIFACT_VERSIONS,groupId+':'+artifactId+'-'+(i+1)+".json");
                    try (Reader reader = response.body().charStream(); Writer writer = new FileWriter(cache)) {
                        LOGGER.info("\tcaching data in {}",cache.getAbsolutePath());
                        CharStreams.copy(reader, writer);
                        cached.add(cache);
                    } catch (IOException x) {
                        throw new ArtifactSearchException("cannot read and cache response" + x);
                    }

                    // If we made it to here, the file was written and closed.
                    if (batchCount == -1) {
                        ArtifactSearchResponse firstResponse = parse(cache);
                        int numFound = firstResponse.getBody().getNumFound();
                        batchCount = (numFound + maxResultsInEachBatch - 1) / maxResultsInEachBatch;
                        LOGGER.info("\tdiscovered there are {} results, so {} batches needed", numFound, batchCount);
                    }
                } else {
                    throw new ArtifactSearchException("query returned unexpected status code " + responseCode + " - " + response.message());
                }
            }
        }
        return cached;
    }

    private static Optional<List<File>> getCachedByClassName(String className) {
        File[] contents = CACHE_BY_CLASSNAME.toPath().resolve(className).toFile().listFiles();
        return Optional.ofNullable(contents).map(List::of);
    }

    private static List<File> getCachedByGroupAndArtifactId(String groupId,String artifactId) {
        Pattern p = Pattern.compile(groupId+':'+artifactId+"-\\d+\\.json");
        return Stream.of(CACHE_ARTIFACT_VERSIONS.listFiles())
            .filter(f -> p.matcher(f.getName()).matches())
            .collect(Collectors.toList());
    }

    static ArtifactSearchResponse parse(Reader input) {
        Gson gson = new Gson();
        return gson.fromJson(input,ArtifactSearchResponse.class);
    }

    static ArtifactSearchResponse parse(File f) {
        Gson gson = new Gson();
        try (FileReader r = new FileReader(f)) {
            return gson.fromJson(r, ArtifactSearchResponse.class);
        } catch (IOException e) {
            LOGGER.error("cannot read / parse cached " + f.getAbsolutePath(),e);
            throw new RuntimeException(e);
        }
    }
}
