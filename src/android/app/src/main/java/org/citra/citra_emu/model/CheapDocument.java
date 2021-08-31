package org.citra.citra_emu.model;

import android.net.Uri;
import android.provider.DocumentsContract;

/**
 * A struct that is much more "cheaper" than DocumentFile.
 * Only contains the information we needed.
 */
public class CheapDocument {
    private final String filename;
    private final Uri uri;
    private final String mimeType;
    private final long size;

    public CheapDocument(String filename, String mimeType, Uri uri, long size) {
        this.filename = filename;
        this.mimeType = mimeType;
        this.uri = uri;
        this.size = size;
    }

    public String getFilename() {
        return filename;
    }

    public Uri getUri() {
        return uri;
    }

    public long getSize() {
        return size;
    }

    public boolean isDirectory() {
        return mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR);
    }
}
