package com.eset.tomasovych.filip.bigfileseset;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_chooser);

        mFilesStack = new Stack<>();

        mDirectoriesRecyclerView = (RecyclerView) findViewById(R.id.directories_list_recyclerView);
        mDirectoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mFiles = getDirectories(Environment.getExternalStorageDirectory().getPath());

        mAdapter = new DirectoryListAdapter(this, mFiles);
        mDirectoriesRecyclerView.setAdapter(mAdapter);
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
