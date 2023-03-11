package nz.ac.wgtn.shadedetector;

import org.apache.commons.cli.*;

/**
 * CLI main class.
 * @author jens dietrich
 */
public class Main {

    public static void main (String[] args) throws ParseException {
        Options options = new Options();
        options.addRequiredOption("g", "group",true, "the Maven group id of the artifact queried for clones");
        options.addRequiredOption("a", "artifact",true, "the Maven artifact id of the artifact queried for clones");
        // @TODO - in the future, we could generalise this to look for version ranges , allow wildcards etc
        options.addRequiredOption("v", "version",true, "the Maven version of the artifact queried for clones");
        options.addRequiredOption("o","output",true,"the name of the file where results will be stored");

        // we need a little language here to pass parameters, such as list:class1,class2
        // needs default
        options.addRequiredOption("s", "selector",true, "the strategy used to select classes");

        options.addOption("h", "help",false, "print instructions");
        options.addOption("c","clonedetector",true,"the clone detector to be used, default is TODO");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);
            String group = cmd.getOptionValue("group");
            String artifact = cmd.getOptionValue("artifact");
            String version = cmd.getOptionValue("version");
            boolean helpRequested = cmd.hasOption("help");
            String selectorDef = cmd.getOptionValue("selector");
            String cloneDetectorDef = cmd.getOptionValue("clonedetector");
        }
        catch (MissingOptionException x) {
            System.out.println(x.getMessage());
            printHelp(options);
        }

        // @TODO implement this !!
    }

    private static void printHelp(Options options) {
        String header = "Arguments:\n\n";
        String footer = "\nPlease report issues at https://github.com/jensdietrich/shading-study/issues/";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java Main", header, options, footer, true);
    }
}
