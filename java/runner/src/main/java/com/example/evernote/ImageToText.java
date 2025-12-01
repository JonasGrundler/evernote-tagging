package com.example.evernote;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;


public class ImageToText {

    private static final int IMG_MIN_WIDTH = 500;
    private static final int IMG_MIN_HEIGHT = 100;

    private static final ImageToText singleton;

    static {
        ImageToText ls = null;
        try {
            ls = new ImageToText();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        singleton = ls;
    }

    public static ImageToText getSingleton() {
        return singleton;
    }

    private ImageToText() throws Exception {
    }

    public void parseImages(EvernoteDoc enDoc, int images) {
        System.out.println("processing " + images + " images");
        if (images > 0) {
            try {
                ServicesClient.getSingleton().ocrImages(images);
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            for (int i = 0; i < images; i++) {
                try {
                    File file = new File(LocalStore.getSingleton().getImages_tmp().toString(), i + ".txt");
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                    enDoc.addImage(new String(bis.readAllBytes(), StandardCharsets.UTF_8));
                    bis.close();
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        }
    }

    public boolean writeImage(byte[] bytes, int image) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) {
                if (img.getWidth() > IMG_MIN_WIDTH && img.getHeight() > IMG_MIN_HEIGHT) {
                    File file = new File(LocalStore.getSingleton().getImages_tmp().toString(), image + ".png");
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                    bos.write(bytes);
                    bos.close();
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return false;
    }

}