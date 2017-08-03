package com.eset.tomasovych.filip.bigfileseset.ui;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.eset.tomasovych.filip.bigfileseset.R;
import com.eset.tomasovych.filip.bigfileseset.Utils.CurrentState;
import com.eset.tomasovych.filip.bigfileseset.Utils.FilesScanner;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<File>> {

    public static final String EXTRA_SELECTED_DIRECTORY_PATH = "com.eset.tomasovych.filip.SELECTED_DIRECTORY_PATH";
    public static final String EXTRA_NUMBER_OF_FILES = "com.eset.tomasovych.filip.NUMBER_OF_FILES";
    public static final String EXTRA_LARGEST_FILES = "com.eset.tomasovych.filip.EXTRA_LARGEST_FILES";
    public static final String EXTRA_PROGRESS = "com.eset.tomasovych.filip.PROGRESS";
    public static final String EXTRA_PROGRESS_MAX = "com.eset.tomasovych.filip.PROGRESS_MAX";
    public static final String EXTRA_CURRENT_STATE = "com.eset.tomasovych.filip.CURRENT_STATE";
    public static final String EXTRA_DIRECTORIES = "com.eset.tomasovych.filip.DIRECTORIES";
    private static final int NOTIFICATION_ID = 302;
    private static final int DIRECTORY_REQUEST_CODE = 93;
    private static final int DIRECTORIES_LOADER_ID = 300;
    private static final int FILES_LOADER_ID = 301;
    private EditText mNumberOFFilesEditText;
    private RecyclerView mDirectoriesRecyclerView;
    private RecyclerView mFilesRecyclerView;
    private ProgressBar mLoadingProgressBar;
    private ProgressBar mCalculatingProgressBar;

    private CurrentState mCurrentState;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();

            if (bundle != null) {
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
            }
        }
    };
    private DirectoryListAdapter mDirectoryListAdapter;
    private FileListAdapter mFIleFileListAdapter;
    private List<File> mSelectedDirectories;
    private List<File> mAllDirectories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mFilesScanner = new FilesScanner(this);

        mNumberOFFilesEditText = (EditText) findViewById(R.id.et_files_number);

        mLoadingProgressBar = (ProgressBar) findViewById(R.id.loading_progess_bar);
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
            Log.d(MainActivity.class.getSimpleName(), "HELOOO : " + files.size());
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
        mAllDirectories = new ArrayList<>();
        getSupportLoaderManager().initLoader(DIRECTORIES_LOADER_ID, null, this);
        getSupportLoaderManager().initLoader(FILES_LOADER_ID, null, this);

        setUpSwipeToDeleteDirectory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_CURRENT_STATE, mCurrentState);
        outState.putSerializable(EXTRA_DIRECTORIES, (Serializable) mSelectedDirectories);

        super.onSaveInstanceState(outState);
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
                mAllDirectories.remove(position);
                mDirectoryListAdapter.swapFiles(mAllDirectories);
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
                    mSelectedDirectories.add(new File(selectedDirectoryPath));

                    Bundle extraPath = new Bundle();
                    extraPath.putString(EXTRA_SELECTED_DIRECTORY_PATH, selectedDirectoryPath);
                    getSupportLoaderManager().restartLoader(DIRECTORIES_LOADER_ID, extraPath, this);
                }
            }
        }
    }

    @Override
    public Loader<List<File>> onCreateLoader(final int id, final Bundle args) {
        return new AsyncTaskLoader<List<File>>(this) {

            List<File> dirs = null;
            List<File> files = null;
            String path = null;
            int numberOfFiles;
            FilesScanner mFilesScanner = new FilesScanner(mHandler);

            @Override
            protected void onStartLoading() {
                if (id == DIRECTORIES_LOADER_ID) {
                    Log.d(MainActivity.class.getSimpleName(), "DIRECTORIES_LOADER_ID");

                    if (args != null) {
                        path = args.getString(EXTRA_SELECTED_DIRECTORY_PATH, null);
                    }

                    if (path == null) {
                        return;
                    }

                    if (dirs != null) {
                        deliverResult(dirs);
                    } else {
                        showLoadingDirs();
                        forceLoad();
                    }

                } else if (id == FILES_LOADER_ID) {
                    Log.d(MainActivity.class.getSimpleName(), "FILES_LOADER_ID");

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
                if (id == DIRECTORIES_LOADER_ID) {
                    return FilesScanner.getSubdirectories(new File(path));
                } else if (id == FILES_LOADER_ID) {

                    return mFilesScanner.getLargestFiles(numberOfFiles, mDirectoryListAdapter.mDirs, mDirectoryListAdapter.directoriesStateMap);
                }

                return null;
            }

            @Override
            public void deliverResult(List<File> data) {
                if (id == DIRECTORIES_LOADER_ID) {
                    dirs = data;
                    super.deliverResult(data);
                } else if (id == FILES_LOADER_ID) {
                    files = data;
                    super.deliverResult(data);
                }
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<List<File>> loader, List<File> data) {
        if (loader.getId() == DIRECTORIES_LOADER_ID) {
            mAllDirectories.addAll(data);
            mDirectoryListAdapter.addFiles(mAllDirectories);
            showDirs();
        } else if (loader.getId() == FILES_LOADER_ID) {
            mFIleFileListAdapter.swapFiles(data);
            showFiles();
        }

    }

    @Override
    public void onLoaderReset(Loader<List<File>> loader) {
        if (loader.getId() == DIRECTORIES_LOADER_ID) {
            mDirectoryListAdapter.swapFiles(null);
        } else if (loader.getId() == FILES_LOADER_ID) {
            mFIleFileListAdapter.swapFiles(null);
        }
    }


    public void searchBiggestFiles(View view) {
        if (mNumberOFFilesEditText.getText().toString().isEmpty()) {
            return;
        }

        int numberOfFiles = Integer.valueOf(mNumberOFFilesEditText.getText().toString());

        if (numberOfFiles <= 0) {
            return;
        }

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
                    .setAutoCancel(true);
        }

        if (progress == 5) {
            mNotificationBuilder.setContentText(getString(R.string.notification_text_sorting));
        }

        Log.d(MainActivity.class.getSimpleName(), "Progress : " + progress);
        if (progress < maxProgress) {
            mNotificationBuilder.setProgress(maxProgress, progress, false);
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
        mCalculatingProgressBar.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.setVisibility(View.INVISIBLE);

        mFilesRecyclerView.setVisibility(View.INVISIBLE);
        mDirectoriesRecyclerView.setVisibility(View.VISIBLE);

        mCurrentState = CurrentState.DIRECTORIES;
    }

    private void showLoadingDirs() {
        mCalculatingProgressBar.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.setVisibility(View.VISIBLE);

        mFilesRecyclerView.setVisibility(View.INVISIBLE);
        mDirectoriesRecyclerView.setVisibility(View.INVISIBLE);

        mCurrentState = CurrentState.DIRECTORIES_LOADING;
    }

    private void showFiles() {
        mCalculatingProgressBar.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.setVisibility(View.INVISIBLE);

        mFilesRecyclerView.setVisibility(View.VISIBLE);
        mDirectoriesRecyclerView.setVisibility(View.INVISIBLE);

        mCurrentState = CurrentState.FILES;
    }

    private void showLoadingFiles() {
        mCalculatingProgressBar.setVisibility(View.VISIBLE);
        mLoadingProgressBar.setVisibility(View.INVISIBLE);

        mFilesRecyclerView.setVisibility(View.INVISIBLE);
        mDirectoriesRecyclerView.setVisibility(View.INVISIBLE);

        mCurrentState = CurrentState.FILES_LOADING;
    }


}
