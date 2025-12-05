#!/usr/bin/env bash

set -e

SHELL_SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TESTENV="services"
JAVA_SRC="$(realpath "$SHELL_SRC/../java/runner/src")"
VENV_PATH=~/python-venv/.test-venv

source "$SHELL_SRC/test_functions_java/f_prepare_tests.sh"
prepare_tests "$SHELL_SRC" "$TESTENV" "$JAVA_SRC" "$VENV_PATH"

mvn test -Dtest=ServicesClientTest
