#!/bin/bash

set -e

SHELL_SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_DIR=$(realpath "$SHELL_SRC/../python/test")
PYTHON_SRC=$(realpath "$SHELL_SRC/../python/src")
VENV_PATH=~/python-venv/.test-venv
DATA=$(realpath "$TEST_DIR/data_test")
DATA_SRC=$(realpath "$TEST_DIR/data_source")

### --------------------------------------------------------------------
### 0) Aktionen auswerten
### --------------------------------------------------------------------
DO_INSTALL=false
DO_TRAIN=false
DO_INFER=false
DO_IMAGE_TO_TEXT=false
DO_TROCR=false

usage() {
  echo "Usage: $0 [install] [train] [infer] [image-to-text] [trocr]"
  echo "       $0               # führt alle Schritte aus"
  echo "       $0 install       # nur Python-Pakete installieren"
  echo "       $0 train         # nur Training (Server + /train)"
  echo "       $0 infer         # nur Inferenz-Tests (Server + /infer)"
  echo "       $0 image-to-text # nur image-to-text-Tests (Server + /ocr_images)"
  echo "       $0 trocr         # nur TrOCR-Tests (Server + /ocr)"
  exit 1
}

if [ $# -eq 0 ]; then
  # keine Argumente -> alles machen
  DO_INSTALL=false
  DO_TRAIN=true
  DO_INFER=true
  DO_IMAGE_TO_TEXT=true
  DO_TROCR=true
else
  for arg in "$@"; do
    case "$arg" in
      1|install)        DO_INSTALL=true ;;
      2|train)          DO_TRAIN=true ;;
      3|infer)          DO_INFER=true ;;
      4|image-to-text)  DO_IMAGE_TO_TEXT=true ;;
      5|trocr)          DO_TROCR=true ;;
      *) echo "Unbekannte Aktion: $arg"; usage ;;
    esac
  done
fi

#echo "Bereite Tests vor mit folgenden Parametern:"
#echo "  TEST             = $TEST_DIR"

#source "$SHELL_SRC/test_functions/f_prepare_tests.sh"
#prepare_tests_env "$TEST_DIR"

echo "Starte Server mit folgenden Parametern:"
echo "  VENV_PATH        = $VENV_PATH"
echo "  PYTHON_SRC       = $PYTHON_SRC"
echo "  SHELL_SRC        = $SHELL_SRC"
echo "  DATA             = $DATA"
echo "  DATA_SRC         = $DATA_SCR"
echo "  DO_INSTALL       = $DO_INSTALL"

source "$SHELL_SRC/functions_python/f_server.sh"
prepare_server "$VENV_PATH" "$PYTHON_SRC" "$SHELL_SRC" "$DATA" "$DATA_SRC" $DO_INSTALL

start_server_managed "$PYTHON_SRC" "$DO_INSTALL"


echo "Starte Tests mit folgenden Parametern:"
echo "  DO_TRAIN         = $DO_TRAIN"
echo "  DO_INFER         = $DO_INFER"
echo "  DO_IMAGE_TO_TEXT = $DO_IMAGE_TO_TEXT"
echo "  DO_TROCR         = $DO_TROCR"

source "$SHELL_SRC/test_functions_python/f_run_tests.sh"
run_tests $DO_TRAIN $DO_INFER $DO_IMAGE_TO_TEXT $DO_TROCR "$TEST_DIR" "$SHELL_SRC"