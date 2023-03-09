package nz.ac.wgtn.shadedetector;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import nz.ac.wgtn.shadedetector.classselectors.SelectClassesFromList;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main API to search artifacts implementing a named class in Maven using the REST API.
 * @author jens dietrich
 */
public class ArtifactSearch {

    private static Logger LOGGER = LoggerFactory.getLogger(ArtifactSearch.class);

    private static File CACHE = new File(".cache/artifacts-using-classes");

    // https://search.maven.org/solrsearch/select?q=c:junit&rows=20&wt=json
    public static final String SEARCH_URL = "https://search.maven.org/solrsearch/select";
    public static final int ROWS_PER_BATCH = 200;  // maximum honoured, for larger numbers the API defaults back to 20
    public static final int BATCHES = 5; // will result in 1k results

    public static void main (String[] args) throws ArtifactSearchException, IOException {
        // for testing only
        File commonsFolder = new File("src/test/resources/commons-collections4-4.0");

        List<String> classList = Utils.loadClassListFromFile("classlists/ysoserial/commons-collections4-4.0/CommonsCollections2.list");
        ClassSelector classSelector = new SelectClassesFromList(classList);

        Map<String,ArtifactSearchResponse> results = findShadingArtifacts(Utils.listSourcecodeFilesInFolder(commonsFolder),classSelector,3,BATCHES,ROWS_PER_BATCH);

        LOGGER.info("Query results obtained: " + results.size());

        List<Artifact> occurInAny = ArtifactSearchResultConsolidationStrategy.Merge.consolidate(results);
        LOGGER.info("Artifacts occurring in any results: " + occurInAny.size());

        List<Artifact> occurInAll = ArtifactSearchResultConsolidationStrategy.Intersect.consolidate(results);
        LOGGER.info("Artifacts occurring in all results: " + occurInAll.size());

        List<Artifact> occurInMultiple = ArtifactSearchResultConsolidationStrategy.MultipleOccurrences.consolidate(results);
        LOGGER.info("Artifacts occurring in more than one results: " + occurInMultiple.size());

    }

    static Map<String,ArtifactSearchResponse> findShadingArtifacts (List<File> projectSources, ClassSelector classSelector, int maxClassesUsedForSearch, int batchCount,int maxResultsInEachBatch) {
        List<String> classNamesSelectedForSearch = classSelector.selectForSearch(projectSources);
        List<String> cappedClassNamesSelectedForSearch = classNamesSelectedForSearch.stream().limit(maxClassesUsedForSearch).collect(Collectors.toList());
        Map<String,ArtifactSearchResponse> responses = new HashMap<>();
        for (String className:cappedClassNamesSelectedForSearch) {
            LOGGER.info("querying for uses of class " + className);
            try {
                responses.put(className, findShadingArtifacts(className, batchCount,maxResultsInEachBatch));
            }
            catch (ArtifactSearchException x) {
                LOGGER.error("artifact search for class " + className + " has failed",x);
            }
        }
        return responses;

    }

    static ArtifactSearchResponse findShadingArtifacts (String className, int batchCount,int maxResultsInEachBatch) throws ArtifactSearchException {
        List<File> cachedResultFiles = getCachedOrFetch(className,batchCount,maxResultsInEachBatch);
        List<ArtifactSearchResponse> results = cachedResultFiles.stream().map(f -> parse(f)).collect(Collectors.toList());
        ArtifactSearchResponse result = ArtifactSearchResponseMerger.merge(results);
        LOGGER.info("\t" + result.getBody().getArtifacts().size() + " artifacts found");
        return result;
    }

    private static List<File> getCachedOrFetch (String className, int batchCount, int maxResultsInEachBatch) throws ArtifactSearchException {
        if (!CACHE.exists()) {
            CACHE.mkdirs();
            LOGGER.info("created cache folder " + CACHE.getAbsolutePath());
        }
        List<File> cached = getCached(className);
        if (!cached.isEmpty()) {
            LOGGER.info("using cached data from " + cached.stream().map(f -> f.getAbsolutePath()).collect(Collectors.joining(", ")));
        }
        else {
            OkHttpClient client = new OkHttpClient();
            cached = new ArrayList<>();
            for (int i=0;i<batchCount;i++) {
                LOGGER.info("\tfetching batch " + (i+1) + "/" + batchCount);

                HttpUrl.Builder urlBuilder = HttpUrl.parse(SEARCH_URL).newBuilder();
                urlBuilder.addQueryParameter("q", "c:" + className);
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
                LOGGER.info("\tresponse code is: " + responseCode);

                if (responseCode == 200) {
                    File cache = new File(CACHE,className+'-'+(i+1)+".json");
                    try (Reader reader = response.body().charStream(); Writer writer = new FileWriter(cache)) {
                        LOGGER.info("\tcaching data in " + cache.getAbsolutePath());
                        CharStreams.copy(reader, writer);
                        cached.add(cache);
                    } catch (IOException x) {
                        throw new ArtifactSearchException("cannot read and cache response" + x);
                    }
                } else {
                    throw new ArtifactSearchException("query returned unexpected status code " + responseCode + " - " + response.message());
                }
            }
        }
        return cached;
    }

    private static List<File> getCached(String className) {
        Pattern p = Pattern.compile(className+"-\\d+\\.json");
        return Stream.of(CACHE.listFiles())
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
