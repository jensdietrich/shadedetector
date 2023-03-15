package nz.ac.wgtn.shadedetector.classselectors;

import nz.ac.wgtn.shadedetector.ClassSelector;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static nz.ac.wgtn.shadedetector.Utils.getUnqualifiedJavaClassNames;

/**
 * Select all classes (sources) from a given list.
 * Pick classes a high number of camel case tokens (prefer DoSomeThingSpecial over DoSomething).
 * @author jens dietrich
 */
public class SelectClassesWithComplexNames implements ClassSelector {

    private int maxSize = 100;

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public String name() {
        return "complexnames";
    }

    @Override
    public List<String> selectForSearch(Path folderOrZipContainingSources) {
        List<String> classNames = getUnqualifiedJavaClassNames(folderOrZipContainingSources);
        final Map<String,Integer> ranks = new HashMap<>();
        classNames.stream().forEach(
            name -> {
                ranks.put(name,tokenizeCamelCase(name).length);
            }
        );
        return classNames.stream()
            .sorted(Comparator.comparingInt(ranks::get).reversed())
            .limit(maxSize)
            .collect(Collectors.toList());
    }

    static String[] tokenizeCamelCase (String input) {
        return input.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    }

}
