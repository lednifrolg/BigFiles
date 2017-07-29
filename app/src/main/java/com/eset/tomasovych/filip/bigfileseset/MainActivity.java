package com.eset.tomasovych.filip.bigfileseset;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int DIRECTORY_REQUEST_CODE = 93;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    private void getFilesPermmision() {

    }

    public void getDirectory(View view) {
        startActivityForResult(new Intent(this, DirectoryChooserActivity.class), DIRECTORY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DIRECTORY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.d(MainActivity.class.getSimpleName(), "RESULT : " + data.getExtras().getString(DirectoryChooserActivity.EXTRA_DIRECTORY_PATH, null));
            }
        }
    }
}
