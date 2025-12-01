from http.client import HTTPException
from PIL import Image, ImageOps, UnidentifiedImageError
import io, numpy as np
from transformers import TrOCRProcessor, VisionEncoderDecoderModel, LogitsProcessorList
from transformers.generation.logits_process import SuppressTokensLogitsProcessor

processor = TrOCRProcessor.from_pretrained("microsoft/trocr-large-handwritten", use_fast=True)
model = VisionEncoderDecoderModel.from_pretrained("microsoft/trocr-large-handwritten")

ALLOWED_DEFAULT = "0123456789.,()"

def read_image_from_bytes(data: bytes) -> Image.Image:
    if not data:
        raise HTTPException(status_code=400, detail="Empty request body")
    try:
        img = Image.open(io.BytesIO(data))
        img = ImageOps.exif_transpose(img).convert("RGB")
        return img
    except UnidentifiedImageError:
        raise HTTPException(status_code=400, detail="Body does not contain a valid image (JPEG/PNG)")

def autocrop_content(pil: Image.Image, thr: int = 200, margin: int = 10, min_area: int = 200) -> Image.Image:
    """Schneidet weiße Ränder ab (alles > thr gilt als weiß)."""
    g = pil.convert("L")
    arr = np.array(g)
    mask = arr < thr  # „tinte“ = dunkel
    if int(mask.sum()) < min_area:
        return pil  # nichts gefunden
    ys, xs = np.where(mask)
    x0, x1 = max(0, xs.min()-margin), min(pil.width, xs.max()+1+margin)
    y0, y1 = max(0, ys.min()-margin), min(pil.height, ys.max()+1+margin)
    return pil.crop((x0, y0, x1, y1))

def upscale_and_clean(pil: Image.Image, target_min_h: int = 900) -> Image.Image:
    """Vergrößert kleine Ausschnitte und macht sie OCR-freundlicher."""
    w, h = pil.size
    if h < target_min_h:
        scale = max(2, round(target_min_h / h))
        pil = pil.resize((w*scale, h*scale), Image.BICUBIC)
    # leichte Binärschwelle (optional)
    g   = pil.convert("L")
    bw  = g.point(lambda p: 255 if p > 210 else 0)  # Otsu wäre feiner; hier simpel
    return Image.merge("RGB", (bw, bw, bw))

def make_restricted_processor(tokenizer, allowed: str = ALLOWED_DEFAULT):
    vocab = tokenizer.get_vocab()
    special = set(tokenizer.all_special_ids or [])
    eos = getattr(tokenizer, "eos_token_id", None)
    pad = getattr(tokenizer, "pad_token_id", None)
    allowed_ids = set()
    for tok, tid in vocab.items():
        if tid in special or tid in {eos, pad}: 
            continue
        s = tokenizer.decode([tid], skip_special_tokens=True, clean_up_tokenization_spaces=False)
        if s and all((c in allowed) or c.isspace() for c in s):
            allowed_ids.add(tid)
    banned = [tid for tid in vocab.values() if tid not in allowed_ids and tid not in special and tid not in {eos, pad}]
    return LogitsProcessorList([SuppressTokensLogitsProcessor(banned)]) if banned else None

def decode_best(pil_img: Image.Image):
    lp = make_restricted_processor(processor.tokenizer, ALLOWED_DEFAULT)
    # 1) Auto-Crop → 2) Upscale/Clean → 3) Greedy
    roi = autocrop_content(pil_img, thr=200, margin=12)
    roi = upscale_and_clean(roi, target_min_h=1000)
    inputs = processor(images=roi, return_tensors="pt")
    out = model.generate(**inputs, max_length=64, num_beams=1, logits_processor=lp)
    return processor.batch_decode(out, skip_special_tokens=True)[0].strip()