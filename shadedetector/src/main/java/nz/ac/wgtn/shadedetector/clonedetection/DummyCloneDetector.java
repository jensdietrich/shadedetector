package nz.ac.wgtn.shadedetector.clonedetection;

import nz.ac.wgtn.shadedetector.CloneDetector;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;


/**
 * Dummy implementation mainly for testing until "real" implementations become available.
 * @author jens dietrich
 */
public class DummyCloneDetector implements CloneDetector {

    @Override
    public Set<CloneRecord> detect(Path original, Path cloneCandidate) {
        return Collections.EMPTY_SET;
    }

    @Override
    public String name() {
        return "dummy";
    }
}
