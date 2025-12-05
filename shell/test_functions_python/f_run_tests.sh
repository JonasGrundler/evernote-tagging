prepare() {

    if [[ $# < 4 ]]; then
    echo "Aufruf: prepare TEST_DIR DATA DATA_SOURCE SHELL_SRC" >&2
    return 1
  fi

  local TEST_DIR=$1
  local DATA=$2
  local DATA_SOURCE=$3
  local SHELL_SRC=$4

  if [[ -z "$TEST_DIR" ]]; then
    echo "TEST_DIR ist nicht gesetzt!"
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
  if [[ -z "$SHELL_SRC" ]]; then
    echo "SHELL_SRC ist nicht gesetzt!"
    return 1
  fi

  #DO_TRAIN=false
  #DO_INFER=false
  #DO_IMAGE_TO_TEXT=false
  #DO_TROCR=false

  source "$SHELL_SRC/test_functions_python/f_prepare_tests.sh"
  prepare_tests_env

  source "$SHELL_SRC/test_functions/f_test_utilities.sh"
}

run_tests() {

  if [[ $# < 4 ]]; then
    echo "Aufruf: run_tests DO_TRAIN DO_INFER DO_IMAGE_TO_TEXT DO_TROCR" >&2
    return 1
  fi

  local DO_TRAIN=$1
  local DO_INFER=$2
  local DO_IMAGE_TO_TEXT=$3
  local DO_TROCR=$4

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

  if [[ $# < 2 ]]; then
    echo "Aufruf: do_training DATA_SOURCE DATA" >&2
    return 1
  fi

  local DATA_SOURCE=$1
  local DATA=$2

  echo "Führe Trainings-Tests durch..."

  EPOCH=$(date +%s)
  sleep 1
  
  # Testaufrufe zum Trainieren der Modelle
  curl -X POST "http://localhost:8000/train" -H "Content-Type: application/json" -d '{"scenario": 0}'
  curl -X POST "http://localhost:8000/train" -H "Content-Type: application/json" -d '{"scenario": 1}'
  curl -X POST "http://localhost:8000/train" -H "Content-Type: application/json" -d '{"scenario": 2}'
    
  echo ""
 
  compare_folders "$DATA_SOURCE/.jg-evernote/model-artifacts" "$DATA/.jg-evernote/model-artifacts" $EPOCH
}

do_infer () {

  if [[ $# < 2 ]]; then
    echo "Aufruf: do_infer DATA_SOURCE DATA" >&2
    return 1
  fi

  local TEST_DIR=$1
  local BASE_NOTES_DIR=$2

  echo "Führe Inferenz-Tests durch..."
  local INFER_TABLE="$TEST_DIR/test_support/infer_table.txt"
  local BASE_NOTES_DIR="$DATA/.jg-evernote/internet-single/notes"

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

  if [[ $# < 2 ]]; then
    echo "Aufruf: do_training DATA_SOURCE DATA" >&2
    return 1
  fi

  local DATA_SOURCE=$1
  local DATA=$2


  EPOCH=$(date +%s)

  echo "Führe Image-to-Text-Tests durch..."
  curl -X POST "http://localhost:8000/ocr_images" \
  -H "Content-Type: application/json" \
  -d '{"images": 4}'
  echo ""

  compare_folders "$DATA_SOURCE/.jg-evernote/images-tmp" "$DATA/.jg-evernote/images-tmp" $EPOCH txt
}

do_trocr() {

  if [[ $# < 2 ]]; then
    echo "Aufruf: do_trocr TEST_DIR DATA" >&2
    return 1
  fi

  local TEST_DIR=$1
  local DATA=$2

  echo "Führe TrOCR-Tests durch..."

  local TROCR_TABLE="$TEST_DIR/test_support/trocr_table.txt"
  local BASE_PNG_DIR="$DATA/.jg-evernote/images-tmp"
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