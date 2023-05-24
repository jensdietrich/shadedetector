# ShadeDetector -- A Tool to Detect Vulnerabilities in Clones and Shaded Components. 

## Overview 

The tools takes the coordinates of Maven artifact (**GAV** - **G**roupId + **A**rtifactId + **V**ersion) and a testable proof-of-vulnerability (TPOV) project as input, 
and will infer and report a list of artifacts that are cloning / shading the input artifact, and are also exposed to the same vulnerability. For each such artifact,
a POV is constructed. 

## Testable Proof-of-vulnerability Projects (TPOV)

### The Structure of a TPOV 

TPOVs make a vulnerability testable. Each TPOV has the following structure:

1. a TPOV is a simple (i.e. non-modular) Maven project
2. a TPOV has a dependency on the vulnerable artifact. 
3. a TPOV has a test-scope dependency on JUnit5,  other dependencies should be avoided or minimised
4. a TPOV has one or more tests that succeed if and only if the vulnerability can be exploited -- i.e. the vulnerability becomes the test oracle. Those tests may be the only classes defined in a POV.
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
