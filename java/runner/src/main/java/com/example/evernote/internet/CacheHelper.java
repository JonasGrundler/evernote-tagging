package com.example.evernote.internet;

import com.example.evernote.LocalStore;

import java.io.*;
import java.nio.file.Paths;

public abstract class CacheHelper implements Serializable {

    private long lastUpdate = -1;

    private File getFile() {
        return getFile(getClass());
    }

    private static File getFile(Class<? extends CacheHelper> C) {
        return Paths.get(LocalStore.getSingleton().getMappings().toString(), C.getName() + ".ser").toFile();
    }

    public void store () {
        long lastLastUpdate = lastUpdate;
        try {
            lastUpdate = System.currentTimeMillis();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getFile()));
            oos.writeObject(this);
            oos.close();
        } catch (Exception e) {
            lastUpdate = lastLastUpdate;
            e.printStackTrace(System.out);
        }
    }

    public static <S extends CacheHelper> S load (Class<S> serializable) {
        S ls = null;
        ObjectInputStream ois = null;
        if (getFile(serializable).exists()) {
            try {
                ois = new ObjectInputStream(new FileInputStream(getFile(serializable)));
                ls = (S) ois.readObject();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (Exception e3) {
                    e3.printStackTrace(System.out);
                }
            }
            if (ls == null) {
                getFile(serializable).delete();
            }
        }
        if (ls == null) {
            try {
                ls = serializable.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
        return ls;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }
}
