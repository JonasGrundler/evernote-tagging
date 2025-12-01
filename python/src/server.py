import warnings

warnings.filterwarnings(
    "ignore",
    message="No ccache found.*",
    category=UserWarning,
    module="paddle.utils.cpp_extension.extension_utils",
)
from fastapi import FastAPI
from trocr.server import router as trocr_router
from training.server import router as training_router
from inference.server import router as inference_router
from image_to_text.server import router as image_to_text_router

app = FastAPI(title="Evernote Services", version="1.4")
app.include_router(trocr_router, tags=["TrOCR OCR"])
app.include_router(training_router, tags=["Tag Model Training"])
app.include_router(inference_router, tags=["Tag Model Inference"])
app.include_router(image_to_text_router, tags=["Image to Text OCR"])