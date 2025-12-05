prepare_tests() {
  local SHELL_SRC=$1
  local TESTENV=$2
  local JAVA_SRC=$3
  local VENV_PATH=$4

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
  if [[ -z "$VENV_PATH" ]]; then
    VENV_PATH="$HOME/python-venv/.venv"
  fi

  TEST="$JAVA_SRC/test/resources/$TESTENV"

  source "$SHELL_SRC/functions_python/f_prepare_tests.sh"
  prepare_tests_env

  # starte den server
  source "$SHELL_SRC/functions_python/f_server.sh"
  start_server_managed
}