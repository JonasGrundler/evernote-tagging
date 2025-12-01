from fastapi import APIRouter
from fastapi import UploadFile, File
from fastapi.responses import JSONResponse
from PIL import Image, ImageOps, UnidentifiedImageError
import io
from trocr.ocr import read_image_from_bytes, decode_best

router = APIRouter()


@router.post("/ocr")  # für Swagger
async def ocr_multipart(file: UploadFile = File(...)):
    data = await file.read()
    img = read_image_from_bytes(data)
    text = decode_best(img)
    return JSONResponse({"text": text})
