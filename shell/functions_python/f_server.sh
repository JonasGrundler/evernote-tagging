prepare_server() {
  echo "1=${1}, 2=${2}, 3=${3}, 4=${4}, 5=${5}"
  local VENV_PATH="$1"
  local PYTHON_SRC="$2"
  local SHELL_SRC="$3"
  local DATA="$4"
  local DATA_SRC="$5"
  local DO_INSTALL="${6:-false}"

  if [[ $# < 5 ]]; then
    echo "Aufruf: prepare_server VENV_PATH PYTHON_SRC SHELL_SRC DATA DATA_SRC [DO_INSTALL:True/False]" >&2
    return 1
  fi

  set -e

  ### --------------------------------------------------------------------
  ### Basis-Pfade
  ### --------------------------------------------------------------------

  UVICORN_PID=""
  TAIL_PID=""
  SERVER_TRAP_INSTALLED="${SERVER_TRAP_INSTALLED:-}"
  
  echo "DATA_SRC=${DATA_SRC}"
  echo "DATA=${DATA}"

  source "$SHELL_SRC/functions/f_server_utilities.sh"
  echo "DATA_SRC=${DATA_SRC}"
  echo "DATA=${DATA}"
  prepare_data_dir "$DATA_SRC" "$DATA"

  source "$SHELL_SRC/functions_python/f_prepare.sh"
  prepare_python_env "$VENV_PATH" "$PYTHON_SRC" "$DO_INSTALL"

  export DATA
}

cleanup_server() {
  echo "Cleanup Server..."
  if [[ -n "${UVICORN_PID:-}" ]] && kill -0 "$UVICORN_PID" 2>/dev/null; then
    echo "Beende Uvicorn (PID $UVICORN_PID)..."
    kill -INT "$UVICORN_PID" 2>/dev/null || true
    echo "Beende tail (PID $TAIL_PID)..."
    kill -9 "$TAIL_PID" 2>/dev/null || true
  fi
}

install_trap_once() {
  if [[ -z "$SERVER_TRAP_INSTALLED" ]]; then
    trap cleanup_server EXIT
    SERVER_TRAP_INSTALLED=1
  fi
}

### --------------------------------------------------------------------
### 1) Server im Hintergrund, mit Warten & trap (für Scripts)
### --------------------------------------------------------------------
start_server_managed() {

  local PYTHON_SRC="$1"
  local UVICORN_LOG="$PYTHON_SRC/uvicorn.log"

  echo "[run_server] Managed start (background, trap, wait)..."

  echo "Starte Uvicorn im Hintergrund..."
  if [ -f "$UVICORN_LOG" ]; then rm -f "$UVICORN_LOG"; fi

  OLDPWD="$(pwd)"
  cd "$PYTHON_SRC"
  touch "$UVICORN_LOG"
  python3 -m uvicorn server:app --reload --host 0.0.0.0 --port 8000 > "$UVICORN_LOG" 2>&1 &
  UVICORN_PID=$!
  tail -f "$UVICORN_LOG" &
  TAIL_PID=$!
  cd "$OLDPWD"

  install_trap_once

  # Warten bis Server "ready" ist
  while true; do
    echo "Server ist noch nicht bereit ..."
    if [ -f "$UVICORN_LOG" ] && grep -q "Application startup complete." "$UVICORN_LOG"; then
      echo "Server ist bereit ✅ (PID: $UVICORN_PID)"
      break
    fi

    if ! kill -0 "$UVICORN_PID" 2>/dev/null; then
      echo "❌ Uvicorn-Prozess ist vorzeitig beendet." >&2
      return 1
    fi

    sleep 2
  done

  # Funktion gibt zurück, aufrufendes Script läuft weiter
}

### --------------------------------------------------------------------
### 2) Server im Vordergrund (für interaktiv, Strg-C)
### --------------------------------------------------------------------
start_server_foreground() {

  local PYTHON_SRC="$1"

  echo "[run_server] Foreground start in $PYTHON_SRC ..."

  (
    cd "$PYTHON_SRC"
    echo "Starte Uvicorn im Vordergrund (Strg-C zum Beenden)..."
    exec python3 -m uvicorn server:app --reload --host 0.0.0.0 --port 8000
  )
}