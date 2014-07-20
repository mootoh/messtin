package net.mootoh.messtin_android.app;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
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

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.Scopes;
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
import com.google.api.services.drive.DriveScopes;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BooklistActivity extends Activity {
    final private static int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    final private static String TAG = "BookListActivity";

    SimpleAdapter adapter;
    List <Map<String, Object>> items = new ArrayList<Map<String, Object>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        setContentView(R.layout.activity_booklist);
        setupAdapter();
        setupGridView();
        setupParse();

        GDriveHelper.createInstance(this, new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
//                fetchBooksFromParseLocally();
//                fetchBooksFromParseRemotely();
                fetchBooksFromGDrive();
//                test();
            }

            @Override
            public void onConnectionSuspended(int why) {
                String msg = why == CAUSE_SERVICE_DISCONNECTED  ? "disconnected" : "no network";
                showError("failed in connecting to GDrive : " + msg);
            }
        });
        */

        testGoogleApiClient();
    }

    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;

    private void testGoogleApiClient() {
        pickUserAccount();
    }

    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    @Override
    protected void onStart() {
        super.onStart();
//        GDriveHelper.getInstance().connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        GDriveHelper.getInstance().disconnect();
    }

    private void test() {
        String resourceId = "0B0v3qwjLutgMZGlVc3ZhbWFibkU";
        com.google.android.gms.common.api.PendingResult<com.google.android.gms.drive.DriveApi.DriveIdResult> pr = Drive.DriveApi.fetchDriveId(GDriveHelper.getInstance().getClient(), resourceId);
        pr.setResultCallback(new ResultCallback<DriveApi.DriveIdResult>() {
            @Override
            public void onResult(DriveApi.DriveIdResult driveIdResult) {
                Log.d(TAG, "received driveId: " + driveIdResult.getDriveId());

                Query query = new Query.Builder()
                        .addFilter(Filters.in(SearchableField.PARENTS, driveIdResult.getDriveId()))
                        .build();

                Drive.DriveApi.query(GDriveHelper.getInstance().getClient(), query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.e(TAG, "failed in retrieving");
                            return;
                        }
                        MetadataBuffer mb = result.getMetadataBuffer();
                        Log.d(TAG, "result count : " + mb.getCount());
                        for (Metadata md: mb) {
                            Log.d(TAG, "md.title: " + md.getTitle() + ", resourceId:" + md.getDriveId().getResourceId());
                        }
                        mb.close();
                    }
                });
            }
        });
    }

    private void fetchBooksFromGDrive() {
        retrieveMesstinFolder(new RetrieveMesstinFolderCallback() {
            @Override
            public void onRetrieved(final DriveId driveId) {
                DriveFolder messtinFolder = Drive.DriveApi.getFolder(GDriveHelper.getInstance().getClient(), driveId);
                messtinFolder.listChildren(GDriveHelper.getInstance().getClient()).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                        MetadataBuffer mb = metadataBufferResult.getMetadataBuffer();
                        for (Metadata md : mb) {
                            Book book = new Book(md.getTitle());
                            book.setRootDriveId(md.getDriveId());
                            Log.d(TAG, "book title: " + md.getTitle() + " driveId: " + md.getDriveId() + " resourceId: " + md.getDriveId().getResourceId());

                            Map<String, Object> item = new HashMap<String, Object>();
                            item.put("title", book.getTitle());
                            item.put("book", book);

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
        Book book = (Book)item.get("book");
        final String title = book.getTitle();
        DriveId driveId = book.getRootDriveId();

        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, "cover.jpg"))
                .addFilter(Filters.in(SearchableField.PARENTS, driveId))
                .build();

        final BooklistActivity self = this;

        Drive.DriveApi.query(GDriveHelper.getInstance().getClient(), query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.d(TAG, "failed in retrieving cover image for " + title);
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                Log.d(TAG, "book " + title + " cover count: " + mb.getCount());
                if (mb.getCount() < 1) {
                    Log.d(TAG, "no cover image for " + title);
                    mb.close();
                    return;
                }

                Metadata md = mb.get(0);
                DriveId coverDriveId = md.getDriveId();
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
                task.execute(coverDriveId);
            }
        });
    }

    private void setupAdapter() {
        adapter = new SimpleAdapter(this, items, R.layout.bookinfo, new String[] { "image", "title"}, new int[] { R.id.image, R.id.title});
        SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRep) {
                if (view.getId() == R.id.title) {
                    ((TextView)view).setText((String) data);
                    return true;
                } else if (view.getId() == R.id.image) {
                    if (data != null)
                        ((ImageView)view).setImageBitmap((Bitmap)data);
                    return true;
                }
                return false;
            }
        };
        adapter.setViewBinder(viewBinder);
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

    private void fetchBooksFromParseRemotely() {
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
                    for (Map<String, Object> item: items) {
                        Book book = (Book)item.get("book");
                        ParseObject storedParseObject = book.getParseObject();
                        if (storedParseObject != null && storedParseObject.equals(obj)) { // TODO: should consider updated time stamp
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
                                showError("failed in pinning to local store: " + e.getMessage());
                                return;
                            }
                            Log.d(TAG, "done pinning to local store");
                        }
                    });

                    final String title = (String)obj.get("title");
                    final Book book = new Book(title);
                    final Map <String, Object> item = new HashMap<String, Object>();
                    item.put("title", title);
                    item.put("book", book);
                    items.add(item);

                    com.google.android.gms.common.api.PendingResult<com.google.android.gms.drive.DriveApi.DriveIdResult> pr = Drive.DriveApi.fetchDriveId(GDriveHelper.getInstance().getClient(), obj.getString("gd_id"));
                    pr.setResultCallback(new ResultCallback<DriveApi.DriveIdResult>() {
                        @Override
                        public void onResult(DriveApi.DriveIdResult driveIdResult) {
                            Log.d(TAG, "received driveId for " + title + ": " + driveIdResult.getDriveId());
                            book.setRootDriveId(driveIdResult.getDriveId());
                            getCoverImage(item);
                        }
                    });

                }
                adapter.notifyDataSetChanged();

                for (Map<String, Object> item: items) {
                    getCoverImage(item);
                }
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
                readIntent.putExtra("title", book.getTitle());
                readIntent.putExtra("driveId", book.getRootDriveId());
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

    private void setupParse() {
        Parse.initialize(this, getString(R.string.parse_app_id), getString(R.string.parse_client_key));
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
        } else if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            if (resultCode == RESULT_OK) {
                final String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                Log.d(TAG, "authorized, email = " + email);

                final BooklistActivity self = this;

                AsyncTask task = new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] params) {
                        try {
                            String token = GoogleAuthUtil.getToken(self, email, DriveScopes.DRIVE_FILE);
                            Log.d(TAG, "auth token = " + token);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (GoogleAuthException e) {
                            Log.e(TAG, "auth exception: " + e.getMessage() + ", " + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
                task.execute((Void)null);

            }
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