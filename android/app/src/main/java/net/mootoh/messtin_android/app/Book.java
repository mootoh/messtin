package net.mootoh.messtin_android.app;

import android.os.Parcel;
import android.os.Parcelable;

import com.parse.ParseObject;

/**
 * Created by mootoh on 5/12/14.
 */
public class Book implements Parcelable {
    static final private String TAG = "Book";

    final String title;
    int pageCount = 0;
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

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int count) {
        pageCount = count;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeInt(pageCount);
        dest.writeString(objectId);
    }

    public static Parcelable.Creator<Book> CREATOR = new Parcelable.Creator<Book>() {
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

    Book(Parcel in) {
        title = in.readString();
        pageCount = in.readInt();
        objectId = in.readString();
    }
}
