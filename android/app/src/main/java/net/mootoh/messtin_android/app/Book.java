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
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
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
public class Book {
    static final private String TAG = "Book";

    final String title;
    DriveId rootDriveId;
    final BooklistActivity activity;
    Metadata coverMetadata;
    List<Metadata> pages;
    RetrieveDriveFileContentsAsyncTask task;
    ParseObject parseObject;
    private Bitmap coverBitmap;

    public Book(String title) {
        this.title = title;
        this.activity = null;
    }

    public Book(String title, DriveId driveId) {
        this.title = title;
        this.rootDriveId = driveId;
        this.activity = null;
    }

    private void initialize(final GoogleApiClient client, final BooklistActivity activity) {
        final Book self = this;

        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, "cover.jpg"))
                .build();

        DriveFolder folder = Drive.DriveApi.getFolder(client, rootDriveId);
        folder.queryChildren(client, query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                Log.d(TAG, "got cover for " + title);

                if (!result.getStatus().isSuccess()) {
                    Log.e(TAG, "failed in retrieving cover image");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                Log.d(TAG, "book cover count = " + mb.getCount());

                for (Metadata md : mb) {
                    Log.d(TAG, "book cover metadata for " + title);
                }
                return;
//                coverMetadata = mb.get(0);
//                DriveId coverImageDriveId = coverMetadata.getDriveId();
//                mb.close();
//
//                Log.d(TAG, "cover image id = " + coverImageDriveId.toString());
                /*
                task = new RetrieveDriveFileContentsAsyncTask(activity, client);
                task.delegate = self;
                task.execute(coverMetadata.getDriveId());
                */

                /*
                DriveFile file = Drive.DriveApi.getFile(client, coverImageDriveId);
                file.openContents(client, DriveFile.MODE_READ_ONLY, null).setResultCallback(new ResultCallback<DriveApi.ContentsResult>() {
                    @Override
                    public void onResult(DriveApi.ContentsResult contentsResult) {
                        Log.d(TAG, "got cover image file for " + title);

                    }
                });
                */
            }
        });
    }


    public String getTitle() {
        return title;
    }

    Book(String title, DriveId driveId, final GoogleApiClient client, final BooklistActivity activity) {
        this.title = title;
        this.rootDriveId = driveId;
        this.activity = activity;
        initialize(client, activity);
    }

    Book(ParseObject object, DriveId messtinDriveId, final GoogleApiClient client, final BooklistActivity activity) {
        title = object.getString("title");
        this.activity = activity;
        this.parseObject = object;
        /*
        final Book self = this;


        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, title))
                .addFilter(Filters.in(SearchableField.PARENTS, messtinDriveId))
                .build();

        Drive.DriveApi.query(client, query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.e(TAG, "failed in retrieving book root DriveId");
                    return;
                }
                Metadata md = result.getMetadataBuffer().get(0);
                rootDriveId = md.getDriveId();
                result.getMetadataBuffer().close();
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
                        Log.d(TAG, "status message = " + result.getStatus().getStatusCode());
                        MetadataBuffer mb = result.getMetadataBuffer();
                        for (Metadata md : mb) {
                            coverMetadata = md;
                            break;
                        }
                        if (coverMetadata == null) {
                            Log.d(TAG, "failed in retrieving cover metadata");
                            mb.close();
                            return;
                        }
                        Log.d(TAG, "cover image id = " + coverMetadata.getDriveId().toString());
                        task = new RetrieveDriveFileContentsAsyncTask(activity, client);
                        task.delegate = self;
                        task.execute(coverMetadata.getDriveId());
                    }
                });
            }
        });
        */
    }

    public Metadata getCoverMetadata() { return coverMetadata; }

    public void setCoverBitmap(Bitmap coverBitmap) {
        this.coverBitmap = coverBitmap;
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
