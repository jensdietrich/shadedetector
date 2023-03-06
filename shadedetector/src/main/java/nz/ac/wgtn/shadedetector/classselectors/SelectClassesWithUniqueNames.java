package nz.ac.wgtn.shadedetector.classselectors;

import nz.ac.wgtn.shadedetector.ClassSelector;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Select all classes (sources) from a given list.
 * Pick classes a high number of camel case tokens (prefer DoSomeThingSpecial over DoSomething).
 * @author jens dietrich
 */
public class SelectClassesWithUniqueNames implements ClassSelector {

    @Override
    public List<File> selectForSearch(List<File> sourceCodeList) {
        final Map<File,Integer> ranks = new HashMap<>();
        sourceCodeList.stream().forEach(
            f -> {
                ranks.put(f,tokenizeCamelCase(f.getName()).length);
            }
        );
        return sourceCodeList.stream()
            .sorted(Comparator.comparingInt(ranks::get).reversed())
            .collect(Collectors.toList());
    }

    static String[] tokenizeCamelCase (String input) {
        return input.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    }

}
