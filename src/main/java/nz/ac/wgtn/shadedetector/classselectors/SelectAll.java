package nz.ac.wgtn.shadedetector.classselectors;

import nz.ac.wgtn.shadedetector.ClassSelector;
import java.nio.file.Path;
import java.util.List;
import static nz.ac.wgtn.shadedetector.Utils.getUnqualifiedJavaClassNames;

/**
 * Select all classes (sources) from a given list.
 * @author jens dietrich
 */
public class SelectAll implements ClassSelector {

    @Override
    public String name() {
        return "all";
    }

    @Override
    public List<String> selectForSearch(Path folderOrZipContainingSources) {
        return  getUnqualifiedJavaClassNames(folderOrZipContainingSources);
    }
}
