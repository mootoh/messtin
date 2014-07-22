package net.mootoh.messtin_android.app;

import android.app.Application;

import com.parse.Parse;

import java.net.MalformedURLException;

/**
 * Created by mootoh on 6/16/14.
 */
public class MesstinApplication extends Application {
    final BookStorage storage;

    public MesstinApplication() {
        Parse.enableLocalDatastore(this);
        storage = new HttpBookStorage("http://10.0.1.4:8000");
    }

    public BookStorage getBookStorage() {
        return storage;
    }
}
