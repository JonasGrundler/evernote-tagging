import os
from pathlib import Path
import local_store
import shutil

MODEL_INFO_PATH = os.path.join(local_store.PATHS[local_store.MAPPINGS], "ModelInfo.properties")

SUFFIX = "suffix"
LAST_PERCENTAGE = "lastPercentage"
MAX_AGE_MS = "maxAgeMs"
H_TRAINING_START_ALLOWED_MIN = "hTrainingStartAllowedMin"
H_TRAINING_START_ALLOWED_MAX = "hTrainingStartAllowedMax"

FILENAME_PIPELINE = "pipeline"
FILENAME_LABELS = "labels"
FILENAME_THRESHOLDS = "thresholds"
FILENAME_REPORT = "report"
FILENAMES: list[dict[str, str]] = []



def load_model_info() -> dict[int, dict[str, str]]:
    models: dict[int, dict[str, str]] = {}

    with open(MODEL_INFO_PATH, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue  # Kommentare / leere Zeilen überspringen

            # Beispiel: "0.suffix=all"
            key, value = line.split("=", 1)
            index_str, field = key.split(".", 1)

            idx = int(index_str)
            if idx not in models:
                models[idx] = {}

            models[idx][field] = parse_value(value)

            if field == SUFFIX:
                while len(FILENAMES) <= idx:
                    FILENAMES.append({})
                FILENAMES[idx][FILENAME_PIPELINE] = f"pipeline_{value}.joblib"
                FILENAMES[idx][FILENAME_LABELS] = f"labels_{value}.joblib"
                FILENAMES[idx][FILENAME_THRESHOLDS] = f"thresholds_{value}.joblib"
                FILENAMES[idx][FILENAME_REPORT] = f"report_{value}.txt"

    return models

def parse_value(s: str):
    s = s.strip()

    # erst int versuchen
    try:
        return int(s)
    except ValueError:
        pass

    # sonst String zurückgeben
    return s

def create_model_info_if_not_exists():
    if not os.path.exists(MODEL_INFO_PATH):
        # Verzeichnis, in dem das aktuelle Script liegt
        SCRIPT_DIR = Path(__file__).resolve().parent

        # Quelldatei: liegt im gleichen Verzeichnis wie das Script
        src = SCRIPT_DIR / "ModelInfo.default.properties"

        # Kopieren (inkl. Metadaten)
        shutil.copy2(src, MODEL_INFO_PATH)

create_model_info_if_not_exists()

MODEL_INFO = load_model_info()