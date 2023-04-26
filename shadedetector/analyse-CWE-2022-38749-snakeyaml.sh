#!/bin/sh

mvn clean package
java -jar target/shadedetector.jar \
  -g org.yaml \
  -a snakeyaml \
  -v 1.25 \
  -o csv.details?dir=out-snakeyaml \
  -o1 csv.summary?file=result-summary-snakeyaml.csv \
  -vul /Users/jens/Development/xshady/CVE-2022-38749 \
  -vos .generated/staged/CVE-2022-38749 \
  -vov .generated/final/CVE-2022-38749
