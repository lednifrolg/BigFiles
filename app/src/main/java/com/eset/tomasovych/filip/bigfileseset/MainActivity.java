package com.eset.tomasovych.filip.bigfileseset;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.eset.tomasovych.filip.bigfileseset.Utils.FilesScanner;
import com.eset.tomasovych.filip.bigfileseset.ui.DirectoryListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<File>> {

    public static final String EXTRA_SELECTED_DIRECTORY_PATH = "com.eset.tomasovych.filip.DSELECTED_DIRECTORY_PATH";
    private static final int DIRECTORY_REQUEST_CODE = 93;
    private static final int LOADER_ID = 199;
    private RecyclerView mDirectoriesRecyclerView;
    private RecyclerView mFilesRecyclerView;
    private DirectoryListAdapter mAdapter;
    private List<File> mSelectedDirectories;
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDirectoriesRecyclerView = (RecyclerView) findViewById(R.id.rv_directory_list);
        mDirectoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mFilesRecyclerView = (RecyclerView) findViewById(R.id.rv_file_list);
        mFilesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new DirectoryListAdapter(this, null, false);
        mDirectoriesRecyclerView.setAdapter(mAdapter);

        mSelectedDirectories = new ArrayList<>();
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

    }

    public void getDirectory(View view) {
        if (counter > 0) {


            new AsyncTask<Void, Void, Void>() {



                @Override
                protected Void doInBackground(Void... voids) {
                    NotificationManager notificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                    builder.setContentTitle("Picture Download")
                            .setContentText("Download in progress")
                            .setSmallIcon(R.drawable.ic_folder);



                    long startTime = SystemClock.elapsedRealtime();
                    FilesScanner filesScanner = new FilesScanner(mAdapter.directoriesStateMap);

                    builder.setProgress(0, 0, true);
                    notificationManager.notify(1, builder.build());

                    final List<File> files = new ArrayList<>();
                    for (File selected : mSelectedDirectories) {
                        files.addAll(filesScanner.getFiles(selected));
                    }

                    final PriorityQueue<File> priorityQueue = new PriorityQueue<>(3);

                    long endTime = SystemClock.elapsedRealtime();
                    long elapsedMilliSeconds = endTime - startTime;
                    double elapsedSeconds = elapsedMilliSeconds / 1000.0;

                    Log.d(MainActivity.class.getSimpleName(), "Files( " + files.size() + ") | Time( " + String.valueOf(elapsedSeconds) + ")");

                    startTime = SystemClock.elapsedRealtime();

                    int incr = 0;
                    int counter = 0;

                    builder.setProgress(100, incr, false);
                    notificationManager.notify(1, builder.build());

                    for (int i = 0; i < 3; i++) {
                        priorityQueue.add(files.get(i));
                    }

                    for (int i = 3; i < files.size(); i++) {
                        if (files.get(i).length() > priorityQueue.peek().length()) {
                            priorityQueue.poll();
                            priorityQueue.add(files.get(i));
                        }

                        if (counter % (files.size() / 10) == 0) {
                            incr += 10;
                            builder.setProgress(100, incr, false);
                            notificationManager.notify(1, builder.build());
                        }

                        counter++;
                    }

                    builder.setProgress(100, 100, false);
                    notificationManager.notify(1, builder.build());

                    Iterator iterator = priorityQueue.iterator();

                    while (iterator.hasNext()) {
                        File file = (File) iterator.next();
                        Log.d(MainActivity.class.getSimpleName(), "File : " + file.getAbsolutePath() + " | size : " + file.length());
                    }

                    endTime = SystemClock.elapsedRealtime();
                    elapsedMilliSeconds = endTime - startTime;
                    elapsedSeconds = elapsedMilliSeconds / 1000.0;

                    Log.d(MainActivity.class.getSimpleName(), "Time HEAP( " + String.valueOf(elapsedSeconds) + ")");

                    builder.setContentText("Download complete")
                            .setProgress(0,0,false);
                    notificationManager.notify(1, builder.build());

                    return null;
                }
            }.execute();

        }
        counter++;
        startActivityForResult(new Intent(this, DirectoryChooserActivity.class), DIRECTORY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DIRECTORY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
//                Log.d(MainActivity.class.getSimpleName(), "RESULT : " + data.getExtras().getString(DirectoryChooserActivity.EXTRA_DIRECTORY_PATH, null));

                String selectedDirectoryPath = data.getExtras().getString(DirectoryChooserActivity.EXTRA_DIRECTORY_PATH, null);

                if (selectedDirectoryPath != null) {
                    mSelectedDirectories.add(new File(selectedDirectoryPath));

                    Bundle extraPath = new Bundle();
                    extraPath.putString(EXTRA_SELECTED_DIRECTORY_PATH, selectedDirectoryPath);
                    getSupportLoaderManager().restartLoader(LOADER_ID, extraPath, this);
                }
            }
        }
    }

    @Override
    public Loader<List<File>> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<List<File>>(this) {

            List<File> files = null;
            String path = null;

            @Override
            protected void onStartLoading() {
                if (args != null) {
                    path = args.getString(EXTRA_SELECTED_DIRECTORY_PATH, null);
                }

                if (path == null) {
                    deliverResult(null);
                    return;
                }


                if (files != null) {
                    deliverResult(files);
                } else {
                    forceLoad();
                }
            }

            @Override
            public List<File> loadInBackground() {
                return FilesScanner.getSubdirectories(new File(path));
            }

            @Override
            public void deliverResult(List<File> data) {
                files = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<List<File>> loader, List<File> data) {
        mAdapter.addFiles(data);
    }

    @Override
    public void onLoaderReset(Loader<List<File>> loader) {
        mAdapter.swapFiles(null);
    }
}
