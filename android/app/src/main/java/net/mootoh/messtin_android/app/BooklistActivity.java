package net.mootoh.messtin_android.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

class ImageAdapter extends BaseAdapter {
    final private Context mContext;
    final private BooklistActivity booklistActivity;
    static final private String TAG = "ImageAdapter";

    public ImageAdapter(BooklistActivity ba) {
        booklistActivity = ba;
        mContext = ba;
    }

    @Override
    public int getCount() {
        return booklistActivity.getBooks().size();
    }

    @Override
    public Object getItem(int position) {
        return booklistActivity.getBooks().get(position);
    }

    @Override
    public long getItemId(int position) {
        Log.d(TAG, "getItemId for " + position);
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(152, 152));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }
        Book book = booklistActivity.getBooks().get(position);
        if (book == null || book.getCoverMetadata() == null) {
            imageView.setImageResource(R.drawable.cover);
        } else {
            Bitmap bm = book.getCoverBitmap();
            imageView.setImageBitmap(bm);
        }

        return imageView;
    }
}

public class BooklistActivity extends ImageHavingActivity {
    final private static int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    final private static String TAG = "BookListActivity";
    public static final String APP_ID = "YOUR_APP_ID";
    public static final String CLIENT_KEY = "YOUR_CLIENT_KEY";

    DriveId messtinFolderId;
    List<Book> books = new ArrayList<Book>();
    ImageAdapter imageAdapter;

    public List<Book> getBooks() {
        return books;
    }

    @Override
    public void onChanged(RetrieveDriveFileContentsAsyncTaskResult result) {
        imageAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booklist);

//        setupParse();

        imageAdapter = new ImageAdapter(this);
        GridView gridView = (GridView) findViewById(R.id.gridview);
        gridView.setAdapter(imageAdapter);

        final BooklistActivity self = this;

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent readIntent = new Intent(self, BookReadActivity.class);
                Book book = books.get(position);
                readIntent.putExtra("book", book.rootDriveId);
                startActivity(readIntent);
            }
        });

        GDriveHelper.createInstance(this, new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                allBookFolders(new AllBookFoldersCallback() {
                    @Override
                    public void onResult(Error error, List<Metadata> metadatas) {
                        if (error != null) {
                            return;
                        }

                        for (Metadata md : metadatas) {
                            Log.d(TAG, "book " + md.getTitle());
                            Book book = new Book(md, GDriveHelper.getInstance().getClient(), self);
                            books.add(book);
                        }
                        imageAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.d("@@@", "onConnetionSuspended");
            }
        });
    }

    private void setupParse() {
        Parse.initialize(this, APP_ID, CLIENT_KEY);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Book");
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                Log.d(TAG, "done fetching: " + parseObject.toString());
                if (e != null) {
                    Log.d(TAG, "failed in retrieving from parse: " + e.getMessage());
                    return;
                }

            }
        });
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
