#!/bin/sh

# TODO: configure a specific (manually updated) vulnerability DB 
root=$(pwd)
for d in CVE-*/*/ ; do
  cd $d;
  mkdir -p scan-results/grype
  echo "running grype analysis on ${d}"
  # turn of all online activity
  export GRYPE_CHECK_FOR_APP_UPDATE=0
  export GRYPE_DB_AUTO_UPDATE=0
  export GRYPE_DB_MAX_ALLOWED_BUILT_AGE=1000000h
  grype --output json --file scan-results/grype/grype-report.json .;
  cd $root;
done
