package nz.ac.wgtn.shadedetector;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Interface describing how analysis results are being consumed / reported.
 * @author jens dietrich
 */
public interface ResultReporter extends NamedService {
    void report (Artifact component, Artifact potentialClone, Set<CloneDetector.CloneRecord> cloneAnalysesResults) throws IOException;
}
