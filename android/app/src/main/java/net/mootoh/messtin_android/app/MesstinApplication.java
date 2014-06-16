package net.mootoh.messtin_android.app;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by mootoh on 6/16/14.
 */
public class MesstinApplication extends Application {
    public MesstinApplication() {
        Parse.enableLocalDatastore(this);
    }
}
