package nz.ac.wgtn.shadedetector;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Miscellaneous utilities.
 * @author jens dietrich
 */
public class Utils {

    private static Logger LOGGER = LoggerFactory.getLogger(ArtifactSearch.class);

    public static List<File> listSourcecodeFilesInFolder(File folder) throws IOException {
        return Files.walk(folder.toPath())
            .map(p -> p.toFile())
            .filter(f -> f.getName().endsWith(".java"))
            .collect(Collectors.toList());
    }

    public static List<String> loadClassListFromFile(File file) throws IOException {
        if (file!=null) {
            LOGGER.info("loading classlist from file " + file.getAbsolutePath());
        }
        Preconditions.checkArgument(file.exists());
        Preconditions.checkArgument(!file.isDirectory());
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return loadClassListFromResource(reader);
        }
    }

    public static List<String> loadClassListFromFile(String fileName) throws IOException {
        return loadClassListFromFile(new File(fileName));
    }

    public static List<String> loadClassListFromResource(String path) throws IOException {
        if (path!=null) {
            LOGGER.info("loading classlist from resource " + path);
        }
        URL url = Utils.class.getClassLoader().getResource(path);
        LOGGER.info("loading classlist from url " + url);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return loadClassListFromResource(reader);
        }
    }

    public static List<String> loadClassListFromResource(BufferedReader reader) throws IOException {
        return reader.lines()
            .filter(line -> !line.isBlank())
            .filter(line -> !line.trim().startsWith("#")) // remove comments
            .collect(Collectors.toList());
    }
}
