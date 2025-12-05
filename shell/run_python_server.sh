#!/bin/bash

set -e


MODE="foreground"                 # foreground oder background
DO_INSTALL="false"

while [ $# -gt 0 ]; do
  case "$1" in
    install)
      DO_INSTALL="true"; shift ;;
    --bg|--background)
      MODE="background"; shift ;;
    --venv)
      if [ -z "${2:-}" ]; then
        echo "Fehler: --venv benötigt einen Pfad"
        exit 1
      fi
      VENV_PATH="$2"; shift 2 ;;
    --data)
      if [ -z "${2:-}" ]; then
        echo "Fehler: --data benötigt einen Pfad"
        exit 1
      fi
      DATA="$2" shift 2 ;;
    --data_src)
      if [ -z "${2:-}" ]; then
        echo "Fehler: --data_src benötigt einen Pfad"
        exit 1
      fi
      DATA_SRC="$2" shift 2 ;;
    --python_src)
      if [ -z "${2:-}" ]; then
        echo "Fehler: --python_src benötigt einen Pfad"
        exit 1
      fi
      PYTHON_SRC="$2" shift 2 ;;
    *)
      echo "Usage: $0 [--venv PFAD] [--data PFAD] [--data_src PFAD] [--python_src] [install] [--bg]"
      echo "  $0                               # Vordergrund, Default-Venv & Default-DATA & Default-SHELL, Default-DATA_SRC im Vordergrund"
      echo "  $0 --venv /pfad/venv             # eigener Venv"
      echo "  $0 --data /pfad/data             # eigenes DATA-Verzeichnis"
      echo "  $0 --data_src /pfad/data         # eigenes DATA_SRC-Verzeichnis"
      echo "  $0 --python_src /pfad/shell      # eigenes PYTHON_SRC-Verzeichnis"
      echo "  $0 install                       # vorher venv/Pakete im Default-Pfad"
      echo "  $0 --bg                          # Hintergrund + warten"
      exit 1
      ;;
  esac
done

if [[ -z "$DATA" ]]; then
  DATA="$HOME"
fi
if [[ -z "$VENV_PATH" ]]; then
  VENV_PATH="$HOME/python-venv/.venv"
fi
if [[ -z "$SHELL_SRC" ]]; then
  SHELL_SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fi
if [[ -z "$DATA_SRC" ]]; then
  DATA_SRC=$(realpath "$SHELL_SRC/../data")
fi
if [[ -z "$PYTHON_SRC" ]]; then
  PYTHON_SRC=$(realpath "$SHELL_SRC/../python/src")
fi


# Ab hier hast du:

echo "Starte Server mit folgenden Parametern:"
echo "  MODE        = $MODE"
echo "  DO_INSTALL  = $DO_INSTALL"
echo "  VENV_PATH   = $VENV_PATH"
echo "  DATA        = $DATA"
echo "  PYTHON_SRC  = $PYTHON_SRC"
echo "  DATA_SRC    = $DATA_SRC"
echo "  SHELL_SRC  =  $SHELL_SRC"

# Funktionen (start_server_foreground / start_server_managed) laden
source "$SHELL_SRC/functions_python/f_server.sh"
prepare_server "$VENV_PATH" "$PYTHON_SRC" "$SHELL_SRC" "$DATA" "$DATA_SRC" $DO_INSTALL

if [[ "$MODE" == "foreground" ]]; then
  start_server_foreground "$PYTHON_SRC"
else
  start_server_managed "$PYTHON_SRC"
  echo "Server läuft im Hintergrund (PID: $UVICORN_PID)."
  echo "Log: $UVICORN_LOG"
fi