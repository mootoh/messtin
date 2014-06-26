package net.mootoh.messtin_android.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import net.mootoh.messtin_android.app.R;

import java.util.List;

public class BookmarkActivity extends Activity {
    private static final String TAG = "BookmarkActivity";
    ParseObject parseObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);

        Intent intent = getIntent();
        String parseObjectId = intent.getStringExtra("parseObjectId");
        ParseQuery parseQ = ParseQuery.getQuery("Book");
        parseQ.getInBackground(parseObjectId, new GetCallback() {
            @Override
            public void done(ParseObject po, ParseException e) {
                if (e != null) {
                    Log.d(TAG, "failed in retrieving object from parse: " + e);
                    return;
                }
                parseObject = po;
                fetchBookmarks();
            }
        });
    }

    private void fetchBookmarks() {
        final BookmarkActivity self = this;
        ParseQuery query = new ParseQuery("Bookmark");
        query.whereEqualTo("book", this.parseObject);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (e != null) {
                    Log.d(TAG, "failed in retrieving bookmark list from parse: " + e);
                    return;
                }

                ArrayAdapter<String> aAdapter = new ArrayAdapter<String>(self, android.R.layout.simple_list_item_1);
                for (ParseObject bookmark : list) {
                    Log.d(TAG, "bookmark: " + bookmark.getNumber("page"));
                    aAdapter.add("Page " + bookmark.getNumber("page").toString());
                }
                ListView lv = (ListView)self.findViewById(R.id.bookmark_list);
                lv.setAdapter(aAdapter);

                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        ListView listView = (ListView) parent;
                        String item = (String) listView.getItemAtPosition(position);
                        String[] strs = item.split("\\s");
                        int page = Integer.parseInt(strs[1]);
//                        Toast.makeText(BookmarkActivity.this, item, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "clicked page = " + page);
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("page", page);
                        setResult(BookReadActivity.JUMP_TO_PAGE, resultIntent);
                        finish();
                    }
                });
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bookmark, menu);
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
