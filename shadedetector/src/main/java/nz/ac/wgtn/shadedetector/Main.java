package nz.ac.wgtn.shadedetector;

import com.google.common.base.Preconditions;
import nz.ac.wgtn.shadedetector.clonedetection.ImportTranslationExtractor;
import nz.ac.wgtn.shadedetector.cveverification.MVNExe;
import nz.ac.wgtn.shadedetector.cveverification.MVNProjectCloner;
import nz.ac.wgtn.shadedetector.cveverification.POMUtils;
import nz.ac.wgtn.shadedetector.resultreporting.CombinedResultReporter;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessResult;
import java.io.IOException;
import java.nio.file.Files;
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

    private static final String DEFAULT_GENERATED_VERIFICATION_PROJECT_GROUP_NAME = "foo";
    private static final String DEFAULT_GENERATED_VERIFICATION_PROJECT_VERSION = "0.0.1";

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

        options.addOption("vul","vulnerabilitydemo",true,"a folder containing a Maven project that verifies a vulnerability in the original library with test(s), and can be used as a template to verify the presence of the vulnerability in a clone");
        options.addOption("vos","vulnerabilityoutput_staging",true,"the root folder where for each clone, a project verifying the presence of a vulnerability is created");
        options.addOption("vov","vulnerabilityoutput_final",true,"the root folder where for each clone, a project created in the staging folder will be moved to if verification succeeds (i.e. if the vulnerability is shown to be present)");
        options.addOption("vg","vulnerabilitygroup",true,"the group name used in the projects generated to verify the presence of a vulnerability (default is \"" + DEFAULT_GENERATED_VERIFICATION_PROJECT_GROUP_NAME + "\")");
        options.addOption("vv","vulnerabilityversion",true,"the version used in the projects generated to verify the presence of a vulnerability (default is \"" + DEFAULT_GENERATED_VERIFICATION_PROJECT_VERSION + "\")");

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
            LOGGER.error("no sources available for sources for {}",artifact.getId());
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
        LOGGER.info("{} potential matches found",matches.size());
        List<Artifact> consolidatedMatches = resultConsolidationStrategy.consolidate(matches);
        LOGGER.info("matched consolidated to {}",consolidatedMatches.size());

        // eliminate matches with dependency -- those are likely to be detected by existing checkers
        List<Artifact> candidates = new ArrayList<>();

        AtomicInteger matchesWithDependency = new AtomicInteger();
        AtomicInteger matchesWithoutDependency = new AtomicInteger();
        AtomicInteger matchesWhereDependencyAnalysisFailed  = new AtomicInteger();
        for (Artifact match:consolidatedMatches) {
            try {
                if (POMAnalysis.references(match,artifact.getGroupId(),artifact.getArtifactId())) {
                    matchesWithDependency.incrementAndGet();
                }
                else {
                    matchesWithoutDependency.incrementAndGet();
                    candidates.add(match);
                }
            } catch (Exception e) {
                matchesWhereDependencyAnalysisFailed.incrementAndGet();
                LOGGER.info("Error fetching or analysing pom for {}",match.getId(),e);
            }
        }
        LOGGER.info("{} potential matches have declared dependency on {}:{}, will be excluded from further analysis",matchesWithDependency.get(),artifact.getGroupId(),artifact.getArtifactId());
        LOGGER.info("{} potential matches detected without declared dependency on {}:{}, will be analysed for clones",matchesWithoutDependency.get(),artifact.getGroupId(),artifact.getArtifactId());
        LOGGER.info("dependency analysis failed for {} artifacts",matchesWhereDependencyAnalysisFailed.get());


        // run clone detection
        AtomicInteger countMatchesAnalysed = new AtomicInteger();
        AtomicInteger countMatchesAnalysedFailed = new AtomicInteger();

        try {
            resultReporter.startReporting(artifact,originalSources);
        }
        catch (IOException x) {
            LOGGER.error("error initialising result reporting",x);
        }

        // see whether vulnerability verification is available
        Path verificationProjectTemplateFolder = null;
        boolean isValidVerificationProjectTemplate = false;
        if (cmd.hasOption("vulnerabilitydemo")) {
            verificationProjectTemplateFolder = Path.of(cmd.getOptionValue("vulnerabilitydemo"));
            try {
                checkVerificationProject(verificationProjectTemplateFolder);
                isValidVerificationProjectTemplate = true;
                LOGGER.info("vulnerability verification project is not valid");
            }
            catch (Exception x) {
                LOGGER.error("vulnerability verification project is valid");
            }
        }
        Path verificationProjectInstancesFolderStaging = null;
        if (cmd.hasOption("vulnerabilityoutput_staging")) {
            verificationProjectInstancesFolderStaging = Path.of(cmd.getOptionValue("vulnerabilityoutput_staging"));
            if (!Files.exists(verificationProjectInstancesFolderStaging)) {
                try {
                    Files.createDirectories(verificationProjectInstancesFolderStaging);
                } catch (IOException e) {
                    throw new RuntimeException("cannot create folder " + verificationProjectInstancesFolderStaging,e);
                }
            }
        }
        LOGGER.info("verification projects will be created in {}",verificationProjectInstancesFolderStaging);
        assert verificationProjectInstancesFolderStaging!=null;

        Path verificationProjectInstancesFolderFinal = null;
        if (cmd.hasOption("vulnerabilityoutput_final")) {
            verificationProjectInstancesFolderFinal = Path.of(cmd.getOptionValue("vulnerabilityoutput_final"));
            if (!Files.exists(verificationProjectInstancesFolderFinal)) {
                try {
                    Files.createDirectories(verificationProjectInstancesFolderFinal);
                } catch (IOException e) {
                    throw new RuntimeException("cannot create folder " + verificationProjectInstancesFolderFinal,e);
                }
            }
        }
        LOGGER.info("verified projects will be moved from {} to {}",verificationProjectInstancesFolderStaging,verificationProjectInstancesFolderFinal);
        assert verificationProjectInstancesFolderFinal!=null;


        String verificationProjectGroupName = DEFAULT_GENERATED_VERIFICATION_PROJECT_GROUP_NAME;
        if (cmd.hasOption("vulnerabilitygroup")) {
            verificationProjectGroupName = cmd.getOptionValue("vulnerabilitygroup");
        }
        LOGGER.info("verification projects will be use group name {}",verificationProjectGroupName);

        String verificationProjectVersion= DEFAULT_GENERATED_VERIFICATION_PROJECT_VERSION;
        if (cmd.hasOption("vulnerabilityversion")) {
            verificationProjectVersion = cmd.getOptionValue("vulnerabilityversion");
        }
        LOGGER.info("verification projects will be use version {}",verificationProjectVersion);

        // filter matches -- only

        for (Artifact match:candidates) {
            countMatchesAnalysed.incrementAndGet();
            LOGGER.info("analysing whether artifact {} matches",match.getId());
            ResultReporter.VerificationState state = ResultReporter.VerificationState.NONE;
            Set<CloneDetector.CloneRecord> cloneAnalysesResults = Set.of();
            List<Path> sources = List.of();
            boolean packagesHaveChangedInClone = false; // indicates shading with packages being renamed
            try {
                Path src = FetchResources.fetchSources(match);
                sources = Utils.listJavaSources(src,true);
                cloneAnalysesResults = cloneDetector.detect(originalSources,src);


                // @TODO plugin arbitrary result reporters
                LOGGER.info("Reporting results for " + match.getId());


                // TODO abstract threshold
                if (cloneAnalysesResults.size()>10) {
                    LOGGER.info("generating project to verifify vulnerability for " + match);
                    String verificationProjectArtifactName = match.toString().replace(":","__");
                    LOGGER.info("\tgroupId: " + verificationProjectGroupName);
                    LOGGER.info("\tartifactId: " + verificationProjectArtifactName);
                    LOGGER.info("\tversion: " + verificationProjectVersion);
                    Path verificationProjectFolderStaged = verificationProjectInstancesFolderStaging.resolve(verificationProjectArtifactName);
                    LOGGER.info("\tproject folder: " + verificationProjectFolderStaged);

                    Map importTranslations = ImportTranslationExtractor.computeImportTranslations(originalSources,src,cloneAnalysesResults);

                    MVNProjectCloner.CloneResult result = MVNProjectCloner.cloneMvnProject(
                        verificationProjectTemplateFolder,
                        verificationProjectFolderStaged,
                        gav,
                        match.asGAV(),
                        new GAV(verificationProjectGroupName,verificationProjectArtifactName,verificationProjectVersion),
                        importTranslations
                    );
                    packagesHaveChangedInClone = true;

                    if (result.isCompiled()) {
                        state = ResultReporter.VerificationState.COMPILED;
                    }
                    if (result.isTested()) { // override
                        state = ResultReporter.VerificationState.TESTED;
                    }

                    if (result.isTested()) {
                        Path verificationProjectFolderFinal = verificationProjectInstancesFolderFinal.resolve(verificationProjectArtifactName);
                        LOGGER.info("\tmoving verified project folder from {} to {}",verificationProjectFolderStaged,verificationProjectFolderFinal);
                        MVNProjectCloner.moveMvnProject(verificationProjectFolderStaged,verificationProjectFolderFinal);
                    }
                }

            } catch (Exception e) {
                LOGGER.error("cannot fetch sources for artifact {}",match.toString(),e);
                countMatchesAnalysedFailed.incrementAndGet();
            }
            finally {
                try {
                    resultReporter.report(artifact,match,sources,cloneAnalysesResults,state,packagesHaveChangedInClone);
                } catch (IOException e) {
                    LOGGER.error("error reporting",e);
                }
            }
        }

        try {
            resultReporter.endReporting(artifact);
        }
        catch (IOException x) {
            LOGGER.error("error finishing result reporting",x);
        }


    }

    // check the vulnerability verification project (template)
    private static void checkVerificationProject(Path verificationProjectTemplateFolder) throws Exception {
        Preconditions.checkArgument(Files.exists(verificationProjectTemplateFolder),"project folder missing: " + verificationProjectTemplateFolder);
        Preconditions.checkArgument(Files.isDirectory(verificationProjectTemplateFolder),"folder expected here: " + verificationProjectTemplateFolder);
        Path pom = verificationProjectTemplateFolder.resolve("pom.xml");
        Preconditions.checkArgument(Files.exists(pom),"not a Maven project (pom.xml missing): " + verificationProjectTemplateFolder);

        try {
            POMUtils.parsePOM(pom);
        }
        catch (Exception x) {
            throw new RuntimeException("Not a valid pom: " + pom,x);
        }

        ProcessResult buildResult = null;
        try {
            buildResult = MVNExe.mvnCleanCompile(verificationProjectTemplateFolder);
        }
        catch (Exception x) {
            throw new RuntimeException("Project cannot be build: " + verificationProjectTemplateFolder,x);
        }
        if (buildResult.getExitValue()!=0) {
            List<String> buildLog = MVNExe.extractOutput(buildResult);
            LOGGER.warn("build output starts here -----------");
            for (String line:buildLog) {
                LOGGER.warn(line);
            }
            LOGGER.warn("build output ends here -----------");
            throw new RuntimeException("Project cannot be build, check logs for output: " + verificationProjectTemplateFolder);
        }

        // TODO could verify that tests fail here !

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
