package net.mootoh.messtin_android.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.api.client.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

abstract class ImageHavingActivity extends Activity {
    public abstract void onChanged(RetrieveDriveFileContentsAsyncTaskResult result);
}

class RetrieveDriveFileContentsAsyncTaskResult {
    private final Bitmap bm;
    private final Metadata md;

    RetrieveDriveFileContentsAsyncTaskResult(Bitmap bm, Metadata md) {
        this.bm = bm;
        this.md = md;
    }

    public Bitmap getBitamp() {
        return bm;
    }

    public Metadata getMetadata() {
        return md;
    }
}
/**
 * Created by mootoh on 5/12/14.
 */
final class RetrieveDriveFileContentsAsyncTask extends AsyncTask<Metadata, Boolean, RetrieveDriveFileContentsAsyncTaskResult> {
    static final String TAG = "RetrieveDriveFileContentsAsyncTask";
    final ImageHavingActivity activity;
    final GoogleApiClient client;

    public RetrieveDriveFileContentsAsyncTask(ImageHavingActivity activity, GoogleApiClient client) {
        this.activity = activity;
        this.client = client;
    }

    @Override
    protected RetrieveDriveFileContentsAsyncTaskResult doInBackground(Metadata... params) {
        // check cache
        if (existInCache(params[0])) {
            return resultFromCache(params[0]);
        }

        DriveFile file = Drive.DriveApi.getFile(client, params[0].getDriveId());
        DriveApi.ContentsResult contentsResult = file.openContents(client, DriveFile.MODE_READ_ONLY, null).await();
        if (!contentsResult.getStatus().isSuccess()) {
            Log.d(TAG, "failed in retrieving the file content");
            return null;
        }
        InputStream is = contentsResult.getContents().getInputStream();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(is, bos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        InputStream is1 = new ByteArrayInputStream(bos.toByteArray());
        InputStream is2 = new ByteArrayInputStream(bos.toByteArray());

        /*
        File outFile = new File(context.getCacheDir(), params[0].toString());
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;
        try {
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        Bitmap bm = BitmapFactory.decodeStream(is1);

        try {
            storeToCache(is2, params[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }

        file.discardContents(client, contentsResult.getContents()).await();
        return new RetrieveDriveFileContentsAsyncTaskResult(bm, params[0]);
    }

    private void storeToCache(InputStream is, Metadata param) throws IOException {
        File file = new File(getCacheFileName(param));
        FileOutputStream os = new FileOutputStream(file);

        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;

        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }

    private String getCacheFileName(Metadata param) {
        return activity.getCacheDir() + "/" + param.getDriveId().getResourceId();
    }

    private RetrieveDriveFileContentsAsyncTaskResult resultFromCache(Metadata param) {
        File file = new File(getCacheFileName(param));
        Bitmap bm = BitmapFactory.decodeFile(getCacheFileName(param));
        return new RetrieveDriveFileContentsAsyncTaskResult(bm, param);
    }

    private boolean existInCache(Metadata param) {
        File file = new File(getCacheFileName(param));
        return file.exists();
    }

    @Override
    protected void onPostExecute(RetrieveDriveFileContentsAsyncTaskResult result) {
        if (result == null) {
            Log.d(TAG, "Error while reading from the file");
            return;
        }
//        Log.d(TAG, "File contents: " + result);
        activity.onChanged(result);
    }
}
