#!/usr/bin/env bash

set -e

SHELL_SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TESTENV="services"
JAVA_SRC="$(realpath "$SHELL_SRC/../java/runner/src")"
VENV_PATH=~/python-venv/.test-venv
PYTHON_SRC=$(realpath "$SHELL_SRC/../python/src")


source "$SHELL_SRC/test_functions_java/f_prepare_tests.sh"
prepare_tests "$SHELL_SRC" "$TESTENV" "$JAVA_SRC" "$PYTHON_SRC" "$VENV_PATH"

(
    export DATA=$(realpath "$JAVA_SRC/test/resources/$TESTENV/data_test")
    cd "../java/runner"
    mvn test -Dtest=ServicesClientTest
)
