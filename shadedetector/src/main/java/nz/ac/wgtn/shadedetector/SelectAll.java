package nz.ac.wgtn.shadedetector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Select all classes (sources) from a given list.
 * @author jens dietrich
 */
public class SelectAll implements ClassSelector {
    @Override
    public List<File> selectForSearch(List<File> sourceCodeList) {
        return new ArrayList<>(sourceCodeList);
    }
}
