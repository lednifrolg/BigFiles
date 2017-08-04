package com.eset.tomasovych.filip.bigfileseset.Utils;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.eset.tomasovych.filip.bigfileseset.ui.MainActivity;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;


public class FilesScanner {

    public static final int MESSAGE_SORT = 1;
    public static final int MESSAGE_LOAD = 2;
    private Handler mHandler;
    private int mMaxProgress = 10;
    private int mCurrentProgress = 0;
    private int mRecursionCounter = 0;
    private HashMap<String, Boolean> mSearchedDirectories;
    private HashMap<String, Boolean> mDirectoriesStateMap;

    public FilesScanner(Handler handler) {
        this.mHandler = handler;
        mSearchedDirectories = new HashMap<>();
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

    private boolean isDirectorySearched(File dir) {
        if (mDirectoriesStateMap.containsKey(dir.getAbsolutePath()) && !mDirectoriesStateMap.get(dir.getAbsolutePath())) {
            return true;
        }

        if (mSearchedDirectories.containsKey(dir.getAbsolutePath()) && mSearchedDirectories.get(dir.getAbsolutePath())) {
            return true;
        }

        mSearchedDirectories.put(dir.getAbsolutePath(), true);

        return false;
    }

    private List<File> getFiles(File root) {
        if (mRecursionCounter % 20 == 0) {
            updateProgress(root.getAbsolutePath());
        }

        mRecursionCounter++;

        File[] list = root.listFiles();
        List<File> files = new ArrayList<>();

        for (File f : list) {
            if (f.isDirectory()) {

                if (isDirectorySearched(f)) {
                    continue;
                }

                files.addAll(getFiles(f));
            } else {
                files.add(f);
            }
        }
        return files;
    }

    synchronized private List<File> getFiles(List<File> directories) {
        List<File> files = new ArrayList<>();

        for (File dir : directories) {

            if (isDirectorySearched(dir)) {
                continue;
            }

            files.addAll(getFiles(dir));
        }

        return files;
    }

    synchronized public List<File> getLargestFiles(int numberOfFiles, List<File> directories, HashMap<String, Boolean> directoriesStateMap) {
        mSearchedDirectories.clear();
        mDirectoriesStateMap = directoriesStateMap;

        long startTime = SystemClock.elapsedRealtime();

        List<File> files = getFiles(directories);

        long endTime = SystemClock.elapsedRealtime();
        long elapsedMilliSeconds = endTime - startTime;
        double elapsedSeconds = elapsedMilliSeconds / 1000.0;

        Log.d(FilesScanner.class.getSimpleName(), "Elapsed Seconds FILES : " + elapsedSeconds);


        if (numberOfFiles >= files.size()) {
            updateProgress(mMaxProgress);
            return files;
        }

        if (numberOfFiles < 1 || files.size() < 0) {
            updateProgress(mMaxProgress);
            return null;
        }

        startTime = SystemClock.elapsedRealtime();

        files = largestFilesSelection(files, numberOfFiles) ;

        endTime = SystemClock.elapsedRealtime();
        elapsedMilliSeconds = endTime - startTime;
        elapsedSeconds = elapsedMilliSeconds / 1000.0;
        Log.d(FilesScanner.class.getSimpleName(), "Elapsed Seconds QUICK : " + elapsedSeconds);


//        startTime = SystemClock.elapsedRealtime();
//
//        largestFilesMinPriorityQueue(files, numberOfFiles);
//
//        endTime = SystemClock.elapsedRealtime();
//        elapsedMilliSeconds = endTime - startTime;
//        elapsedSeconds = elapsedMilliSeconds / 1000.0;
//        Log.d(FilesScanner.class.getSimpleName(), "Elapsed Seconds HEAP : " + elapsedSeconds);


        updateProgress(files);

        mCurrentProgress = 0;

        return files;
    }


    private void updateProgress(List<File> largestFiles) {
        Bundle bundle = new Bundle();
        bundle.putInt(MainActivity.EXTRA_PROGRESS, mMaxProgress);
        bundle.putInt(MainActivity.EXTRA_PROGRESS_MAX, mMaxProgress);

        if (largestFiles.size() < 500) {
            bundle.putSerializable(MainActivity.EXTRA_LARGEST_FILES, (Serializable) largestFiles);
        }

        sendMessage(bundle, MESSAGE_SORT);
    }

    private void updateProgress(String dirName) {
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.EXTRA_DIR_CURRENT_PROGRESS, dirName);

        sendMessage(bundle, MESSAGE_LOAD);
    }

    private void updateProgress(int progressValue) {
        if (mCurrentProgress + progressValue >= mMaxProgress) {
            mCurrentProgress = mMaxProgress;
        } else {
            mCurrentProgress += progressValue;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(MainActivity.EXTRA_PROGRESS, mCurrentProgress);
        bundle.putInt(MainActivity.EXTRA_PROGRESS_MAX, mMaxProgress);

        sendMessage(bundle, MESSAGE_SORT);
    }

    private void sendMessage(Bundle bundle, int msgWhat) {
        Message msg = new Message();
        msg.what = msgWhat;
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private List<File> largestFilesSelection(List<File> files, int numberOfFiles) {
        int from = 0, to = files.size() - 1;

        // if from == to we reached the kth element
        while (from < to) {
            int r = from, w = to;
            long mid = files.get((r + w) / 2).length();

            // stop if the reader and writer meets
            while (r < w) {

                if (files.get(r).length() <= mid) { // put the large values at the end
                    File tmp = files.get(w);
                    files.set(w, files.get(r));
                    files.set(r, tmp);
                    w--;
                } else { // the value is smaller than the pivot, skip
                    r++;
                }
            }

            // if we stepped up (r++) we need to step one down
            if (files.get(r).length() < mid)
                r--;

            // the r pointer is on the end of the first k elements
            if (numberOfFiles <= r) {
                to = r;
            } else {
                from = r + 1;
            }
        }

        List<File> largestFiles = new ArrayList<>();

        for (File file : files) {
            if (file.length() > files.get(numberOfFiles).length()) {
                largestFiles.add(file);
            }

            if (files.size() == numberOfFiles) {
                break;
            }
        }

        return largestFiles;
    }

    private List<File> largestFilesMinPriorityQueue(List<File> files, int numberOfFiles) {
        int progressCycle = 1;
        int increment = 1;

        if (files.size() > mMaxProgress) {
            progressCycle = (int) Math.ceil(files.size() / (double) (mMaxProgress));
        } else if (files.size() < mMaxProgress) {
            increment = (int) Math.ceil((mMaxProgress) / (double) files.size());
        }

        final PriorityQueue<File> minHeap = new PriorityQueue<>(numberOfFiles, new FileComparator());

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
        files.clear();

        while (iterator.hasNext()) {
            files.add((File) iterator.next());
        }

        return files;
    }

    private class FileComparator implements Comparator<File> {

        @Override
        public int compare(File file, File file2) {
            if (file.length() < file2.length()) {
                return -1;
            }

            if (file.length() > file2.length()) {
                return 1;
            }

            return 0;
        }
    }

}
