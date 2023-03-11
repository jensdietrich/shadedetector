package nz.ac.wgtn.shadedetector;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CLI main class.
 * @author jens dietrich
 */
public class Main {

    private static Logger LOGGER = LoggerFactory.getLogger(ArtifactSearchResultConsolidationStrategyFactory.class);

    private static ClassSelectorFactory CLASS_SELECTOR_FACTORY = new ClassSelectorFactory();
    private static CloneDetectorFactory CLONE_DETECTOR_FACTORY = new CloneDetectorFactory();
    private static ArtifactSearchResultConsolidationStrategyFactory CONSOLIDATION_STRATEGY_FACTORY = new ArtifactSearchResultConsolidationStrategyFactory();

    public static void main (String[] args) throws ParseException {
        Options options = new Options();
        options.addRequiredOption("g", "group",true, "the Maven group id of the artifact queried for clones");
        options.addRequiredOption("a", "artifact",true, "the Maven artifact id of the artifact queried for clones");
        // @TODO - in the future, we could generalise this to look for version ranges , allow wildcards etc
        options.addRequiredOption("v", "version",true, "the Maven version of the artifact queried for clones");
        options.addRequiredOption("o","output",true,"the name of the file where results will be stored");

        // we need a little language here to pass parameters, such as list:class1,class2
        // needs default
        options.addOption("s", "classselector",true, "the strategy used to select classes (optional, default is\"" + CLASS_SELECTOR_FACTORY.getDefault().name() + "\"");

        options.addOption("h", "help",false, "print instructions");
        options.addOption("c","clonedetector",true,"the clone detector to be used (optional, default is \"" + CLONE_DETECTOR_FACTORY.getDefault().name() + "\"");
        options.addOption("r","resultconsolidation",true,"the query result consolidation strategy to be used (optional, default is \"" + CONSOLIDATION_STRATEGY_FACTORY.getDefault().name() + "\"");


        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        }
        catch (MissingOptionException x) {
            LOGGER.error(x.getMessage(),x);
            printHelp(options);
        }

        String group = cmd.getOptionValue("group");
        String artifact = cmd.getOptionValue("artifact");
        String version = cmd.getOptionValue("version");
        boolean helpRequested = cmd.hasOption("help");

        if (cmd.hasOption("help")) {
            printHelp(options);
        }

        CloneDetector cloneDetector = instantiateOptional(CLONE_DETECTOR_FACTORY,cmd,"clone detector","clonedetector");
        ClassSelector classSelector = instantiateOptional(CLASS_SELECTOR_FACTORY,cmd,"class selector","classselector");
        ArtifactSearchResultConsolidationStrategy resultConsolidationStrategy = instantiateOptional(CONSOLIDATION_STRATEGY_FACTORY,cmd,"result consolidation strategy","resultconsolidation");

        // @TODO implement rest !!
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
        formatter.printHelp("java Main", header, options, footer, true);
    }
}
