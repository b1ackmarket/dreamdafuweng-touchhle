package org.touchhle.android;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Minimal stub DocumentsProvider for touchHLE.
 * The full Kotlin implementation is not needed for basic game functionality.
 */
public class DocumentsProvider extends android.provider.DocumentsProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(projection != null ? projection : new String[]{
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_TITLE
        });
        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(projection != null ? projection : new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID
        });
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(projection != null ? projection : new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID
        });
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        throw new FileNotFoundException("Not implemented");
    }
}
