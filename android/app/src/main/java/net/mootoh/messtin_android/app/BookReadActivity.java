package net.mootoh.messtin_android.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by mootoh on 5/11/14.
 */
public class BookReadActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookread);

        Intent intent = getIntent();
        String driveId = intent.getStringExtra("book");

    }

}
