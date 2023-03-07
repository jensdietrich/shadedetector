package nz.ac.wgtn.shadedetector;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import nz.ac.wgtn.shadedetector.classselectors.SelectClassesFromList;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main API to search artifacts implementing a named class in Maven using the REST API.
 * @author jens dietrich
 */
public class ArtifactSearch {

    private static Logger LOGGER = LoggerFactory.getLogger(ArtifactSearch.class);

    private static File CACHE = new File(".cache");

    // https://search.maven.org/solrsearch/select?q=c:junit&rows=20&wt=json
    public static final String SEARCH_URL = "https://search.maven.org/solrsearch/select";
    public static final int DEFAULT_ROWS_REQUESTED = 200;  // maximum honoured, for larger numbers the API defaults back to 20

    public static void main (String[] args) throws ArtifactSearchException, IOException {
        // for testing only
        File commonsFolder = new File("src/test/resources/commons-collections4-4.0");

        List<String> classList = Utils.loadClassListFromFile("classlists/ysoserial/commons-collections4-4.0/CommonsCollections2.list");
        ClassSelector classSelector = new SelectClassesFromList(classList);

        Map<String,ArtifactSearchResponse> results = findShadingArtifacts(Utils.listSourcecodeFilesInFolder(commonsFolder),classSelector,3,DEFAULT_ROWS_REQUESTED);

        LOGGER.info("Query results obtained: " + results.size());

        List<Artifact> occurInAny = ArtifactSearchResultConsolidationStrategy.Merge.consolidate(results);
        LOGGER.info("Artifacts occurring in any results: " + occurInAny.size());

        List<Artifact> occurInAll = ArtifactSearchResultConsolidationStrategy.Intersect.consolidate(results);
        LOGGER.info("Artifacts occurring in all results: " + occurInAll.size());

        List<Artifact> occurInMultiple = ArtifactSearchResultConsolidationStrategy.MultipleOccurrences.consolidate(results);
        LOGGER.info("Artifacts occurring in more than one results: " + occurInMultiple.size());

    }

    static Map<String,ArtifactSearchResponse> findShadingArtifacts (List<File> projectSources, ClassSelector classSelector, int maxClassesUsedForSearch, int batchSize) {
        List<String> classNamesSelectedForSearch = classSelector.selectForSearch(projectSources);
        List<String> cappedClassNamesSelectedForSearch = classNamesSelectedForSearch.stream().limit(maxClassesUsedForSearch).collect(Collectors.toList());
        Map<String,ArtifactSearchResponse> responses = new HashMap<>();
        for (String className:cappedClassNamesSelectedForSearch) {
            LOGGER.info("querying for uses of class " + className);
            try {
                responses.put(className, findShadingArtifacts(className, batchSize));
            }
            catch (ArtifactSearchException x) {
                LOGGER.error("artifact search for class " + className + " has failed",x);
            }
        }
        return responses;

    }

    static ArtifactSearchResponse findShadingArtifacts (String className, int batchSize) throws ArtifactSearchException {
        Reader reader = getCachedOrFetch(className,batchSize);
        ArtifactSearchResponse result = read(reader);
        LOGGER.info("\t" + result.getBody().getArtifacts().size() + " artifacts found");
        return result;
    }

    private static Reader getCachedOrFetch (String className, int batchSize) throws ArtifactSearchException {
        if (!CACHE.exists()) {
            CACHE.mkdirs();
            LOGGER.info("created cache folder " + CACHE.getAbsolutePath());
        }
        File cached = getCached(className,batchSize);
        if (cached.exists()) {
            LOGGER.info("using cached data from " + cached.getAbsolutePath());
        }
        else {
            OkHttpClient client = new OkHttpClient();

            HttpUrl.Builder urlBuilder = HttpUrl.parse(SEARCH_URL).newBuilder();
            urlBuilder.addQueryParameter("q", "c:"+className);
            urlBuilder.addQueryParameter("wt", "json");
            urlBuilder.addQueryParameter("rows", ""+batchSize);

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

            if (responseCode==200) {
                try (Reader reader = response.body().charStream(); Writer writer = new FileWriter(cached)) {
                    LOGGER.info("\tcaching data in " + cached.getAbsolutePath());
                    CharStreams.copy(reader,writer);
                }
                catch (IOException x) {
                    throw new ArtifactSearchException("cannot read and cache response" + x);
                }
            }
            else {
                throw new ArtifactSearchException("query returned unexpected status code " + responseCode);
            }
        }
        try {
            return new FileReader(cached);
        } catch (FileNotFoundException e) {
            throw new ArtifactSearchException(e);
        }
    }

    private static File getCached(String className, int batchSize) {
        return new File(CACHE,className + "-" + batchSize + ".json");
    }

    static ArtifactSearchResponse read(Reader input) {
        Gson gson = new Gson();
        return gson.fromJson(input,ArtifactSearchResponse.class);
    }
}
