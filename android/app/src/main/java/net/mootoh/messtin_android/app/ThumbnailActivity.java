package net.mootoh.messtin_android.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.HashMap;

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

public class ThumbnailActivity extends Activity {
    static final private String TAG = "ThumbnailActivity";
    HashMap <Integer, Bitmap> bitmaps = new HashMap<Integer, Bitmap>();

    public HashMap <Integer, Bitmap> getBitmaps() {
        return bitmaps;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thumbnail);
        final Book book = getIntent().getParcelableExtra("book");

        final ThumbnailImageAdapter imageAdapter = new ThumbnailImageAdapter(this);
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

        for (int i=1; i<book.pageCount; i++) {
            final int page = i;
            BookStorage storage = ((MesstinApplication)getApplication()).getBookStorage();
            String path = "tm/" + storage.filenameForPage(page);
            storage.retrieve(book, path, new OnImageRetrieved() {
                @Override
                public void onRetrieved(Error error, InputStream is) {
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    bitmaps.put(page, bitmap);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        }
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
}
