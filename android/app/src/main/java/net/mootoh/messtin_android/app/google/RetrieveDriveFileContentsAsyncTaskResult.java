package net.mootoh.messtin_android.app.google;

import android.graphics.Bitmap;

public class RetrieveDriveFileContentsAsyncTaskResult {
    private final Bitmap bm;

    RetrieveDriveFileContentsAsyncTaskResult(Bitmap bm) {
        this.bm = bm;
    }

    public Bitmap getBitamp() {
        return bm;
    }
}
