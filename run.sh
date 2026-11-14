#!/bin/sh
set -eu
cd "$(dirname "$0")"
case "${1:-web}" in
    web) exec sh app/run.sh ;;
    https) exec sh app/https.sh ;;
    test) exec sh app/test.sh ;;
    console) exec sh 03_Demo/06_Packages/Java/run.sh saved ;;
    saved|menu|demo|check) exec sh 03_Demo/06_Packages/Java/run.sh "$1" ;;
    *) echo 'Dùng: sh run.sh [web|https|test|console|menu|demo|check]' >&2; exit 2 ;;
esac
