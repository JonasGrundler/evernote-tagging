prepare_tests_env() {

  local DATA=$1

  if [[ -z "$DATA" ]]; then
    echo "DATA ist nicht gesetzt!"
    return 1
  fi

  echo "Verzeichnis data_test vorbereiten (leeren und neu erstellen)..."
  if [ -d "$DATA/.jg-evernote" ]; then rm -rf "$DATA/.jg-evernote"; fi

  mkdir -p "$DATA"
}