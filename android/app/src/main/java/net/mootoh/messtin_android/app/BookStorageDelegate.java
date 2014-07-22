package net.mootoh.messtin_android.app;

import android.graphics.Bitmap;

/**
 * Created by takayama.motohiro on 7/22/14.
 */
public interface BookStorageDelegate {
    public void onRetrievedPage(BookStorage storage, Book book, Bitmap bitmap);
    public void onRetrievedCover(BookStorage storage, Book book, Bitmap bitmap);
}
