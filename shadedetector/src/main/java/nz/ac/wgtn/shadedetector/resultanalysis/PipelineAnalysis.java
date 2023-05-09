package nz.ac.wgtn.shadedetector.resultanalysis;

import com.google.common.base.Preconditions;
import nz.ac.wgtn.shadedetector.Main;
import nz.ac.wgtn.shadedetector.resultreporting.ProgressReporter;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility to report pipeline throughput
 * @author jens dietrich
 */
public class PipelineAnalysis {

    public static void main (String[] args) throws IOException {

        Preconditions.checkArgument(args.length==2,"two arguments required - the result folder containing summary reports, and the output file name");
        File SUMMARY_FOLDER = new File(args[0]);
        Preconditions.checkArgument(SUMMARY_FOLDER.exists());
        File OUT_SUMMARY = new File(args[1]);

        System.out.println("creating summary report in " + OUT_SUMMARY.getAbsolutePath());
        try (PrintWriter out = new PrintWriter(new FileWriter(OUT_SUMMARY))) {
            out.println("\\begin{table*}");
            out.println("\t\\begin{tabular}{|l|p{2cm}p{2cm}p{2cm}p{2cm}p{2cm}p{2cm}|}");
            out.println("\t\\hline");
            out.println(asLatexTableRow(
    "vulnerability",
                "query results",
                "consolidated",
                "no dependency",
                "clones detected",
                "pov compiled",
                "pov tested"
            ));
            out.println("\t\\hline");

            // BODY of table

            // counters for summary in last row
            Map<Main.ProcessingStage,Integer> versionedCounters = new HashMap<>();
            Map<Main.ProcessingStage,Integer> unversionedCounters = new HashMap<>();

            Stream.of(SUMMARY_FOLDER.listFiles())
                .sorted()
                .filter(f -> f.getName().startsWith("CVE-"))
                .filter(f -> f.getName().endsWith(".properties"))
                .forEach(f -> {
                    Properties properties = new Properties();
                    try (Reader reader = new FileReader(f)) {
                        properties.load(reader);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    out.println(asLatexTableRow(
                        f.getName().replace(".properties",""),
                        getValue(properties, Main.ProcessingStage.QUERY_RESULTS,versionedCounters,unversionedCounters),
                        getValue(properties, Main.ProcessingStage.CONSOLIDATED_QUERY_RESULTS,versionedCounters,unversionedCounters),
                        getValue(properties, Main.ProcessingStage.NO_DEPENDENCY_TO_VULNERABLE,versionedCounters,unversionedCounters),
                        getValue(properties, Main.ProcessingStage.CLONE_DETECTED,versionedCounters,unversionedCounters),
                        getValue(properties, Main.ProcessingStage.POC_INSTANCE_COMPILED,versionedCounters,unversionedCounters),
                        getValue(properties, Main.ProcessingStage.POC_INSTANCE_TESTED,versionedCounters,unversionedCounters)
                    ));
                });

            out.println("\t\\hline");

            out.println(asLatexTableRow(
                    "(sum)",
                    getAggregatedValue(Main.ProcessingStage.QUERY_RESULTS,versionedCounters,unversionedCounters),
                    getAggregatedValue(Main.ProcessingStage.CONSOLIDATED_QUERY_RESULTS,versionedCounters,unversionedCounters),
                    getAggregatedValue(Main.ProcessingStage.NO_DEPENDENCY_TO_VULNERABLE,versionedCounters,unversionedCounters),
                    getAggregatedValue(Main.ProcessingStage.CLONE_DETECTED,versionedCounters,unversionedCounters),
                    getAggregatedValue(Main.ProcessingStage.POC_INSTANCE_COMPILED,versionedCounters,unversionedCounters),
                    getAggregatedValue(Main.ProcessingStage.POC_INSTANCE_TESTED,versionedCounters,unversionedCounters)
            ));
            out.println("\t\\hline");

            out.println("\t\\end{tabular}");
            out.println("\t\\caption{\\label{tab:pipeline}Processed Artifacts at each stage, numbers in brackets are classes of artifacts with the same group and artifact id (i.e., versions are ignored)}");
            out.println("\\end{table*}");
        }
    }

    private static String getValue(Properties properties, Main.ProcessingStage stage,Map<Main.ProcessingStage,Integer> versionedCounters,Map<Main.ProcessingStage,Integer> unversionedCounters) {
        String value = "?";
        String v = properties.getProperty(stage.name());
        if (v!=null) {
            int i = Integer.valueOf(v);
            final int j = i; // final for lambda
            versionedCounters.compute(stage,(k, oldValue) ->  oldValue==null ? j : j+oldValue);
            value = String.format("%,d", i);

            v = properties.getProperty(stage.name()+ ProgressReporter.UNVERSIONED_KEY_EXTENSION);
            if (v!=null) {
                i = Integer.valueOf(v);
                final int l = i; // final for lambda
                unversionedCounters.compute(stage,(k, oldValue) ->  oldValue==null ? l : l+oldValue);
                value = value + " (" + String.format("%,d", i) + ')';
            }
        }
        return value;
    }

    private static String getAggregatedValue(Main.ProcessingStage stage,Map<Main.ProcessingStage,Integer> versionedCounters,Map<Main.ProcessingStage,Integer> unversionedCounters) {
        return String.format("%,d", versionedCounters.get(stage))
            + " ("
            + String.format("%,d", unversionedCounters.get(stage))
            + ')';
    }

    private static String asLatexTableRow(Object... cellValues) {
        return Stream.of(cellValues)
            .map(obj -> obj.toString())
            .collect(Collectors.joining("&","\t"," \\\\"));
    }

}
