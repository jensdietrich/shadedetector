package nz.ac.wgtn.shadedetector;

import java.io.File;
import java.util.List;

/**
 * Given a list of files containing Java source code (i.e. *.java), return a list of those files
 * to be used to locate potential clones. The list returned should be contained in the list used as a parameter.
 * The order matters as this defines the order in which classes will be used in queries, and in some cases a subset might be used.
 * @author jens dietrich
 */
public interface ClassSelector {
    List<File> selectForSearch(List<File> sourceCodeList);
}
