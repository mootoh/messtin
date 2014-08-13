package net.mootoh.messtin_android.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
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
import com.ortiz.touch.TouchImageView;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import net.mootoh.messtin_android.app.google.GDriveHelper;

/**
 * Created by mootoh on 5/11/14.
 */
public class BookReadActivity extends Activity {
    private static final String TAG = "BookReadActivity";
    private static final String KEY_PAGE_NUMBER = "KEY_PAGE_NUMBER";
    public static final int JUMP_TO_PAGE = 2;

    int currentPage = 1;
    Book book;
    String title;
    ParseObject parseObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_bookread);

        Intent intent = getIntent();
        book = intent.getParcelableExtra("book");
        title = book.getTitle();

        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt(KEY_PAGE_NUMBER);
        } else {
            SharedPreferences pref = getPreferences(MODE_PRIVATE);
            currentPage = pref.getInt(book.getObjectId() + ":page", currentPage);
        }

        updateTitle();

        ParseQuery parseQ = ParseQuery.getQuery("Book");
        parseQ.getInBackground(book.getObjectId(), new GetCallback() {
            @Override
            public void done(ParseObject po, ParseException e) {
                if (e != null) {
                    Log.d(TAG, "failed in retrieving object from parse: " + e);
                    return;
                }
                parseObject = po;
            }
        });

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

        retrievePage(currentPage, true);
        hideSystemUI();

        BroadcastReceiver cacheReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getStringExtra("error") != null) {
                    Log.e(TAG, "failed in fetching : " + intent.getStringExtra("error"));
                    return;
                }
/*
                items.get(intent.getIntExtra("index", 0)).put("image", intent.getParcelableExtra("bitmap"));
                adapter.notifyDataSetChanged();
                */
                setProgressBarIndeterminateVisibility(false);

//                if (! toShow) return;
                ImageView iv = (ImageView) findViewById(R.id.imageView);
                Bitmap bitmap = intent.getParcelableExtra("bitmap");
                iv.setImageBitmap(bitmap);
                updateTitle();
                iv.invalidate();
            }
        };
        IntentFilter ifilter = new IntentFilter(CacheService.ACTION_FETCH_RESULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(cacheReceiver, ifilter);

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
        edit.putInt(book.getObjectId() + ":page", currentPage);
        edit.commit();
    }

    private boolean checkPageBound(int page) {
//        if (page <= 0 || page > pageCount) {
        if (page <= 0 || page > book.getPageCount()) {
            Log.d(TAG, "out of bound: page=" + page);
            return false;
        }
        return true;
    }

    private void retrievePage(final int page, final boolean toShow) {
        if (!checkPageBound(page)) return;

        setProgressBarIndeterminateVisibility(true);

        Intent intent = new Intent(this, CacheService.class);
        intent.setAction(CacheService.ACTION_FETCH);
        intent.putExtra("book", book);

        String name = "%03d";
        name = String.format(name, page);
        intent.putExtra("name", name);
        intent.putExtra("toShow", toShow);
        startService(intent);

        /*
        BookStorage storage = ((MesstinApplication)getApplication()).getBookStorage();
        storage.retrieve(book, page, new OnImageRetrieved() {
            @Override
            public void onRetrieved(Error error, final Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setProgressBarIndeterminateVisibility(false);

                        if (! toShow) return;
                        ImageView iv = (ImageView) findViewById(R.id.imageView);
                        iv.setImageBitmap(bitmap);
                        updateTitle();
                        iv.invalidate();
                    }
                });
            }
        });
        */
    }

    public void nextPage() {
        if (!checkPageBound(currentPage + 1)) return;
        currentPage++;
        retrievePage(currentPage, true);
        retrievePage(currentPage + 1, false);
    }

    private void prevPage() {
        if (!checkPageBound(currentPage - 1)) return;
        currentPage--;
        retrievePage(currentPage, true);
        retrievePage(currentPage - 1, false);
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
                            retrievePage(page, true);
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
                thumbnail.putExtra("book", book);
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
            retrievePage(pageTo, true);

        }
    }
}
