package com.hiddenramblings.tagmo.nfctech;

import android.content.Context;
import android.net.Uri;

import com.eightbit.content.DocumentsUri;
import com.eightbit.io.Debug;
import com.hiddenramblings.tagmo.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class KeyManager {

    private static final String FIXED_KEY_MD5 = "0AD86557C7BA9E75C79A7B43BB466333";
    private static final String UNFIXED_KEY_MD5 = "2551AFC7C8813008819836E9B619F7ED";

    private static final int KEY_FILE_SIZE = 80;

    byte[] fixedKey = null;
    byte[] unfixedKey = null;

    Context context;

    public KeyManager(Context context) {
        this.context = context;
        isKeyMissing();
    }

    boolean hasLocalFile(String file) {
        String[] files = context.fileList();
        for (String file1 : files) {
            if (file1.equals(file))
                return true;
        }
        return false;
    }

    private byte[] loadKeyFromStorage(String file) {
        try (FileInputStream fs = context.openFileInput(file)) {
            byte[] key = new byte[KEY_FILE_SIZE];
            if (fs.read(key) != KEY_FILE_SIZE)
                throw new IOException(context.getString(R.string.key_size_invalid));
            return key;
        } catch (Exception e) {
            Debug.Error(R.string.key_read_error, e);
        }
        return null;
    }

    public boolean hasFixedKey() {
        if (hasLocalFile(FIXED_KEY_MD5))
            fixedKey = loadKeyFromStorage(FIXED_KEY_MD5);
        return fixedKey != null;
    }

    public boolean hasUnFixedKey() {
        if (hasLocalFile(UNFIXED_KEY_MD5))
            unfixedKey = loadKeyFromStorage(UNFIXED_KEY_MD5);
        return unfixedKey != null;
    }

    public boolean isKeyMissing() {
        return !hasFixedKey() || !hasUnFixedKey();
    }

    void saveKeyFile(String file, byte[] key) throws IOException {
        try (FileOutputStream fos = context.openFileOutput(file, Context.MODE_PRIVATE)) {
            fos.write(key);
        }
    }

    private void readKey(InputStream strm) throws IOException {
        byte[] data = new byte[KEY_FILE_SIZE];
        int rlen = strm.read(data, 0, data.length);
        if (rlen <= 0)
            throw new IOException(context.getString(R.string.invalid_key_error));

        if (rlen < KEY_FILE_SIZE)
            throw new IOException(context.getString(R.string.key_size_error));

        String md5 = TagUtils.md5(data);
        if (FIXED_KEY_MD5.equals(md5)) {
            saveKeyFile(FIXED_KEY_MD5, data);
            this.fixedKey = loadKeyFromStorage(FIXED_KEY_MD5);
        } else if (UNFIXED_KEY_MD5.equals(md5)) {
            saveKeyFile(UNFIXED_KEY_MD5, data);
            this.unfixedKey = loadKeyFromStorage(UNFIXED_KEY_MD5);
            if (!hasFixedKey()) readKey(strm);
        } else {
            throw new IOException(context.getString(R.string.key_signature_error));
        }
    }

    public void loadKey(Uri file) throws IOException {
        try (InputStream strm = context.getContentResolver().openInputStream(file)) {
            readKey(strm);
        }
    }
}
