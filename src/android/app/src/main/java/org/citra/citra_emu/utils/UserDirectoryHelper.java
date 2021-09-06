package org.citra.citra_emu.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.citra.citra_emu.CitraApplication;
import org.citra.citra_emu.R;

public class UserDirectoryHelper {
    public static final int REQUEST_CODE_WRITE_PERMISSION = 500;
    public static final String CITRA_DATA_DIRECTORY = "CITRA_DATA_DIRECTORY";
    public static final SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(CitraApplication.getAppContext());

    public static void grantCitraWritePermission(Activity activity) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.putExtra(Intent.EXTRA_TITLE, R.string.select_citra_user_folder);
        i.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        activity.startActivityForResult(i, REQUEST_CODE_WRITE_PERMISSION);
    }

    public static boolean hasWriteAccess() {
        Context context = CitraApplication.getAppContext();
        String directoryString = mPreferences.getString(CITRA_DATA_DIRECTORY, "");
        try {
            Uri uri = Uri.parse(directoryString);
            int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
            DocumentFile root = DocumentFile.fromTreeUri(context, uri);
            if (root != null && root.exists()) return true;
            context.getContentResolver().releasePersistableUriPermission(uri, takeFlags);
        } catch (Exception e) {
            Log.error("[DataDirectoryHelper]: Cannot check citra data directory permission, error: " + e.getMessage());
        }
        return false;
    }

    public static boolean setCitraDataDirectory(String uriString) {
        return mPreferences.edit().putString(CITRA_DATA_DIRECTORY, uriString).commit();
    }

    @Nullable
    public static Uri getCitraDataDirectory() {
        String directoryString = mPreferences.getString(CITRA_DATA_DIRECTORY, "");
        if (directoryString.isEmpty()) {
            return null;
        }
        return Uri.parse(directoryString);
    }
}
