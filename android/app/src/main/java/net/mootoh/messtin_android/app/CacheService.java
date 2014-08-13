package net.mootoh.messtin_android.app;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.api.client.util.IOUtils;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

final class MesstinContract {
    public static abstract class FileEntry implements BaseColumns {
        public static final String TABLE_NAME = "file";
        public static final String COLUMN_NAME_PATH = "path";
        public static final String COLUMN_NAME_LAST_USED = "lastUsed";
    }
}

class MesstinDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "messtin.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String TIMESTAMP_TYPE = " TIMESTAMP";
    private static final String COMMA_SEP = ",";

    private static final java.lang.String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + MesstinContract.FileEntry.TABLE_NAME + " (" +
            MesstinContract.FileEntry._ID + " INTEGER PRIMARY KEY," +
            MesstinContract.FileEntry.COLUMN_NAME_PATH + TEXT_TYPE + " NOT NULL " + COMMA_SEP +
            MesstinContract.FileEntry.COLUMN_NAME_LAST_USED + TIMESTAMP_TYPE + " NOT NULL " + COMMA_SEP +
            " )";

    public MesstinDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}

public class CacheService extends IntentService {
    public static final String ACTION_FETCH = "ACTION_FETCH";
    public static final String ACTION_FETCH_RESULT = "ACTION_FETCH_RESULT";
    public static final String ACTION_CALC_SPACE_USED = "ACTION_CALC_SPACE_USED";
    private static final int DEFAULT_LIMIT = 1 * 1024 * 1024;
    private static final String TAG = "CacheService";

    private int limit_ = DEFAULT_LIMIT; // upper bound for total file size in cache

    DiskLruCache diskLruCache_;

    public CacheService() {
        super("Cache Service");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            diskLruCache_ = DiskLruCache.open(getCacheDir(), 1, 1, DEFAULT_LIMIT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(ACTION_FETCH)) {
            fetch(intent);
        } else if (action.equals(ACTION_CALC_SPACE_USED)) {
            calcSpaceUsed();
        }
    }

    private void calcSpaceUsed() {
        File cacheDir = getCacheDir();
        long size = calcSizeIn(cacheDir);
        Log.d(TAG, "space used in cache:" + size);
    }

    private long calcSizeIn(File file) {
        if (file == null)
            return 0;

        long size = 0;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            for (File f : children) {
                size += calcSizeIn(f);
            }
            return size;
        }
        return file.length();
    }

    private void fetch(final Intent intent) {
        // download unless cached
        MesstinApplication app = (MesstinApplication) getApplication();
        BookStorage storage = app.getBookStorage();

        final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

        final Book book = intent.getParcelableExtra("book");
        final String name = intent.getStringExtra("name");

        /*
        storage.retrieveCover(book, new OnImageRetrieved() {
            @Override
            public void onRetrieved(Error error, Bitmap bitmap) {
                // let sender know the fetch operation finished
                Intent ret = new Intent(ACTION_FETCH_RESULT);
                ret.putExtra("book", book);

                if (error != null) {
                    ret.putExtra("error", error.getMessage());
//                    Log.e(TAG, "failed in retrieving cover:" + error.getMessage());
                    lbm.sendBroadcast(ret);
                    return;
                }

                ret.putExtra("bitmap", bitmap);
                ret.putExtra("index", intent.getIntExtra("index", 0));
                lbm.sendBroadcast(ret);

                // check disk space used
                // if it exceeds the limit, delete the least-recent-used file
            }
        });
        */

        String objId = book.getObjectId();
        final String path = objId.toLowerCase() + "-" + name;
        try {
            DiskLruCache.Snapshot snapshot = diskLruCache_.get(path);
            if (snapshot == null) {
                // retrieve from server
                final URL url;

                Uri.Builder builder = Uri.parse(getString(R.string.storage_server_url)).buildUpon();
                builder.appendPath(objId);
                builder.appendPath(name + ".jpg");

                try {
                    url = new URL(builder.toString());
                } catch (MalformedURLException e) {
                    Log.d(TAG, "failed");
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

                            DiskLruCache.Editor editor = diskLruCache_.edit(path);
                            OutputStream os = editor.newOutputStream(0);

                            int bufferSize = 1024;
                            byte[] buffer = new byte[bufferSize];
                            int len = 0;

                            while ((len = isForCache.read(buffer)) != -1) {
                                os.write(buffer, 0, len);
                            }
                            editor.commit();

                            Bitmap bitmap = BitmapFactory.decodeStream(isForBitmap);

                            Intent ret = new Intent(ACTION_FETCH_RESULT);
                            ret.putExtra("book", book);
                            ret.putExtra("bitmap", bitmap);
                            ret.putExtra("index", intent.getIntExtra("index", 0));
                            ret.putExtra("toShow", intent.getBooleanExtra("toShow", false));
                            lbm.sendBroadcast(ret);

                        } catch(Exception ex) {
                            Log.e(TAG, "failed: " + ex);
                        }
                        return null;
                    }
                };
                task.execute(url);
                return;
            }

            InputStream is = snapshot.getInputStream(0);
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            Intent ret = new Intent(ACTION_FETCH_RESULT);
            ret.putExtra("book", book);
            ret.putExtra("bitmap", bitmap);
            ret.putExtra("index", intent.getIntExtra("index", 0));
            ret.putExtra("toShow", intent.getBooleanExtra("toShow", false));
            lbm.sendBroadcast(ret);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public int getLimit() {
        return limit_;
    }
    private int getCurrentUsage() {
        return 0;
    }
}
