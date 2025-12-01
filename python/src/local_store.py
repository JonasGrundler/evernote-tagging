import os
from pathlib import Path

PATHS: dict[str, str] = {}

BASE = "base"

MODEL_ARTIFACTS = "model_artifacts"

IMAGES_TMP = "images_tmp"

MAPPINGS = "mappings"

ENEX_BATCH = "enex_batch"
ENEX_BATCH_CSV = "enex_batch_csv"
ENEX_BATCH_EXPORTS = "enex_batch_exports"
ENEX_BATCH_PARSED = "enex_batch_parsed"

INTERNET_SINGLE = "internet_single"
INTERNET_SINGLE_NOTES = "internet_single_notes"
INTERNET_SINGLE_OAUTH = "internet_single_oauth"
INTERNET_SINGLE_TAGGED = "internet_single_tagged"
INTERNET_SINGLE_CSV = "internet_single_csv"
INTERNET_SINGLE_TRAINING = "internet_single_training"

PATHS[BASE] = os.path.join(os.getenv("DATA", Path.home()), ".jg-evernote")

PATHS[MODEL_ARTIFACTS] = os.path.join(PATHS[BASE], "model-artifacts")

PATHS[ENEX_BATCH] = os.path.join(PATHS[BASE], "enex-batch")
PATHS[ENEX_BATCH_CSV] = os.path.join(PATHS[ENEX_BATCH], "csv")
PATHS[ENEX_BATCH_EXPORTS] = os.path.join(PATHS[ENEX_BATCH], "exports")
PATHS[ENEX_BATCH_PARSED] = os.path.join(PATHS[ENEX_BATCH], "parsed")

PATHS[INTERNET_SINGLE] = os.path.join(PATHS[BASE], "internet-single")
PATHS[INTERNET_SINGLE_NOTES] = os.path.join(PATHS[INTERNET_SINGLE], "notes")
PATHS[INTERNET_SINGLE_OAUTH] = os.path.join(PATHS[INTERNET_SINGLE], "exports")
PATHS[INTERNET_SINGLE_TAGGED] = os.path.join(PATHS[INTERNET_SINGLE], "tagged")
PATHS[INTERNET_SINGLE_CSV] = os.path.join(PATHS[INTERNET_SINGLE], "csv")
PATHS[INTERNET_SINGLE_TRAINING] = os.path.join(PATHS[INTERNET_SINGLE], "training")

PATHS[MAPPINGS] = os.path.join(PATHS[BASE], "mappings")

PATHS[IMAGES_TMP] = os.path.join(PATHS[BASE], "images-tmp")

for key, path in PATHS.items():
    Path(path).mkdir(parents=True, exist_ok=True)

print(f"Ensured paths for local store '{PATHS[BASE]}'.")