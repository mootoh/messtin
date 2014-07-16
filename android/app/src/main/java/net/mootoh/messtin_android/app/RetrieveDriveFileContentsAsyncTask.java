package net.mootoh.messtin_android.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

interface RetrieveDriveFileContentsAsyncTaskDelegate {
    public void onError(RetrieveDriveFileContentsAsyncTask task, Error error);
    public void onFinished(RetrieveDriveFileContentsAsyncTask task, RetrieveDriveFileContentsAsyncTaskResult result);
}

class RetrieveDriveFileContentsAsyncTaskResult {
    private final Bitmap bm;

    RetrieveDriveFileContentsAsyncTaskResult(Bitmap bm) {
        this.bm = bm;
    }

    public Bitmap getBitamp() {
        return bm;
    }
}
/**
 * Created by mootoh on 5/12/14.
 */
final class RetrieveDriveFileContentsAsyncTask extends AsyncTask<DriveId, Boolean, RetrieveDriveFileContentsAsyncTaskResult> {
    static final String TAG = "RetrieveDriveFileContentsAsyncTask";
    private final GoogleApiClient client;
    private final File cacheDir;
    RetrieveDriveFileContentsAsyncTaskDelegate delegate;
    private Error error;
    private int page;

    public RetrieveDriveFileContentsAsyncTask(final GoogleApiClient client, final File cacheDir) {
        this.client = client;
        this.cacheDir = cacheDir;
    }

    @Override
    protected RetrieveDriveFileContentsAsyncTaskResult doInBackground(DriveId... params) {
        DriveId driveId = params[0];

        // check cache
        if (existInCache(driveId)) {
            return resultFromCache(driveId);
        }

        DriveFile file = Drive.DriveApi.getFile(client, driveId);
        DriveApi.ContentsResult contentsResult = file.openContents(client, DriveFile.MODE_READ_ONLY, null).await();
        if (!contentsResult.getStatus().isSuccess()) {
            error = new Error("failed in retrieving the file content for " + driveId);
            return null;
        }

        Log.d(TAG, "got image content for " + driveId);
/*
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(contentsResult.getContents().getInputStream(), bos);
        } catch (IOException e) {
            error = new Error("failed in copying downloaded contents: " + e.getMessage());
            return null;
        }

        InputStream isForBitmap = new ByteArrayInputStream(bos.toByteArray());
        InputStream isForCache  = new ByteArrayInputStream(bos.toByteArray());

        Bitmap bm = BitmapFactory.decodeStream(isForBitmap);

        try {
            storeToCache(isForCache, params[0]);
        } catch (IOException e) {
            error = new Error("failed in storing downloaded contents to cache: " + e.getMessage());
            return null;
        }
*/
        Bitmap bm = BitmapFactory.decodeStream(contentsResult.getContents().getInputStream());
        file.discardContents(client, contentsResult.getContents()).await();
        return new RetrieveDriveFileContentsAsyncTaskResult(bm);
    }

    private void storeToCache(InputStream is, DriveId driveId) throws IOException {
        File file = new File(getCacheFileName(driveId));
        FileOutputStream os = new FileOutputStream(file);

        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;

        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }

    private String getCacheFileName(DriveId driveId) {
        return cacheDir + "/" + driveId.getResourceId();
    }

    private RetrieveDriveFileContentsAsyncTaskResult resultFromCache(DriveId driveId) {
        File file = new File(getCacheFileName(driveId));
        Bitmap bm = BitmapFactory.decodeFile(getCacheFileName(driveId));
        return new RetrieveDriveFileContentsAsyncTaskResult(bm);
    }

    private boolean existInCache(DriveId driveId) {
        File file = new File(getCacheFileName(driveId));
        return file.exists();
    }

    @Override
    protected void onPostExecute(RetrieveDriveFileContentsAsyncTaskResult result) {
        if (delegate == null)
            return;

        if (result == null) {
            delegate.onError(this, error);
            return;
        }

        delegate.onFinished(this, result);
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }
}
