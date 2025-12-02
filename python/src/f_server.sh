# benötigt VENV_PATH von aufrufendem Script (für f_prepare_python.sh)
# benötigt SRC von aufrufendem Script (für cd)
# benötigt DATA von aufrufendem Script (für uvicorn env var)
if [[ -z "$VENV_PATH" ]]; then
  echo "VENV_PATH ist nicht gesetzt!"
  return 1
fi
if [[ -z "$SRC" ]]; then
  echo "SRC ist nicht gesetzt!"
  return 1
fi
if [[ -z "$DATA" ]]; then
  echo "DATA ist nicht gesetzt!"
  return 1
fi

set -e

### --------------------------------------------------------------------
### Basis-Pfade
### --------------------------------------------------------------------

UVICORN_LOG="$SRC/uvicorn.log"

UVICORN_PID=""
TAIL_PID=""
SERVER_TRAP_INSTALLED="${SERVER_TRAP_INSTALLED:-}"

source "$SRC/f_prepare_python.sh"

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

  echo "[run_server] Managed start (background, trap, wait)..."

  prepare_python_call "$1"

  echo "Starte Uvicorn im Hintergrund..."
  if [ -f "$UVICORN_LOG" ]; then rm -f "$UVICORN_LOG"; fi

  export DATA

  OLDPWD="$(pwd)"
  cd "$SRC"
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

  echo "[run_server] Foreground start..."

  prepare_python_call "$1"

  (
    export DATA
    cd "$SRC"
    echo "Starte Uvicorn im Vordergrund (Strg-C zum Beenden)..."
    exec python3 -m uvicorn server:app --reload --host 0.0.0.0 --port 8000
  )
}

prepare_python_call() {
  prepare_python_env "$1"
}