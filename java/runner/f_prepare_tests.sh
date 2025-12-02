SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -z "$SCRIPT_DIR" ]]; then
  echo "SCRIPT_DIR not set"
  exit 1
fi
if [[ -z "$VENV_PATH" ]]; then
  VENV_PATH="$HOME/python-venv/.venv"
fi
SRC="$SCRIPT_DIR/../../python/src"
TEST="$SCRIPT_DIR/src/test/resources"

# ... initialisiert DATA (.../data_test) mit den Inhalten von DATA_SOURCE (.../data_source)
source "$SRC/../test/f_prepare_tests.sh"
prepare_tests_env

# starte den server
source "$SRC/f_server.sh"
start_server_managed