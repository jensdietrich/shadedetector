package nz.ac.wgtn.shadedetector;

import com.google.gson.Gson;
import java.io.Reader;

public class ArtifactSearch {

    static ArtifactSearchResponse read(Reader input) {
        Gson gson = new Gson();
        return gson.fromJson(input,ArtifactSearchResponse.class);
    }
}
