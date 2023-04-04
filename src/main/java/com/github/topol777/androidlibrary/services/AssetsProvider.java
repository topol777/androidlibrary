package com.github.topol777.androidlibrary.services;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Help open app/resources/assets files access
//
// manifest.xml
// <application>
//   <provider
//     android:name="com.github.topol777.androidlibrary.services.AssetsProvider"
//     android:authorities="com.github.topol777.android-library"
//     android:exported="false"
//     android:grantUriPermissions="true">
//   </provider>
// </application>
//
// url example:
// content://com.github.topol777.android-library/image.jpg

public class AssetsProvider extends ContentProvider {
    public Map<Uri, String> types = new HashMap<>();
    public Map<Uri, String> names = new HashMap<>();
    public Map<Uri, AssetFileDescriptor> files = new HashMap<>();

    public ProviderInfo info;

    protected static HashMap<Class, AssetsProvider> infos = new HashMap<>();

    public static AssetsProvider getProvider() {
        return infos.get(AssetsProvider.class);
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        this.info = info;
        AssetManager am = context.getAssets();
        try {
            String[] a = am.list("");
            for (String f : a) {
                try {
                    addFile(f, am.openFd(f));
                } catch (FileNotFoundException ignore) {
                    // ignore folders
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Sanity check our security
        if (info.exported)
            throw new SecurityException("Provider must not be exported");
        if (!info.grantUriPermissions)
            throw new SecurityException("Provider must grant uri permissions");
        infos.put(getClass(), this);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        AssetManager am = getContext().getAssets();
        String file_name = uri.getLastPathSegment();
        if (file_name == null)
            throw new FileNotFoundException();
        try {
            return am.openFd(file_name);
        } catch (IOException e) {
            throw StorageProvider.fnfe(e);
        }
    }

    public Uri addFile(String name, AssetFileDescriptor file) {
        Uri u = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(info.authority).path(name).build();
        String type = MimeTypeMap.getFileExtensionFromUrl(u.toString());
        type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(type);
        types.put(u, type);
        names.put(u, name);
        files.put(u, file);
        return u;
    }

    @Override
    public String getType(Uri p1) {
        return types.get(p1);
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public Cursor query(Uri p1, String[] p2, String p3, String[] p4, String p5) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        if (projection == null) {
            projection = FileProvider.COLUMNS;
        }

        String file_name = uri.getLastPathSegment();
        if (file_name == null)
            return null;

        String[] cols = new String[projection.length];
        Object[] values = new Object[projection.length];
        int i = 0;
        for (String col : projection) {
            if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                cols[i] = OpenableColumns.DISPLAY_NAME;
                values[i++] = names.get(uri);
            } else if (OpenableColumns.SIZE.equals(col)) {
                cols[i] = OpenableColumns.SIZE;
                values[i++] = files.get(uri).getLength();
            }
        }
        cols = FileProvider.copyOf(cols, i);
        values = FileProvider.copyOf(values, i);
        final MatrixCursor cursor = new MatrixCursor(cols, 1);
        cursor.addRow(values);
        return cursor;
    }
}