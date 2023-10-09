#!/bin/sh

# Redirect input from a log file to this command, or pipe a bunch of logfiles into it with cat.
perl -lne 'print $1 if (/analysing whether artifact '"$1"' matches/ ... /analysing whether artifact /) && /confidence: (\S+)/' | jq -s '{sum: add, count: length, avg: (add / length)}'
