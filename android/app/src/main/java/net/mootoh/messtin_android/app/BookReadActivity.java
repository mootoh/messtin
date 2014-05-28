package net.mootoh.messtin_android.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.ortiz.touch.TouchImageView;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mootoh on 5/11/14.
 */
public class BookReadActivity extends ImageHavingActivity {
    private static final String TAG = "BookReadActivity";
    private static final String KEY_PAGE_NUMBER = "KEY_PAGE_NUMBER";
    Map<String, Metadata> allMetadata = new HashMap<String, Metadata>();
    int currentPage = 1;
    DriveId driveId;
    String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_bookread);

        Intent intent = getIntent();
        DriveId driveId = intent.getParcelableExtra("book");
        this.driveId = driveId;
        title = intent.getStringExtra("title");

        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt(KEY_PAGE_NUMBER);
        }

        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        currentPage = pref.getInt(driveId.toString() + ":page", currentPage);

        updateTitle();

        setup(driveId);

        final TouchImageView iv = (TouchImageView)findViewById(R.id.imageView);
        iv.setMaxZoom(5);
        iv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        float x = event.getX();
                        float y = event.getY();
                        
                        float left = iv.getWidth()/4;
                        float right = iv.getWidth()*3/4;
                        float top = iv.getHeight()/4;
                        float bottom = iv.getHeight()*3/4;
                        
                        if (x > right) {
                            nextPage();
                        } else if (x < left) {
                            prevPage();
                        } else if (y > top && y < bottom) {
                            toggleFullscreen();
                        }
                        break;
                }
                return false;
            }
        });

        hideSystemUI();
    }

    private void updateTitle() {
        setTitle(title + " - " + currentPage);
    }

    boolean isInFullscreen = true;
    private void toggleFullscreen() {
        if (isInFullscreen) {
            showSystemUI();
        } else {
            hideSystemUI();
        }
        isInFullscreen = !isInFullscreen;
    }

    private void showSystemUI() {
        View v = findViewById(R.id.imageView);
        v.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        View v = findViewById(R.id.imageView);
        v.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt(KEY_PAGE_NUMBER, currentPage);
        saveCurrentPage();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentPage();
    }

    private void saveCurrentPage() {
        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        edit.putInt(driveId.toString() + ":page", currentPage);
        edit.commit();
    }

    public void setup(DriveId driveId) {
        Query query = new Query.Builder()
                .addFilter(Filters.in(SearchableField.PARENTS, driveId))
                .build();

        final BookReadActivity self = this;
        Drive.DriveApi.query(GDriveHelper.getInstance().getClient(), query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.d("@@@", "failed in retrieving cover image");
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                for (Metadata md : mb) {
//                    Log.d(TAG, "image " + md.getTitle() + " id = " + md.getDriveId().toString());
                    allMetadata.put(md.getTitle(), md);
                }

                retrievePage(currentPage);
            }
        });
    }

    private boolean checkPageBound(int page) {
        if (page <= 0 || page > allMetadata.size()) {
            Log.d(TAG, "out of bound: page=" + page);
            return false;
        }
        return true;
    }

    private String filenameForPage(int page) {
        String name = "%03d.jpg";
        name = String.format(name, page);
        return name;
    }

    private void retrievePage(int page) {
        if (! checkPageBound(page)) return;

        String name = filenameForPage(page);
        Log.d(TAG, "retrieving page = " + name);
        RetrieveDriveFileContentsAsyncTask task = new RetrieveDriveFileContentsAsyncTask(this, GDriveHelper.getInstance().getClient());
        setProgressBarIndeterminateVisibility(true);
        task.execute(allMetadata.get(name));
    }

    public void nextPage() {
        if (! checkPageBound(currentPage+1)) return;
        currentPage++;
        retrievePage(currentPage);
        retrievePage(currentPage + 1);
    }

    private void prevPage() {
        if (! checkPageBound(currentPage-1)) return;
        currentPage--;
        retrievePage(currentPage);
        retrievePage(currentPage - 1);
    }

    @Override
    public void onChanged(RetrieveDriveFileContentsAsyncTaskResult result) {
        setProgressBarIndeterminateVisibility(false);

        ImageView iv = (ImageView)findViewById(R.id.imageView);
        String name = filenameForPage(currentPage);

        if (result.getMetadata().equals(allMetadata.get(name))) {
            iv.setImageBitmap(result.getBitamp());
            updateTitle();
        }
    }

    class GotoPageDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final View customView = inflater.inflate(R.layout.dialog_gotopage, null);
            builder.setMessage("Goto page")
                    .setView(customView)
                    .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText et = (EditText)customView.findViewById(R.id.page_to_go);
                            int page = Integer.parseInt(et.getText().toString());
                            currentPage = page;
                            retrievePage(page);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    });
            return builder.create();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bookread, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_goto_page) {
            GotoPageDialogFragment gpdf = new GotoPageDialogFragment();
            gpdf.show(getFragmentManager(), "yay");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
