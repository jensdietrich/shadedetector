.PHONY: all all.snyk_reports

ALL_ARTIFACTS = $(wildcard CVE-*/*)

ALL_SNYK_REPORTS = $(foreach f,$(ALL_ARTIFACTS),$(f)/scan-results/snyk/snyk-report.json)

all: all.snyk_reports

all.snyk_reports: $(ALL_SNYK_REPORTS)

%/scan-results/snyk/snyk-report.json: %/pom.xml
	mkdir -p $(@D)
	cd $* && snyk test --json --json-file-output=$(@:$*/%=%)
