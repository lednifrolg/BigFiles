package com.eset.tomasovych.filip.bigfileseset.Utils;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.eset.tomasovych.filip.bigfileseset.ui.MainActivity;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;


public class FilesScanner {

    private Handler mHandler;
    private int mMaxProgress = 10;
    private int mCurrentProgress = 0;

    public FilesScanner(Handler handler) {
        this.mHandler = handler;
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

    public static String formatFileSize(long fileSize) {
        long unit = 1024;

        if (fileSize < unit)
            return fileSize + " B";

        int exp = (int) (Math.log(fileSize) / Math.log(unit));
        String pre = ("KMGTPE").charAt(exp - 1) + "i";
        return String.format("%.1f %sB", fileSize / Math.pow(unit, exp), pre);
    }

    public List<File> getFiles(File root, HashMap<String, Boolean> directoriesStateMap) {
        File[] list = root.listFiles();
        List<File> files = new ArrayList<>();

        for (File f : list) {
            if (f.isDirectory()) {
                if ((directoriesStateMap.containsKey(f.getAbsolutePath()) && !directoriesStateMap.get(f.getAbsolutePath()))) {
                    // this directory is unchecked so its skipped
                } else {
                    files.addAll(getFiles(f, directoriesStateMap));
                }
            } else {
                files.add(f);
            }
        }

        return files;
    }

    synchronized private List<File> getFiles(List<File> directories, HashMap<String, Boolean> directoriesStateMap) {
        List<File> files = new ArrayList<>();

        int progressCycle = 1;
        int increment = 1;

        if (directories.size() > mMaxProgress / 2) {
            progressCycle = (int) Math.ceil(directories.size() / (double) (mMaxProgress / 2));
        } else if (directories.size() < mMaxProgress / 2) {
            increment = (int) Math.ceil((mMaxProgress / 2) / (double) directories.size());
        }

        int inc = 0;
        for (File dir : directories) {

            if (directoriesStateMap.containsKey(dir.getAbsolutePath()) && !directoriesStateMap.get(dir.getAbsolutePath())) {
                continue;
            }

            File[] dirFiles = dir.listFiles();

            for (File file : dirFiles) {
                if (!file.isDirectory()) {
                    files.add(file);
                }
            }

            if (inc % progressCycle == 0) {
                Log.d(FilesScanner.class.getSimpleName(), "Progress getFIles");
                updateProgress(increment);
            }
            inc++;
        }

        return files;
    }

    synchronized public List<File> getLargestFiles(int numberOfFiles, List<File> directories, HashMap<String, Boolean> directoriesStateMap) {
        List<File> files = getFiles(directories, directoriesStateMap);

        if (numberOfFiles >= files.size()) {
            updateProgress(mMaxProgress);
            return files;
        }

        if (numberOfFiles < 1 || files.size() < 0) {
            updateProgress(mMaxProgress);
            return null;
        }

        int progressCycle = 1;
        int increment = 1;

        if (files.size() > mMaxProgress / 2) {
            progressCycle = (int) Math.ceil(files.size() / (double) (mMaxProgress / 2));
        } else if (files.size() < mMaxProgress / 2) {
            increment = (int) Math.ceil((mMaxProgress / 2) / (double) files.size());
        }


        final PriorityQueue<File> minHeap = new PriorityQueue<>(numberOfFiles);
        List<File> largestFiles = new ArrayList<>();

        for (int i = 0; i < numberOfFiles; i++) {
            minHeap.add(files.get(i));

            if (i % progressCycle == 0) {
                updateProgress(increment);
            }

        }

        for (int i = numberOfFiles; i < files.size(); i++) {
            if (files.get(i).length() > minHeap.peek().length()) {
                minHeap.poll();
                minHeap.add(files.get(i));
            }

            if (i % progressCycle == 0) {
                updateProgress(increment);
            }
        }

        Iterator iterator = minHeap.iterator();

        while (iterator.hasNext()) {
            largestFiles.add((File) iterator.next());
        }

        updateProgress(largestFiles);


        return largestFiles;
    }

    private void updateProgress(List<File> largestFiles) {
        Bundle bundle = new Bundle();
        bundle.putInt(MainActivity.EXTRA_PROGRESS, mMaxProgress);
        bundle.putInt(MainActivity.EXTRA_PROGRESS_MAX, mMaxProgress);
        bundle.putSerializable(MainActivity.EXTRA_LARGEST_FILES, (Serializable) largestFiles);

        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    synchronized private void updateProgress(int progressValue) {
        Bundle bundle = new Bundle();
        bundle.putInt(MainActivity.EXTRA_PROGRESS, mCurrentProgress);
        bundle.putInt(MainActivity.EXTRA_PROGRESS_MAX, mMaxProgress);

        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        if (mCurrentProgress + progressValue > mMaxProgress) {
            mCurrentProgress = mMaxProgress;
        } else {
            mCurrentProgress += progressValue;
        }
    }

}
