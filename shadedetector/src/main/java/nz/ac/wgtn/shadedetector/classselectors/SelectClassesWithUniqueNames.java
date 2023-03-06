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
    public List<String> selectForSearch(List<File> sourceCodeList) {
        final Map<String,Integer> ranks = new HashMap<>();
        List<String> names = getNamesAsList(sourceCodeList);
        names.stream().forEach(
            name -> {
                ranks.put(name,tokenizeCamelCase(name).length);
            }
        );
        return names.stream()
            .sorted(Comparator.comparingInt(ranks::get).reversed())
            .collect(Collectors.toList());
    }

    static String[] tokenizeCamelCase (String input) {
        return input.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    }

}
