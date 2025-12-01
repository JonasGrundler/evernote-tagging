if [[ -z "$TEST" ]]; then
  echo "TEST ist nicht gesetzt!"
  return 1
fi
if [[ -z "$DATA" ]]; then
  echo "TEST ist nicht gesetzt!"
  return 1
fi

#DO_TRAIN=false
#DO_INFER=false
#DO_IMAGE_TO_TEXT=false
#DO_TROCR=false

source "$TEST/f_prepare_tests.sh"
prepare_tests_env

run_tests() {
  if $DO_TRAIN; then
      echo "Führe Trainings-Tests durch..."
      # Testaufrufe zum Trainieren der Modelle
      curl -X POST "http://localhost:8000/train" -H "Content-Type: application/json" -d '{"scenario": 0}'
      curl -X POST "http://localhost:8000/train" -H "Content-Type: application/json" -d '{"scenario": 1}'
      curl -X POST "http://localhost:8000/train" -H "Content-Type: application/json" -d '{"scenario": 2}'
      echo ""
  fi

  if $DO_INFER; then
      echo "Führe Inferenz-Tests durch..."
      # Testaufrufe für Inferenz
      for csv_file in "$DATA"/.jg-evernote/internet-single/notes/*.csv; do
      # falls kein Match -> Literal, überspringen
      [ -e "$csv_file" ] || continue

          for scenario in 0 1 2; do
              echo "Rufe /infer auf: scenario=$scenario, csv_path=$csv_file"
              curl -X POST "http://localhost:8000/infer" \
              -H "Content-Type: application/json" \
              -d "{\"scenario\": $scenario, \"csv_path\": \"${csv_file}\"}"
              echo ""
          done
      done
  fi

  if $DO_IMAGE_TO_TEXT; then
      echo "Führe Image-to-Text-Tests durch..."
      curl -X POST "http://localhost:8000/ocr_images" \
      -H "Content-Type: application/json" \
      -d '{"images": 4}'
      echo ""
  fi

  if $DO_TROCR; then
      curl -X POST "http://localhost:8000/ocr" \
        -H "accept: application/json" \
      -F "file=@${DATA}/.jg-evernote/images-tmp/4.png;filename=image.png;type=application/octet-stream"
      echo ""
  fi
}