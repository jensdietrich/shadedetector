.PHONY: all all.snyk_reports all.steady_reports all.owaspdepcheck_reports

ALL_ARTIFACTS = $(wildcard CVE-*/*)

ALL_SNYK_REPORTS = $(foreach f,$(ALL_ARTIFACTS),$(f)/scan-results/snyk/snyk-report.json)
ALL_STEADY_REPORTS = $(foreach f,$(ALL_ARTIFACTS),$(f)/scan-results/steady/vulas-report.json)
ALL_OWASPDEPCHECK_REPORTS = $(foreach f,$(ALL_ARTIFACTS),$(f)/scan-results/dependency-check/dependency-check-report.json)

# Default goal: Run everything
all: all.snyk_reports all.steady_reports all.owaspdepcheck_reports

all.snyk_reports: $(ALL_SNYK_REPORTS)

all.steady_reports: $(ALL_STEADY_REPORTS)

all.owaspdepcheck_reports: $(ALL_OWASPDEPCHECK_REPORTS)

%/scan-results/snyk/snyk-report.json: %/pom.xml
	mkdir -p $(@D)
	cd $* && snyk test --json --json-file-output=$(@:$*/%=%)

%/scan-results/steady/vulas-report.json: %/pom.xml
	mkdir -p $(@D)
	cd $* && mvn org.eclipse.steady:plugin-maven:3.2.5:app && mvn org.eclipse.steady:plugin-maven:3.2.5:report -Dvulas.report.reportDir=$(abspath $(@D))

%/scan-results/dependency-check/dependency-check-report.json: %/pom.xml
	mkdir -p $(@D)
	cd $* && dependency-check -scan . -f JSON -o $(@D:$*/%=%) -prettyPrint
