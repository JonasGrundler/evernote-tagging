if [[ -z "$TEST" ]]; then
  echo "TEST ist nicht gesetzt!"
  return 1
fi
if [[ -z "$DATA" ]]; then
  echo "DATA ist nicht gesetzt!"
  return 1
fi
if [[ -z "$DATA_SOURCE" ]]; then
  echo "DATA_SOURCE ist nicht gesetzt!"
  return 1
fi

#DO_TRAIN=false
#DO_INFER=false
#DO_IMAGE_TO_TEXT=false
#DO_TROCR=false

source "$TEST/f_prepare_tests.sh"
prepare_tests_env

source "$TEST/f_test_utilities.sh"

run_tests() {
  if $DO_TRAIN; then
    do_training
  fi

  if $DO_INFER; then
    do_infer
  fi

  if $DO_IMAGE_TO_TEXT; then
    do_image_to_text
  fi

  if $DO_TROCR; then
    do_trocr
  fi
}

do_training () {
  echo "Führe Trainings-Tests durch..."

  EPOCH=$(date +%s)

  # Testaufrufe zum Trainieren der Modelle
  curl -X POST "http://localhost:8000/train" -H "Content-Type: application/json" -d '{"scenario": 0}'
  curl -X POST "http://localhost:8000/train" -H "Content-Type: application/json" -d '{"scenario": 1}'
  curl -X POST "http://localhost:8000/train" -H "Content-Type: application/json" -d '{"scenario": 2}'
    
  echo ""
 
  compare_folders "$DATA_SOURCE/.jg-evernote/model-artifacts" "$DATA/.jg-evernote/model-artifacts" $EPOCH
}

do_infer () {
  echo "Führe Inferenz-Tests durch..."
  INFER_TABLE="$TEST/test_support/infer_table.txt"
  BASE_NOTES_DIR="$DATA/.jg-evernote/internet-single/notes"

  all_ok=true

  # Tabelle zeilenweise lesen
  while IFS='|' read -r id scenario expected_json; do

    expected_json=${expected_json%$'\r'}

    csv_file="$BASE_NOTES_DIR/$id.csv"

    if [ ! -f "$csv_file" ]; then
      echo "⚠ CSV fehlt: $csv_file"
      all_ok=false
      continue
    fi

    echo "Teste id=$id, scenario=$scenario"

    # JSON-Payload bauen (ohne Anführungs-Quoting-Hölle)
    payload=$(printf '{"scenario": %s, "csv_path": "%s"}' "$scenario" "$csv_file")

    # Service aufrufen
    response=$(curl -s -X POST "http://localhost:8000/infer" \
      -H "Content-Type: application/json" \
      -d "$payload")

    # --- einfache Variante: 1:1 String-Vergleich ---
    if [ "$response" = "$expected_json" ]; then
      echo "  ✅ OK"
    else
      echo "  ❌ Mismatch"
      echo "    expected: $expected_json"
      echo "    got:      $response"
      all_ok=false
    fi
    echo

  done < "$INFER_TABLE"

  if $all_ok; then
    echo "infer ok ✅"
  else
    echo "infer failed ❌"
    exit 1
  fi
}

do_image_to_text () {

  EPOCH=$(date +%s)

  echo "Führe Image-to-Text-Tests durch..."
  curl -X POST "http://localhost:8000/ocr_images" \
  -H "Content-Type: application/json" \
  -d '{"images": 4}'
  echo ""

  compare_folders "$DATA_SOURCE/.jg-evernote/images-tmp" "$DATA/.jg-evernote/images-tmp" $EPOCH txt
}

do_trocr() {
  echo "Führe TrOCR-Tests durch..."

  TROCR_TABLE="$TEST/test_support/trocr_table.txt"
  BASE_PNG_DIR="$DATA/.jg-evernote/images-tmp"
  all_ok=true

  # Tabelle zeilenweise lesen
  while IFS='|' read -r id expected_json; do

    expected_json=${expected_json%$'\r'}

    png_file=""$BASE_PNG_DIR/$id.png""

    if [ ! -f "$png_file" ]; then
      echo "⚠ PNG fehlt: $png_file"
      all_ok=false
      continue
    fi

    echo "Teste id=$id"

    # JSON-Payload bauen (ohne Anführungs-Quoting-Hölle)
    response=$(curl -X POST "http://localhost:8000/ocr" \
    -H "accept: application/json" \
    -F "file=@${png_file};filename=image.png;type=application/octet-stream")

    # --- einfache Variante: 1:1 String-Vergleich ---
    if [ "$response" = "$expected_json" ]; then
      echo "  ✅ OK"
    else
      echo "  ❌ Mismatch"
      echo "    expected: $expected_json"
      echo "    got:      $response"
      all_ok=false
    fi
    echo

  done < "$TROCR_TABLE"

  if $all_ok; then
    echo "trocr ok ✅"
  else
    echo "trocr failed ❌"
    exit 1
  fi

}