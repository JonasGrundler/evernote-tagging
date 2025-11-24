from fastapi import FastAPI, UploadFile, File, Query, HTTPException, Request
from fastapi.responses import JSONResponse
import io

app = FastAPI(title="Training Service", version="1.4")

@app.post("/training")  # für Swagger
async def ocr_multipart(file: UploadFile = File(...), restrict: bool = Query(True), allowed: str = Query(ALLOWED_DEFAULT)):
    data = await file.read()
    img = read_image_from_bytes(data)
    text = decode_best(img, restrict, allowed)
    return JSONResponse({"text": text})
