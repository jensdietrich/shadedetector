#!/bin/sh

EXISTING_GHSA="$1"
shift
ls -d "$@" | perl -lne 's|.*/||; ($g, $a, $v) = split /__/; print "{\"affected\": [{\"package\": {\"ecosystem\": \"Maven\", \"name\": \"$g:$a\"}, \"versions\": [\"$v\"]}]}"' | jq -s '.[0] + {"affected": (.[0].affected + .[1].affected)}' "$EXISTING_GHSA" -
