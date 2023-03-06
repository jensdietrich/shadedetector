package nz.ac.wgtn.shadedetector.classselectors;

import nz.ac.wgtn.shadedetector.ClassSelector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Select all classes (sources) from a given list.
 * @author jens dietrich
 */
public class SelectAll implements ClassSelector {
    @Override
    public List<String> selectForSearch(List<File> sourceCodeList) {
        return getNamesAsList(sourceCodeList);
    }
}
