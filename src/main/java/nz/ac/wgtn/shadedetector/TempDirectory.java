package nz.ac.wgtn.shadedetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * This is necessary because it turns out that File::deleteOnExit() will only delete a directory if it is empty.
 * Obviously, most temporary directories someone might want to create will not wind up empty.
 * This class also gives more control over when the directories are deleted.
 */
public class TempDirectory {
    private static Logger LOGGER = LoggerFactory.getLogger(Cache.class);
    private static Path TEMP_ROOT = Cache.getRoot().toPath();

    private static HashMap<String, ArrayList<Path>> dirsByPrefix = new LinkedHashMap<>();

    public static Path create(String prefix) throws IOException {
        Path tempDir = Files.createTempDirectory(TEMP_ROOT, prefix);
        //TODO: Make dirsByPrefix update threadsafe
        if (!dirsByPrefix.containsKey(prefix)) {
            dirsByPrefix.put(prefix, new ArrayList<>());
        }
        dirsByPrefix.get(prefix).add(tempDir);
        return tempDir;
    }

    /**
     * Recursively delete all files and directories in the given directory.
     * This method may be slow, since it iterates the complete list of all paths with the given prefix.
     * @return true if the dir was successfully deleted, false otherwise
     */
    public static boolean delete(String prefix, Path dir) {
        if (!dirsByPrefix.containsKey(prefix) || !dirsByPrefix.get(prefix).remove(dir)) {
            return false;
        }

        return recursivelyDeleteQuietly(dir.toFile());
    }

    /**
     * Recursively delete all temp directories created with the given prefix.
     * Prefer calling this once to calling delete() on each directory.
     * @return true if all dirs were successfully deleted, false otherwise
     */
    public static boolean deleteAllForPrefix(String prefix) {
        if (!dirsByPrefix.containsKey(prefix)) {
            return true;
        }

        boolean success = true;
        for (Path p : dirsByPrefix.get(prefix)) {
            success = recursivelyDeleteQuietly(p.toFile()) && success;
        }

        dirsByPrefix.remove(prefix);
        return success;
    }

    /**
     * Recursively delete all temp directories created with any prefix.
     * @return true if all dirs were successfully deleted, false otherwise
     */
    public static boolean deleteAll() {
        boolean success = true;
        for (String prefix : dirsByPrefix.keySet()) {
            success = deleteAllForPrefix(prefix) && success;
        }

        dirsByPrefix.clear();
        return success;
    }

    private static boolean recursivelyDeleteQuietly(File dirOrFile) {
        try {
            recursivelyDelete(dirOrFile);
        } catch (IOException e) {
            LOGGER.error("Error recursively deleting {}: ", dirOrFile, e);
            return false;
        }

        return true;
    }

    // From https://stackoverflow.com/a/779529/47984
    private static void recursivelyDelete(File dirOrFile) throws FileNotFoundException {
        if (dirOrFile.isDirectory()) {
            for (File c : dirOrFile.listFiles()) {
                recursivelyDelete(c);
            }
        }

        if (!dirOrFile.delete()) {
            throw new FileNotFoundException("Failed to delete file: " + dirOrFile);
        }
    }

    // Ensure that all temp dirs are deleted before the program exits. Based on java.io.DeleteOnExitHook.
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(TempDirectory::deleteAll));
    }
}
