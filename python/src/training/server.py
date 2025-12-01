from fastapi import APIRouter
from pydantic import BaseModel
from datetime import datetime

# dein Trainingsscript heißt z.B. train_tags.py
from training.train_tag_model4 import run_training_scenario


class TrainRequest(BaseModel):
    scenario: int


router = APIRouter()

@router.post("/train")
def train_endpoint(req: TrainRequest):
    started = datetime.now()

    result = run_training_scenario(req.scenario)

    finished = datetime.now()
