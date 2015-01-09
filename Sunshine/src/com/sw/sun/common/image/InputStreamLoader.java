
package com.sw.sun.common.image;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.sw.sun.common.android.GlobalData;

/**
 * an internal public class that can be used by other classes
 * 
 * @author kevin
 * @hide libra only
 */
public class InputStreamLoader {
    private Context mContext;

    private Uri mUri;

    private String mPath;

    private String mZipPath;

    private int mResId = -1;

    private InputStream mInputStream;

    private ZipFile mZipFile;

    ByteArrayInputStream mByteArrayInputStream;

    public InputStreamLoader(String path) {
        mPath = path;
    }

    public InputStreamLoader(int resId) {
        mResId = resId;
    }

    public InputStreamLoader(Context context, Uri uri) {
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            mPath = uri.getPath();
        } else {
            mContext = context;
            mUri = uri;
        }
    }

    public InputStreamLoader(String zipPath, String entry) {
        mZipPath = zipPath;
        mPath = entry;
    }

    public InputStreamLoader(byte[] data) {
        mByteArrayInputStream = new ByteArrayInputStream(data);
    }

    public InputStream get() {
        close();

        try {
            if (mUri != null) {
                mInputStream = mContext.getContentResolver().openInputStream(mUri);
            } else if (mZipPath != null) {
                mZipFile = new ZipFile(mZipPath);
                mInputStream = mZipFile.getInputStream(mZipFile.getEntry(mPath));
            } else if (mPath != null) {
                mInputStream = new FileInputStream(mPath);
            } else if (mByteArrayInputStream != null) {
                mByteArrayInputStream.reset();
                mInputStream = mByteArrayInputStream;
            } else if (mResId != -1) {
                mInputStream = GlobalData.app().getApplicationContext().getResources()
                        .openRawResource(mResId);
            }
        } catch (Exception e) {
        }

        if (mInputStream != null) {
            // create BufferedInputStream only if not instanceof
            // ByteArrayInputStream
            if (!(mInputStream instanceof ByteArrayInputStream)) {
                mInputStream = new BufferedInputStream(mInputStream, 16384);
            }
        }
        return mInputStream;
    }

    public void close() {
        try {
            if (mInputStream != null) {
                mInputStream.close();
            }

            if (mZipFile != null) {
                mZipFile.close();
            }
        } catch (IOException e) {
        }
    }
}
