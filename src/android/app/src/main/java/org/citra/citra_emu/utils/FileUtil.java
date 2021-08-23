package org.citra.citra_emu.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import androidx.documentfile.provider.DocumentFile;

import org.citra.citra_emu.CitraApplication;
import org.citra.citra_emu.model.CheapDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    static final String DECODE_METHOD = "UTF-8";
    static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    static final String TEXT_PLAIN = "text/plain";

    /**
     * Create a file from directory with filename.
     * @param directory parent path for file.
     * @param filename file display name.
     * @return boolean
     */
    public static boolean createFile(String directory, String filename) {
        Context context = CitraApplication.getAppContext();
        try {
            Uri directoryUri = Uri.parse(directory);
            DocumentFile parent;
            parent = DocumentFile.fromTreeUri(context, directoryUri);
            if (parent == null) return false;
            filename = URLDecoder.decode(filename, DECODE_METHOD);
            int extensionPosition = filename.lastIndexOf('.');
            String extension = "";
            if (extensionPosition > 0) {
                extension = filename.substring(extensionPosition);
            }
            String mimeType = APPLICATION_OCTET_STREAM;
            if (extension.equals(".txt")) {
                mimeType = TEXT_PLAIN;
            }
            if (parent.findFile(filename) != null) return true;
            DocumentFile createdFile = parent.createFile(mimeType, filename);
            return createdFile != null;
        } catch (Exception e) {
            Log.error("[FileUtil]: Cannot create file, error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Create a directory from directory with filename.
     * @param directory parent path for directory.
     * @param directoryName directory display name.
     * @return boolean
     */
    public static boolean createDir(String directory, String directoryName) {
        Context context = CitraApplication.getAppContext();
        try {
            Uri directoryUri = Uri.parse(directory);
            DocumentFile parent;
            parent = DocumentFile.fromTreeUri(context, directoryUri);
            if (parent == null) {
                return false;
            }
            directoryName = URLDecoder.decode(directoryName, DECODE_METHOD);
            if (parent.findFile(directoryName) != null) return true;
            DocumentFile createdDirectory = parent.createDirectory(directoryName);
            return createdDirectory != null;
        } catch (Exception e) {
            Log.error("[FileUtil]: Cannot create file, error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Open content uri and return file descriptor to JNI.
     * @param path Native content uri path
     * @param openmode will be one of "r", "r", "rw", "wa", "rwa"
     * @return file descriptor
     */
    public static int openContentUri(String path, String openmode) {
        Context context = CitraApplication.getAppContext();
        try {
            Uri uri = Uri.parse(path);
            ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, openmode);
            if (parcelFileDescriptor == null) {
                Log.error("[FileUtil]: Cannot get the file descriptor from uri: " + path);
                return -1;
            }
            return parcelFileDescriptor.detachFd();
        }
        catch (Exception e) {
            Log.error("[FileUtil]: Cannot open content uri, error: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Reference:  https://stackoverflow.com/questions/42186820/documentfile-is-very-slow
     * This function will be faster than DoucmentFile.listFiles
     * @param uri Directory uri.
     * @return CheapDocument lists.
     */
    public static CheapDocument[] listFiles(Uri uri) {
        Context context = CitraApplication.getAppContext();
        final ContentResolver resolver = context.getContentResolver();
        final String[] columns = new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };
        Cursor c = null;
        final List<CheapDocument> results = new ArrayList<>();
        try {
            String docId;
            if (DocumentsContract.isDocumentUri(context, uri)) {
                docId = DocumentsContract.getDocumentId(uri);
            } else {
                docId = DocumentsContract.getTreeDocumentId(uri);
            }
            final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, docId);
            c = resolver.query(childrenUri, columns, null, null, null);
            while(c.moveToNext()) {
                final String documentId = c.getString(0);
                final String documentName = c.getString(1);
                final String documentMimeType = c.getString(2);
                final Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId);
                CheapDocument document = new CheapDocument(documentName, documentMimeType, documentUri);
                results.add(document);
            }
        } catch (Exception e) {
            Log.error("[FileUtil]: Cannot list file error: " + e.getMessage());
        } finally {
            closeQuietly(c);
        }
        return results.toArray(new CheapDocument[0]);
    }

    /**
     * Check whether given path exists.
     * @param path Native content uri path
     * @return bool
     */
    public static boolean Exists(String path) {
        Context context = CitraApplication.getAppContext();
        Cursor c = null;
        try {
            Uri mUri = Uri.parse(path);
            final String[] columns = new String[] { DocumentsContract.Document.COLUMN_DOCUMENT_ID };
            c = context.getContentResolver().query(mUri, columns, null, null, null);
            return c.getCount() > 0;
        } catch (Exception e) {
            Log.info("[FileUtil] Cannot find file from given path, error: " + e.getMessage());
        } finally {
            FileUtil.closeQuietly(c);
        }
        return false;
    }

    /**
     * Check whether given path is a directory
     * @param path content uri path
     * @return bool
     */
    public static boolean isDirectory(String path) {
        final Context context = CitraApplication.getAppContext();
        final ContentResolver resolver = context.getContentResolver();
        final String[] columns = new String[] {
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };
        boolean isDirectory = false;
        Cursor c = null;
        try {
            Uri mUri = Uri.parse(path);
            c = resolver.query(mUri, columns, null, null, null);
            c.moveToNext();
            final String mimeType = c.getString(0);
            isDirectory = mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR);
        } catch (Exception e) {
            Log.error("[FileUtil]: Cannot list files, error: " + e.getMessage());
        } finally {
            closeQuietly(c);
        }
        return isDirectory;
    }

    /**
     * Get file display name from given path
     * @param path content uri path
     * @return String display name
     */
    public static String getFilename(String path) {
        final Context context = CitraApplication.getAppContext();
        final ContentResolver resolver = context.getContentResolver();
        final String[] columns = new String[] {
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
        };
        String filename = "";
        Cursor c = null;
        try {
            Uri mUri = Uri.parse(path);
            c = resolver.query(mUri, columns, null, null, null);
            c.moveToNext();
            filename = c.getString(0);
        } catch (Exception e) {
            Log.error("[FileUtil]: Cannot get file size, error: " + e.getMessage());
        } finally {
            closeQuietly(c);
        }
        return filename;
    }

    /**
     * Get file size from given path.
     * @param path content uri path
     * @return long file size
     */
    public static long getFileSize(String path) {
        final Context context = CitraApplication.getAppContext();
        final ContentResolver resolver = context.getContentResolver();
        final String[] columns = new String[] {
                DocumentsContract.Document.COLUMN_SIZE
        };
        long size = 0;
        Cursor c =null;
        try {
            Uri mUri = Uri.parse(path);
            c = resolver.query(mUri, columns, null, null, null);
            c.moveToNext();
            size = c.getLong(0);
        } catch (Exception e) {
            Log.error("[FileUtil]: Cannot get file size, error: " + e.getMessage());
        } finally {
            closeQuietly(c);
        }
        return size;
    }

    public static boolean copyFile(String sourcePath, String destinationParentPath, String destinationFilename) {
        Context context = CitraApplication.getAppContext();
        try {
            Uri sourceUri = Uri.parse(sourcePath);
            Uri destinationUri = Uri.parse(destinationParentPath);
            DocumentFile destinationParent = DocumentFile.fromTreeUri(context, destinationUri);
            if (destinationParent == null) return false;
            String filename = URLDecoder.decode(destinationFilename, "UTF-8");
            DocumentFile destination = destinationParent.createFile("application/octet-stream", filename);
            if (destination == null) return false;
            InputStream input = context.getContentResolver().openInputStream(sourceUri);
            OutputStream output = context.getContentResolver().openOutputStream(destination.getUri());
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            input.close();
            output.flush();
            output.close();
            return true;
        } catch (Exception e) {
            Log.error("[FileUtil]: Cannot copy file, error: " + e.getMessage());
        }
        return false;
    }

    public static boolean renameFile(String path, String destinationFilename) {
        Context context = CitraApplication.getAppContext();
        try {
            Uri uri = Uri.parse(path);
            DocumentsContract.renameDocument(context.getContentResolver(), uri, destinationFilename);
            return true;
        } catch (Exception e) {
            Log.error("[FileUtil]: Cannot rename file, error: " + e.getMessage());
        }
        return false;
    }

    public static boolean deleteDocument(String path) {
        Context context = CitraApplication.getAppContext();
        try {
            Uri uri = Uri.parse(path);
            DocumentsContract.deleteDocument(context.getContentResolver(), uri);
            return true;
        } catch (Exception e) {
            Log.error("[FileUtil]: Cannot delete document, error: " + e.getMessage());
        }
        return false;
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        final long length = file.length();

        // You cannot create an array using a long type.
        if (length > Integer.MAX_VALUE) {
            // File is too large
            throw new IOException("File is too large!");
        }

        byte[] bytes = new byte[(int) length];

        int offset = 0;
        int numRead;

        try (InputStream is = new FileInputStream(file)) {
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        return bytes;
    }

    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }
}
