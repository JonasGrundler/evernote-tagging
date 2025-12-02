import os
import numpy as np
from PIL import Image
from paddleocr import TextDetection, TextRecognition
from datetime import datetime
import sys
import local_store
import argparse


sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

os.environ["CUDA_VISIBLE_DEVICES"] = ""

WORKING_DIR = local_store.PATHS[local_store.IMAGES_TMP]

# 1) Modelle laden
det_model = TextDetection(
    model_name="PP-OCRv5_server_det",
    device="cpu"
)
print (datetime.now().strftime("%H:%M:%S") + " ---------------------------- 0")
rec_model = TextRecognition(
    model_name="PP-OCRv5_mobile_rec",
    device="cpu"
)

def ocr_images(images: int):

    sys.stdout.write("begin\n")
    sys.stdout.flush()

    print (datetime.now().strftime("%H:%M:%S"))

    IMAGES = images

    for i in range(0, IMAGES):
        IMG_PATH = os.path.join(WORKING_DIR, str(i) + ".png")

        # 2) Detection
        det_results = det_model.predict(input=IMG_PATH, batch_size=1)
        det_out = det_results[0]

        # 3) Detection-Result „auspacken“
        if hasattr(det_out, "to_dict"):
            det_dict = det_out.to_dict()
        elif hasattr(det_out, "res"):
            det_dict = det_out.res
        else:
            det_dict = det_out  # schon dict

        polys = det_dict["dt_polys"]
        scores = det_dict["dt_scores"]

        # 4) Originalbild öffnen
        img = Image.open(IMG_PATH).convert("RGB")

        all_texts = []

        for poly, det_score in zip(polys, scores):
            poly = np.array(poly, dtype=np.int32)
            x_min = int(poly[:, 0].min())
            x_max = int(poly[:, 0].max())
            y_min = int(poly[:, 1].min())
            y_max = int(poly[:, 1].max())

            if x_max <= x_min or y_max <= y_min:
                continue

            crop_pil = img.crop((x_min, y_min, x_max, y_max))

            # 👉 WICHTIG: PIL → numpy
            crop_np = np.array(crop_pil)  # RGB, uint8

            # PaddleX mag auch eine LISTE von Bildern
            rec_results = rec_model.predict(input=[crop_np], batch_size=1)

            if not rec_results:   # wurde ignoriert
                continue

            rec_out = rec_results[0]

            # wieder auspacken
            if hasattr(rec_out, "to_dict"):
                rec_dict = rec_out.to_dict()
            elif hasattr(rec_out, "res"):
                rec_dict = rec_out.res
            else:
                rec_dict = rec_out

            # typische Keys:
            text = (
                rec_dict.get("rec_text")
                or rec_dict.get("text")
                or rec_dict.get("label")
                or ""
            )
            rec_score = rec_dict.get("rec_score") or rec_dict.get("score")

            if text.strip():
                all_texts.append((text, rec_score, det_score))

        print("file:" + os.path.join(WORKING_DIR, str(i) + ".txt"))

        # 5) Ausgabe
        with open(os.path.join(WORKING_DIR, str(i) + ".txt"), "w", encoding="utf-8") as f:
            for text, r_s, d_s in reversed(all_texts):
                if r_s is not None:
                    f.write(f"{text}\n")
                else:
                    f.write(f"{text}\n")

    print (datetime.now().strftime("%H:%M:%S"))

    sys.stdout.write("done\n")
    sys.stdout.flush()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--images",
        type=int,
        default=None,
        help="Number of images to process"
    )
    args = parser.parse_args()

    IMAGES = args.images

    ocr_images(IMAGES)
