package net.mootoh.messtin_android.app;

import android.graphics.Bitmap;

/**
 * Created by takayama.motohiro on 7/22/14.
 */
public interface OnImageRetrieved {
    public void onRetrieved(Error error, Bitmap bitmap);
}
