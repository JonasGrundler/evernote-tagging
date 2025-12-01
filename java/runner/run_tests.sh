#!/usr/bin/env bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SRC="SCRIPT_DIR$/../../python/src"
TEST="SCRIPT_DIR$/src/test/resources"

# ... initialisiert DATA (.../data_test) mit den Inhalten von DATA_SOURCE (.../data_source)
source $SRC/f_prepare_tests.sh

export DATA
$SRC/run_server.sh --bg

mvn test
