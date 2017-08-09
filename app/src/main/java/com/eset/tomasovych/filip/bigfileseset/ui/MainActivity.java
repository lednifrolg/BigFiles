package com.eset.tomasovych.filip.bigfileseset.ui;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eset.tomasovych.filip.bigfileseset.R;
import com.eset.tomasovych.filip.bigfileseset.Utils.CurrentState;
import com.eset.tomasovych.filip.bigfileseset.Utils.FilesScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<File>> {

    public static final String EXTRA_NUMBER_OF_FILES = "com.eset.tomasovych.filip.NUMBER_OF_FILES";
    public static final String EXTRA_LARGEST_FILES = "com.eset.tomasovych.filip.EXTRA_LARGEST_FILES";
    public static final String EXTRA_PROGRESS = "com.eset.tomasovych.filip.PROGRESS";
    public static final String EXTRA_PROGRESS_MAX = "com.eset.tomasovych.filip.PROGRESS_MAX";
    public static final String EXTRA_CURRENT_STATE = "com.eset.tomasovych.filip.CURRENT_STATE";
    public static final String EXTRA_DIRECTORIES = "com.eset.tomasovych.filip.DIRECTORIES";
    public static final String EXTRA_DIR_CURRENT_PROGRESS = "com.eset.tomasovych.filip.EXTRA_DIR_CURRENT_PROGRESS";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MESSAGE_CANCEL = -1;
    private static final int NOTIFICATION_ID = 302;
    private static final int DIRECTORY_REQUEST_CODE = 93;
    private static final int FILES_LOADER_ID = 301;
    private static final int SAVED_FILES_LOADER_ID = 300;
    private final Handler mHandler = new MyHandler();
    private FilesScanner mFilesScanner;
    private int mBackPressedCounter = 0;
    private TextInputLayout mTextInputLayout;
    private EditText mNumberOFFilesEditText;
    private TextView mLoadingDirectoryTextView;
    private RecyclerView mDirectoriesRecyclerView;
    private RecyclerView mFilesRecyclerView;
    private ProgressBar mCalculatingProgressBar;
    private FloatingActionButton mFab;
    private CurrentState mCurrentState;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private DirectoryListAdapter mDirectoryListAdapter;
    private FileListAdapter mFIleFileListAdapter;
    private List<File> mSelectedDirectories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFilesScanner = new FilesScanner(mHandler, this);

        mFab = (FloatingActionButton) findViewById(R.id.fab_search);

        mTextInputLayout = (TextInputLayout) findViewById(R.id.text_input_layout);
        mTextInputLayout.setHint(getString(R.string.number_of_files_hint));
        mNumberOFFilesEditText = (EditText) findViewById(R.id.et_files_number);
        mLoadingDirectoryTextView = (TextView) findViewById(R.id.tv_loading_directory);

        mCalculatingProgressBar = (ProgressBar) findViewById(R.id.calculating_progress_bar);

        mDirectoriesRecyclerView = (RecyclerView) findViewById(R.id.rv_directory_list);
        mDirectoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mDirectoryListAdapter = new DirectoryListAdapter(this, null, false);
        mDirectoriesRecyclerView.setAdapter(mDirectoryListAdapter);

        mFilesRecyclerView = (RecyclerView) findViewById(R.id.rv_file_list);
        mFilesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mFIleFileListAdapter = new FileListAdapter(this, null);
        mFilesRecyclerView.setAdapter(mFIleFileListAdapter);

        mSelectedDirectories = new ArrayList<>();
        getSupportLoaderManager().initLoader(FILES_LOADER_ID, null, this);

        setUpSwipeToDeleteDirectory();
        setUpKeyboardDoneListener();

        // Check if application is opened via Notification, if it is load Files from serialized objects
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean(EXTRA_LARGEST_FILES, false)) {
            getSupportLoaderManager().initLoader(SAVED_FILES_LOADER_ID, null, this);
        }

        if (savedInstanceState != null) {
            loadSavedState(savedInstanceState);
        }
    }

    /**
     * Set onEditorActionListener for keyboard special button
     */
    private void setUpKeyboardDoneListener() {
        mNumberOFFilesEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_SEARCH) {
                    onSearchBiggestFilesClick(textView);
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Setup activity based on savedInstance state
     *
     * @param savedInstanceState
     */
    private void loadSavedState(Bundle savedInstanceState) {
        mCurrentState = (CurrentState) savedInstanceState.getSerializable(EXTRA_CURRENT_STATE);
        mSelectedDirectories = (List<File>) savedInstanceState.getSerializable(EXTRA_DIRECTORIES);

        mDirectoryListAdapter.swapDirs(mSelectedDirectories);

        if (mCurrentState == null) {
            return;
        }

        switch (mCurrentState) {
            case DIRECTORIES:
                showDirs();
                break;
            case FILES:
                showFiles();
                break;
            case FILES_LOADING:
                showLoadingFiles();
                break;
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_CURRENT_STATE, mCurrentState);
        outState.putSerializable(EXTRA_DIRECTORIES, (Serializable) mSelectedDirectories);

        super.onSaveInstanceState(outState);
    }

    /**
     * Custom behavior for Back button
     */
    @Override
    public void onBackPressed() {
        if (mCurrentState == CurrentState.FILES_LOADING) {
            mBackPressedCounter++;

            if (mBackPressedCounter == 2) {
                getSupportLoaderManager().destroyLoader(FILES_LOADER_ID);
                showDirs();
                mBackPressedCounter = 0;

                Message cancelNotification = new Message();
                cancelNotification.what = MESSAGE_CANCEL;
                mHandler.sendMessage(cancelNotification);
            } else {
                Toast.makeText(this, R.string.toast_search_cancel_message, Toast.LENGTH_SHORT).show();
            }
        } else if (mCurrentState == CurrentState.FILES && mSelectedDirectories != null && mSelectedDirectories.size() > 0) {
            mBackPressedCounter = 0;
            showDirs();
        } else {
            mBackPressedCounter = 0;
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.default_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                showAboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Open new dialog with About information
     */
    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Big File Finder for Android\n" +
                "Eset Android developer challenge\n" +
                "Created by Filip Tomasovych, August 2017")
                .setTitle("About").setIcon(R.drawable.ic_about_black);

        builder.setPositiveButton("Close", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Set ItemTouchHelper for Swipe-to-delete from chosen directories
     */
    private void setUpSwipeToDeleteDirectory() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = (int) viewHolder.itemView.getTag();
                mSelectedDirectories.remove(position);
                mDirectoryListAdapter.swapDirs(mSelectedDirectories);
            }
        }).attachToRecyclerView(mDirectoriesRecyclerView);
    }

    /**
     * Start activity for choosing directory
     *
     * @param view
     */
    public void onChooseDirectoryClick(View view) {
        startActivityForResult(new Intent(this, DirectoryChooserActivity.class), DIRECTORY_REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DIRECTORY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                String selectedDirectoryPath = data.getExtras().getString(DirectoryChooserActivity.EXTRA_DIRECTORY_PATH, null);

                if (selectedDirectoryPath != null) {
                    File selectedDir = new File(selectedDirectoryPath);
                    mSelectedDirectories.add(selectedDir);
                    mDirectoryListAdapter.swapDirs(mSelectedDirectories);
                    showDirs();
                }
            }
        }
    }

    @Override
    public Loader<List<File>> onCreateLoader(final int id, final Bundle args) {
        return new AsyncTaskLoader<List<File>>(this) {

            List<File> files = null;
            int numberOfFiles;

            @Override
            protected void onStartLoading() {
                if (id == FILES_LOADER_ID) {
                    if (args != null) {
                        numberOfFiles = args.getInt(EXTRA_NUMBER_OF_FILES, 0);
                    } else {
                        numberOfFiles = 0;
                        return;
                    }

                    if (files != null) {
                        deliverResult(files);
                    } else {
                        showLoadingFiles();
                        mCalculatingProgressBar.setMax(10);
                        forceLoad();
                    }
                } else if (id == SAVED_FILES_LOADER_ID) {
                    showLoadingFiles();
                    forceLoad();
                }
            }

            @Override
            public List<File> loadInBackground() {
                if (id == FILES_LOADER_ID) {
                    return mFilesScanner.getLargestFiles(numberOfFiles, mSelectedDirectories, mDirectoryListAdapter.getDirectoriesStateMap());
                } else if (id == SAVED_FILES_LOADER_ID) {
                    return loadSavedFiles();
                }

                return null;
            }

            @Override
            public void deliverResult(List<File> data) {
                if (id == FILES_LOADER_ID) {
                    files = data;
                    super.deliverResult(data);
                } else if (id == SAVED_FILES_LOADER_ID) {
                    super.deliverResult(data);
                }
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<List<File>> loader, List<File> data) {
        if (loader.getId() == FILES_LOADER_ID) {
            mFIleFileListAdapter.swapFiles(data);

            if (mCurrentState != CurrentState.DIRECTORIES) {
                showFiles();
            }

        } else if (loader.getId() == SAVED_FILES_LOADER_ID) {
            if (data != null) {
                mFIleFileListAdapter.swapFiles(data);
                showFiles();
            } else {
                showDirs();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<List<File>> loader) {
        if (loader.getId() == FILES_LOADER_ID) {
            mFIleFileListAdapter.swapFiles(null);
            showDirs();
            mCalculatingProgressBar.setProgress(0);

            Message cancelNotification = new Message();
            cancelNotification.what = MESSAGE_CANCEL;
            mHandler.sendMessage(cancelNotification);
        }
    }

    /**
     * Onclick method for searching biggest files
     *
     * @param view
     */
    public void onSearchBiggestFilesClick(View view) {
        if (mNumberOFFilesEditText.getText().toString().isEmpty()) {
            mTextInputLayout.setError(getString(R.string.empty_input_error));
            return;
        }

        int numberOfFiles;

        try {
            numberOfFiles = Integer.valueOf(mNumberOFFilesEditText.getText().toString());
        } catch (NumberFormatException ex) {
            mTextInputLayout.setError(getString(R.string.number_too_big_error));
            Log.e(TAG, ex.toString());
            return;
        }

        if (numberOfFiles <= 0) {
            mTextInputLayout.setError(getString(R.string.number_too_small_error));
            return;
        }

        if (mSelectedDirectories == null || mSelectedDirectories.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_directories, Toast.LENGTH_SHORT).show();
            return;
        }

        mTextInputLayout.setError(null);

        if (mCurrentState == CurrentState.FILES_LOADING) {
            Toast.makeText(this, R.string.toast_sort_running, Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide soft keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        Bundle extra = new Bundle();
        extra.putInt(EXTRA_NUMBER_OF_FILES, numberOfFiles);
        getSupportLoaderManager().restartLoader(FILES_LOADER_ID, extra, this);
    }

    /**
     * Show and update notification progress
     *
     * @param progress
     * @param maxProgress
     * @param filesSaved
     */
    private void showNotification(int progress, int maxProgress, boolean filesSaved) {
        if (mNotificationBuilder == null || mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationBuilder = new NotificationCompat.Builder(this);

            mNotificationBuilder.setContentTitle(getString(R.string.notification_title))
                    .setContentText(getString(R.string.notification_text_files))
                    .setSmallIcon(R.drawable.ic_file)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_file))
                    .setAutoCancel(true);
        }

        Log.d(TAG, "Progress : " + progress);

        if (progress < maxProgress) {
            if (progress == 0) {
                mNotificationBuilder.setContentText(getString(R.string.notification_text_sorting));
            }

            mNotificationBuilder.setProgress(maxProgress, progress, false);
            mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
        } else if (progress == 0 && maxProgress == 0) {
            mNotificationBuilder.setProgress(maxProgress, progress, true);
            mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
        } else {
            if (filesSaved) {
                Intent resultIntent = new Intent(this, MainActivity.class);
                resultIntent.putExtra(EXTRA_LARGEST_FILES, true);
                PendingIntent resultPendingIntent =
                        PendingIntent.getActivity(
                                this,
                                0,
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                mNotificationBuilder.setContentIntent(resultPendingIntent);
            }

            mNotificationBuilder.setContentText(getString(R.string.notification_text_done));
            mNotificationBuilder.setProgress(0, 0, false);
            mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
        }
    }

    /**
     * Cancel notification loading
     */
    private void cancelNotificationLoading() {
        if (mNotificationBuilder != null && mNotificationManager != null) {
            mNotificationBuilder.setContentText(getString(R.string.notification_canceled_message));
            mNotificationBuilder.setProgress(0, 0, false);
            mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
        }
    }

    /**
     * Load serialized files
     *
     * @return
     */
    private List<File> loadSavedFiles() {
        try {
            FileInputStream fis = getApplicationContext().openFileInput(FilesScanner.SERIALIZED_FILES_FILE_NAME);
            ObjectInputStream is = new ObjectInputStream(fis);
            List<File> largestFiles = (List<File>) is.readObject();
            is.close();
            fis.close();

            deleteFile(FilesScanner.SERIALIZED_FILES_FILE_NAME);

            return largestFiles;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showDirs() {
        mFab.show();

        mCalculatingProgressBar.setVisibility(View.INVISIBLE);
        mLoadingDirectoryTextView.setVisibility(View.INVISIBLE);

        mFilesRecyclerView.setVisibility(View.INVISIBLE);
        mDirectoriesRecyclerView.setVisibility(View.VISIBLE);

        mCurrentState = CurrentState.DIRECTORIES;
    }

    private void showFiles() {
        mFab.show();

        mCalculatingProgressBar.setVisibility(View.INVISIBLE);
        mLoadingDirectoryTextView.setVisibility(View.INVISIBLE);

        mFilesRecyclerView.setVisibility(View.VISIBLE);
        mDirectoriesRecyclerView.setVisibility(View.INVISIBLE);

        mCalculatingProgressBar.setProgress(0);
        mCurrentState = CurrentState.FILES;
    }

    private void showLoadingFiles() {
        mFab.hide();

        mCalculatingProgressBar.setIndeterminate(true);
        mCalculatingProgressBar.setVisibility(View.VISIBLE);
        mLoadingDirectoryTextView.setVisibility(View.VISIBLE);

        mFilesRecyclerView.setVisibility(View.INVISIBLE);
        mDirectoriesRecyclerView.setVisibility(View.INVISIBLE);

        mCurrentState = CurrentState.FILES_LOADING;
    }

    /**
     * Handler class for receiving updates from background threads and updating UI and notifications
     */
    private class MyHandler extends Handler {
        boolean isLoading = false;

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_CANCEL) {
                cancelNotificationLoading();
                return;
            }

            Bundle bundle = msg.getData();

            if (bundle != null) {
                if (msg.what == FilesScanner.MESSAGE_SORT) {
                    if (isLoading) {
                        mCalculatingProgressBar.setIndeterminate(false);
                        mLoadingDirectoryTextView.setText(getString(R.string.notification_text_sorting));
                        isLoading = false;
                    }


                    int progress = msg.getData().getInt(EXTRA_PROGRESS);
                    int maxProgress = msg.getData().getInt(EXTRA_PROGRESS_MAX);
                    boolean filesSaved = bundle.getBoolean(EXTRA_LARGEST_FILES);

                    if (mCalculatingProgressBar.getMax() != maxProgress) {
                        mCalculatingProgressBar.setMax(maxProgress);
                    }

                    if (progress <= maxProgress) {
                        mCalculatingProgressBar.setProgress(progress);
                    }

                    showNotification(progress, maxProgress, filesSaved);
                } else if (msg.what == FilesScanner.MESSAGE_LOAD) {

                    if (!isLoading) {
                        showNotification(0, 0, false);

                        if (!mCalculatingProgressBar.isIndeterminate()) {
                            mCalculatingProgressBar.setIndeterminate(true);

                        }
                        isLoading = true;
                    }

                    String loadingDir = bundle.getString(EXTRA_DIR_CURRENT_PROGRESS, "");
                    mLoadingDirectoryTextView.setText(loadingDir);
                }
            }
        }
    }
}
