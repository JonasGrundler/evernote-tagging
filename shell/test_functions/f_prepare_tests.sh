# benötigt TEST von aufrufendem Script

prepare_tests_env() {

  local TEST=$1

  if [[ -z "$TEST" ]]; then
    echo "TEST ist nicht gesetzt!"
    return 1
  fi

  echo "Checke, ob das Verzeichnis TEST existiert..."
  if [ ! -d "$TEST" ]; then
    echo "Verzeichnis '$TEST' existiert nicht – breche ab."
    exit 1
  fi

  local DATA_SOURCE="$TEST/data_source"
  echo "Checke, ob das Verzeichnis data_source existiert..."
  if [ ! -d "$DATA_SOURCE" ]; then
    echo "Verzeichnis '$DATA_SOURCE' existiert nicht – breche ab."
    exit 1
  fi

  echo "Verzeichnis data_test vorbereiten..."
  local DATA="$TEST/data_test"
  if [ -d "$DATA" ]; then rm -rf "$DATA"; fi

  mkdir -p "$DATA"
  cp -r "$DATA_SOURCE/." "$DATA"
  echo "$DATA_SOURCE/." "$DATA"
}