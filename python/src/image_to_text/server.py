from fastapi import APIRouter
from pydantic import BaseModel

# dein Trainingsscript heißt z.B. train_tags.py
from image_to_text.image_to_text import ocr_images

class ImageToTextRequest(BaseModel):
    images: int


router = APIRouter()

@router.post("/ocr_images")
def ocr_images_endpoint(req: ImageToTextRequest):

    ocr_images(req.images)