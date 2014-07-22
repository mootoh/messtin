package net.mootoh.messtin_android.app;

import android.app.Activity;
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
import android.widget.Toast;

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
import com.ortiz.touch.TouchImageView;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import net.mootoh.messtin_android.app.google.GDriveHelper;
import net.mootoh.messtin_android.app.google.RetrieveDriveFileContentsAsyncTask;
import net.mootoh.messtin_android.app.google.RetrieveDriveFileContentsAsyncTaskDelegate;
import net.mootoh.messtin_android.app.google.RetrieveDriveFileContentsAsyncTaskResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mootoh on 5/11/14.
 */
public class BookReadActivity extends Activity implements RetrieveDriveFileContentsAsyncTaskDelegate {
    private static final String TAG = "BookReadActivity";
    private static final String KEY_PAGE_NUMBER = "KEY_PAGE_NUMBER";
    public static final int JUMP_TO_PAGE = 2;

    Map<String, DriveId> allDriveId = new HashMap<String, DriveId>();
    int currentPage = 1;
    int pageCount = 0;
    DriveId driveId;
    String title;
    ParseObject parseObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_bookread);

        Intent intent = getIntent();
        DriveId driveId = intent.getParcelableExtra("driveId");
        this.driveId = driveId;
        title = intent.getStringExtra("title");

        Log.d(TAG, "driveId for book " + title + ": " + driveId);

        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt(KEY_PAGE_NUMBER);
        }

        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        currentPage = pref.getInt(driveId.toString() + ":page", currentPage);

        updateTitle();

        setup(driveId);
/*
        ParseQuery parseQ = ParseQuery.getQuery("Book");
        parseQ.getInBackground(parseObjectId, new GetCallback() {
            @Override
            public void done(ParseObject po, ParseException e) {
                if (e != null) {
                    Log.d(TAG, "failed in retrieving object from parse: " + e);
                    return;
                }
                parseObject = po;
            }
        });
*/

        final TouchImageView iv = (TouchImageView) findViewById(R.id.imageView);
        iv.setMaxZoom(5);
        iv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        float x = event.getX();
                        float y = event.getY();

                        float left = iv.getWidth() / 4;
                        float right = iv.getWidth() * 3 / 4;
                        float top = iv.getHeight() / 4;
                        float bottom = iv.getHeight() * 3 / 4;

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
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
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
//        retrieveDriveIds(null);
        retrieveFolder(driveId);
    }

    private void retrieveFolder(DriveId driveId) {
        DriveFolder folder = Drive.DriveApi.getFolder(GDriveHelper.getInstance().getClient(), driveId);
        folder.listChildren(GDriveHelper.getInstance().getClient()).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                MetadataBuffer mb = metadataBufferResult.getMetadataBuffer();
                for (Metadata md : mb) {
                    Log.d(TAG, "title: " + md.getTitle());
                }
                mb.close();
            }
        });
    }

    private void retrieveDriveIds(String nextToken) {
        Query.Builder builder = new Query.Builder()
                .addFilter(Filters.in(SearchableField.PARENTS, driveId));
        if (nextToken != null)
            builder.setPageToken(nextToken);
        Query query = builder.build();

        final BookReadActivity self = this;
        Drive.DriveApi.query(GDriveHelper.getInstance().getClient(), query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.d("@@@", "failed in retrieving pages for book: " + title);
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();

                for (Metadata md : mb) {
//                    Log.d(TAG, "image " + md.getTitle() + " id = " + md.getDriveId().toString());
                    allDriveId.put(md.getTitle(), md.getDriveId());
                }
                String token = mb.getNextPageToken();
                mb.close();

                if (token == null) {
                    retrievePage(currentPage);
                    return;
                }
                retrieveDriveIds(token);
            }
        });
    }

    private boolean checkPageBound(int page) {
//        if (page <= 0 || page > pageCount) {
        if (page <= 0 || page > allDriveId.size()) {
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

    private void retrievePage(final int page) {
        /*
        if (!checkPageBound(page)) return;

        String name = filenameForPage(page);
        Log.d(TAG, "retrieving page = " + name);
        RetrieveDriveFileContentsAsyncTask task = new RetrieveDriveFileContentsAsyncTask(GDriveHelper.getInstance().getClient(), this.getCacheDir());
        task.setPage(page);
        task.delegate = this;
        setProgressBarIndeterminateVisibility(true);

        task.execute(allDriveId.get(name));
        */

        String name = filenameForPage(page);
        Query query = new Query.Builder()
                .addFilter(Filters.in(SearchableField.PARENTS, driveId))
//                .addFilter(Filters.eq(SearchableField.TITLE, name))
                .build();

        final BookReadActivity self = this;
        Drive.DriveApi.query(GDriveHelper.getInstance().getClient(), query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.d("@@@", "failed in retrieving page " + page + " for book: " + title);
                    return;
                }
                MetadataBuffer mb = result.getMetadataBuffer();
                Log.d(TAG, "getCount = " + mb.getCount());
                if (mb.getCount() < 1) {
                    Log.e(TAG, "failed in fetching a page " + page);
                    return;
                }

                for (Metadata md : mb) {
                    DriveId pageDriveId = md.getDriveId();
                    Log.d(TAG, "pageDriveId = " + pageDriveId + ", title = " + md.getTitle());
                }

                Metadata md = mb.get(0);
                DriveId pageDriveId = md.getDriveId();
                Log.d(TAG, "pageDriveId = " + pageDriveId + ", title = " + md.getTitle());
                mb.close();

                RetrieveDriveFileContentsAsyncTask task = new RetrieveDriveFileContentsAsyncTask(GDriveHelper.getInstance().getClient(), self.getCacheDir());
                task.setPage(page);
                task.delegate = self;
                setProgressBarIndeterminateVisibility(true);
                task.execute(pageDriveId);
            }
        });
    }

    public void nextPage() {
        if (!checkPageBound(currentPage + 1)) return;
        currentPage++;
        retrievePage(currentPage);
        retrievePage(currentPage + 1);
    }

    private void prevPage() {
        if (!checkPageBound(currentPage - 1)) return;
        currentPage--;
        retrievePage(currentPage);
        retrievePage(currentPage - 1);
    }

    @Override
    public void onError(RetrieveDriveFileContentsAsyncTask task, Error error) {
        setProgressBarIndeterminateVisibility(false);

    }

    @Override
    public void onFinished(RetrieveDriveFileContentsAsyncTask task, RetrieveDriveFileContentsAsyncTaskResult result) {
        setProgressBarIndeterminateVisibility(false);

        ImageView iv = (ImageView) findViewById(R.id.imageView);
        String name = filenameForPage(currentPage);

        if (task.getPage() == currentPage) {
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
                            EditText et = (EditText) customView.findViewById(R.id.page_to_go);
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
        final BookReadActivity self = this;
        switch (id) {
            case R.id.action_goto_page:
                GotoPageDialogFragment gpdf = new GotoPageDialogFragment();
                gpdf.show(getFragmentManager(), "yay");
                return true;
            case R.id.action_save_bookmark:
                ParseObject bookmark = new ParseObject("Bookmark");
                bookmark.put("page", currentPage);
                bookmark.put("book", this.parseObject);
                bookmark.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Log.d(TAG, "failed in saving a bookmark: " + e);
                            return;
                        }
                        Toast.makeText(self, "Bookmark saved", Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            case R.id.action_show_bookmark:
                Intent bookmarkActivity = new Intent(this, BookmarkActivity.class);
                bookmarkActivity.putExtra("parseObjectId", this.parseObject.getObjectId());
                startActivityForResult(bookmarkActivity, JUMP_TO_PAGE);
                return true;
            case R.id.action_show_thumbnails:
                Intent thumbnail = new Intent(this, ThumbnailActivity.class);
                thumbnail.putExtra("parentDriveId", driveId);
                startActivityForResult(thumbnail, JUMP_TO_PAGE);
                return true;
            case R.id.action_pin_this_book:
                pinThisBook();
                Toast.makeText(this, "Pinned", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void pinThisBook() {
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == JUMP_TO_PAGE) {
            int pageTo = data.getIntExtra("page", currentPage);
            currentPage = pageTo;
            retrievePage(pageTo);

        }
    }
}
