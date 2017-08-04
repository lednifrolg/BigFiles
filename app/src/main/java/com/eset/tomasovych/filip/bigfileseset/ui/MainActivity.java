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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.KeyEvent;
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
    private static final int NOTIFICATION_ID = 302;
    private static final int DIRECTORY_REQUEST_CODE = 93;
    private static final int FILES_LOADER_ID = 301;
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

    private final Handler mHandler = new Handler() {

        boolean isLoading = false;

        @Override
        public void handleMessage(Message msg) {
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
                    List<File> largestFiles = (List<File>) bundle.getSerializable(EXTRA_LARGEST_FILES);

                    if (mCalculatingProgressBar.getMax() != maxProgress) {
                        mCalculatingProgressBar.setMax(maxProgress);
                    }

                    if (progress <= maxProgress) {
                        mCalculatingProgressBar.setProgress(progress);
                    }

                    showNotification(progress, maxProgress, largestFiles);
                } else if (msg.what == FilesScanner.MESSAGE_LOAD) {

                    if (!isLoading) {
                        showNotification(0, 0, null);
                        mCalculatingProgressBar.setIndeterminate(true);
                        isLoading = true;
                    }

                    String loadingDir = bundle.getString(EXTRA_DIR_CURRENT_PROGRESS, "");
                    mLoadingDirectoryTextView.setText(loadingDir);
                }
            }
        }
    };
    private DirectoryListAdapter mDirectoryListAdapter;
    private FileListAdapter mFIleFileListAdapter;
    private List<File> mSelectedDirectories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            List<File> files = (List<File>) extras.getSerializable(EXTRA_LARGEST_FILES);
            for (File f : files) {
                Log.d(MainActivity.class.getSimpleName(), f.getAbsolutePath());
            }
            mFIleFileListAdapter = new FileListAdapter(this, files);
            showFiles();
        } else {
            mFIleFileListAdapter = new FileListAdapter(this, null);
        }

        mFilesRecyclerView.setAdapter(mFIleFileListAdapter);

        mSelectedDirectories = new ArrayList<>();
        getSupportLoaderManager().initLoader(FILES_LOADER_ID, null, this);

        setUpSwipeToDeleteDirectory();
        setUpKeyboardDoneListener();

        if (savedInstanceState != null) {
            loadSavedState(savedInstanceState);
        }
    }

    private void setUpKeyboardDoneListener() {
        mNumberOFFilesEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_SEARCH) {
                    searchBiggestFiles(textView);
                    return true;
                }
                return false;
            }
        });
    }

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

    @Override
    public void onBackPressed() {
        if (mCurrentState == CurrentState.FILES_LOADING) {
            mBackPressedCounter++;

            if (mBackPressedCounter == 2) {
                getSupportLoaderManager().destroyLoader(FILES_LOADER_ID);
                mBackPressedCounter = 0;
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

    public void getDirectory(View view) {
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
            FilesScanner mFilesScanner = new FilesScanner(mHandler);

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
                }
            }

            @Override
            public List<File> loadInBackground() {
                if (id == FILES_LOADER_ID) {
                    return mFilesScanner.getLargestFiles(numberOfFiles, mSelectedDirectories, mDirectoryListAdapter.directoriesStateMap);
                }

                return null;
            }

            @Override
            public void deliverResult(List<File> data) {
                if (id == FILES_LOADER_ID) {
                    files = data;
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
        }
    }

    @Override
    public void onLoaderReset(Loader<List<File>> loader) {
        if (loader.getId() == FILES_LOADER_ID) {
            mFIleFileListAdapter.swapFiles(null);
            showDirs();
            mCalculatingProgressBar.setProgress(0);
        }
    }


    public void searchBiggestFiles(View view) {
        if (mNumberOFFilesEditText.getText().toString().isEmpty()) {
            mTextInputLayout.setError(getString(R.string.empty_input_error));
            return;
        }

        int numberOfFiles;

        try {
            numberOfFiles = Integer.valueOf(mNumberOFFilesEditText.getText().toString());
        } catch (NumberFormatException ex) {
            mTextInputLayout.setError(getString(R.string.number_too_big_error));
            Log.e(MainActivity.class.getSimpleName(), ex.toString());
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

    private void showNotification(int progress, int maxProgress, List<File> largestFiles) {
        if (mNotificationBuilder == null || mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationBuilder = new NotificationCompat.Builder(this);

            mNotificationBuilder.setContentTitle(getString(R.string.notification_title))
                    .setContentText(getString(R.string.notification_text_files))
                    .setSmallIcon(R.drawable.ic_file)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_file))
                    .setAutoCancel(true);
        }

        Log.d(MainActivity.class.getSimpleName(), "Progress : " + progress);

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
            if (largestFiles != null) {
                Intent resultIntent = new Intent(this, MainActivity.class);
                resultIntent.putExtra(EXTRA_LARGEST_FILES, (Serializable) largestFiles);
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
        Log.d(MainActivity.class.getSimpleName(), "showFiles");
        mCalculatingProgressBar.setVisibility(View.INVISIBLE);
        mLoadingDirectoryTextView.setVisibility(View.INVISIBLE);

        mFilesRecyclerView.setVisibility(View.VISIBLE);
        mDirectoriesRecyclerView.setVisibility(View.INVISIBLE);

        mCalculatingProgressBar.setProgress(0);
        mCurrentState = CurrentState.FILES;
    }

    private void showLoadingFiles() {
        mFab.hide();
        mCalculatingProgressBar.setVisibility(View.VISIBLE);
        mLoadingDirectoryTextView.setVisibility(View.VISIBLE);

        mFilesRecyclerView.setVisibility(View.INVISIBLE);
        mDirectoriesRecyclerView.setVisibility(View.INVISIBLE);

        mCurrentState = CurrentState.FILES_LOADING;
    }
}
