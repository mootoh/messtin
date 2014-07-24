package net.mootoh.messtin_android.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.drive.DriveId;
import com.google.api.client.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by takayama.motohiro on 7/22/14.
 */
public class HttpBookStorage implements BookStorage {
    private static final String TAG = "HttpBookStorage";
    private final File cacheDir;
    private final Context context;
    String baseUrl;

    HttpBookStorage(String url, Context context) {
        this.baseUrl = url;
        this.context = context;
        this.cacheDir = context.getCacheDir();
    }

    private String filenameForPage(int page) {
        String name = "%03d.jpg";
        name = String.format(name, page);
        return name;
    }

    @Override
    public void retrieve(Book book, int page, OnImageRetrieved callback) {
        retrieveImage(filenameForPage(page), book, callback);
    }

    @Override
    public void retrieveCover(Book book, final OnImageRetrieved callback) {
        retrieveImage("cover.jpg", book, callback);
    }

    public void retrieveImage(String name, Book book, final OnImageRetrieved callback) {
        String objId = book.getObjectId();
        final String path = objId + "/" + name;

        if (existInCache(path)) {
            Bitmap bitmap = resultFromCache(path);
            callback.onRetrieved(null, bitmap);
        }

        createDirInCache(objId);

        final URL url;

        Uri.Builder builder = Uri.parse(baseUrl).buildUpon();
        builder.appendPath(objId);
        builder.appendPath(name);

        try {
            url = new URL(builder.toString());
        } catch (MalformedURLException e) {
            callback.onRetrieved(new Error(e.getMessage()), null);
            return;
        }

        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setReadTimeout(10000 /* milliseconds */);
                    conn.setConnectTimeout(15000 /* milliseconds */);
                    conn.setRequestMethod("GET");

                    conn.connect();
                    int response = conn.getResponseCode();
                    Log.d(TAG, "The response is: " + response);
                    InputStream is = conn.getInputStream();

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    IOUtils.copy(is, bos);

                    InputStream isForBitmap = new ByteArrayInputStream(bos.toByteArray());
                    InputStream isForCache  = new ByteArrayInputStream(bos.toByteArray());

                    Bitmap bm = BitmapFactory.decodeStream(isForBitmap);
                    storeToCache(isForCache, path);

                    callback.onRetrieved(null, bm);
                } catch (Exception e) {
                    callback.onRetrieved(new Error(e.getMessage()), null);
                }

                return null;
            }
        };
        task.execute(url);
    }

    private void createDirInCache(String objId) {
        File dir = new File(cacheDir + "/" + objId);
        if (dir.exists())
            return;
        if (! dir.mkdir())
            throw new Error("failed in creating a directory for book: " + objId);

        File thumbnailDir = new File(cacheDir + "/" + objId + "/tm");
        if (thumbnailDir.exists())
            return;
        if (! thumbnailDir.mkdir())
            throw new Error("failed in creating a thumbnail directory for book: " + objId);
    }

    private Bitmap resultFromCache(String path) {
        return BitmapFactory.decodeFile(getCacheFileName(path));
    }

    private boolean existInCache(String path) {
        File file = new File(getCacheFileName(path));
        return file.exists();
    }

    private String getCacheFileName(String path) {
        return cacheDir + "/" + path;
    }

    private void storeToCache(InputStream is, String path) throws IOException {
        String cachePath = getCacheFileName(path);
        File file = new File(cachePath);
        FileOutputStream os = new FileOutputStream(file);

        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;

        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }
}
