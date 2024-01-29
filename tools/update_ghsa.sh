#!/bin/sh

if [ "$#" -lt 2 ]
then
	echo "Syntax: $0 existing_ghsa.json [cve/]g__a__v [[cve/]g__a__v ...] > updated_ghsa.json"
	echo "Each cve/g__a__v should look like CVE-1234-5678/somegroup__someartifact__1.2.3"
	echo "No attempt is made to handle a GA or GAV already existing in existing_ghsa.json."
	exit 1
fi

EXISTING_GHSA="$1"
shift

# Remaining arguments are dirs that should all refer to the same CVE
CVE="$(basename $(dirname $1))"

NOW=`date --iso-8601=seconds --utc | sed -e 's/+00:00/Z/'`
ls -d "$@" |
	perl -lne 's|/$||; s|.*/||; ($g, $a, $v) = split /__/; print "{\"name\": \"$g:$a\", \"versions\": [\"$v\"]}"' |
	# GitHub can't handle multiple versions per affected package; workaround per https://github.com/github/advisory-database/pull/2841#issuecomment-1787952423 is to make a separate affected package for each version
	#jq -s 'group_by(.name) | {"affected": [.[] | {"package": {"ecosystem": "Maven", "name": .[0].name}, "versions": [.[].versions[]]}]}' | 		# Group all versions with same groupId and artifactId
	jq -s '{"affected": [.[] | {"package": {"ecosystem": "Maven", "name": .name}, "versions": .versions}]}' | 
	jq -j -s '.[0] + {"modified": "'"$NOW"'", "affected": (.[0].affected + .[1].affected), "references": (.[0].references + [{"type": "EVIDENCE", "url": "https://github.com/jensdietrich/xshady-release/tree/main/'"$CVE"'"}, {"type": "WEB", "url": "https://arxiv.org/pdf/2306.05534.pdf"}])}' "$EXISTING_GHSA" -								# Merge into existing GHSA JSON
