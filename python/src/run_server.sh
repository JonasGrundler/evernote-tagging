#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$SCRIPT_DIR"

MODE="foreground"                 # foreground oder background
DO_INSTALL="false"

if [[ -z "$DATA" ]]; then
  DATA="$HOME"
fi
if [[ -z "$VENV_PATH" ]]; then
  VENV_PATH="$HOME/python-venv/.venv"
fi


while [ $# -gt 0 ]; do
  case "$1" in
    install)
      DO_INSTALL="true"
      shift
      ;;
    --bg|--background)
      MODE="background"
      shift
      ;;
    --venv)
      # nächstes Argument ist der Pfad zur venv
      if [ -z "${2:-}" ]; then
        echo "Fehler: --venv benötigt einen Pfad"
        echo "Usage: $0 [--venv PFAD] [--data PFAD] [install] [--bg]"
        exit 1
      fi
      VENV_PATH="$2"
      shift 2
      ;;
    --data)
      # nächstes Argument ist das DATA-Verzeichnis
      if [ -z "${2:-}" ]; then
        echo "Fehler: --data benötigt einen Pfad"
        echo "Usage: $0 [--venv PFAD] [--data PFAD] [install] [--bg]"
        exit 1
      fi
      DATA="$2"
      shift 2
      ;;
    *)
      echo "Usage: $0 [--venv PFAD] [--data PFAD] [install] [--bg]"
      echo "  $0                               # Vordergrund, Default-Venv & Default-DATA"
      echo "  $0 --venv /pfad/venv             # Vordergrund mit eigener Venv"
      echo "  $0 --data /pfad/data             # Vordergrund mit eigenem DATA-Verzeichnis"
      echo "  $0 --venv /v ... --data /d ...   # Venv + DATA beide gesetzt"
      echo "  $0 install                       # vorher venv/Pakete im Default-Pfad"
      echo "  $0 --bg                          # Hintergrund + warten"
      echo "  $0 --venv /v --data /d install --bg"
      echo "                                   # alles kombinierbar"
      exit 1
      ;;
  esac
done

# Ab hier hast du:
#   - MODE       = foreground | background
#   - DO_INSTALL = "true" | "false"
#   - VENV_PATH  = Pfad zur gewünschten Venv
#   - DATA_DIR   = Pfad zum DATA-Verzeichnis

echo "Starte Server mit folgenden Parametern:"
echo "  MODE        = $MODE"
echo "  DO_INSTALL  = $DO_INSTALL"
echo "  VENV_PATH   = $VENV_PATH"
echo "  DATA        = $DATA"
echo "  SRC         = $SRC"

# Funktionen (start_server_foreground / start_server_managed) laden
# SCRIPT_DIR wird dort benötigt
# VENV_PATH wird dort benötigt
# SRC wird dort benötigt
# DATA wird dort benötigt
source "$SCRIPT_DIR/f_server.sh"

if [[ "$MODE" == "foreground" ]]; then
  start_server_foreground "$DO_INSTALL"
else
  start_server_managed "$DO_INSTALL"
  echo "Server läuft im Hintergrund (PID: $UVICORN_PID)."
  echo "Log: $UVICORN_LOG"
fi