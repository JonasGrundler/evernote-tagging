from fastapi import APIRouter
from pydantic import BaseModel
import pandas as pd

# dein Trainingsscript heißt z.B. train_tags.py
from inference.inference_tags import predict_for_csv_path_scenario

class InferenceRequest(BaseModel):
    scenario: int
    csv_path: str

class InferenceResponse(BaseModel):
    tags: list[str]

router = APIRouter()

@router.post("/infer", response_model=InferenceResponse)
def infer_endpoint(req: InferenceRequest):
    df, _ = predict_for_csv_path_scenario(req.scenario, req.csv_path)

    first_tags: list[str] = []

    if "predicted_tags" in df.columns and len(df) > 0:
        value = df.loc[0, "predicted_tags"]

        if isinstance(value, (list, tuple)):
            # schon Liste -> aufräumen/als String listen
            first_tags = [
                str(t).strip()
                for t in value
                if str(t).strip() and str(t).strip().lower() != "nan"
            ]
        elif pd.isna(value):
            # NaN -> keine Tags
            first_tags = []
        else:
            # z.B. "Apple, PayPal" oder "nan, netgo"
            first_tags = [
                t.strip()
                for t in str(value).split(",")
                if t.strip() and t.strip().lower() != "nan"
            ]
    return InferenceResponse(tags=first_tags)