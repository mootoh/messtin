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
    private static final String KEY_PAGE_NUMBER = "KEY_PAGE_NUMBER";
    Map<String, Metadata> allMetadata = new HashMap<String, Metadata>();
    int currentPage = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt(KEY_PAGE_NUMBER);
        }
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

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt(KEY_PAGE_NUMBER, currentPage);
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
                if (!result.getStatus().isSuccess()) {
                    Log.d("@@@", "failed in retrieving cover image");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                for (Metadata md : mb) {
                    Log.d(TAG, "image " + md.getTitle() + " id = " + md.getDriveId().toString());
                    allMetadata.put(md.getTitle(), md);

                    if (md.getTitle().equals("001.jpg") || md.getTitle().equals("002.jpg")) {
                        RetrieveDriveFileContentsAsyncTask task = new RetrieveDriveFileContentsAsyncTask(self, GDriveHelper.getInstance().getClient());
                        task.execute(md);
                    }
                }
            }
        });
    }

    private boolean checkPageBound(int page) {
        if (page <= 0 || page > allMetadata.size()) {
            Log.d(TAG, "out of bound: page=" + page);
            return false;
        }
        return true;
    }

    private String filenameForPage(int page) {
        String name = "%03d.jpg";
        name = String.format(name, page);
        return name;
    }

    private void retrievePage(int page) {
        if (! checkPageBound(page)) return;

        String name = filenameForPage(page);
        Log.d(TAG, "retrieving page = " + name);
        RetrieveDriveFileContentsAsyncTask task = new RetrieveDriveFileContentsAsyncTask(this, GDriveHelper.getInstance().getClient());
        task.execute(allMetadata.get(name));
    }

    public void nextPage() {
        if (! checkPageBound(currentPage+1)) return;
        currentPage++;
        retrievePage(currentPage);
        retrievePage(currentPage + 1);
    }

    private void prevPage() {
        if (! checkPageBound(currentPage-1)) return;
        currentPage--;
        retrievePage(currentPage);
        retrievePage(currentPage - 1);
    }

    @Override
    public void onChanged(RetrieveDriveFileContentsAsyncTaskResult result) {
        ImageView iv = (ImageView)findViewById(R.id.imageView);
        String name = filenameForPage(currentPage);

        if (result.getMetadata().equals(allMetadata.get(name))) {
            iv.setImageBitmap(result.getBitamp());
        }
    }
}
