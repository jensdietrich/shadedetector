package nz.ac.wgtn.shadedetector.resultreporting;

import com.google.common.base.Preconditions;
import nz.ac.wgtn.shadedetector.Artifact;
import nz.ac.wgtn.shadedetector.CloneDetector;
import nz.ac.wgtn.shadedetector.ResultReporter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Interface to combine various reporters.
 * Not itself registered as a service, but can be used to combine services.
 * @author jens dietrich
 */
public class CombinedResultReporter implements ResultReporter  {

    private ResultReporter[] delegates = null;

    public CombinedResultReporter(ResultReporter[] delegates) {
        Preconditions.checkArgument(delegates.length>0);
        this.delegates = delegates;
    }

    public CombinedResultReporter(List<ResultReporter> delegatesList) {
        this(delegatesList.toArray(new ResultReporter[delegatesList.size()]));
    }

    @Override
    public String name() {
        return "delegated";
    }

    @Override
    public void report(Artifact component, Artifact potentialClone, Set<CloneDetector.CloneRecord> cloneAnalysesResults) throws IOException {
        for (ResultReporter reporter:delegates) {
            reporter.report(component,potentialClone,cloneAnalysesResults);
        }
    }
}
