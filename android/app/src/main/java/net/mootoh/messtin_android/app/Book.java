package net.mootoh.messtin_android.app;

import android.graphics.Bitmap;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveId;
import com.parse.ParseObject;

/**
 * Created by mootoh on 5/12/14.
 */
public class Book {
    static final private String TAG = "Book";

    final String title;
    DriveId rootDriveId;
    Bitmap coverBitmap;

    public Book(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return "";
    }
}
