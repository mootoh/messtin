package net.mootoh.messtin_android.app;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.api.services.drive.DriveRequest;
import com.parse.ParseObject;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by mootoh on 5/12/14.
 */
//class Book implements Parcelable {
class Book implements RetrieveDriveFileContentsAsyncTaskDelegate {
    static final private String TAG = "Book";

    final String title;
    private BookDelegate delegate;
    DriveId rootDriveId;
    final BooklistActivity activity;
    Metadata coverMetadata;
    List<Metadata> pages;
    RetrieveDriveFileContentsAsyncTask task;
    ParseObject parseObject;

    private void initialize(final GoogleApiClient client, final BooklistActivity activity) {
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, "cover.jpg"))
                .addFilter(Filters.in(SearchableField.PARENTS, rootDriveId))
                .build();

        Drive.DriveApi.query(client, query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.e(TAG, "failed in retrieving cover image");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                try {
                    coverMetadata = mb.get(0);
                } catch (IllegalStateException ex) {
                    ex.printStackTrace();
                    return;
                }

                Log.d(TAG, "cover image id = " + coverMetadata.getDriveId().toString());
            }
        });
    }

    @Override
    public void onFinished(RetrieveDriveFileContentsAsyncTaskResult result) {
        delegate.gotCoverImage(this, result.getBitamp());
    }

    interface BookDelegate {
        public void gotDriveId(Book aBook, DriveId driveId);
        public void gotCoverImage(Book aBook, Bitmap coverImage);
    }

    Book(Metadata md, final GoogleApiClient client, final BooklistActivity activity) {
        title = md.getTitle();
        rootDriveId = md.getDriveId();
        this.activity = activity;
        initialize(client, activity);
    }

    Book(ParseObject object, final GoogleApiClient client, final BooklistActivity activity, final BookDelegate delegate) {
        title = object.getString("title");
        this.activity = activity;
        this.parseObject = object;
        this.delegate = delegate;
        final Book self = this;

        com.google.android.gms.common.api.PendingResult<com.google.android.gms.drive.DriveApi.DriveIdResult> pr = Drive.DriveApi.fetchDriveId(client, object.getString("gd_id"));
        pr.setResultCallback(new ResultCallback<DriveApi.DriveIdResult>() {
            @Override
            public void onResult(DriveApi.DriveIdResult driveIdResult) {
                rootDriveId = driveIdResult.getDriveId();
                delegate.gotDriveId(self, rootDriveId);

                Query query = new Query.Builder()
                        .addFilter(Filters.eq(SearchableField.TITLE, "cover.jpg"))
                        .addFilter(Filters.in(SearchableField.PARENTS, rootDriveId))
                        .build();

                Drive.DriveApi.query(client, query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.e(TAG, "failed in retrieving cover image");
                            delegate.gotCoverImage(self, null);
                            return;
                        }
                        MetadataBuffer mb = result.getMetadataBuffer();
                        for (Metadata md : mb) {
                            coverMetadata = md;
                            break;
                        }
                        if (coverMetadata == null) {
                            return;
                        }

                        Log.d(TAG, "cover image id = " + coverMetadata.getDriveId().toString());

                        task = new RetrieveDriveFileContentsAsyncTask(activity, client);
                        task.delegate = self;
                        task.execute(coverMetadata);
                    }
                });
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
            return task.get().getBitamp();
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
