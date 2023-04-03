package nz.ac.wgtn.shadedetector.cveverification;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility to query and manipulate parsed Java code.
 * @author jens dietrich
 */

public class ASTUtils {

    private static Logger LOGGER = LoggerFactory.getLogger(POMProjectCloner.class);

    static void updateImports(Path projectFolder, Function<String,String> importTranslation) throws IOException {

        List<Path> sources = Files.walk(projectFolder)
            .filter(file -> !Files.isDirectory(file))
            .filter(file -> file.toFile().getName().endsWith(".java"))
            .collect(Collectors.toList());


        for (Path src:sources) {
            boolean importsHaveChanged = false;
            CompilationUnit cu = StaticJavaParser.parse(src);
            NodeList imports = cu.getImports();
            for (int i=0;i<imports.size();i++) {
                ImportDeclaration imprt = (ImportDeclaration)imports.get(i);
                String val = imprt.getNameAsString();
                String newVal = importTranslation.apply(val);
                if (newVal!=null && !val.equals(newVal)) {
                    imprt.setName(newVal);
                    importsHaveChanged = true;
                }
            }

            if (importsHaveChanged) {
                LOGGER.info("writing java sources with updated imports");
                Files.writeString(src, cu.toString());
            }
        }


    }
}
