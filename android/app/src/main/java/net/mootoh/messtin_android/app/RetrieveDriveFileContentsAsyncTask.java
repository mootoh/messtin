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

import java.io.InputStream;

abstract class ImageHavingActivity extends Activity {
    public abstract void onChanged(Bitmap bm);
}

/**
 * Created by mootoh on 5/12/14.
 */
final class RetrieveDriveFileContentsAsyncTask extends AsyncTask<DriveId, Boolean, Bitmap> {
    static final String TAG = "RetrieveDriveFileContentsAsyncTask";
    final ImageHavingActivity activity;
    final GoogleApiClient client;

    public RetrieveDriveFileContentsAsyncTask(ImageHavingActivity activity, GoogleApiClient client) {
        this.activity = activity;
        this.client = client;
    }

    @Override
    protected Bitmap doInBackground(DriveId... params) {
        DriveFile file = Drive.DriveApi.getFile(client, params[0]);
        DriveApi.ContentsResult contentsResult = file.openContents(client, DriveFile.MODE_READ_ONLY, null).await();
        if (!contentsResult.getStatus().isSuccess()) {
            Log.d(TAG, "failed in retrieving the file content");
            return null;
        }
        InputStream is = contentsResult.getContents().getInputStream();
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
        Bitmap bm = BitmapFactory.decodeStream(is);
        file.discardContents(client, contentsResult.getContents()).await();
        return bm;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result == null) {
            Log.d(TAG, "Error while reading from the file");
            return;
        }
        Log.d(TAG, "File contents: " + result);
        activity.onChanged(result);
    }
}
