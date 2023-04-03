package nz.ac.wgtn.shadedetector.cveverification;

import com.google.common.base.Preconditions;
import nz.ac.wgtn.shadedetector.GAV;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Clone a project, replace dependencies.
 * Rewrite imports using a translation function of required (in case packages have been renamed).
 * @author jens dietrich
 */
public class MVNProjectCloner {

    private static Logger LOGGER = LoggerFactory.getLogger(MVNProjectCloner.class);

    public static void cloneMvnProject (Path originalProjectFolder, Path clonedProjectFolder, GAV originalDependency, GAV cloneDependency, GAV clonedProjectCoordinates, Function<String,String> importTranslation) throws IOException, JDOMException {

        Preconditions.checkNotNull(originalProjectFolder);
        Preconditions.checkArgument(Files.exists(originalProjectFolder));
        Preconditions.checkArgument(Files.isDirectory(originalProjectFolder));

        Path originalPom = originalProjectFolder.resolve("pom.xml");
        Preconditions.checkArgument(Files.exists(originalPom));

        LOGGER.info("Cloning project {}",originalProjectFolder);
        LOGGER.info("checking original pom {}",originalPom);
        Element originalDependencyElement = POMUtils.findDependency(originalPom,originalDependency); // not used, just to verify that element exists
        LOGGER.info("Original dependency {} to be replaced found",originalDependency.asString());

        // copy folder recursively
        copyMvnProject(originalProjectFolder,clonedProjectFolder);

        Path clonedPom = clonedProjectFolder.resolve("pom.xml");

        // replace dependency and coordinates (some redundant pom parsing, but more readable)
        POMUtils.replaceDependency(clonedPom,originalDependency,cloneDependency);
        POMUtils.replaceCoordinates(clonedPom,clonedProjectCoordinates);

        // rewrite imports
        ASTUtils.updateImports(clonedProjectFolder, importTranslation);

        // todo build maven to confirm tests

    }


    static void copyMvnProject(Path originalProjectFolder, Path clonedProjectFolder) throws IOException {
        if (Files.exists(clonedProjectFolder)) {
            Files.createDirectories(clonedProjectFolder);
        }

        // if cloned folder is not empty, make it empty
        Files.walk(clonedProjectFolder)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);

        Files.walkFileTree(originalProjectFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {

                if (dir.toString().contains("target")) throw new RuntimeException("todo: filter out target, git and IDE meta data");
                Files.createDirectories(clonedProjectFolder.resolve(originalProjectFolder.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                if (file.toString().contains("target")) throw new RuntimeException("todo: filter out target, git and IDE meta data");
                Files.copy(file,clonedProjectFolder.resolve(originalProjectFolder.relativize(file)));
                return FileVisitResult.CONTINUE;
            }
        });

    }

}
