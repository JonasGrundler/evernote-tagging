#!/usr/bin/env bash

set -e

SHELL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"$SHELL_DIR/run_tests_java"
"$SHELL_DIR/run_tests_python_server"