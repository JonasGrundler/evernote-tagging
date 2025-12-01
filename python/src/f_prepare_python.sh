# benötigt SRC von aufrufendem Script
# benötigt VENV_PATH von aufrufendem Script
if [[ -z "$SRC" ]]; then
  echo "SRC_DIR ist nicht gesetzt!"
  return 1
fi
if [[ -z "$VENV_PATH" ]]; then
  echo "VENV_PATH ist nicht gesetzt!"
  return 1
fi

set -e

prepare_python_env() {
  local DO_INSTALL="${1:-false}"  # optional: "true" oder "install"

  if [[ -z "$VENV_PATH" ]]; then
    echo "Usage: prepare_python_env VENV_PATH [install|true]"
    return 1
  fi

  if [[ "$DO_INSTALL" == "true" || "$DO_INSTALL" == "install" ]]; then
    echo "Python-Venv neu anlegen unter: $VENV_PATH"
    rm -rf "$VENV_PATH"
    python3 -m venv "$VENV_PATH"
  else
    echo "Nutze bestehende Venv unter: $VENV_PATH (kein Neuaufsetzen)"
  fi

  # aktivieren
  # (falls das schiefgeht, abbrechen)
  if [[ ! -f "$VENV_PATH/bin/activate" ]]; then
    echo "Fehler: $VENV_PATH/bin/activate nicht gefunden"
    return 1
  fi

  # aktiviert die venv im aktuellen Shell-Kontext
  # (wichtig: kein 'bash -c', sondern direkt source)
  # shellcheck disable=SC1090
  source "$VENV_PATH/bin/activate"

  if [[ "$DO_INSTALL" == "true" || "$DO_INSTALL" == "install" ]]; then
    echo "Installiere Python-Pakete..."
    pip install --no-cache-dir -r "$SRC/requirements.cpu.txt"
    pip install --no-cache-dir "torch==2.9.1" --index-url https://download.pytorch.org/whl/cpu
    pip install --no-cache-dir "torchvision==0.24.1" --index-url https://download.pytorch.org/whl/cpu
  fi
}
