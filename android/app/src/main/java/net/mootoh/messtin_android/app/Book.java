package net.mootoh.messtin_android.app;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by mootoh on 5/12/14.
 */
//class Book implements Parcelable {
class Book {
    static final private String TAG = "Book";

    final String title;
    final DriveId rootDriveId;
    final BooklistActivity activity;
    Metadata coverMetadata;
    List<Metadata> pages;
    RetrieveDriveFileContentsAsyncTask task;

    Book(Metadata md, final GoogleApiClient client, final BooklistActivity activity) {
        title = md.getTitle();
        rootDriveId = md.getDriveId();
        this.activity = activity;

        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, "cover.jpg"))
                .addFilter(Filters.in(SearchableField.PARENTS, rootDriveId))
                .build();

        Drive.DriveApi.query(client, query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.d("@@@", "failed in retrieving cover image");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                coverMetadata = mb.get(0);
                Log.d(TAG, "cover image id = " + coverMetadata.getDriveId().toString());
                task = new RetrieveDriveFileContentsAsyncTask(activity, client);
                task.execute(coverMetadata.getDriveId());
            }
        });
    }

    public Metadata getCoverMetadata() { return coverMetadata; }

    public Bitmap getCoverBitmap() {
        if (task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.d(TAG, "download still in progress...");
            return null;
        }
        try {
            return task.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
/*
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeParcelable(rootDriveId);
    }
    */
}
