package net.mootoh.messtin_android.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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

    DriveId messtinFolderId;
    List<Book> books = new ArrayList<Book>();
    SimpleAdapter adapter;
    List <Map<String, Object>> items = new ArrayList<Map<String, Object>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booklist);

        setupAdapter();
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
        ((SimpleAdapter)adapter).setViewBinder(viewBinder);

        setupParse();
        setupGridView();
        final BooklistActivity self = this;

        GDriveHelper.createInstance(this, new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Log.d(TAG, "GDrive connected");
                Query query = new Query.Builder()
                        .addFilter(Filters.eq(SearchableField.TITLE, "messtin"))
                        .addFilter(Filters.eq(SearchableField.TRASHED, false))
                        .build();

                Drive.DriveApi.query(GDriveHelper.getInstance().getClient(), query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.d("@@@", "failed in retrieving messtin dir");
                            return;
                        }
                        MetadataBuffer mb = result.getMetadataBuffer();
                        messtinFolderId = mb.get(0).getDriveId();
                        mb.close();
                        Log.d(TAG, "messtin folder id = " + messtinFolderId.toString());
//                        fetchLocalParseObjects();

                        DriveFolder messtinFolder = Drive.DriveApi.getFolder(GDriveHelper.getInstance().getClient(), messtinFolderId);
                        messtinFolder.listChildren(GDriveHelper.getInstance().getClient()).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                            @Override
                            public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                                MetadataBuffer mb = metadataBufferResult.getMetadataBuffer();
                                for (Metadata md : mb) {
                                    Log.d(TAG, "book " + md.getDriveId() + ", title = " + md.getTitle());
                                    final Map<String, Object> item = new HashMap<String, Object>();
//                                    Book book = new Book(md.getTitle());
                                    item.put("title", md.getTitle());
                                    item.put("driveId", md.getDriveId());
                                    items.add(item);
                                    /*
                                    Book book = new Book(md.getTitle(), md.getDriveId(), GDriveHelper.getInstance().getClient(), self, new Book.BookDelegate() {
                                        @Override
                                        public void gotDriveId(Book aBook, DriveId driveId) {
                                            Log.d(TAG, "got book drive id " + driveId);
                                            item.put("title", aBook.title);
                                            adapter.notifyDataSetChanged();
                                        }

                                        @Override
                                        public void gotCoverImage(Book aBook, Bitmap coverImage) {
                                            item.put("image", coverImage);
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                                    */
//                                    books.add(book);
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

//                fetchLocalParseObjects();
//                fetchRemoteParseObjects();
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.d(TAG, "GDrive onConnetionSuspended");
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
                    Log.d("@@@", "failed in retrieving cover image for " + title);
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                Log.d(TAG, "book cover count = " + mb.getCount());
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
                        Log.d(TAG, "failed in retrieving: " + error.getMessage());
                    }

                    @Override
                    public void onFinished(RetrieveDriveFileContentsAsyncTask task, RetrieveDriveFileContentsAsyncTaskResult result) {
                        item.put("image", result.getBitamp());
//                        book.setCoverBitmap(result.getBitamp());
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
        final BooklistActivity self = this;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Book");
        query.fromLocalDatastore();
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e != null) {
                    Log.d(TAG, "failed in retrieving Parse objects from local store: " + e.getMessage());
                    return;
                }
                for (ParseObject obj : parseObjects) {
                    fetchBook(obj);
                }
            }
        });
    }

    private void fetchBook(ParseObject po) {
        Log.d(TAG, "parseObject " + po.getObjectId());
        final Map<String, Object> item = new HashMap<String, Object>();
        final Book book = new Book(po, messtinFolderId, GDriveHelper.getInstance().getClient(), this);
        books.add(book);
        items.add(item);
        adapter.notifyDataSetChanged();
    }

    private void fetchRemoteParseObjects() {
        final BooklistActivity self = this;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Book");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e != null) {
                    Log.d(TAG, "failed in retrieving from parse: " + e.getMessage());
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
/* FIXME
                    Book book = new Book(obj, GDriveHelper.getInstance().getClient(), self);
                    books.add(book);
                    */
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
                /*
                Book book = books.get(position);
                readIntent.putExtra("book", book.rootDriveId);
                readIntent.putExtra("title", book.title);
                readIntent.putExtra("parseObjectId", book.parseObject.getObjectId());
                */

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
                Toast toast = Toast.makeText(self, book.parseObject.getString("description"), Toast.LENGTH_SHORT);
                toast.show();

                return true;
            }
        });
    }

    private void setupParse() {
        Parse.initialize(this, getString(R.string.parse_app_id), getString(R.string.parse_client_key));
    }

    interface MesstinRootFolderCallback {
        public void onResult(Error error, DriveId driveId);
    }

    private void messtinRootFolder(final MesstinRootFolderCallback callback) {
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, "messtin"))
                .addFilter(Filters.eq(SearchableField.TRASHED, false))
                .build();

        Drive.DriveApi.query(GDriveHelper.getInstance().getClient(), query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.d("@@@", "failed in retrieving messtin dir");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                Log.d(TAG, "messtin root dir count = " + mb.getCount());
                messtinFolderId = mb.get(0).getDriveId();
                mb.close();
                Log.d(TAG, "messtin folder id = " + messtinFolderId.toString());
                callback.onResult(null, messtinFolderId);
            }
        });
    }

    interface AllBookFoldersCallback {
        public void onResult(Error error, List<Metadata> books);
    }

    private void allBookFolders(final AllBookFoldersCallback callback) {
        messtinRootFolder(new MesstinRootFolderCallback() {
            @Override
            public void onResult(Error error, DriveId driveId) {
                if (error != null) {
                    return;
                }
                DriveFolder messtinFolder = Drive.DriveApi.getFolder(GDriveHelper.getInstance().getClient(), messtinFolderId);
                messtinFolder.listChildren(GDriveHelper.getInstance().getClient()).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                        MetadataBuffer mb = metadataBufferResult.getMetadataBuffer();
                        ArrayList<Metadata> books = new ArrayList<Metadata>();
                        for (Metadata md : mb) {
                            if (! md.isTrashed())
                                books.add(md);
                        }
                        callback.onResult(null, books);
                    }
                });
            }
        });
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
}
