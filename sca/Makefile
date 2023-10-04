# Makefile for compiling SCA tool reports under scan-results for each vulnerable artifact using snyk, Steady, OWASP Dependency Check and grype.
#
# If file modification times have been lost (e.g., after git checkout), run "make touch_existing_reports" first to ensure that make sees any existing reports as already up-to-date.
#
# "make" or "make all" will compile missing/out-of-date reports for all 4 tools for every vulnerable artifact
# "make all.snyk_reports" (etc.) will do the same, but restricted to snyk reports (etc.)
# "make CVE-1234-5678/some__artifact__version/scan-results/snyk/snyk-report.json" will compile just that report, if missing or out-of-date
# A report is considered out-of-date if the pom.xml file's modification time is newer. (Yes, this is somewhat arbitrary.)
#
# Tips: "make -n" to see what would happen, "make -k" to keep going if there are errors, "make -B" to force (re)generation, "make -j 4" to run up to 4 tasks concurrently

.PHONY: all all.snyk_reports all.steady_reports all.owaspdepcheck_reports all.grype_reports touch_existing_reports

ALL_ARTIFACTS = $(wildcard CVE-*/*)

ALL_SNYK_REPORTS = $(foreach f,$(ALL_ARTIFACTS),$(f)/scan-results/snyk/snyk-report.json)
ALL_STEADY_REPORTS = $(foreach f,$(ALL_ARTIFACTS),$(f)/scan-results/steady/steady-report.json)
ALL_OWASPDEPCHECK_REPORTS = $(foreach f,$(ALL_ARTIFACTS),$(f)/scan-results/dependency-check/dependency-check-report.json)
ALL_GRYPE_REPORTS = $(foreach f,$(ALL_ARTIFACTS),$(f)/scan-results/grype/grype-report.json)

# Default target: Run everything that isn't up-to-date
all: all.snyk_reports all.steady_reports all.owaspdepcheck_reports all.grype_reports

all.snyk_reports: $(ALL_SNYK_REPORTS)

all.steady_reports: $(ALL_STEADY_REPORTS)

all.owaspdepcheck_reports: $(ALL_OWASPDEPCHECK_REPORTS)

all.grype_reports: $(ALL_GRYPE_REPORTS)

# After file modification times are lost (e.g., after git checkout), run this first to ensure that make sees any existing reports as already up-to-date.
touch_existing_reports:
	find . -path './CVE-*/*/scan-results/*/*-report.json' | xargs touch

%/scan-results/snyk/snyk-report.json: %/pom.xml
	@echo "Running snyk to generate $@..."
	mkdir -p $(@D)
	-cd $* && snyk test --json --json-file-output=$(@:$*/%=%)

%/scan-results/steady/steady-report.json: %/pom.xml
	@echo "Running Steady to generate $@..."
	mkdir -p $(@D)
	-cd $* && mvn org.eclipse.steady:plugin-maven:3.2.5:app && mvn org.eclipse.steady:plugin-maven:3.2.5:report -Dvulas.report.reportDir=$(abspath $(@D))
	mv $(@D)/vulas-report.json $@
	rm -f $(@D)/vulas-report.{x,ht}ml

# Maven's Dependency Check can fail due to rate-limiting, in which case we effectively delete the file to enable easy rerunning.
%/scan-results/dependency-check/dependency-check-report.json: %/pom.xml
	@echo "Running OWASP Dependency Check to generate $@..."
	mkdir -p $(@D)
	-cd $* && mvn org.owasp:dependency-check-maven:8.2.1:check -Dformat=json -DprettyPrint=true -Dodc.outputDirectory=$(@D:$*/%=%)
	( if [ `jq '.scanInfo.analysisExceptions|length' $@` -ne 0 ]; then mv $@ $@.FAILED_WITH_EXCEPTIONS; exit 1; fi )

# Env vars turn off all online activity
%/scan-results/grype/grype-report.json: %/pom.xml
	@echo "Running grype to generate $@..."
	mkdir -p $(@D)
	-cd $* && GRYPE_CHECK_FOR_APP_UPDATE=0 GRYPE_DB_AUTO_UPDATE=0 GRYPE_DB_MAX_ALLOWED_BUILT_AGE=1000000h grype --output json --file $(@:$*/%=%) .
