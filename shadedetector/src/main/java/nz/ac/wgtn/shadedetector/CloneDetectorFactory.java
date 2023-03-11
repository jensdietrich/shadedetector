package nz.ac.wgtn.shadedetector;

import nz.ac.wgtn.shadedetector.clonedetection.DummyCloneDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instantiates and sets up a CloneDetector from a configuration string.
 * @author jens dietrich
 */
public class CloneDetectorFactory extends  AbstractServiceLoaderFactory<CloneDetector> {

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(CloneDetectorFactory.class);
    }

    @Override
    public CloneDetector getDefault() {
        return new DummyCloneDetector();
    }

    @Override
    public CloneDetector create(String configuration) {
        return create(CloneDetector.class,"clone detector",configuration);
    }
}
