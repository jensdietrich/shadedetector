package nz.ac.wgtn.shadedetector.resultreporting;

import nz.ac.wgtn.shadedetector.Artifact;
import nz.ac.wgtn.shadedetector.CloneDetector;
import nz.ac.wgtn.shadedetector.ResultReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reporting based on CSV.
 * @author jens dietrich
 */
public class CSVResultReporter implements ResultReporter {

    public static final String SEP = "\t";
    private static Logger LOGGER = LoggerFactory.getLogger(CSVResultReporter.class);

    public static final String[] COLUMNS = new String[] {
        "original-artifact",
        "cloning-artifact",
        "original-classfile",
        "clones-classfile",
        "similarity score"
    };

    // the destination folder, a separet file will be created for each artifact
    private String dest = null;

    public CSVResultReporter(String dest) {
        this.dest = dest;
    }

    public CSVResultReporter() {
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    @Override
    public String name() {
        return "csv";
    }

    @Override
    public void report(Artifact component, Artifact potentialClone, Set<CloneDetector.CloneRecord> cloneAnalysesResults) throws IOException {
        String header = Stream.of(COLUMNS).collect(Collectors.joining(SEP));
        List<String> rows = new ArrayList<>();
        rows.add(header);

        for (CloneDetector.CloneRecord record:cloneAnalysesResults) {
            String row = "" + component.getId() + SEP + potentialClone.getId() + SEP + record.getOriginal() + SEP + record.getClone() + SEP + record.getConvidence();
            rows.add(row);
        }

        Path folder = Path.of(dest);
        if (Files.notExists(folder)) {
            Files.createDirectories(folder);
        }

        Path report = Paths.get(dest, potentialClone.getId()+".csv");
        LOGGER.info("Reporting to " + report.toFile().getAbsolutePath());
        Files.write(report,rows);
    }

}
