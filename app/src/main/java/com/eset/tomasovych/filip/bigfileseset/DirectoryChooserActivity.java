package com.eset.tomasovych.filip.bigfileseset;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.eset.tomasovych.filip.bigfileseset.ui.DirectoryListAdapter;

import java.io.File;
import java.io.FileFilter;
import java.util.Stack;

public class DirectoryChooserActivity extends AppCompatActivity {

    public static final String EXTRA_DIRECTORY_PATH = "com.eset.tomasovych.filip.DIRECTORY_PATH";
    private RecyclerView mDirectoriesRecyclerView;
    private DirectoryListAdapter mAdapter;
    private File[] mFiles;
    private File mCurrentDir;
    private Stack<File[]> mFilesStack;

    private static final int PERMISSION_READ_EXTERNAL_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_chooser);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL_CODE);
        }

        mFilesStack = new Stack<>();

        mDirectoriesRecyclerView = (RecyclerView) findViewById(R.id.directories_list_recyclerView);
        mDirectoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mFiles = getDirectories(Environment.getExternalStorageDirectory().getPath());

        mAdapter = new DirectoryListAdapter(this, mFiles);
        mDirectoriesRecyclerView.setAdapter(mAdapter);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_READ_EXTERNAL_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mFiles = getDirectories(Environment.getExternalStorageDirectory().getPath());
                mAdapter.swapFiles(mFiles);
            } else {
                finish();
            }
        }
    }

    // get path of a clicked directory item and swap adapter with new files
    public void onDirectoryItemClick(View view) {
        TextView dirPathTextView = (TextView) view.findViewById(R.id.tv_directory_path);
        String directoryPath = (String) dirPathTextView.getTag();

        mFilesStack.push(mFiles);
        mFiles = getDirectories(directoryPath);
        mAdapter.swapFiles(mFiles);
    }


    // finish activity with proper result intent containing selected directory path
    public void onSelectCurrentDirectoryClick(View view) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_DIRECTORY_PATH, mCurrentDir.getAbsolutePath());
        setResult(RESULT_OK, resultIntent);
        finish();
    }


    // check if Files stack is empty
    // if is finish activity, else pop previous Files from stack
    @Override
    public void onBackPressed() {
        if (mFilesStack.empty()) {
            finish();
        } else {
            mFiles = mFilesStack.pop();
            mAdapter.swapFiles(mFiles);
            mCurrentDir = mFiles[0].getParentFile();
            setTitle(mCurrentDir.getPath());
        }
    }

    // get directories from absolute path
    private File[] getDirectories(String path) {
        mCurrentDir = new File(path);
        setTitle(mCurrentDir.getPath());
        return mCurrentDir.listFiles(new DirectoryFilter());
    }

    // File filter to only pass Directories
    private class DirectoryFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    }
}
