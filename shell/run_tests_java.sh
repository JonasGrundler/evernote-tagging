#!/usr/bin/env bash

set -e

SHELL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"$SHELL_DIR/run_tests_java_services.sh"
"$SHELL_DIR/run_tests_java_enex.sh"