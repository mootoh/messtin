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
    private ParseObject parseObject;
    private String objectId;

    public Book(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return "";
    }

    public void setRootDriveId(DriveId rootDriveId) {
        this.rootDriveId = rootDriveId;
    }

    public DriveId getRootDriveId() {
        return rootDriveId;
    }

    public ParseObject getParseObject() {
        return parseObject;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectId() {
        return objectId;
    }
}
