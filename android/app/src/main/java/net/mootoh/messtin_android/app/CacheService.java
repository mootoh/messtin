package net.mootoh.messtin_android.app;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;

public class CacheService extends IntentService {
    public static final String ACTION_FETCH = "ACTION_FETCH";
    public static final String ACTION_FETCH_RESULT = "ACTION_FETCH_RESULT";
    public static final String ACTION_CALC_SPACE_USED = "ACTION_CALC_SPACE_USED";
    private static final int DEFAULT_LIMIT = 1<<30;
    private static final String TAG = "CacheService";

    private int limit_ = DEFAULT_LIMIT; // upper bound for total file size in cache

    public CacheService() {
        super("Cache Service");
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
                // if it exceeds the limit, delete the oldest-less-used file
            }
        });
    }

    public int getLimit() {
        return limit_;
    }
    private int getCurrentUsage() {
        return 0;
    }
}
