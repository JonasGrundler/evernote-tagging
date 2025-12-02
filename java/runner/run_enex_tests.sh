#!/usr/bin/env bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# setzt außerdem SRC -> zeigt auf das python-dir mit zusätzlichen shell scripts
TESTENV="enex"
source "$SCRIPT_DIR/f_prepare_tests.sh"
source "$SRC/../test/f_test_utilities.sh"

EPOCH=$(date +%s)

# EnexFilesProcessor
#rm -rf "$SCRIPT_DIR/src/test/resources/$TESTENV/data_test/.jg-evernote/enex-batch/parsed"
#mkdir "$SCRIPT_DIR/src/test/resources/$TESTENV/data_test/.jg-evernote/enex-batch/parsed"
#mvn test -Dtest=EnexFilesProcessorTest
#D1="$SCRIPT_DIR/src/test/resources/$TESTENV/data_source/.jg-evernote/enex-batch/parsed"
#D2="$SCRIPT_DIR/src/test/resources/$TESTENV/data_test/.jg-evernote/enex-batch/parsed"
#compare_folders $D1 $D2 $EPOCH

# EnDocsToCSVsTest
mvn test -Dtest=EnDocsToCSVsTest
D1="$SCRIPT_DIR/src/test/resources/$TESTENV/data_source/.jg-evernote/enex-batch/csv"
D2="$SCRIPT_DIR/src/test/resources/$TESTENV/data_test/.jg-evernote/enex-batch/csv"
compare_folders $D1 $D2 $EPOCH