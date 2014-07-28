package net.mootoh.messtin_android.app;

/**
 * Created by takayama.motohiro on 7/22/14.
 */
public interface BookStorage {
    public void retrieve(Book book, int page, OnImageRetrieved callback);
    public void retrieveCover(Book book, OnImageRetrieved callback);
    public void retrieveThumbnail(Book book, int page, OnImageRetrieved callback);
}
