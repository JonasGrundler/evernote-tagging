#!/usr/bin/env bash

set -e

SHELL_SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TESTENV="enex"
JAVA_SRC="$(realpath "$SHELL_SRC/../java/runner/src")"
VENV_PATH=~/python-venv/.test-venv
PYTHON_SRC=$(realpath "$SHELL_SRC/../python/src")

echo "SHELL_SRC=$SHELL_SRC"
echo "TESTENV=$TESTENV"
echo "JAVA_SRC=$JAVA_SRC"
echo "VENV_PATH=$VENV_PATH"
echo "PYTHON_SRC=$PYTHON_SRC"


# setzt außerdem SRC -> zeigt auf das python-dir mit zusätzlichen shell scripts
source "$SHELL_SRC/test_functions_java/f_prepare_tests.sh"
prepare_tests "$SHELL_SRC" "$TESTENV" "$JAVA_SRC" "$PYTHON_SRC" "$VENV_PATH"

# fürs compare
source "$SHELL_SRC/test_functions/f_test_utilities.sh"

EPOCH=$(date +%s)
sleep 1

DATA=$(realpath "$JAVA_SRC/test/resources/$TESTENV/data_test")

# EnexFilesProcessor
rm -rf "$JAVA_SRC/test/resources/$TESTENV/data_test/.jg-evernote/enex-batch/parsed"
mkdir "$JAVA_SRC/test/resources/$TESTENV/data_test/.jg-evernote/enex-batch/parsed"
(
    export DATA
    cd "../java/runner"
    mvn test -Dtest=EnexFilesProcessorTest
)
D1="$JAVA_SRC/test/resources/$TESTENV/data_source/.jg-evernote/enex-batch/parsed"
D2="$JAVA_SRC/test/resources/$TESTENV/data_test/.jg-evernote/enex-batch/parsed"
compare_folders $D1 $D2 $EPOCH

# EnDocsToCSVsTest
rm -rf "$JAVA_SRC/test/resources/$TESTENV/data_test/.jg-evernote/enex-batch/csv"
mkdir "$JAVA_SRC/test/resources/$TESTENV/data_test/.jg-evernote/enex-batch/csv"
(
    export DATA
    cd "../java/runner"
    mvn test -Dtest=EnDocsToCSVsTest
)
D1="$JAVA_SRC/test/resources/$TESTENV/data_source/.jg-evernote/enex-batch/csv"
D2="$JAVA_SRC/test/resources/$TESTENV/data_test/.jg-evernote/enex-batch/csv"
compare_folders $D1 $D2 $EPOCH
