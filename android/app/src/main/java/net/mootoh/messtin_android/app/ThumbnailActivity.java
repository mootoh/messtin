package net.mootoh.messtin_android.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

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
import java.util.Vector;

class ThumbnailImageAdapter extends BaseAdapter {
    static final private String TAG = "ThumbnailImageAdapter";
    final ThumbnailActivity activity_;

    public ThumbnailImageAdapter (ThumbnailActivity activity) {
        this.activity_ = activity;
    }

    @Override
    public int getCount() {
        return activity_.getBitmaps().size();
    }

    @Override
    public Object getItem(int position) {
        return activity_.getBitmaps().get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(activity_);
            imageView.setLayoutParams(new GridView.LayoutParams(152, 152));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageBitmap(activity_.getBitmaps().get(position));
        return imageView;
    }
}

public class ThumbnailActivity extends ImageHavingActivity {
    static final private String TAG = "ThumbnailActivity";
    HashMap <Integer, Bitmap> bitmaps = new HashMap<Integer, Bitmap>();

    public HashMap <Integer, Bitmap> getBitmaps() {
        return bitmaps;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thumbnail);

        DriveId parentDriveId = getIntent().getParcelableExtra("parentDriveId");

        Query query = new Query.Builder()
                .addFilter(Filters.in(SearchableField.PARENTS, parentDriveId))
                .addFilter(Filters.eq(SearchableField.TITLE, "tm"))
                .build();

        final ThumbnailActivity self = this;
        Drive.DriveApi.query(GDriveHelper.getInstance().getClient(), query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.d("@@@", "failed in retrieving thumbnails");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                Metadata md = mb.get(0);

                Query query2 = new Query.Builder()
                        .addFilter(Filters.in(SearchableField.PARENTS, md.getDriveId()))
                        .build();

                Drive.DriveApi.query(GDriveHelper.getInstance().getClient(), query2).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(DriveApi.MetadataBufferResult result2) {
                        for (Metadata md : result2.getMetadataBuffer()) {
                            Log.d(TAG, "thumbnail: " + md.getTitle());
                            RetrieveDriveFileContentsAsyncTask task = new RetrieveDriveFileContentsAsyncTask(self, GDriveHelper.getInstance().getClient());
                            task.execute(md);
                        }
                    }
                });
            }
        });

        ThumbnailImageAdapter imageAdapter = new ThumbnailImageAdapter(this);
        GridView gridView = (GridView) findViewById(R.id.gridview);
        gridView.setAdapter(imageAdapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("page", position);
                setResult(BookReadActivity.JUMP_TO_PAGE, resultIntent);
                finish();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.thumbnail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onChanged(RetrieveDriveFileContentsAsyncTaskResult result) {
        String title = result.md.getTitle();
        int page = Integer.parseInt(title.substring(0, 3));
        Log.d(TAG, "page number: " + page);
        bitmaps.put(new Integer(page), result.getBitamp());

        GridView gridView = (GridView) findViewById(R.id.gridview);
        ((BaseAdapter)gridView.getAdapter()).notifyDataSetChanged();
    }
}
