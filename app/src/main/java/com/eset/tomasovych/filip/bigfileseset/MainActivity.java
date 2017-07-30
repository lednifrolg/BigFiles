package com.eset.tomasovych.filip.bigfileseset;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
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
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<File>> {

    public static final String EXTRA_SELECTED_DIRECTORY_PATH = "com.eset.tomasovych.filip.DSELECTED_DIRECTORY_PATH";
    private static final int DIRECTORY_REQUEST_CODE = 93;
    private static final int LOADER_ID = 199;
    private RecyclerView mDirectoriesRecyclerView;
    private DirectoryListAdapter mAdapter;
    private List<File> mSelectedDirectories;
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDirectoriesRecyclerView = (RecyclerView) findViewById(R.id.directories_list_recyclerView);
        mDirectoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new DirectoryListAdapter(this, null, false);
        mDirectoriesRecyclerView.setAdapter(mAdapter);

        mSelectedDirectories = new ArrayList<>();
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

    }

    public void getDirectory(View view) {
        if (counter > 0) {
            FilesScanner filesScanner = new FilesScanner(mAdapter.directoriesStateMap);
            final List<File> files = filesScanner.getFiles(mSelectedDirectories.get(0));
            final PriorityQueue<File> priorityQueue = new PriorityQueue<>(3);

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... voids) {
                    for (int i = 0; i < 3; i++) {
                        priorityQueue.add(files.get(i));
                    }

                    for (int i = 3; i < files.size(); i++) {
                        if (files.get(i).length() > priorityQueue.peek().length()) {
                            priorityQueue.poll();
                            priorityQueue.add(files.get(i));
                        }
                    }

                    Iterator iterator = priorityQueue.iterator();

                    while (iterator.hasNext()) {
                        File file = (File) iterator.next();
                        Log.d(MainActivity.class.getSimpleName(), "File : " + file.getAbsolutePath() + " | size : " + file.length());
                    }

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
