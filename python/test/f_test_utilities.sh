compare_folders() {
  # 1) Erstes Argument = DIR1, zweites = DIR2
  local DIR1="$1"
  local DIR2="$2"
  local CUT_EPOCH=$3

  echo "Vergleiche Ordner '$DIR1' und '$DIR2' (Dateien neuer als Epoch $CUT_EPOCH)..."

  local all_ok

  if [ ! -d $DIR1 ] || [ ! -d $DIR2 ]; then
    echo "Verzeichnis '$DIR1' oder '$DIR2' existiert nicht!" >&2
    all_ok=false
  else
    all_ok=true
    sleep 1
    # wenn weniger als 2 Argumente → Fehler
    if [[ -z "$DIR1" || -z "$DIR2" ]]; then
      echo "Usage: compare_folders DIR1 DIR2 [ext1 ext2 ...]" >&2
      return 1
    fi

    # die restlichen Argumente sind die erlaubten Endungen (ohne Punkt)
    shift 3
    local exts=("$@")   # kann leer sein → dann alle Dateien

    # Alle Dateien in DIR2 durchgehen
    while IFS= read -r -d '' f2; do
      # ggf. nach Endungen filtern
      if ((${#exts[@]} > 0)); then
        local ext match=false
        ext="${f2##*.}"           # alles nach letztem Punkt

        for e in "${exts[@]}"; do
          if [[ "$ext" == "$e" ]]; then
            match=true
            break
          fi
        done

        # wenn keine Extension passt → nächste Datei
        $match || continue
      fi

      # relativen Pfad ermitteln
      local rel="${f2#$DIR2/}"
      local f1="$DIR1/$rel"

      # 1) Gegenstück in DIR1 vorhanden?
      if [ ! -f "$f1" ]; then
        echo "Fehlt in DIR1: $rel"
        all_ok=false
        break
      fi

      # 2) Inhalt gleich?
      if ! cmp -s "$f1" "$f2"; then
        echo "Inhalt unterschiedlich: $rel"
        all_ok=false
        break
      fi

      # 3) mtime von f2 > CUT_EPOCH?
      local mtime_f2
      mtime_f2=$(stat -c %Y "$f2")
      if [ "$mtime_f2" -le "$CUT_EPOCH" ]; then
        echo "Zu alt (DIR2): $rel (mtime=$mtime_f2, cut=$CUT_EPOCH)"
        all_ok=false
        break
      fi

    done < <(find "$DIR2" -type f -print0)
  fi

  if $all_ok; then
    echo "compare ok ✅"
  else
    echo "compare failed ❌"
    exit 1
  fi
}