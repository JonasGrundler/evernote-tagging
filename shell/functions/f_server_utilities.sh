prepare_data_dir() {
  local SRC=$1
  local DST=$2

  if [[ -z "$SRC" || -z "$DST" ]]; then
    echo "Usage: copy_missing SRC_DIR DST_DIR" >&2
    return 1
  fi
  if [[ ! -d "$SRC" ]]; then
    echo "Source directory does not exist: $SRC" >&2
    return 1
  fi

  # Wir laufen im SRC-Verzeichnis, damit wir relative Pfade haben
  echo "prepare_data_dir: Kopiere von $SRC nach $DST"
  ( 
    cd "$SRC" || exit 1

    find . -type f -print0 | while IFS= read -r -d '' f; do
      # f ist z.B. ./sub/dir/file.txt → rel = sub/dir/file.txt
      local rel="${f#./}"
      local target="$DST/$rel"

      if [[ ! -e "$target" ]]; then
        mkdir -p "$(dirname "$target")"
        cp -p "$rel" "$target"
        echo "kopiert: $rel"
      fi
    done
  )
}