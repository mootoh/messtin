package net.mootoh.messtin_android.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpBookStorage extends BookStorage {
    private static final String TAG = "HttpBookStorage";
    String baseUrl;

    HttpBookStorage(String url) {
        this.baseUrl = url;
    }

    @Override
    public void retrieve(Book book, String path, OnImageRetrieved callback) {
        retrieveImage(path, book, callback);
    }

    public void retrieveImage(final String path, Book book, final OnImageRetrieved callback) {
        final URL url;

        Uri.Builder builder = Uri.parse(baseUrl).buildUpon();
        builder.appendPath(book.getObjectId());
        builder.appendPath(path);

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
                    Log.d(TAG, "The response is: " + response + " for url:" + url);
                    callback.onRetrieved(null, conn.getInputStream());
                } catch (Exception e) {
                    callback.onRetrieved(new Error(e.getMessage()), null);
                }

                return null;
            }
        };
        task.execute(url);
    }
}