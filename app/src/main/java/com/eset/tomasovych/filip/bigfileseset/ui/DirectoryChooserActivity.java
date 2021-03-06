package com.eset.tomasovych.filip.bigfileseset.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.eset.tomasovych.filip.bigfileseset.R;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class DirectoryChooserActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<File>> {

    public static final String EXTRA_DIRECTORY_PATH = "com.eset.tomasovych.filip.DIRECTORY_PATH";
    public static final String EXTRA_DIRECTORY_STACK = "com.eset.tomasovych.filip.DIRECTORY_STACK";
    private static final int PERMISSION_READ_EXTERNAL_CODE = 1;
    private static final int LOADER_ID = 200;
    private RecyclerView mDirectoriesRecyclerView;
    private DirectoryListAdapter mAdapter;
    private List<File> mDirs;
    private File mCurrentDir;
    private Stack<List<File>> mDirsStack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_chooser);


        if (savedInstanceState != null) {
            mDirsStack = (Stack<List<File>>) savedInstanceState.getSerializable(EXTRA_DIRECTORY_STACK);
            mCurrentDir = (File) savedInstanceState.getSerializable(EXTRA_DIRECTORY_PATH);
        } else {
            mDirsStack = new Stack<>();
        }

        mDirectoriesRecyclerView = (RecyclerView) findViewById(R.id.rv_directory_list);
        mDirectoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new DirectoryListAdapter(this, null, true);
        mDirectoriesRecyclerView.setAdapter(mAdapter);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL_CODE);
        } else {
            loadFiles(Environment.getExternalStorageDirectory().getPath());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_DIRECTORY_STACK, mDirsStack);
        outState.putSerializable(EXTRA_DIRECTORY_PATH, mCurrentDir);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_READ_EXTERNAL_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFiles(Environment.getExternalStorageDirectory().getPath());
            } else {
                finish();
            }
        }
    }

    /**
     * get path of a clicked directory item and swap adapter with new files
     *
     * @param view
     */
    public void onDirectoryItemClick(View view) {
        TextView dirPathTextView = (TextView) view.findViewById(R.id.tv_directory_path);
        String directoryPath = (String) dirPathTextView.getTag();

        mDirsStack.push(mDirs);

        loadFiles(directoryPath);
    }

    /**
     * restart Loader with new directory path
     *
     * @param directoryPath
     */
    private void loadFiles(String directoryPath) {
        Bundle extraPath = new Bundle();
        extraPath.putString(EXTRA_DIRECTORY_PATH, directoryPath);
        getSupportLoaderManager().restartLoader(LOADER_ID, extraPath, this);
    }

    /**
     * finish activity with proper result intent containing selected directory path
     *
     * @param view
     */
    public void onSelectCurrentDirectoryClick(View view) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_DIRECTORY_PATH, mCurrentDir.getAbsolutePath());
        setResult(RESULT_OK, resultIntent);
        finish();
    }


    /**
     * Custom back button behavior
     */
    @Override
    public void onBackPressed() {
        if (mDirsStack.empty()) {
            finish();
        } else {
            mDirs = mDirsStack.pop();
            mAdapter.swapDirs(mDirs);

            mCurrentDir = mDirs.get(0).getParentFile();
            setTitle(mCurrentDir.getPath());
        }
    }

    // get directories from absolute path
    private List<File> getDirectories(String path) {
        mCurrentDir = new File(path);
        return Arrays.asList(mCurrentDir.listFiles(new DirectoryFilter()));
    }

    @Override
    public Loader<List<File>> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<List<File>>(this) {

            List<File> files = null;
            String path = null;

            @Override
            protected void onStartLoading() {
                path = args.getString(EXTRA_DIRECTORY_PATH, null);

                if (files != null) {
                    deliverResult(files);
                } else {
                    forceLoad();
                }
            }

            @Override
            public List<File> loadInBackground() {
                return getDirectories(path);
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
        mDirs = data;
        mAdapter.swapDirs(mDirs);
        if (mCurrentDir != null) {
            setTitle(mCurrentDir.getPath());
        }
    }

    @Override
    public void onLoaderReset(Loader<List<File>> loader) {
        mAdapter.swapDirs(null);
        setTitle("");
    }

    /**
     * Select directory and return to MainActivity
     *
     * @param view
     */
    public void onSelectDirectoryFabClick(View view) {
        String directoryPath = (String) view.getTag();

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_DIRECTORY_PATH, directoryPath);
        setResult(RESULT_OK, resultIntent);
        finish();
    }


    // File filter to only pass Directories
    private class DirectoryFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    }
}
