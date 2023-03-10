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
        options.addRequiredOption("a", "artifacy",true, "the Maven artifact id of the artifact queried for clones");
        // @TODO - in the future, we could generalise this to look for version ranges , allow wildcards etc
        options.addRequiredOption("v", "version",true, "the Maven version of the artifact queried for clones");

        // we need a little language here to pass parameters, such as list:class1,class2
        options.addRequiredOption("s", "selector",true, "the strategy used to select classes");

        options.addOption("c","clonedetector",true,"the clone detector to be used, default is TODO");
        options.addOption("o","output",true,"the name of the file where resuls will be stored");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // @TODO implement this !!
    }
}
