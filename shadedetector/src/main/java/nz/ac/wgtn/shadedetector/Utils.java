package nz.ac.wgtn.shadedetector;

import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.nio.file.FileSystem;

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

    public static boolean isZip (File f) {
        int fileSignature = 0;
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            fileSignature = raf.readInt();
        } catch (IOException e) {}
        return fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708;
    }


    public static List<String> getUnqualifiedJavaClassNames(Path zipOrFolder) {
        try {
            return listJavaSources(zipOrFolder).stream()
                    .map(f -> f.getFileName().toString())
                    .map(n -> n.replace(".java",""))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Error collecting Java class names from source code",e);
            throw new RuntimeException(e);
        }
    }

    public static List<Path> listJavaSources(Path zipOrFolder) throws IOException {
        return listContent(zipOrFolder,path -> path.toString().endsWith(".java"));
    }

    public static List<Path> listContent(Path zipOrFolder, Predicate<Path> filter) throws IOException {
        if (zipOrFolder.toFile().isDirectory()) {
            return Files.list(zipOrFolder)
                .filter(file -> !Files.isDirectory(file))
                .filter(filter)
                .collect(Collectors.toList());
        }
        else {

            Map<String, String> env = new HashMap<>();
            FileSystem fs = FileSystems.newFileSystem(zipOrFolder, env, null);
            return Streams.stream(fs.getRootDirectories())
                .flatMap(root -> {
                    try {
                        return Files.walk(root);
                    }
                    catch (IOException x) {
                        LOGGER.error("Error extracting content of file system",x);
                        throw new RuntimeException(x);
                    }
                })
                .filter(filter)
                .collect(Collectors.toList());

        }

    }
}
