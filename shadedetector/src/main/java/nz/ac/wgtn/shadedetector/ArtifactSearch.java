package nz.ac.wgtn.shadedetector;

import com.google.gson.Gson;
import nz.ac.wgtn.shadedetector.classselectors.SelectClassesWithUniqueNames;
import okhttp3.*;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main API to search artifacts implementing a named class in Maven using the REST API.
 * @author jens dietrich
 */
public class ArtifactSearch {

    // https://search.maven.org/solrsearch/select?q=c:junit&rows=20&wt=json
    public static final String SEARCH_URL = "https://search.maven.org/solrsearch/select";
    public static final int DEFAULT_ROWS_REQUESTED = 200;  // maximum honoured, for larger numbers the API defaults back to 20

    public static void main (String[] args) throws ArtifactSearchException, IOException {
        // for testing only
        File commonsFolder = new File("src/test/resources/commons-collections4-4.0");
        List<Artifact> artifacts = findShadingArtifacts(Utils.listSourcecodeFilesInFolder(commonsFolder),new SelectClassesWithUniqueNames(),3,DEFAULT_ROWS_REQUESTED);

    }

    static List<Artifact> findShadingArtifacts (List<File> projectSources, ClassSelector classSelector, int maxClassesUsedForSearch, int batchSize) throws ArtifactSearchException {
        List<File> sourceFilesSelectedForSearch = classSelector.selectForSearch(projectSources);
        List<File> cappedSourceFilesSelectedForSearch = sourceFilesSelectedForSearch.stream().limit(maxClassesUsedForSearch).collect(Collectors.toList());
        for (File f:cappedSourceFilesSelectedForSearch) {
            String className = f.getName().replace(".java","");
            List<Artifact> artifacts = findShadingArtifacts (className,batchSize);
            System.out.println("artifacts matching " + className + ": " + artifacts.size());
        }
        // todo merge consolidate
        return null;

    }

    static List<Artifact> findShadingArtifacts (String className, int batchSize) throws ArtifactSearchException {
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(SEARCH_URL).newBuilder();
        urlBuilder.addQueryParameter("q", "c:"+className);
        urlBuilder.addQueryParameter("wt", "json");
        urlBuilder.addQueryParameter("rows", ""+batchSize);

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder().url(url).build();

        Call call = client.newCall(request);
        Response response = null;
        try {
            response = call.execute();
        } catch (IOException x) {
            throw new ArtifactSearchException(x);
        }

        int responseCode = response.code();
        System.out.println("response code: " + responseCode);

        if (responseCode==200) {
            Reader reader = response.body().charStream();
            ArtifactSearchResponse artifactResponse = read(reader);
            List<Artifact> artifacts = artifactResponse.getBody().getArtifacts();
            System.out.println("artifacts retrieved: " + artifacts.size());
            return artifacts;
        }
        else {
            throw new ArtifactSearchException("query returned unexpected status code " + responseCode);
        }
    }

    static ArtifactSearchResponse read(Reader input) {
        Gson gson = new Gson();
        return gson.fromJson(input,ArtifactSearchResponse.class);
    }
}
