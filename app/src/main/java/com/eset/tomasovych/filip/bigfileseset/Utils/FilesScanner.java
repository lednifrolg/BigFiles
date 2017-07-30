package com.eset.tomasovych.filip.bigfileseset.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FilesScanner {

    private HashMap<String, Boolean> mDirectoriesStateMap;

    public FilesScanner(HashMap<String, Boolean> mDirectoriesStateMap) {
        this.mDirectoriesStateMap = mDirectoriesStateMap;
    }

    public static List<File> getSubdirectories(File root) {

        File[] list = root.listFiles();
        List<File> dirs = new ArrayList<>();

        for (File f : list) {
            if (f.isDirectory()) {
                dirs.add(f);
                dirs.addAll(getSubdirectories(f));
            }
        }

        return dirs;
    }

    public List<File> getFiles(File root) {

        File[] list = root.listFiles();
        List<File> files = new ArrayList<>();

        for (File f : list) {
            if (f.isDirectory()) {
                if (mDirectoriesStateMap.containsKey(f.getAbsolutePath()) && !mDirectoriesStateMap.get(f.getAbsolutePath())) {
                    // this directory is unchecked so its skipped
                    continue;
                } else {
                    files.addAll(getFiles(f));
                }
            } else {
                files.add(f);
            }
        }

        return files;
    }

}
