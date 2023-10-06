#!/bin/sh

EXISTING_GHSA="$1"
shift
ls -d "$@" | perl -lne 's|.*/||; ($g, $a, $v) = split /__/; print "{\"name\": \"$g:$a\", \"versions\": [\"$v\"]}"' | jq -s 'group_by(.name) | {"affected": [.[] | {"package": {"ecosystem": "Maven", "name": .[0].name}, "versions": [.[].versions[]]}]}' | jq -j -s '.[0] + {"affected": (.[0].affected + .[1].affected)}' "$EXISTING_GHSA" -
