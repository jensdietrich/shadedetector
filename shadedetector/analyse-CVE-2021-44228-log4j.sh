#!/bin/sh

mvn clean package
java -jar target/shadedetector.jar \
  -g org.apache.logging.log4j \
  -a log4j-core \
  -v 2.14.1 \
  -o csv.details?dir=out-CVE202144228-log4j \
  -o1 csv.summary?file=result-summary-CVE202144228-log4j.csv \
  -vul /Users/jens/Development/xshady/CVE-2021-44228 \
  -vos .generated/staged/CVE-2021-44228 \
  -vov .generated/final/CVE-2021-44228
