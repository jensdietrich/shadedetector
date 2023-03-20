package nz.ac.wgtn.shadedetector;

import nz.ac.wgtn.shadedetector.resultreporting.CombinedResultReporter;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CLI main class.
 * @author jens dietrich
 */
public class Main {

    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static ClassSelectorFactory CLASS_SELECTOR_FACTORY = new ClassSelectorFactory();
    private static CloneDetectorFactory CLONE_DETECTOR_FACTORY = new CloneDetectorFactory();
    private static ArtifactSearchResultConsolidationStrategyFactory CONSOLIDATION_STRATEGY_FACTORY = new ArtifactSearchResultConsolidationStrategyFactory();
    private static ResultReporterFactory RESULT_REPORTER_FACTORY = new ResultReporterFactory();

    public static void main (String[] args) throws ParseException {
        Options options = new Options();
        options.addRequiredOption("g", "group",true, "the Maven group id of the artifact queried for clones");
        options.addRequiredOption("a", "artifact",true, "the Maven artifact id of the artifact queried for clones");
        // @TODO - in the future, we could generalise this to look for version ranges , allow wildcards etc
        options.addRequiredOption("v", "version",true, "the Maven version of the artifact queried for clones");

        // we need a little language here to pass parameters, such as list:class1,class2
        // needs default
        options.addOption("s", "classselector",true, "the strategy used to select classes (optional, default is\"" + CLASS_SELECTOR_FACTORY.getDefault().name() + "\")");
        options.addOption("o", "output",true, "the component used to process and report results (optional, default is \"" + RESULT_REPORTER_FACTORY.getDefault().name() + "\")");
        options.addOption("o1", "output1",true, "an additional component used to process and report results");
        options.addOption("o2", "output2",true, "an additional component used to process and report results");
        options.addOption("o3", "output3",true, "an additional component used to process and report results");
        options.addOption("c","clonedetector",true,"the clone detector to be used (optional, default is \"" + CLONE_DETECTOR_FACTORY.getDefault().name() + "\")");
        options.addOption("r","resultconsolidation",true,"the query result consolidation strategy to be used (optional, default is \"" + CONSOLIDATION_STRATEGY_FACTORY.getDefault().name() + "\")");

        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        }
        catch (MissingOptionException x) {
            LOGGER.error(x.getMessage(),x);
            printHelp(options);
        }

        String groupId = cmd.getOptionValue("group");
        String artifactId = cmd.getOptionValue("artifact");
        String version = cmd.getOptionValue("version");
        GAV gav = new GAV(groupId,artifactId,version);

        CloneDetector cloneDetector = instantiateOptional(CLONE_DETECTOR_FACTORY,cmd,"clone detector","clonedetector");
        ClassSelector classSelector = instantiateOptional(CLASS_SELECTOR_FACTORY,cmd,"class selector","classselector");
        ArtifactSearchResultConsolidationStrategy resultConsolidationStrategy = instantiateOptional(CONSOLIDATION_STRATEGY_FACTORY,cmd,"result consolidation strategy","resultconsolidation");
        ResultReporter firstResultReporter = instantiateOptional(RESULT_REPORTER_FACTORY,cmd,"result reporter","output");

        List<ResultReporter> resultReporters = new ArrayList<>();
        resultReporters.add(firstResultReporter);
        if (cmd.hasOption("output1")) {
            resultReporters.add(instantiateOptional(RESULT_REPORTER_FACTORY,cmd,"result reporter","output1"));
        }
        if (cmd.hasOption("output2")) {
            resultReporters.add(instantiateOptional(RESULT_REPORTER_FACTORY,cmd,"result reporter","output2"));
        }
        if (cmd.hasOption("output3")) {
            resultReporters.add(instantiateOptional(RESULT_REPORTER_FACTORY,cmd,"result reporter","output3"));
        }
        ResultReporter resultReporter = resultReporters.size()==1 ?
            resultReporters.get(0) :
            new CombinedResultReporter(resultReporters);

        // find artifact
        List<Artifact> allVersions = null;
        Artifact artifact = null;
        try {
            // note: fetching artifacts for all versions could be postponed
            ArtifactSearchResponse response = ArtifactSearch.findVersions(groupId,artifactId,1,ArtifactSearch.ROWS_PER_BATCH);
            allVersions = response.getBody().getArtifacts();
            artifact = allVersions.stream()
                .filter(a -> a.getVersion().equals(version))
                .findFirst().orElse(null);

        } catch (ArtifactSearchException e) {
            LOGGER.error("cannot fetch artifacts for "+groupId+":"+artifactId,e);
        }
        if (allVersions==null || allVersions.size()==0 || artifact==null) {
            LOGGER.error("cannot locate artifacts for {}:{}",groupId,artifactId);
            System.exit(1);
        }

        // find sources
        Path originalSources = null;
        try {
            originalSources = FetchResources.fetchSources(artifact);
        } catch (IOException e) {
            LOGGER.error("cannot fetch sources for " + gav.asString(),e);
        }
        if (originalSources==null) {
            LOGGER.error("cannot fecth sources for {}",artifact.toString());
            System.exit(1);
        }

        // find all potentially matching artifacts
        Map<String,ArtifactSearchResponse> matches = null;
        try {
            matches = ArtifactSearch.findShadingArtifacts(originalSources,classSelector,10, ArtifactSearch.BATCHES,ArtifactSearch.ROWS_PER_BATCH);
        }
        catch (Exception e) {
            LOGGER.error("cannot fetch artifacts with matching classes from {}",gav,e);
        }

        // consolidate results
        List<Artifact> consolidatedMatches = resultConsolidationStrategy.consolidate(matches);

        // run clone detection

        AtomicInteger countMatchesAnalysed = new AtomicInteger();
        AtomicInteger countMatchesAnalysedFailed = new AtomicInteger();

        try {
            resultReporter.startReporting(artifact,originalSources);
        }
        catch (IOException x) {
            LOGGER.error("error initialising result reporting",x);
        }

        for (Artifact match:consolidatedMatches) {
            countMatchesAnalysed.incrementAndGet();
            LOGGER.info("analysing whether artifact {} matches",match.getId());
            try {
                Path src = FetchResources.fetchSources(match);
                Set<CloneDetector.CloneRecord> cloneAnalysesResults = cloneDetector.detect(originalSources,src);

                // @TODO plugin arbitrary result reporters
                LOGGER.info("Reporting results for " + match.getId());
                resultReporter.report(artifact,match,Utils.listJavaSources(src,true),cloneAnalysesResults);

            } catch (Exception e) {
                LOGGER.error("cannot fetch sources for artifact {}",match.toString(),e);
                countMatchesAnalysedFailed.incrementAndGet();
            }
        }

        try {
            resultReporter.endReporting(artifact);
        }
        catch (IOException x) {
            LOGGER.error("error finishing result reporting",x);
        }


    }

    private static <T extends NamedService> T instantiateOptional(AbstractServiceLoaderFactory<T> factory, CommandLine cmd, String description, String key) {
        T service = cmd.hasOption(key)
            ? factory.create(cmd.getOptionValue(key))
            : factory.getDefault();
        assert service!=null;
        LOGGER.info("using {}: {}",description,service.name());
        return service;
    }

    private static void printHelp(Options options) {
        String header = "Arguments:\n\n";
        String footer = "\nPlease report issues at https://github.com/jensdietrich/shading-study/issues/";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -cp <classpath> Main", header, options, footer, true);
    }
}
