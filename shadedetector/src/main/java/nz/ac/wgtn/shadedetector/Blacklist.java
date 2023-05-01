package nz.ac.wgtn.shadedetector;

import java.util.Set;

/**
 * List of artifacts (GAVSs) for which the analysis is problematic.
 * TODO: move into file.
 * @author jens dietrich
 */
public class Blacklist {
    ;

    static boolean contains (Artifact artifact) {
        // outofmemory when extracting from jar -- check for zip bomb or similar
        return artifact.getGroupId().equals("dev.dejvokep");
    }
}
