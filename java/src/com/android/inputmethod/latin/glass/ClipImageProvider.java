package com.android.inputmethod.latin.glass;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

public final class ClipImageProvider extends ContentProvider {
    public static Uri uriFor(final Context context, final String fileName) {
        return new Uri.Builder().scheme("content")
                .authority(context.getPackageName() + ".clipimages")
                .appendPath(fileName)
                .build();
    }

    private File fileFor(final Uri uri) throws FileNotFoundException {
        final String name = uri.getLastPathSegment();
        final File dir = ClipboardHistory.getClipsDir(getContext());
        if (name == null || dir == null || name.contains("/") || name.startsWith(".")) {
            throw new FileNotFoundException(String.valueOf(uri));
        }
        final File file = new File(dir, name);
        if (!file.isFile()) throw new FileNotFoundException(String.valueOf(uri));
        return file;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(final Uri uri, final String mode)
            throws FileNotFoundException {
        if (!"r".equals(mode)) throw new SecurityException("read only");
        return ParcelFileDescriptor.open(fileFor(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(final Uri uri) {
        final String name = uri.getLastPathSegment();
        if (name == null) return null;
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection,
            final String[] selectionArgs, final String sortOrder) {
        final MatrixCursor cursor = new MatrixCursor(
                new String[] { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE });
        try {
            final File file = fileFor(uri);
            cursor.addRow(new Object[] { file.getName(), file.length() });
        } catch (FileNotFoundException e) {
        }
        return cursor;
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String selection,
            final String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
