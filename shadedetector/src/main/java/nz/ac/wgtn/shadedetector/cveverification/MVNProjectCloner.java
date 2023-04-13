package nz.ac.wgtn.shadedetector.cveverification;

import com.google.common.base.Preconditions;
import nz.ac.wgtn.shadedetector.GAV;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessResult;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Clone a project, replace dependencies.
 * Rewrite imports using a translation function of required (in case packages have been renamed).
 * @author jens dietrich
 */
public class MVNProjectCloner {

    private static Logger LOGGER = LoggerFactory.getLogger(MVNProjectCloner.class);


    // log files created
    public static final String COMPILATION_LOG = ".mvn-compile.log";
    public static final String TEST_LOG = ".mvn-test.log";

    // status code
    public static final int CLONED = 0;
    public static final int COMPILED = 1;
    public static final int TESTED = 2;

    public static class CloneResult {

        private Set<Integer> status = new HashSet<>();
        private String[] logs = new String[3];
        private boolean renamedImports = false;

        public boolean isRenamedImports() {
            return renamedImports;
        }

        public void setRenamedImports(boolean renamedImports) {
            this.renamedImports = renamedImports;
        }

        private void checkStatus(int s) {
            Preconditions.checkArgument(s == CLONED || s == COMPILED || s==TESTED);
        }

        void failed(int s, String log) {
            checkStatus(s);
            logs[s] = log;
        }

        void success(int s) {
            checkStatus(s);
            status.add(s);
        }

        public boolean isCloned() {
            return status.contains(CLONED);
        }

        public boolean isCompiled() {
            return status.contains(COMPILED);
        }

        public boolean isTested() {
            return status.contains(TESTED);
        }

    }

    public static CloneResult cloneMvnProject (Path originalProjectFolder, Path clonedProjectFolder, GAV originalDependency, GAV cloneDependency, GAV clonedProjectCoordinates, Map<String,String> importTranslation) throws IOException, JDOMException {

        CloneResult result = new CloneResult();

        Preconditions.checkNotNull(originalProjectFolder);
        Preconditions.checkArgument(Files.exists(originalProjectFolder));
        Preconditions.checkArgument(Files.isDirectory(originalProjectFolder));

        Path originalPom = originalProjectFolder.resolve("pom.xml");
        Preconditions.checkArgument(Files.exists(originalPom));

        LOGGER.info("Cloning project {}",originalProjectFolder);
        LOGGER.info("checking original pom {}",originalPom);
        // Element originalDependencyElement = POMUtils.findDependency(originalPom,originalDependency); // not used, just to verify that element exists
        LOGGER.info("Original dependency {} to be replaced found",originalDependency.asString());

        // copy folder recursively

        try {
            copyMvnProject(originalProjectFolder, clonedProjectFolder);

            Path clonedPom = clonedProjectFolder.resolve("pom.xml");

            // replace dependency and coordinates (some redundant pom parsing, but more readable)
            POMUtils.replaceDependency(clonedPom, originalDependency, cloneDependency);
            POMUtils.replaceCoordinates(clonedPom, clonedProjectCoordinates);

            // rewrite imports
            boolean importsHaveChanged = ASTUtils.updateImports(clonedProjectFolder, importTranslation);

            result.setRenamedImports(importsHaveChanged);
            result.success(CLONED);

        }
        catch (IOException x) {
            LOGGER.error("error cloning " + clonedProjectFolder,x);
            result.failed(CLONED,printStacktrace(x));
        }

        if (!result.isCloned()) return result;

        Path logFile = clonedProjectFolder.resolve(COMPILATION_LOG);
        try {
            ProcessResult pr = MVNExe.mvnCleanCompile(clonedProjectFolder);
            if (pr.getExitValue()==0) {
                result.success(COMPILED);
            }
            else {
                String out = pr.outputUTF8();
                result.failed(COMPILED,out);
                Files.write(logFile,List.of(out));
            }
        }
        catch (Exception x) {
            LOGGER.error("error compiling " + clonedProjectFolder,x);
            String stacktrace = printStacktrace(x);
            result.failed(COMPILED,stacktrace);
            Files.write(logFile,List.of(stacktrace));
        }

        if (!result.isCompiled()) return result;

        logFile = clonedProjectFolder.resolve(TEST_LOG);
        try {
            ProcessResult pr = MVNExe.mvnTest(clonedProjectFolder);
            if (pr.getExitValue()==0) {
                result.success(TESTED);
            }
            else {
                String out = pr.outputUTF8();
                result.failed(TESTED,out);
                Files.write(logFile,List.of(out));
            }
        }
        catch (Exception x) {
            LOGGER.error("error testing " + clonedProjectFolder,x);
            String stacktrace = printStacktrace(x);
            result.failed(TESTED,stacktrace);
            Files.write(logFile,List.of(stacktrace));
        }

        return result;

    }

    static Set<Path> getMvnFoldersNotToCopyOrMove(Path folder) {
        return Set.of(
            folder.resolve("target"),
            folder.resolve(".idea"),
            folder.resolve("scan-results"),
            folder.resolve(".git")
        );
    }

    public static void moveMvnProject(Path originalProjectFolder, Path clonedProjectFolder) throws IOException {
        copyMvnProject(originalProjectFolder,clonedProjectFolder);
        Files.walk(originalProjectFolder)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    public static void copyMvnProject(Path originalProjectFolder, Path clonedProjectFolder) throws IOException {

//        Files.delete(clonedProjectFolder);
        if (!Files.exists(clonedProjectFolder)) {
            Files.createDirectories(clonedProjectFolder);
        }

//        // if cloned folder is not empty, make it empty
//        Files.walk(clonedProjectFolder)
//            .sorted(Comparator.reverseOrder())
//            .map(Path::toFile)
//            .forEach(File::delete);

        Files.walkFileTree(originalProjectFolder, new SimpleFileVisitor<Path>() {

            Set<Path> skipCopyFolders = getMvnFoldersNotToCopyOrMove(originalProjectFolder);

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                if (skipCopyFolders.contains(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                else {
                    Files.createDirectories(clonedProjectFolder.resolve(originalProjectFolder.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                if (Files.isRegularFile(file) && !Files.isHidden(file)) {
                    Files.copy(file, clonedProjectFolder.resolve(originalProjectFolder.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String printStacktrace(Throwable x) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        x.printStackTrace(pw);
        return sw.toString();
    }

}
