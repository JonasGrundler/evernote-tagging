from fastapi import FastAPI, UploadFile, File, Query, HTTPException, Request
from fastapi.responses import JSONResponse
from PIL import Image, ImageOps, UnidentifiedImageError
import io
from ocr import read_image_from_bytes, decode_best

app = FastAPI(title="TrOCR Service", version="1.4")

@app.post("/ocr")  # für Swagger
async def ocr_multipart(file: UploadFile = File(...)):
    data = await file.read()
    img = read_image_from_bytes(data)
    text = decode_best(img)
    return JSONResponse({"text": text})
