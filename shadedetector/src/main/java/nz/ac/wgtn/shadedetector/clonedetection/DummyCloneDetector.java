package nz.ac.wgtn.shadedetector.clonedetection;

import nz.ac.wgtn.shadedetector.CloneDetector;
import java.io.File;

/**
 * Dummy implementation mainly for testing until "real" implementations become available.
 * @author jens dietrich
 */
public class DummyCloneDetector implements CloneDetector {

    @Override
    public double getSimilarityScore(File src1, File src2) {
        return src1.getName().equals(src2.getName()) ? 1 : 0;
    }

    @Override
    public String name() {
        return "dummy";
    }
}
