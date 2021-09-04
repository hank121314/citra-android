package org.citra.citra_emu.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;
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
    static final String PATH_TREE = "tree";
    static final String DECODE_METHOD = "UTF-8";
    static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    static final String TEXT_PLAIN = "text/plain";

    /**
     * Create a file from directory with filename.
     * @param context Application context
     * @param directory parent path for file.
     * @param filename file display name.
     * @return boolean
     */
    @Nullable
    public static DocumentFile createFile(Context context, String directory, String filename) {
        try {
            Uri directoryUri = Uri.parse(directory);
            DocumentFile parent;
            parent = DocumentFile.fromTreeUri(context, directoryUri);
            if (parent == null) return null;
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
            DocumentFile isExist = parent.findFile(filename);
            if (isExist != null) return isExist;
            return parent.createFile(mimeType, filename);
        } catch (Exception e) {
            Log.error("[FileUtil]: Cannot create file, error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Create a directory from directory with filename.
     * @param context Application context
     * @param directory parent path for directory.
     * @param directoryName directory display name.
     * @return boolean
     */
    @Nullable
    public static DocumentFile createDir(Context context, String directory, String directoryName) {
        try {
            Uri directoryUri = Uri.parse(directory);
            DocumentFile parent;
            parent = DocumentFile.fromTreeUri(context, directoryUri);
            if (parent == null) return null;
            directoryName = URLDecoder.decode(directoryName, DECODE_METHOD);
            DocumentFile isExist = parent.findFile(directoryName);
            if (isExist != null) return isExist;
            return parent.createDirectory(directoryName);
        } catch (Exception e) {
            Log.error("[FileUtil]: Cannot create file, error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Open content uri and return file descriptor to JNI.
     * @param context Application context
     * @param path Native content uri path
     * @param openmode will be one of "r", "r", "rw", "wa", "rwa"
     * @return file descriptor
     */
    public static int openContentUri(Context context, String path, String openmode) {
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
     * @param context Application context
     * @param uri Directory uri.
     * @return CheapDocument lists.
     */
    public static CheapDocument[] listFiles(Context context, Uri uri) {
        final ContentResolver resolver = context.getContentResolver();
        final String[] columns = new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
        };
        Cursor c = null;
        final List<CheapDocument> results = new ArrayList<>();
        try {
            String docId;
            if (isRootTreeUri(uri)) {
                docId = DocumentsContract.getTreeDocumentId(uri);
            } else {
                docId = DocumentsContract.getDocumentId(uri);
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
    public static boolean Exists(Context context, String path) {
        Cursor c = null;
        try {
            Uri mUri = Uri.parse(path);
            final String[] columns = new String[] { DocumentsContract.Document.COLUMN_DOCUMENT_ID };
            c = context.getContentResolver().query(mUri, columns, null, null, null);
            return c.getCount() > 0;
        } catch (Exception e) {
            Log.info("[FileUtil] Cannot find file from given path, error: " + e.getMessage());
        } finally {
            closeQuietly(c);
        }
        return false;
    }

    /**
     * Check whether given path is a directory
     * @param path content uri path
     * @return bool
     */
    public static boolean isDirectory(Context context, String path) {
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
    public static String getFilename(Context context, String path) {
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

    public static String[] getFilesName(Context context, String path) {
        Uri uri = Uri.parse(path);
        List<String> files = new ArrayList<>();
        for (CheapDocument file: FileUtil.listFiles(context, uri)) {
            files.add(file.getFilename());
        }
        return files.toArray(new String[0]);
    }

    /**
     * Get file size from given path.
     * @param path content uri path
     * @return long file size
     */
    public static long getFileSize(Context context, String path) {
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

    public static boolean copyFile(Context context, String sourcePath, String destinationParentPath, String destinationFilename) {
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

    public static boolean renameFile(Context context, String path, String destinationFilename) {
        try {
            Uri uri = Uri.parse(path);
            DocumentsContract.renameDocument(context.getContentResolver(), uri, destinationFilename);
            return true;
        } catch (Exception e) {
            Log.error("[FileUtil]: Cannot rename file, error: " + e.getMessage());
        }
        return false;
    }

    public static boolean deleteDocument(Context context, String path) {
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

    public static boolean isRootTreeUri(Uri uri) {
        final List<String> paths = uri.getPathSegments();
        return paths.size() == 2 && PATH_TREE.equals(paths.get(0));
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
