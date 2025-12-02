#!/usr/bin/env bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "$SCRIPT_DIR/f_prepare_tests.sh"

rm -rf "$SCRIPT_DIR/src/test/resources/data_test/.jg-evernote/enex-batch/parsed"
mkdir "$SCRIPT_DIR/src/test/resources/data_test/.jg-evernote/enex-batch/parsed"

mvn test -Dtest=EnexFilesProcessorTest