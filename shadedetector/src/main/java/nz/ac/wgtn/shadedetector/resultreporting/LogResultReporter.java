package nz.ac.wgtn.shadedetector.resultreporting;

import nz.ac.wgtn.shadedetector.Artifact;
import nz.ac.wgtn.shadedetector.CloneDetector;
import nz.ac.wgtn.shadedetector.ResultReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Set;

/**
 * Simple reporting using a logger.
 * @author jens dietrich
 */
public class LogResultReporter implements ResultReporter {

    private static Logger LOGGER = LoggerFactory.getLogger(LogResultReporter.class);

    public LogResultReporter() {
    }

    @Override
    public String name() {
        return "log";
    }

    @Override
    public void report(Artifact component, Artifact potentialClone, Set<CloneDetector.CloneRecord> cloneAnalysesResults) throws IOException {
        for (CloneDetector.CloneRecord record:cloneAnalysesResults) {
            LOGGER.info("Potential clone");
            LOGGER.info("\tcomponent: {}", component.getId());
            LOGGER.info("\tcloning-component: {}", potentialClone.getId());
            LOGGER.info("\toriginal-class: {}", record.getOriginal().toString());
            LOGGER.info("\tcloned-class: {}", record.getClone().toString());
            LOGGER.info("\tconfidence: {}", record.getConvidence());
        }
    }

}
