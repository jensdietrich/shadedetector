#!/bin/sh

if [ "$#" -lt 2 ]
then
	echo "Syntax: $0 existing_ghsa.json logfile.log [logfile.log ...] > updated_ghsa.json"
	echo "There must be at least one logfile argument, and all must refer to the same CVE."
	echo "No attempt is made to handle a GA or GAV already existing in existing_ghsa.json."
	exit 1
fi

EXISTING_GHSA="$1"
shift

# Remaining arguments are log files that should all refer to the same CVE
# Extract new JSON data from log files, merge them into the old JSON, and write to stdout.
`dirname $0`/extract_vuln_range.pl "$@" | jq -j -s '.[0] + {"affected": (.[0].affected + .[1].affected), "modified": .[1].modified, "references": (.[0].references + .[1].references)}' "$EXISTING_GHSA" -
