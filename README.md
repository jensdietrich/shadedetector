# ShadeDetector -- A Tool to Detect Vulnerabilities in Cloned or Shaded Components

## Overview 

The tools takes the coordinates of Maven artifact (**GAV** - **G**roupId + **A**rtifactId + **V**ersion) and a testable proof-of-vulnerability (TPOV) project as input, 
and will infer and report a list of artifacts that are cloning / shading the input artifact, and are also exposed to the same vulnerability. For each such artifact,
a TPOV is constructed from the original TPOV, proving the precense of the vulnerability. 

## Testable Proof-of-vulnerability Projects (TPOV)

### The Structure of a TPOV 

TPOVs make a vulnerability testable. Each TPOV has the following structure:

1. a TPOV is a simple (i.e. non-modular) Maven project
2. a TPOV has a dependency on the vulnerable artifact. 
3. a TPOV has a test-scope dependency on JUnit5,  other dependencies should be avoided or minimised.
4. a TPOV has one or more tests that succeed if and only if the vulnerability can be exploited -- i.e. the vulnerability becomes the test oracle. Those tests may be the only classes defined in a TPOV.
5. a TPOV test may declare dependencies on certain OS or JRE versions using standard JUnit annotations such as  `@EnabledOnOs` or `@EnabledOnJre`
6. sources in a TPOV should not directly use fully classified class names, instead, imports shopuld be used (this is to aid the tool to automatically refactor dependencies) 

### Sourcing TPOVs

1. some TPOVs can be found here: https://github.com/jensdietrich/xshady/
2. there are numerous proof-of-vulnerability (POV) projects on GitHub, such as https://github.com/frohoff/ysoserial , usually those projects need to be modified to make them TPOVs as described above
3. this is a collection of POVs: https://github.com/tuhh-softsec/vul4j  , see also *Bui QC, Scandariato R, Ferreyra NE. Vul4J: a dataset of reproducible Java vulnerabilities geared towards the study of program repair techniques. MSR'22.

## Building

The project must be build with Java 11 or better. To build run `mvn package`. This will create the executable `shadedetector.jar` in `/target`.

## Running 

```
usage: java -cp <classpath> Main -a <arg> [-c <arg>] [-env <arg>] -g <arg> [-o <arg>] [-o1 <arg>] [-o2 <arg>] [-o3 <arg>] [-ps <arg>] [-r <arg>] [-s
       <arg>] -v <arg> [-vg <arg>] [-vos <arg>] [-vov <arg>] [-vul <arg>] [-vv <arg>]
Arguments:

 -a,--artifact <arg>                        the Maven artifact id of the artifact queried for clones
 -c,--clonedetector <arg>                   the clone detector to be used (optional, default is "ast")
 -env,--testenvironment <arg>               a property file defining environment variables used when running tests on generated projects used to
                                            verify vulnerabilities, for instance, this can be used to set the Java version
 -g,--group <arg>                           the Maven group id of the artifact queried for clones
 -o,--output <arg>                          the component used to process and report results (optional, default is "log")
 -o1,--output1 <arg>                        an additional component used to process and report results
 -o2,--output2 <arg>                        an additional component used to process and report results
 -o3,--output3 <arg>                        an additional component used to process and report results
 -ps,--stats <arg>                          the file to which progress stats will be written (default is "stats.log"
 -r,--resultconsolidation <arg>             the query result consolidation strategy to be used (optional, default is "moreThanOne")
 -s,--classselector <arg>                   the strategy used to select classes (optional, default is"complexnames")
 -v,--version <arg>                         the Maven version of the artifact queried for clones
 -vg,--vulnerabilitygroup <arg>             the group name used in the projects generated to verify the presence of a vulnerability (default is "foo")
 -vos,--vulnerabilityoutput_staging <arg>   the root folder where for each clone, a project verifying the presence of a vulnerability is created
 -vov,--vulnerabilityoutput_final <arg>     the root folder where for each clone, a project created in the staging folder will be moved to if
                                            verification succeeds (i.e. if the vulnerability is shown to be present)
 -vul,--vulnerabilitydemo <arg>             a folder containing a Maven project that verifies a vulnerability in the original library with test(s),
                                            and can be used as a template to verify the presence of the vulnerability in a clone
 -vv,--vulnerabilityversion <arg>           the version used in the projects generated to verify the presence of a vulnerability (default is "0.0.1")
         
 ```
 
## Setting the Environment

With `-env` an environment can be set to be used to build / test the TPOVs. If TPOV tests require a Java version different from the one used to run the tool, this can be used to set `JAVA_HOME` to point to a partiocular version of the Java Development Kit (JDK, not just JRE as TPOVs are compiled).

## Known Issues

In principe the tool can be run withj Java 11. However, we did encounter rare cases where the analysis gets stuck and eventually fails with an `OutOfMemoryError`. This seems to be caused by a [bug in the zip file system in Java 11](https://bugs.openjdk.org/browse/JDK-7143743). We recommend using Java 17 if this is a problem. 

It is also possible to added artifacts to `nz.ac.wgtn.shadedetector.Blacklist` to exclude them from the analysis. 

## Customising / Extending

Several strategies are implemented as pluggable services. I.e. strategies are described via interfaces, with service providers declared in library manifests, see for instance [src/main/resources/META_INF/services](src/main/resources/META_INF/services) for the onboard default providers. Each provider has a unqique name that can be used as an argument value in the CLI. All interfaces are defined in `nz.ac.wgtn.shadedetector`. The service is selected by a factory `nz.ac.wgtn.shadedetector.<Service>Factory` that also defined what is being used as the default service provider. 

| Service     | Interface   | CLI Argument(s) | Description | Default |
| ----------- | ----------- | -----------     | ----------- |  ----------- |
| result reporter      | `ResultReporter`  | `-o`,`-o1`,`-o2`,`-o2` | consumes analysis results, e.g. to generate reports | report results using standard *log4j* logging |
| class selector       | `ClassSelector`  | `-s` | selects the classes from the input artifact to be used to query Maven for potenial clones | pick 10 classes with the highest number of camel case tokens (i.e. complex class names) |
| clone detector       | `CloneDetector`  | `-c` | the clone detector used to compare two source code files (from the input artifact and a potenial clone) | custom AST-based clone detection that ignores comments and package names in type references | 
| consolidation strategy | `ArtifactSearchResultConsolidationStrategy` | `-r` | the strategy used to consolidate artifact sets obtained by REST queries for a single class into a single set | an artifact must appear in at least two sets | 

Some services can be customised further by setting properties (corresponding to bean properties in the respective service provider classes). For instance, consider the following arguments setting up output reporting:

```
  -o csv.details?dir=results/details/CVE-2022-45688-commonstext -o1 csv.summary?file=results/summary-CVE-2022-45688-commonstext.csv
```

This sets up two reporters named `csv.details` (corresponding to `nz.ac.wgtn.shadedetector.resultreporting.CSVDetailedResultReporter`) and `csv.summary` (corresponding to `nz.ac.wgtn.shadedetector.resultreporting.CSVSummaryResultReporter`), respectively. This is followed by a configuration consisting of &-separated key-value pairs, setting properties of the respective instance. In this case, the files / folders where reports are to be generated are set.

