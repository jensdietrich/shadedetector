package nz.ac.wgtn.shadedetector;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import nz.ac.wgtn.shadedetector.clonedetection.ImportTranslationExtractor;
import nz.ac.wgtn.shadedetector.cveverification.*;
import nz.ac.wgtn.shadedetector.pov.PovProject;
import nz.ac.wgtn.shadedetector.pov.PovProjectParser;
import nz.ac.wgtn.shadedetector.resultreporting.CombinedResultReporter;
import nz.ac.wgtn.shadedetector.resultreporting.ProgressReporter;
import org.apache.commons.cli.*;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CLI main class.
 * @author jens dietrich
 */
public class Main {


    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static final String TEST_LOG = ".mvn-test.log";

    private static ClassSelectorFactory CLASS_SELECTOR_FACTORY = new ClassSelectorFactory();
    private static CloneDetectorFactory CLONE_DETECTOR_FACTORY = new CloneDetectorFactory();
    private static ArtifactSearchResultConsolidationStrategyFactory CONSOLIDATION_STRATEGY_FACTORY = new ArtifactSearchResultConsolidationStrategyFactory();
    private static ResultReporterFactory RESULT_REPORTER_FACTORY = new ResultReporterFactory();

    private static final String DEFAULT_GENERATED_VERIFICATION_PROJECT_GROUP_NAME = "foo";
    private static final String DEFAULT_GENERATED_VERIFICATION_PROJECT_VERSION = "0.0.1";

    private static final String DEFAULT_PROGRESS_STATS_NAME = "stats.log";

    public enum ProcessingStage {QUERY_RESULTS, CONSOLIDATED_QUERY_RESULTS, NO_DEPENDENCY_TO_VULNERABLE, CLONE_DETECTED, POC_INSTANCE_COMPILED, POC_INSTANCE_TESTED, POC_INSTANCE_TESTED_SHADED, TESTED}

    // resources will be copied into verification projects instantiated for clones
    private static final String[] SCA_SCRIPTS = {
            "/run-owasp-dependencycheck.sh",
            "/run-snyk.sh"
    };

    public static void main (String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("g", "group",true, "the Maven group id of the artifact queried for clones (default read from PoV's pov-project.json)");
        options.addOption("a", "artifact",true, "the Maven artifact id of the artifact queried for clones (default read from PoV's pov-project.json)");
        // @TODO - in the future, we could generalise this to look for version ranges , allow wildcards etc
        options.addOption("v", "version",true, "the Maven version of the artifact queried for clones (default read from PoV's pom.xml)");

        // we need a little language here to pass parameters, such as list:class1,class2
        // needs default
        options.addOption("s", "classselector",true, "the strategy used to select classes (optional, default is\"" + CLASS_SELECTOR_FACTORY.getDefault().name() + "\")");
        options.addOption("o", "output",true, "the component used to process and report results (optional, default is \"" + RESULT_REPORTER_FACTORY.getDefault().name() + "\")");
        options.addOption("o1", "output1",true, "an additional component used to process and report results");
        options.addOption("o2", "output2",true, "an additional component used to process and report results");
        options.addOption("o3", "output3",true, "an additional component used to process and report results");
        options.addOption("c","clonedetector",true,"the clone detector to be used (optional, default is \"" + CLONE_DETECTOR_FACTORY.getDefault().name() + "\")");
        options.addOption("r","resultconsolidation",true,"the query result consolidation strategy to be used (optional, default is \"" + CONSOLIDATION_STRATEGY_FACTORY.getDefault().name() + "\")");

        options.addRequiredOption("vul","vulnerabilitydemo",true,"a folder containing a Maven project that verifies a vulnerability in the original library with test(s), and can be used as a template to verify the presence of the vulnerability in a clone; values for -g, -a, -v and -sig are read from any contained pov-project.json");
        options.addRequiredOption("vos","vulnerabilityoutput_staging",true,"the root folder where for each clone, a project verifying the presence of a vulnerability is created");
        options.addRequiredOption("vov","vulnerabilityoutput_final",true,"the root folder where for each clone, a project created in the staging folder will be moved to if verification succeeds (i.e. if the vulnerability is shown to be present)");
        options.addOption("vg","vulnerabilitygroup",true,"the group name used in the projects generated to verify the presence of a vulnerability (default is \"" + DEFAULT_GENERATED_VERIFICATION_PROJECT_GROUP_NAME + "\")");
        options.addOption("vv","vulnerabilityversion",true,"the version used in the projects generated to verify the presence of a vulnerability (default is \"" + DEFAULT_GENERATED_VERIFICATION_PROJECT_VERSION + "\")");

        options.addOption("env","testenvironment",true,"a property file defining environment variables used when running tests on generated projects used to verify vulnerabilities, for instance, this can be used to set the Java version");
        options.addOption("ps","stats",true,"the file to which progress stats will be written (default is \"" + DEFAULT_PROGRESS_STATS_NAME + "\")");
        options.addOption("l","log",true,"a log file name (optional, if missing logs will only be written to console)");
        options.addOption("cache", "cachedir", true, "path to root of cache folder hierarchy (default is \"" + Cache.getRoot() +"\")");

        options.addOption("sig","vulnerabilitysignal",true,"indicates the test signal indicating that the vulnerability is present, must be of one of: " + Stream.of(TestSignal.values()).map(v -> v.name()).collect(Collectors.joining(",")) + " (default read from testSignalWhenVulnerable in PoV's pov-project.json)");


        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        }
        catch (MissingOptionException x) {
            LOGGER.error(x.getMessage(),x);
            printHelp(options);
            System.exit(1);
        }

        if (cmd.hasOption("log")) {
            String logFile = cmd.getOptionValue("log");
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            PatternLayoutEncoder ple = new PatternLayoutEncoder();
            ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
            ple.setContext(lc);
            ple.start();
            FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
            fileAppender.setFile(logFile);
            fileAppender.setEncoder(ple);
            fileAppender.setContext(lc);
            fileAppender.start();
            Logger rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            ((ch.qos.logback.classic.Logger)rootLogger).addAppender(fileAppender);
            LOGGER.info("file log appender set up, log file is: " + new File(logFile).getAbsolutePath());
        }

        if (cmd.hasOption("cachedir")) {
            String cacheDir = cmd.getOptionValue("cachedir");
            Cache.setRoot(new File(cacheDir));
            LOGGER.info("set cache root dir to {}", cacheDir);
        }

        // see whether vulnerability verification is available
        Path verificationProjectTemplateFolder = null;
        if (cmd.hasOption("vulnerabilitydemo")) {
            verificationProjectTemplateFolder = Path.of(cmd.getOptionValue("vulnerabilitydemo"));
            try {
                checkVerificationProject(verificationProjectTemplateFolder);
                LOGGER.info("vulnerability verification project is not valid");
            }
            catch (Exception x) {
                LOGGER.error("vulnerability verification project is valid");
            }
        }

        String groupId = null;
        String artifactId = null;
        String version = null;
        TestSignal expectedTestSignal = null;
        // Get defaults from PoV metadata
        File povMetadataFile = verificationProjectTemplateFolder.resolve("pov-project.json").toFile();
        if (povMetadataFile.exists()) {
            try {
                LOGGER.info("Reading PoV metadata from {}", povMetadataFile.getAbsolutePath());
                PovProject povMetaData = PovProjectParser.parse(povMetadataFile);
                expectedTestSignal = povMetaData.getTestSignalWhenVulnerable();
                String[] tokens = povMetaData.getArtifact().split(":");
                String groupIdFromMetadata = tokens[0];
                String artifactIdFromMetadata = tokens[1];

                // pov-project.json stores the complete set of vulnerableVersions according to the external DB entry, but NOT
                // the specific version the PoV project repros the vuln on (i.e., depends on). That needs to be extracted from its pom.xml.
                // Here we assume it's the only dependency with matching groupId and artifactId mentioned in pom.xml.
                try {
                    List<MVNDependency> possibleArtifactsUnderTest = POMAnalysis.getMatchingDependencies(verificationProjectTemplateFolder.resolve("pom.xml").toFile(), dep -> dep.getGroupId().equals(groupIdFromMetadata) && dep.getArtifactId().equals(artifactIdFromMetadata));
                    String versionFromMetadata = null;
                    if (possibleArtifactsUnderTest.size() != 1) {
                        LOGGER.error("Found {} dependency artifacts in PoV matching {}:{}, was expecting 1", possibleArtifactsUnderTest.size(), groupIdFromMetadata, artifactIdFromMetadata);
                        // Fall through. version will remain null unless specified by user with -v
                    }
                    else {
                        versionFromMetadata = possibleArtifactsUnderTest.get(0).getVersion();
                    }
                    groupId = groupIdFromMetadata;
                    artifactId = artifactIdFromMetadata;
                    version = versionFromMetadata;
                    LOGGER.info("Read {}:{}:{}, testSignalWhenVulnerable={} from PoV metadata (version came from pom.xml)", groupIdFromMetadata, artifactIdFromMetadata, versionFromMetadata, expectedTestSignal);
                }
                catch (Exception e) {
                    LOGGER.error("Exception while reading pom.xml to extract version", e);
                    System.exit(1);
                }
            } catch (FileNotFoundException e) {
                LOGGER.error("Error instantiating test signal from pov meta data");
            }
        }
        // Command-line arguments to -g, -a, -v, -sig override values read from pov-project.json
        groupId = cmd.getOptionValue("group", groupId);
        if (groupId == null) {
            LOGGER.error("Group ID could not be read from pov-project.json metadata, so must be specified with -g or --group");
            System.exit(1);
        }
        artifactId = cmd.getOptionValue("artifact", artifactId);
        if (artifactId == null) {
            LOGGER.error("Artifact ID could not be read from pov-project.json metadata, so must be specified with -a or --artifact");
            System.exit(1);
        }
        version = cmd.getOptionValue("version", version);
        if (version == null) {
            LOGGER.error("Version could not be read from pov-project.json metadata, so must be specified with -v or --version");
            System.exit(1);
        }
        GAV gav = new GAV(groupId,artifactId,version);
        LOGGER.info("PoV template GAV: {}", gav.asString());

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

        Properties testEnviron = new Properties();
        if (cmd.hasOption("testenvironment")) {
            String testEnvironDef = cmd.getOptionValue("testenvironment");
            Path testEvironFile = Path.of(testEnvironDef);
            Preconditions.checkArgument(Files.exists(testEvironFile),"test environment file not found: " + testEvironFile);
            try (Reader reader = Files.newBufferedReader(testEvironFile)) {
                testEnviron.load(reader);
                LOGGER.error("test environment loaded from {}",testEvironFile);
            } catch (IOException e) {
                LOGGER.error("cannot load test environment from {}",testEvironFile,e);
                throw new RuntimeException(e);
            }
        }


        ResultReporter resultReporter = resultReporters.size()==1 ?
            resultReporters.get(0) :
            new CombinedResultReporter(resultReporters);

        File progressStats = new File(DEFAULT_PROGRESS_STATS_NAME);
        if (cmd.hasOption("stats")) {
            progressStats = new File(cmd.getOptionValue("stats"));
        }
        LOGGER.error("progress stats will be written to {}",progressStats.getAbsolutePath());
        ProgressReporter progressReporter = new ProgressReporter(progressStats); // TODO make configurable


        // find artifact
        List<Artifact> allVersions = null;
        Artifact artifact = null;
        try {
            // note: fetching artifacts for all versions could be postponed
            ArtifactSearchResponse response = ArtifactSearch.findVersions(groupId,artifactId,1,ArtifactSearch.ROWS_PER_BATCH);
            allVersions = response.getBody().getArtifacts();
            final String finalVersion = version;    // To make the compiler happy compiling a lambda
            artifact = allVersions.stream()
                .filter(a -> a.getVersion().equals(finalVersion))
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

        Set<Artifact> allMatches = matches.values().stream().flatMap(response -> response.getBody().getArtifacts().stream()).collect(Collectors.toSet());
        progressReporter.artifactsProcessed(ProcessingStage.QUERY_RESULTS,allMatches);

        // consolidate results
        LOGGER.info("{} potential matches found",allMatches.size());
        List<Artifact> consolidatedMatches = resultConsolidationStrategy.consolidate(matches);
        LOGGER.info("matched consolidated to {}",consolidatedMatches.size());

        progressReporter.artifactsProcessed(ProcessingStage.CONSOLIDATED_QUERY_RESULTS,consolidatedMatches);

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
        progressReporter.artifactsProcessed(ProcessingStage.NO_DEPENDENCY_TO_VULNERABLE,candidates);

        // run clone detection
        AtomicInteger countMatchesAnalysed = new AtomicInteger();
        AtomicInteger countMatchesAnalysedFailed = new AtomicInteger();

        try {
            resultReporter.startReporting(artifact,originalSources);
        }
        catch (IOException x) {
            LOGGER.error("error initialising result reporting",x);
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


        // set up signal (may already have been read from pov-projects.json)
        String vulnerabilitySignalAsString = cmd.getOptionValue("vulnerabilitysignal");
        if (vulnerabilitySignalAsString != null) {
            vulnerabilitySignalAsString = vulnerabilitySignalAsString.toUpperCase();
            if (!vulnerabilitySignalAsString.equals("AUTO")) { // "auto" is now the default; ignore here for backcompat
                expectedTestSignal = TestSignal.valueOf(vulnerabilitySignalAsString); // will throw illegal argument exception if no such constant exists
            }
        }

        if (expectedTestSignal == null) {
            LOGGER.error("could not determine testSignalWhenVulnerable from the pov-project.json metadata, and --vulnerabilitysignal was not specified");
            System.exit(1);
        }

        LOGGER.info("test signal is {}",expectedTestSignal);
        assert expectedTestSignal != null;

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


        // sets mainly used to produce stats later
        Set<Artifact> cloneDetected = new HashSet<>();
        Set<Artifact> compiledSuccessfully = new HashSet<>();
        Set<Artifact> testedSuccessfully = new HashSet<>();
        Set<Artifact> shaded = new HashSet<>();

        for (Artifact match:candidates) {
            countMatchesAnalysed.incrementAndGet();
            LOGGER.info("analysing whether artifact {} matches",match.getId());
            ResultReporter.VerificationState state = ResultReporter.VerificationState.NONE;
            Set<CloneDetector.CloneRecord> cloneAnalysesResults = Set.of();
            List<Path> sources = List.of();
            boolean packagesHaveChangedInClone = false; // indicates shading with packages being renamed

            if (Blacklist.contains(match)) {
                LOGGER.warn("Skipping blacklisted artifact: " + match.asGAV().asString());
            }
            else {
                try {
                    Path src = FetchResources.fetchSources(match);
                    sources = Utils.listJavaSources(src, true);
                    cloneAnalysesResults = cloneDetector.detect(originalSources, src);

                    LOGGER.info("Reporting results for " + match.getId());

                    // TODO abstract threshold
                    if (cloneAnalysesResults.size() > 10) {
                        cloneDetected.add(match);
                        LOGGER.info("generating project to verifify vulnerability for " + match);
                        String verificationProjectArtifactName = match.toString().replace(":", "__");
                        LOGGER.info("\tgroupId: " + verificationProjectGroupName);
                        LOGGER.info("\tartifactId: " + verificationProjectArtifactName);
                        LOGGER.info("\tversion: " + verificationProjectVersion);
                        Path verificationProjectFolderStaged = verificationProjectInstancesFolderStaging.resolve(verificationProjectArtifactName);
                        LOGGER.info("\tproject folder: " + verificationProjectFolderStaged);

                        Map importTranslations = ImportTranslationExtractor.computeImportTranslations(originalSources, src, cloneAnalysesResults);

                        MVNProjectCloner.CloneResult result = MVNProjectCloner.cloneMvnProject(
                                verificationProjectTemplateFolder,
                                verificationProjectFolderStaged,
                                gav,
                                match.asGAV(),
                                new GAV(verificationProjectGroupName, verificationProjectArtifactName, verificationProjectVersion),
                                importTranslations,
                                testEnviron
                        );
                        packagesHaveChangedInClone = result.isRenamedImports();
                        if (packagesHaveChangedInClone) {
                            shaded.add(match);
                        }

                        if (result.isCompiled()) {
                            state = ResultReporter.VerificationState.COMPILED;
                            compiledSuccessfully.add(match);
                        }
                        if (result.isTested()) { // override
                            state = ResultReporter.VerificationState.TESTED;
                        }

                        if (result.isTested()) {
                            boolean vulnerabilityIsPresent = isVulnerabilityPresent(expectedTestSignal, verificationProjectFolderStaged);
                            if (vulnerabilityIsPresent) {
                                testedSuccessfully.add(match);
                                Path verificationProjectFolderFinal = verificationProjectInstancesFolderFinal.resolve(verificationProjectArtifactName);
                                LOGGER.info("\tmoving verified project folder from {} to {}", verificationProjectFolderStaged, verificationProjectFolderFinal);
                                MVNProjectCloner.moveMvnProject(verificationProjectFolderStaged, verificationProjectFolderFinal);

                                // re-test to create surefire reports
                                LOGGER.error("running build test on final project {}", verificationProjectFolderFinal);
                                Path buildLog = verificationProjectFolderFinal.resolve(TEST_LOG);
                                try {
                                    ProcessResult pr = MVNExe.mvnTest(verificationProjectFolderFinal, testEnviron);
                                    String out = pr.outputUTF8();
                                    Files.write(buildLog, List.of(out));

                                    boolean vulnerabilityIsPresentInFinal = isVulnerabilityPresent(expectedTestSignal, verificationProjectFolderFinal);
                                    if (!vulnerabilityIsPresentInFinal) {
                                        LOGGER.error("error testing final project {} -- vulnerability was present in staging but not in final", verificationProjectFolderFinal);
                                    }
                                } catch (Exception x) {
                                    LOGGER.error("error testing final project {}", verificationProjectFolderFinal, x);
                                    String stacktrace = Utils.printStacktrace(x);
                                    Files.write(buildLog, List.of(stacktrace));
                                }
                            }

                        }
                    }

                } catch (Exception e) {
                    LOGGER.error("cannot fetch sources for artifact {}", match.toString(), e);
                    countMatchesAnalysedFailed.incrementAndGet();
                } finally {
                    try {
                        resultReporter.report(artifact, match, sources, cloneAnalysesResults, state, packagesHaveChangedInClone);
                    } catch (IOException e) {
                        LOGGER.error("error reporting", e);
                    }
                }
            }
        }

        progressReporter.artifactsProcessed(ProcessingStage.CLONE_DETECTED,cloneDetected);
        progressReporter.artifactsProcessed(ProcessingStage.POC_INSTANCE_COMPILED,compiledSuccessfully);
        progressReporter.artifactsProcessed(ProcessingStage.POC_INSTANCE_TESTED,testedSuccessfully);
        progressReporter.artifactsProcessed(ProcessingStage.POC_INSTANCE_TESTED_SHADED, Sets.intersection(testedSuccessfully,shaded));

        try {
            progressReporter.endReporting();
            LOGGER.info("finished progress reporting, results written to {}",progressReporter.getOutput().getAbsolutePath());
        }
        catch (IOException x) {
            LOGGER.error("error finishing progress reporting",x);
        }

        try {
            resultReporter.endReporting(artifact);
        }
        catch (IOException x) {
            LOGGER.error("error finishing result reporting",x);
        }
    }

    private static boolean isVulnerabilityPresent(TestSignal expectedTestSignal, Path verificationProjectFolder) throws IOException, JDOMException {
        Path surefireReports = verificationProjectFolder.resolve("target/surefire-reports");

        if (Files.exists(surefireReports)) {
            SurefireUtils.TestResults testResults = SurefireUtils.parseSurefireReports(surefireReports);
            boolean vulnerabilityIsPresent = testResults.assertExpectedOutcome(expectedTestSignal);
            LOGGER.info("tests in {}: {} passed, {} failed, {} errors, {} skipped -> vuln is {}sent",
                    verificationProjectFolder,
                    testResults.getTestCount() - (testResults.getFailureCount() + testResults.getErrorCount() + testResults.getSkippedCount()),
                    testResults.getFailureCount(),
                    testResults.getErrorCount(),
                    testResults.getSkippedCount(),
                    vulnerabilityIsPresent ? "pre" : "ab"
            );
            return vulnerabilityIsPresent;
        }

        LOGGER.warn("no surefire reports found in {}, will assume that tests have not passed", verificationProjectFolder);
        return false;
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
        formatter.setWidth(150);
        formatter.printHelp("java -cp <classpath> Main", header, options, footer, true);
    }
}
