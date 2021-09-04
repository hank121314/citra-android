package org.citra.citra_emu.utils;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

public final class FileBrowserHelper {
    public static void openDirectoryPicker(FragmentActivity activity, int requestCode, int title) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.putExtra(Intent.EXTRA_TITLE, title);
        activity.startActivityForResult(i, requestCode);
    }

    public static void openFilePicker(FragmentActivity activity, int requestCode, int title,
                                      boolean allowMultiple) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("application/octet-stream");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        i.putExtra(Intent.EXTRA_TITLE, title);
        activity.startActivityForResult(i, requestCode);
    }

    public static String getSelectedDirectory(Intent result) {
        return result.getDataString();
    }

    @Nullable
    public static String[] getSelectedFiles(Intent result, Context context, List<String> extension) {
        ClipData clipData = result.getClipData();
        List<DocumentFile> files = new ArrayList();
        if (clipData == null) {
            files.add(DocumentFile.fromSingleUri(context, result.getData()));
        } else {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                Uri uri = item.getUri();
                files.add(DocumentFile.fromSingleUri(context, uri));
            }
        }
        if (!files.isEmpty()) {
            List<String> filepaths = new ArrayList();
            for (int i = 0; i < files.size(); i++) {
                DocumentFile file = files.get(i);
                String filename = file.getName();
                int extensionStart = filename.lastIndexOf('.');
                if (extensionStart > 0) {
                    String fileExtension = filename.substring(extensionStart + 1);
                    if (extension.contains(fileExtension)) {
                        filepaths.add(file.getUri().toString());
                    }
                }
            }
            if (filepaths.isEmpty()) {
                return null;
            }
            return filepaths.toArray(new String[0]);
        }
        return null;
    }
}
