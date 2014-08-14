package net.mootoh.messtin_android.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BooklistActivity extends Activity {
    final private static String TAG = "BooklistActivity";

    BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.bookinfo, null);
            }

            TextView titleView = (TextView)convertView.findViewById(R.id.title);
            ImageView imageView = (ImageView)convertView.findViewById(R.id.image);

            titleView.setText((String)items.get(position).get("title"));
            imageView.setImageBitmap((Bitmap) items.get(position).get("image"));
            return convertView;
        }
    };

    List <Map<String, Object>> items = new ArrayList<Map<String, Object>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_booklist);
        setupGridView();
        fetchBooksFromParseRemotely();

        BroadcastReceiver cacheReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getStringExtra("error") != null) {
                    Log.e(TAG, "failed in fetching : " + intent.getStringExtra("error"));
                    return;
                }

                items.get(intent.getIntExtra("index", 0)).put("image", intent.getParcelableExtra("bitmap"));
                adapter.notifyDataSetChanged();
            }
        };

        IntentFilter ifilter = new IntentFilter(CacheService.ACTION_FETCH_RESULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(cacheReceiver, ifilter);
    }

    private void fetchBooksFromParseLocally() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Book");
        query.fromLocalDatastore();
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e != null) {
                    showError("failed in retrieving Parse objects from local store: " + e.getMessage());
                    return;
                }
                for (ParseObject obj : parseObjects) {
                    fetchCoverImageWithParseObject(obj);
                }
            }
        });
    }

    private void fetchCoverImageWithParseObject(ParseObject obj) {
    }

    private void fetchBooksFromParseRemotely() {
        final Activity self = this;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Book");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e != null) {
                    showError("failed in retrieving books from parse: " + e.getMessage());
                    return;
                }
                int i=0;
                for (ParseObject obj : parseObjects) {
                    final String objId = obj.getObjectId();
                    final String title = (String)obj.get("title");
                    final Book book = new Book(title);
                    book.setPageCount(obj.getInt("pages"));
                    book.setObjectId(objId);
                    final Map <String, Object> item = new HashMap<String, Object>();
                    item.put("title", title);
                    item.put("book", book);
                    items.add(item);

                    Intent intent = new Intent(self, CacheService.class);
                    intent.setAction(CacheService.ACTION_FETCH);
                    intent.putExtra("book", book);
                    intent.putExtra("index", i);
                    intent.putExtra("path", "cover");
                    startService(intent);
                    i++;
                }

                adapter.notifyDataSetChanged();
            }
        });
    }

    private void setupGridView() {
        GridView gridView = (GridView) findViewById(R.id.gridview);
        gridView.setAdapter(adapter);
        final BooklistActivity self = this;

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent readIntent = new Intent(self, BookReadActivity.class);
                Map<String, ?> item = items.get(position);
                Book book = (Book)item.get("book");
                readIntent.putExtra("book", book);
                startActivity(readIntent);
            }
        });

        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Book book = (Book)items.get(position).get("book");
                Toast toast = Toast.makeText(self, book.getDescription(), Toast.LENGTH_SHORT);
                toast.show();

                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.booklist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, OverallSettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_refresh) {
            items.clear();
            adapter.notifyDataSetChanged();
            fetchBooksFromParseRemotely();

            // debug
            calcSpaceUsed();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    }

    private void showError(final String msg) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public void calcSpaceUsed() {
        Intent intent = new Intent(this, CacheService.class);
        intent.setAction(CacheService.ACTION_CALC_SPACE_USED);
        startService(intent);
    }
}
