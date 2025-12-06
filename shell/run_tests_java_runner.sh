#!/usr/bin/env bash

set -e

SHELL_SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TESTENV="services"
JAVA_SRC="$(realpath "$SHELL_SRC/../java/runner/src")"
VENV_PATH=~/python-venv/.test-venv
PYTHON_SRC=$(realpath "$SHELL_SRC/../python/src")
DATA=$(realpath "$JAVA_SRC/test/resources/$TESTENV/data_test")
EN_USER_CONF_NB="98 Automation-Test"
ABLAGE="98 Ablage"

source "$SHELL_SRC/test_functions_java/f_prepare_tests.sh"
prepare_tests "$SHELL_SRC" "$TESTENV" "$JAVA_SRC" "$PYTHON_SRC" "$VENV_PATH"

(
    export EN_USER_CONF_NB
    export ABLAGE
    export DATA
    cd "../java/runner"
    mvn test -Dtest=EvernoteTaggingTest
)
