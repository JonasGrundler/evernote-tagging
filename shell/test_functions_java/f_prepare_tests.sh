prepare_tests() {
  local SHELL_SRC=$1
  local TESTENV=$2
  local JAVA_SRC=$3
  local PYTHON_SRC=$4
  local VENV_PATH=$5

  if [[ -z "$SHELL_SRC" ]]; then
    echo "SHELL_SRC not set"
    exit 1
  fi
  if [[ -z "$TESTENV" ]]; then
    echo "TESTENV not set"
    exit 1
  fi
  if [[ -z "$JAVA_SRC" ]]; then
    echo "JAVA_SRC not set"
    exit 1
  fi
  if [[ -z "$PYTHON_SRC" ]]; then
    echo "PYTHON_SRC not set"
    exit 1
  fi
  if [[ -z "$VENV_PATH" ]]; then
    VENV_PATH="$HOME/python-venv/.venv"
  fi

  local TEST="$JAVA_SRC/test/resources/$TESTENV"
  local DATA="$TEST/data_test"
  local DATA_SRC="$TEST/data_source"

  source "$SHELL_SRC/test_functions/f_prepare_tests.sh"
  prepare_tests_env "$DATA"

  # starte den server
  source "$SHELL_SRC/functions_python/f_server.sh"
  prepare_server "$VENV_PATH" "$PYTHON_SRC" "$SHELL_SRC" "$DATA" "$DATA_SRC" $DO_INSTALL

  start_server_managed "$PYTHON_SRC" "$DO_INSTALL"
}