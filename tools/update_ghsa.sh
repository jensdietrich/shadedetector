#!/bin/sh

EXISTING_GHSA="$1"
shift
ls -d "$@" | perl -lne 's|.*/||; ($g, $a, $v) = split /__/; print "{\"affected\": [{\"package\": {\"ecosystem\": \"Maven\", \"name\": \"$g:$a\"}, \"versions\": [\"$v\"]}]}"' | jq -s '.[0] * .[1]' "$EXISTING_GHSA" -
