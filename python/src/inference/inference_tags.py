import os
import numpy as np
import pandas as pd
import joblib
import sys
import io
import argparse
import local_store
import model_info

# ==========================
# 0) CONFIG
# ==========================
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")


# Spalten im CSV
# id,year,created,filename,title,author,text,tags
ID_COL       = "id"
YEAR_COL     = "year"
CREATED_COL  = "created"
FILENAME_COL = "filename"
TITLE_COL    = "title"
AUTHOR_COL   = "author"
TEXT_COL     = "text"
TAGS_COL     = "tags"  # optional, falls im Input schon vorhanden

TITLE_BOOST   = 2
AUTHOR_PREFIX = "__AUTHOR_"

def load_model_artifacts(base_dir: str, fn_pipeline: str, fn_labels: str, fn_thresholds: str) -> tuple[object, object, np.ndarray]:
    base_dir = local_store.PATHS[local_store.MODEL_ARTIFACTS]

    pipeline_path   = os.path.join(base_dir, fn_pipeline)
    labels_path     = os.path.join(base_dir, fn_labels)
    thresholds_path = os.path.join(base_dir, fn_thresholds)

    pipe = joblib.load(pipeline_path)
    mlb  = joblib.load(labels_path)
    raw_thresholds = joblib.load(thresholds_path)
    thresholds = np.asarray(raw_thresholds, dtype=float).ravel()

    return pipe, mlb, thresholds

# ==========================
# 1) MODELLE LADEN
# ==========================
PIPELINE   = [None] * (len(model_info.MODEL_INFO))
MLB        = [None] * (len(model_info.MODEL_INFO))
THRESHOLDS = [None] * (len(model_info.MODEL_INFO))

print("[Info] Lade Modelle...")
for idx in model_info.MODEL_INFO:
    print(f"  Modell {idx}: Suffix = {model_info.MODEL_INFO[idx][model_info.SUFFIX]}")
    pipe, mlb, th = load_model_artifacts(
        local_store.PATHS[local_store.MODEL_ARTIFACTS], 
        model_info.FILENAMES[idx][model_info.FILENAME_PIPELINE],
        model_info.FILENAMES[idx][model_info.FILENAME_LABELS],
        model_info.FILENAMES[idx][model_info.FILENAME_THRESHOLDS]
    )
    PIPELINE[idx]   = pipe
    MLB[idx]        = mlb
    THRESHOLDS[idx] = th

    print(f"[Info] Anzahl Labels im Modell: {len(MLB[idx].classes_)}")
    print(f"[Info] Thresholds-Länge       : {THRESHOLDS[idx].shape[0]}")

# ==========================
# 2) PREPROCESSING
# ==========================

def build_text(row: pd.Series) -> str:
    """
    Baut den gleichen zusammengesetzten Text wie beim Training:
    - Basis: text
    - Titel mehrfach angehängt (TITLE_BOOST)
    - Autor als stabiler Token AUTHOR_PREFIX + name
    """
    text   = str(row.get(TEXT_COL, "") or "")
    title  = str(row.get(TITLE_COL, "") or "").strip()
    author = str(row.get(AUTHOR_COL, "") or "").strip()

    parts = [text]

    if title:
        parts.append((" " + title) * TITLE_BOOST)

    if author:
        author_token = AUTHOR_PREFIX + author.lower().replace(" ", "_")
        parts.append(author_token)

    return " ".join(parts).strip()


def add_model_features(df: pd.DataFrame) -> pd.DataFrame:
    """
    Erzeugt die Spalten, die die Pipeline erwartet:
    - text_aug (TF-IDF)
    """
    df["text_aug"] = df.apply(build_text, axis=1)
    return df[["text_aug"]]


# ==========================
# 3) PREDICT-FUNKTIONEN
# ==========================

def predict_for_df(df: pd.DataFrame, pipeline: any, thresholds: np.ndarray, mlb: any):
    """
    Nimmt ein DF mit Spalten (title,author,text,...) und gibt:
    - pred_tags_list (Liste von Listen mit Label-Strings)
    - proba (n_samples x n_labels)
    zurück.
    """
    X_feat = add_model_features(df.copy())

    proba = pipeline.predict_proba(X_feat)
    proba = np.asarray(proba)

    ths = thresholds
    n_classes = proba.shape[1]

    if ths.shape[0] != n_classes:
        print(f"[WARN] thresholds len {ths.shape[0]} != n_classes {n_classes} – passe an.")
        if ths.shape[0] > n_classes:
            ths = ths[:n_classes]
        else:
            pad = np.full(n_classes - ths.shape[0], 0.5, dtype=float)
            ths = np.concatenate([ths, pad])

    mask = proba >= ths[None, :]
    pred_indices_per_row = [np.where(mask[i])[0] for i in range(mask.shape[0])]

    labels = mlb.classes_
    pred_tags_list = [[labels[j] for j in inds] for inds in pred_indices_per_row]

    return pred_tags_list, proba


def predict_for_csv_path(csv_path: str, pipeline: any, thresholds: np.ndarray, mlb: any):
    """
    - CSV von Pfad einlesen
    - Tags vorhersagen
    - Spalte 'predicted_tags' hinzufügen (kommagetrennt)
    - optional als neues CSV speichern
    """
    df = pd.read_csv(csv_path)

    # Falls Spalten fehlen, auffüllen
    for col in [ID_COL, YEAR_COL, CREATED_COL, FILENAME_COL, TITLE_COL, AUTHOR_COL, TEXT_COL]:
        if col not in df.columns:
            df[col] = ""

    pred_tags_list, proba = predict_for_df(df, pipeline, thresholds, mlb)

    df["predicted_tags"] = [", ".join(tags) for tags in pred_tags_list]

    return df, proba

def predict_for_csv_path_scenario(scenario: int, csv_path: str):
    """
    Wrapper, der das passende Modell für das Szenario auswählt.
    """
    if scenario not in model_info.MODEL_INFO:
        raise ValueError(f"Unbekanntes Szenario: {scenario}")

    pipeline   = PIPELINE[scenario]
    mlb        = MLB[scenario]
    thresholds = THRESHOLDS[scenario]

    return predict_for_csv_path(csv_path, pipeline, thresholds, mlb)


# ==========================
# 4) MAIN-LOOP (für Java / CLI)
# ==========================


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--model_path",
        type=str,
        default=None,
        help="Model Path"
    )
    parser.add_argument(
        "--suffix",
        type=str,
        default=None,
        help="Versions-Suffix für die Artefakt-Dateinamen, z.B. 'v5', 'v6_experiment1'"
    )
    parser.add_argument(
        "--csv_path",
        type=str,
        default=None,
        help="CSV"
    )
    parser.add_argument(
        "--scenario",
        type=int,
        default=None,
        help="Vordefiniertes Szenario aus model_info.py nutzen (überschreibt andere Parameter)"
    )
    args = parser.parse_args()

    MODEL_PATH = args.model_path
    SUFFIX = args.suffix
    CSV_PATH = args.csv_path
    SCENARIO = args.scenario

    df_out = None

    if (SCENARIO is None):
        MODEL_DIR      = local_store.PATHS[local_store.BASE]
        PIPELINE_PATH  = os.path.join(MODEL_DIR, f"pipeline_{SUFFIX}.joblib")
        LABELS_PATH    = os.path.join(MODEL_DIR, f"labels_{SUFFIX}.joblib")
        THRESH_PATH    = os.path.join(MODEL_DIR, f"thresholds_{SUFFIX}.joblib")
        sys.stdout.write(f"MODEL_DIR     = {MODEL_DIR}\n")
        sys.stdout.write(f"PIPELINE_PATH = {PIPELINE_PATH}\n")
        sys.stdout.write(f"LABELS_PATH   = {LABELS_PATH}\n")
        sys.stdout.write(f"THRESH_PATH   = {THRESH_PATH}\n")
        sys.stdout.flush()
        pipe, mlb, th = load_model_artifacts()
        df_out, _ = predict_for_csv_path(
            CSV_PATH,
            pipe,
            th,
            mlb
        )
    else:
        df_out, _ = predict_for_csv_path_scenario(SCENARIO, CSV_PATH)

    first_tags = ""
    if "predicted_tags" in df_out.columns and len(df_out) > 0:
        first_tags = df_out.loc[0, "predicted_tags"]

    sys.stdout.write(first_tags + "\n")
    sys.stdout.flush()

