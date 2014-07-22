package net.mootoh.messtin_android.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by takayama.motohiro on 7/22/14.
 */
public class HttpBookStorage implements BookStorage {
    private static final String TAG = "HttpBookStorage";
    BookStorageDelegate delegate;
    String baseUrl;

    HttpBookStorage(String url) {
        this.baseUrl = url;
    }

    @Override
    public void retrieve(Book book, int page, OnImageRetrieved callback) {
    }

    @Override
    public void retrieveCover(Book book, final OnImageRetrieved callback) {
        String objId = book.getObjectId();
        final URL url;

        Uri.Builder builder = Uri.parse(baseUrl).buildUpon();
        builder.appendPath(objId);
        builder.appendPath("cover.jpg");

        try {
            url = new URL( builder.toString());
        } catch (MalformedURLException e) {
            callback.onRetrieved(new Error(e.getMessage()), null);
            return;
        }

        Log.d(TAG, "url: " + url);
        Log.d(TAG, "host = " + url.getHost());
        Log.d(TAG, "autho = " + url.getAuthority());
        Log.d(TAG, "port = " + url.getPort());
        Log.d(TAG, "path = " + url.getPath());
        Log.d(TAG, "proto = " + url.getProtocol());

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

                    Bitmap bm = BitmapFactory.decodeStream(is);
                    callback.onRetrieved(null, bm);
                } catch (Exception e) {
                    callback.onRetrieved(new Error(e.getMessage()), null);
                }

                return null;
            }
        };
        task.execute(url);
    }
}
