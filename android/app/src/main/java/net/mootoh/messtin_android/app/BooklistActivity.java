package net.mootoh.messtin_android.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
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
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;

class ImageAdapter extends BaseAdapter {
    private Context mContext;

    public ImageAdapter(Context ctx) {
        mContext = ctx;
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
//        return null;
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(152, 152));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }
        imageView.setImageResource(R.drawable.cover);
        return imageView;
    }
}

public class BooklistActivity extends Activity {
    final private static int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booklist);

        GridView gridView = (GridView) findViewById(R.id.gridview);
        gridView.setAdapter(new ImageAdapter(this));;

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(BooklistActivity.this, "" + position, Toast.LENGTH_SHORT).show();
            }
        });

        final Activity self = this;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d("@@@", "onConneted");
                        listUpFiles();
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

    private void listUpFiles() {
        DriveFolder rootFolder = Drive.DriveApi.getRootFolder(mGoogleApiClient);

        rootFolder.listChildren(mGoogleApiClient).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (! result.getStatus().isSuccess()) {
                    Log.d("@@@", "failed in retrieving messtin dir");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                for (Metadata md : mb) {
                    Log.d("@@@@", "meta data " + md.getTitle() + " " + md.getDriveId());
                }
            }
        });

        /*
        rootFolder.createFolder(mGoogleApiClient, new MetadataChangeSet.Builder()
                .setTitle("New folder").build());
                */
        /*
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, "cover.jpg"))
//                .addFilter(Filters.eq(SearchableField.MIME_TYPE, "image/jpeg"))
                .build();
//                .addFilter(Filters.eq(SearchableField.MIME_TYPE, "image/jpeg")).build();

        Drive.DriveApi.query(mGoogleApiClient, query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (! result.getStatus().isSuccess()) {
                    Log.d("@@@", "failed in retrieving messtin dir");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                for (Metadata md : mb) {
                    Log.d("@@@@", "meta data " + md.getTitle() + " " + md.getDriveId());
                }
            }
        });
        */
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
