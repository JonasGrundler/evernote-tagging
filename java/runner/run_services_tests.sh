#!/usr/bin/env bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

TESTENV="services"
source "$SCRIPT_DIR/f_prepare_tests.sh"

mvn test -Dtest=ServicesClientTest
