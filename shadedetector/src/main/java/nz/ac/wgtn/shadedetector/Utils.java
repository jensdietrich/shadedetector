package nz.ac.wgtn.shadedetector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Miscellaneous utilities.
 * @author jens dietrich
 */
public class Utils {
    public static List<File> listSourcecodeFilesInFolder(File folder) throws IOException {
        return Files.walk(folder.toPath())
            .map(p -> p.toFile())
            .filter(f -> f.getName().endsWith(".java"))
            .collect(Collectors.toList());
    }
}
