package net.mootoh.messtin_android.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BooklistActivity extends Activity {
    final private static int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    final private static String TAG = "BookListActivity";

    SimpleAdapter adapter;
    List<Book> books = new ArrayList<Book>();
    List <Map<String, Object>> items = new ArrayList<Map<String, Object>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booklist);
        setupAdapter();
        setupGridView();
        setupParse();

        GDriveHelper.createInstance(this, new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Log.d(TAG, "GDrive connected");

                fetchLocalParseObjects();
//                fetchRemoteParseObjects();

                retrieveMesstinFolder(new RetrieveMesstinFolderCallback() {
                    @Override
                    public void onRetrieved(final DriveId driveId) {
                        DriveFolder messtinFolder = Drive.DriveApi.getFolder(GDriveHelper.getInstance().getClient(), driveId);
                        messtinFolder.listChildren(GDriveHelper.getInstance().getClient()).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                            @Override
                            public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                                MetadataBuffer mb = metadataBufferResult.getMetadataBuffer();
                                for (Metadata md : mb) {
                                    final Map<String, Object> item = new HashMap<String, Object>();
                                    item.put("title", md.getTitle());
                                    item.put("driveId", md.getDriveId());
                                    items.add(item);
                                    adapter.notifyDataSetChanged();
                                }
                                mb.close();

                                for (Map<String, Object> item: items) {
                                    getCoverImage(item);
                                }
                            }
                        });
                    }
                });
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.d(TAG, "GDrive onConnetionSuspended");
            }
        });
    }

    private interface RetrieveMesstinFolderCallback {
        public void onRetrieved(final DriveId driveId);
    }

    private void retrieveMesstinFolder(final RetrieveMesstinFolderCallback callback) {
        Query query = new Query.Builder()
            .addFilter(Filters.eq(SearchableField.TITLE, "messtin"))
            .addFilter(Filters.eq(SearchableField.TRASHED, false))
            .build();

        Drive.DriveApi.query(GDriveHelper.getInstance().getClient(), query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    showError("Cannot find messtin folder in Google Drive");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                DriveId rootDriveId = mb.get(0).getDriveId();
                mb.close();
                callback.onRetrieved(rootDriveId);
            }
        });
    }

    private void getCoverImage(final Map<String, Object> item) {
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, "cover.jpg"))
                .addFilter(Filters.in(SearchableField.PARENTS, (DriveId)item.get("driveId")))
                .build();

        final BooklistActivity self = this;

        Drive.DriveApi.query(GDriveHelper.getInstance().getClient(), query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                String title = (String)item.get("title");

                if (!result.getStatus().isSuccess()) {
                    showError("failed in retrieving cover image for " + title);
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                Log.d(TAG, "book " + title + " cover count: " + mb.getCount());
                if (mb.getCount() < 1) {
                    Log.d(TAG, "no cover image for " + title);
                    return;
                }

                Metadata md = mb.get(0);
                DriveId driveId = md.getDriveId();
                mb.close();

                RetrieveDriveFileContentsAsyncTask task = new RetrieveDriveFileContentsAsyncTask(GDriveHelper.getInstance().getClient(), self.getCacheDir());
                task.delegate = new RetrieveDriveFileContentsAsyncTaskDelegate() {
                    @Override
                    public void onError(RetrieveDriveFileContentsAsyncTask task, Error error) {
                        showError("failed in retrieving cover image: " + error.getMessage());
                    }

                    @Override
                    public void onFinished(RetrieveDriveFileContentsAsyncTask task, RetrieveDriveFileContentsAsyncTaskResult result) {
                        item.put("image", result.getBitamp());
                        adapter.notifyDataSetChanged();
                    }
                };
                task.execute(driveId);
            }
        });
    }

    private void setupAdapter() {
        adapter = new SimpleAdapter(this, items, R.layout.bookinfo, new String[] { "image", "title"}, new int[] { R.id.image, R.id.title});
        SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRep) {
                if (view.getId() == R.id.title) {
                    ((TextView)view).setText((String)data);
                    return true;
                } else if (view.getId() == R.id.image) {
                    ((ImageView)view).setImageBitmap((Bitmap)data);
                    return true;
                }
                return false;
            }
        };
        adapter.setViewBinder(viewBinder);
    }

    private void fetchLocalParseObjects() {
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
        final Map<String, Object> item = new HashMap<String, Object>();
        item.put("title", obj.get("title"));
        items.add(item);

        RetrieveDriveFileContentsAsyncTask task = new RetrieveDriveFileContentsAsyncTask(GDriveHelper.getInstance().getClient(), this.getCacheDir());
        task.delegate = new RetrieveDriveFileContentsAsyncTaskDelegate() {
            @Override
            public void onError(RetrieveDriveFileContentsAsyncTask task, Error error) {
                Log.d(TAG, "failed in retrieving: " + error.getMessage());
            }

            @Override
            public void onFinished(RetrieveDriveFileContentsAsyncTask task, RetrieveDriveFileContentsAsyncTaskResult result) {
                item.put("image", result.getBitamp());
                adapter.notifyDataSetChanged();
            }
        };
        DriveId driveId = DriveId.decodeFromString((String)obj.get("gd_id"));
        task.execute(driveId);
    }
/*
    private void fetchRemoteParseObjects() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Book");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e != null) {
                    showError("failed in retrieving books from parse: " + e.getMessage());
                    return;
                }
                for (ParseObject obj : parseObjects) {
                    boolean found = false;
                    for (Book book : books) {
                        if (book.parseObject.equals(obj)) {
                            Log.d(TAG, "parseObject " + obj.getObjectId() + " already exists, skipping");
                            found = true;
                            break;
                        }
                    }
                    if (found)
                        continue;

                    obj.pinInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                Log.e(TAG, "failed in pinning to local store: " + e.getMessage());
                            }
                            Log.d(TAG, "done pinning to local store");
                        }
                    });
//FIXME
//                    Book book = new Book(obj, GDriveHelper.getInstance().getClient(), self);
//                    books.add(book);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
 */
    private void setupGridView() {
        GridView gridView = (GridView) findViewById(R.id.gridview);
        gridView.setAdapter(adapter);
        final BooklistActivity self = this;

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent readIntent = new Intent(self, BookReadActivity.class);
                Map<String, ?> item = items.get(position);
                readIntent.putExtra("book", (DriveId)item.get("driveId"));
                readIntent.putExtra("title", (String)item.get("title"));
                startActivity(readIntent);
            }
        });

        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Book book = books.get(position);
                Toast toast = Toast.makeText(self, book.getDescription(), Toast.LENGTH_SHORT);
                toast.show();

                return true;
            }
        });
    }

    private void setupParse() {
        Parse.initialize(this, getString(R.string.parse_app_id), getString(R.string.parse_client_key));
    }

    @Override
    protected void onStart() {
        super.onStart();
        GDriveHelper.getInstance().getClient().connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.booklist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, OverallSettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == RESOLVE_CONNECTION_REQUEST_CODE && resultCode == RESULT_OK) {
            GDriveHelper.getInstance().getClient().connect();
        }
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
}