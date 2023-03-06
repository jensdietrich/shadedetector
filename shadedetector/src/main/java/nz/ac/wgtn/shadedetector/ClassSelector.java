package nz.ac.wgtn.shadedetector;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Given a list of files containing Java source code (i.e. *.java), return a list of those files
 * to be used to locate potential clones.
 * Then list return should contain unqualified class names (i.e. package names and .java extensions removed).
 * The order matters as this defines the order in which classes will be used in queries, and in some cases a subset might be used.
 * @author jens dietrich
 */
public interface ClassSelector {
    List<String> selectForSearch(List<File> sourceCodeList);


    default Set<String> getNamesAsSet (List<File> sourceCodeList) {
        return sourceCodeList.stream()
            .filter(f -> f.getName().endsWith(".java"))
            .map(f -> f.getName())
            .map(n -> n.replace(".java", ""))
            .collect(Collectors.toSet());
    }

    default List<String> getNamesAsList (List<File> sourceCodeList) {
        return sourceCodeList.stream()
            .filter(f -> f.getName().endsWith(".java"))
            .map(f -> f.getName())
            .map(n -> n.replace(".java", ""))
            .collect(Collectors.toList());
    }
}
