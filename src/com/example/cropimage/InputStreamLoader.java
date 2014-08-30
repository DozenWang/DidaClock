package com.example.cropimage;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

public class InputStreamLoader {

    private Context mContext;
    private Uri mUri;
    private String mPath;
    private String mZipPath;
    private FileAccessable mFileAccessable;

    private InputStream mInputStream;
    private ZipFile mZipFile;

    ByteArrayInputStream mByteArrayInputStream;

    public InputStreamLoader(String path) {
        mPath = path;
    }

    public InputStreamLoader(Context context, Uri uri) {
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            mPath = uri.getPath();
        }
        else {
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

    public InputStreamLoader(FileAccessable fileAccessable) {
        mFileAccessable = fileAccessable;
    }

    public InputStream get() {
        close();

        try {
            if (mFileAccessable != null) {
                mInputStream = mFileAccessable.getInputStream();
            }
            else if (mUri != null) {
                mInputStream = mContext.getContentResolver().openInputStream(mUri);
            }
            else if (mZipPath != null) {
                mZipFile = new ZipFile(mZipPath);
                mInputStream = mZipFile.getInputStream(mZipFile.getEntry(mPath));
            }
            else if (mPath != null) {
                mInputStream = new FileInputStream(mPath);
            } else if (mByteArrayInputStream != null) {
                mByteArrayInputStream.reset();
                mInputStream = mByteArrayInputStream;
            }
        } catch (Exception e) {
        }

        if (mInputStream != null) {
            // create BufferedInputStream only if not instanceof ByteArrayInputStream
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
