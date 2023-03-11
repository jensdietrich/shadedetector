package nz.ac.wgtn.shadedetector.classselectors;

import com.google.common.base.Preconditions;
import nz.ac.wgtn.shadedetector.ClassSelector;
import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Explicitly specify classes (names) to look for.
 * The main use case is to identify classes necessary to exploit a given vulnerability.
 * Often, this can be found be a simple analysis of github projects created to demonstrate
 * vulnerabilities, such as https://github.com/frohoff/ysoserial.
 * There is a verification step that checks that classes with such names actually exist.
 * If not, an IllegalStateException will be thrown.
 * @author jens dietrich
 */
public class SelectClassesFromList implements ClassSelector  {

    private List<String> classList = null;

    public SelectClassesFromList(List<String> classList) {
        this.classList = classList;
    }

    public SelectClassesFromList() {}

    @Override
    public String name() {
        return "list";
    }

    @Override
    public List<String> selectForSearch(List<File> sourceCodeList) {
        Set<String> classNames = getNamesAsSet(sourceCodeList);
        for (String name:classList) {
            Preconditions.checkState(classNames.contains(name),"No class found in sourceCodeList named \"" + name + "\"");
        }
        return classList;
    }
}
