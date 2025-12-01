# benötigt TEST von aufrufendem Script

if [[ -z "$TEST" ]]; then
  echo "TEST ist nicht gesetzt!"
  return 1
fi

prepare_tests_env() {

  echo "Checke, ob das Verzeichnis TEST existiert..."
  if [ ! -d "$TEST" ]; then
    echo "Verzeichnis '$TEST' existiert nicht – breche ab."
    exit 1
  fi

  DATA_SOURCE="$TEST/data_source"
  echo "Checke, ob das Verzeichnis data_source existiert..."
  if [ ! -d "$DATA_SOURCE" ]; then
    echo "Verzeichnis '$DATA_SOURCE' existiert nicht – breche ab."
    exit 1
  fi

  echo "Verzeichnis data_test vorbereiten..."
  DATA="$TEST/data_test"
  if [ -d "$DATA" ]; then rm -rf "$DATA"; fi

  mkdir -p "$DATA"
  cp -r "$DATA_SOURCE/." "$DATA"
  echo "$DATA_SOURCE/." "$DATA"
}