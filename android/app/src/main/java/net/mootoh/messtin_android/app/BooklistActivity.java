package net.mootoh.messtin_android.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

final class RetrieveDriveFileContentsAsyncTask extends AsyncTask<DriveId, Boolean, Bitmap> {
    static final String TAG = "RetrieveDriveFileContentsAsyncTask";
    final BooklistActivity activity;
    final GoogleApiClient client;

    public RetrieveDriveFileContentsAsyncTask(BooklistActivity activity, GoogleApiClient client) {
        this.activity = activity;
        this.client = client;
    }

    @Override
    protected Bitmap doInBackground(DriveId... params) {
        DriveFile file = Drive.DriveApi.getFile(client, params[0]);
        DriveApi.ContentsResult contentsResult = file.openContents(client, DriveFile.MODE_READ_ONLY, null).await();
        if (!contentsResult.getStatus().isSuccess()) {
            Log.d(TAG, "failed in retrieving the file content");
            return null;
        }
        InputStream is = contentsResult.getContents().getInputStream();
        /*
        File outFile = new File(context.getCacheDir(), params[0].toString());
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;
        try {
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        Bitmap bm = BitmapFactory.decodeStream(is);
        file.discardContents(client, contentsResult.getContents()).await();
        return bm;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result == null) {
            Log.d(TAG, "Error while reading from the file");
            return;
        }
        Log.d(TAG, "File contents: " + result);
        activity.onChanged();
    }
}

class Book {
    static final private String TAG = "Book";

    final String title;
    final DriveId rootDriveId;
    final BooklistActivity activity;
    Metadata coverMetadata;
    List<Metadata> pages;
    RetrieveDriveFileContentsAsyncTask task;

    Book(Metadata md, final GoogleApiClient client, final BooklistActivity activity) {
        title = md.getTitle();
        rootDriveId = md.getDriveId();
        this.activity = activity;

        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, "cover.jpg"))
                .addFilter(Filters.in(SearchableField.PARENTS, rootDriveId))
                .build();

        Drive.DriveApi.query(client, query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.d("@@@", "failed in retrieving cover image");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                coverMetadata = mb.get(0);
                Log.d(TAG, "cover image id = " + coverMetadata.getDriveId().toString());
                task = new RetrieveDriveFileContentsAsyncTask(activity, client);
                task.execute(coverMetadata.getDriveId());
            }
        });
    }

    public Metadata getCoverMetadata() { return coverMetadata; }

    public Bitmap getCoverBitmap() {
        if (task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.d(TAG, "download still in progress...");
            return null;
        }
        try {
            return task.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}

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
        Log.d(TAG, "getCount " + booklistActivity.getBooks().size());
        return booklistActivity.getBooks().size();
    }

    @Override
    public Object getItem(int position) {
        Log.d(TAG, "getItem for " + position);
        return booklistActivity.getBooks().get(position);
    }

    @Override
    public long getItemId(int position) {
        Log.d(TAG, "getItemId for " + position);
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "getView for " + position);
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

public class BooklistActivity extends Activity {
    final private static int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    final private static String TAG = "BookListActivity";

    GoogleApiClient mGoogleApiClient;
    DriveId messtinFolderId;
    List<Book> books = new ArrayList<Book>();
    ImageAdapter imageAdapter;

    public List<Book> getBooks() {
        return books;
    }

    public void onChanged() {
        imageAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booklist);

        imageAdapter = new ImageAdapter(this);
        GridView gridView = (GridView) findViewById(R.id.gridview);
        gridView.setAdapter(imageAdapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(BooklistActivity.this, "" + position, Toast.LENGTH_SHORT).show();
            }
        });

        final BooklistActivity self = this;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d("@@@", "onConneted");

                        allBookFolders(new AllBookFoldersCallback() {
                            @Override
                            public void onResult(Error error, List<Metadata> metadatas) {
                                if (error != null) {
                                    return;
                                }

                                for (Metadata md : metadatas) {
                                    Log.d(TAG, "book " + md.getTitle());
                                    Book book = new Book(md, mGoogleApiClient, self);
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
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d("@@@", "onConnetionFailed: " + connectionResult.toString());
                        if (connectionResult.hasResolution()) {
                            try {
                                connectionResult.startResolutionForResult(self, RESOLVE_CONNECTION_REQUEST_CODE);
                            } catch (IntentSender.SendIntentException e) {
                                // Unable to resolve, message user appopriately
                            }
                        } else {
                            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), self, 0).show();
                        }
                    }
                })
                .build();
    }

    interface MesstinRootFolderCallback {
        public void onResult(Error error, DriveId driveId);
    }

    private void messtinRootFolder(final MesstinRootFolderCallback callback) {
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, "messtin"))
                .build();

        Drive.DriveApi.query(mGoogleApiClient, query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.d("@@@", "failed in retrieving messtin dir");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
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
                DriveFolder messtinFolder = Drive.DriveApi.getFolder(mGoogleApiClient, messtinFolderId);
                messtinFolder.listChildren(mGoogleApiClient).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                        MetadataBuffer mb = metadataBufferResult.getMetadataBuffer();
                        ArrayList<Metadata> books = new ArrayList<Metadata>();
                        for (Metadata md : mb) {
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
        mGoogleApiClient.connect();
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
            mGoogleApiClient.connect();
        }
    }
}
