package net.mootoh.messtin_android.app;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by mootoh on 6/16/14.
 */
public class MesstinApplication extends Application {
    BookStorage storage;

    public MesstinApplication() {
        Parse.enableLocalDatastore(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        storage = new HttpBookStorage(getString(R.string.storage_server_url));
    }

    public BookStorage getBookStorage() {
        return storage;
    }
}
