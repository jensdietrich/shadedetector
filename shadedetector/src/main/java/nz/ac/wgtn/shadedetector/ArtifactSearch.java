package nz.ac.wgtn.shadedetector;

import com.google.gson.Gson;
import nz.ac.wgtn.shadedetector.classselectors.SelectAll;
import okhttp3.*;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * Main API to search artifacts implementing a named class in Maven using the REST API.
 * @author jens dietrich
 */
public class ArtifactSearch {

    // https://search.maven.org/solrsearch/select?q=c:junit&rows=20&wt=json
    public static final String SEARCH_URL = "https://search.maven.org/solrsearch/select";
    public static final int DEFAULT_ROWS_REQUESTED = 200;  // maximum honoured, for larger numbers the API defaults back to 20
    public static final ClassSelector SELECTOR = new SelectAll();

    public static void main (String[] args) throws ArtifactSearchException {
        OkHttpClient client = new OkHttpClient();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(SEARCH_URL).newBuilder();
        urlBuilder.addQueryParameter("q", "c:IterableSortedMap");
        urlBuilder.addQueryParameter("wt", "json");
        urlBuilder.addQueryParameter("rows", ""+DEFAULT_ROWS_REQUESTED);

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

        }
        else {
            throw new ArtifactSearchException("query returned unexpected status code " + responseCode);
        }

    }

//    static List<Artifact> findShadingArtifacts (List<File> projectSources) {
//
//    }

    static ArtifactSearchResponse read(Reader input) {
        Gson gson = new Gson();
        return gson.fromJson(input,ArtifactSearchResponse.class);
    }
}
