package net.mootoh.messtin_android.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mootoh on 5/11/14.
 */
public class BookReadActivity extends ImageHavingActivity {
    private static final String TAG = "BookReadActivity";
    Map<String, Metadata> allMetadata = new HashMap<String, Metadata>();
    int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookread);

        Intent intent = getIntent();
        DriveId driveId = intent.getParcelableExtra("book");
        Log.d(TAG, "driveId = " + driveId.toString());

        setup(driveId);

        final ImageView iv = (ImageView)findViewById(R.id.imageView);
        iv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        float x = event.getX();
                        if (x > iv.getWidth() / 2) {
                            nextPage();
                        } else {
                            prevPage();
                        }
                        break;
                }
                return false;
            }
        });
    }

    public void setup(DriveId driveId) {
        Query query = new Query.Builder()
//                .addFilter(Filters.eq(SearchableField.TITLE, "jpg"))
                .addFilter(Filters.in(SearchableField.PARENTS, driveId))
                .build();

        final BookReadActivity self = this;
        Drive.DriveApi.query(GDriveHelper.getInstance().getClient(), query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                Log.d(TAG, "onResult---------------------");
                if (!result.getStatus().isSuccess()) {
                    Log.d("@@@", "failed in retrieving cover image");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                for (Metadata md : mb) {
                    Log.d(TAG, "image " + md.getTitle() + " id = " + md.getDriveId().toString());
                    allMetadata.put(md.getTitle(), md);

                    if (md.getTitle().equals("001.jpg")) {
                        RetrieveDriveFileContentsAsyncTask task = new RetrieveDriveFileContentsAsyncTask(self, GDriveHelper.getInstance().getClient());
                        task.execute(md.getDriveId());
                    }
                }
            }
        });
    }

    public void nextPage() {
        currentPage++;
        String name = "%03d.jpg";
        name = String.format(name, currentPage);
        Log.d(TAG, "next page = " + name);
        RetrieveDriveFileContentsAsyncTask task = new RetrieveDriveFileContentsAsyncTask(this, GDriveHelper.getInstance().getClient());
        task.execute(allMetadata.get(name).getDriveId());
    }

    private void prevPage() {
        currentPage--;
        String name = "%03d.jpg";
        name = String.format(name, currentPage);
        Log.d(TAG, "prev page = " + name);
        RetrieveDriveFileContentsAsyncTask task = new RetrieveDriveFileContentsAsyncTask(this, GDriveHelper.getInstance().getClient());
        task.execute(allMetadata.get(name).getDriveId());
    }

    @Override
    public void onChanged(Bitmap bm) {
        ImageView iv = (ImageView)findViewById(R.id.imageView);
        iv.setImageBitmap(bm);
    }
}
