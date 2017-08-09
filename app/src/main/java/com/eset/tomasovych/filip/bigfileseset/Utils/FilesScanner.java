package com.eset.tomasovych.filip.bigfileseset.Utils;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.eset.tomasovych.filip.bigfileseset.ui.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Utility class for searching files, directories and getting largest files
 */
public class FilesScanner {

    public static final int MESSAGE_SORT = 1;
    public static final int MESSAGE_LOAD = 2;
    private static final String TAG = FilesScanner.class.getSimpleName();
    public static String SERIALIZED_FILES_FILE_NAME = "largest_files.ser";
    private Context mContext;
    private int mNumOfOperations = 0;
    private int mProgressCycle = 1;
    private int mProgressIncrement = 1;
    private Handler mHandler;
    private int mMaxProgress = 10;
    private int mCurrentProgress = 0;
    private int mRecursionCounter = 0;
    private HashMap<String, Boolean> mSearchedDirectories;
    private HashMap<String, Boolean> mDirectoriesStateMap;

    public FilesScanner(Handler handler, Context context) {
        this.mHandler = handler;
        mContext = context;
        mSearchedDirectories = new HashMap<>();
    }

    /**
     * Get all subdirectories in a given root directory
     *
     * @param root Root directory
     * @return List of subdirectories
     */
    public static List<File> getSubdirectories(File root) {

        File[] list = root.listFiles();
        List<File> dirs = new ArrayList<>();

        if (list == null || list.length < 1) {
            return dirs;
        }

        for (File f : list) {
            if (f.isDirectory()) {
                dirs.add(f);
                dirs.addAll(getSubdirectories(f));
            }
        }

        return dirs;
    }

    /**
     * Format bytes to readable string size
     *
     * @param fileSize Size ine bytes
     * @return Size in readable form (KiB, MiB, GiB...)
     */
    public static String formatFileSize(long fileSize) {
        long unit = 1024;

        if (fileSize < unit)
            return fileSize + " B";

        int exp = (int) (Math.log(fileSize) / Math.log(unit));
        String pre = ("KMGTPE").charAt(exp - 1) + "i";
        return String.format("%.1f %sB", fileSize / Math.pow(unit, exp), pre);
    }

    /**
     * Select k largest files
     *
     * @param files
     * @param left  first index of the list
     * @param right last index of the list
     * @param k     number of files to search for
     * @return List of k Largest files
     */
    private List<File> select(List<File> files, int left, int right, int k) {
        if (left == right) {
            return files;
        }

        for (; ; ) {
            mNumOfOperations++;

            if (mNumOfOperations % mProgressCycle == 0) {
                updateProgress(mProgressIncrement);
            }
            // Choose random pivot for partition
            int pivotIndex = (left + right) / 2;
            pivotIndex = partition(files, left, right, pivotIndex);

            if (k == pivotIndex) {
                return new ArrayList<>(files.subList(0, k));
            } else if (k < pivotIndex) {
                right = pivotIndex - 1;
            } else {
                left = pivotIndex + 1;
            }
        }
    }

    /**
     * QuickSelect partition implementation https://en.wikipedia.org/wiki/Quickselect
     */
    private int partition(List<File> files, int left, int right, int pivotIndex) {
        long pivotValue = files.get(pivotIndex).length();

        File tmp = files.get(right);
        files.set(right, files.get(pivotIndex));
        files.set(pivotIndex, tmp);

        int storeIndex = left;

        for (int i = left; i < right; i++) {
            mNumOfOperations++;

            if (mNumOfOperations % mProgressCycle == 0) {
                updateProgress(mProgressIncrement);
            }

            if (files.get(i).length() > pivotValue) {
                tmp = files.get(i);
                files.set(i, files.get(storeIndex));
                files.set(storeIndex, tmp);

                storeIndex++;
            }
        }

        tmp = files.get(right);
        files.set(right, files.get(storeIndex));
        files.set(storeIndex, tmp);

        return storeIndex;
    }

    /**
     * Helper method for checking if given directory was already searched for files or if is in unselected Directories
     * Also marks current dir as searched
     *
     * @param dir
     * @return True if was already searched, otherwise False and marks it as searched
     */
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

    /**
     * Recursive method for finding all Files in a given Directory and all of its subdirectories
     *
     * @param root Root directory
     * @return List of all Files from directory and subdirectories
     */
    private List<File> getFiles(File root) {
        if (mRecursionCounter % 20 == 0) {
            updateProgress(root.getAbsolutePath());
        }

        mRecursionCounter++;

        File[] list = root.listFiles();
        List<File> files = new ArrayList<>();

        if (list == null || list.length < 1) {
            return files;
        }

        for (File file : list) {
            if (file.isDirectory()) {

                if (isDirectorySearched(file)) {
                    continue;
                }

                files.addAll(getFiles(file));
            } else {
                files.add(file);
            }
        }
        return files;
    }

    /**
     * Get all files from directories and their subdirectories
     *
     * @param directories - List of input directories
     * @return Files from all input directories and subdirectories
     */
    private List<File> getFiles(List<File> directories) {
        List<File> files = new ArrayList<>();

        for (File dir : directories) {

            if (isDirectorySearched(dir)) {
                continue;
            }

            files.addAll(getFiles(dir));
        }

        return files;
    }

    /**
     * @param numberOfFiles       number of files to search for
     * @param directories         List of directories
     * @param directoriesStateMap Map of unchecked directories
     * @return Unordered List of largest files from selected directories
     */
    public List<File> getLargestFiles(int numberOfFiles, List<File> directories, HashMap<String, Boolean> directoriesStateMap) {
        mSearchedDirectories.clear();
        mDirectoriesStateMap = directoriesStateMap;

        long startTime = SystemClock.elapsedRealtime();

        List<File> files = getFiles(directories);

        long endTime = SystemClock.elapsedRealtime();
        long elapsedMilliSeconds = endTime - startTime;
        double elapsedSeconds = elapsedMilliSeconds / 1000.0;

        Log.d(TAG, "Elapsed Seconds FILES : " + elapsedSeconds);


        if (numberOfFiles >= files.size()) {
            updateProgress(mMaxProgress);
            return files;
        }

        if (numberOfFiles < 1 || files.size() < 0) {
            updateProgress(mMaxProgress);
            return null;
        }

        startTime = SystemClock.elapsedRealtime();

        files = largestFilesSelection(files, numberOfFiles);

        endTime = SystemClock.elapsedRealtime();
        elapsedMilliSeconds = endTime - startTime;
        elapsedSeconds = elapsedMilliSeconds / 1000.0;
        Log.d(TAG, "Elapsed Seconds QUICK : " + elapsedSeconds);


        updateProgress(files);

        mCurrentProgress = 0;

        return files;
    }

    /**
     * Send progress update message and serialize largest files
     *
     * @param largestFiles
     */
    private void updateProgress(List<File> largestFiles) {
        Bundle bundle = new Bundle();
        bundle.putInt(MainActivity.EXTRA_PROGRESS, mMaxProgress);
        bundle.putInt(MainActivity.EXTRA_PROGRESS_MAX, mMaxProgress);
        bundle.putBoolean(MainActivity.EXTRA_LARGEST_FILES, true);

        // serialize files
        saveFiles(largestFiles);

        sendMessage(bundle, MESSAGE_SORT);
    }

    /**
     * Send progress update message with current directory being searched
     *
     * @param dirName
     */
    private void updateProgress(String dirName) {
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.EXTRA_DIR_CURRENT_PROGRESS, dirName);

        sendMessage(bundle, MESSAGE_LOAD);
    }

    /**
     * Send progress update message with progress value
     *
     * @param progressValue
     */
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

    /**
     * Send Handler massage
     *
     * @param bundle
     * @param msgWhat type of message
     */
    private void sendMessage(Bundle bundle, int msgWhat) {
        Message msg = new Message();
        msg.what = msgWhat;
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Get N largest files
     *
     * @param files
     * @param numberOfFiles
     * @return List of largest files
     */
    private List<File> largestFilesSelection(List<File> files, int numberOfFiles) {
        setupProgressVariables(files.size() * 2);
        List<File> largestFiles = select(files, 0, files.size() - 1, numberOfFiles);

        Log.d(TAG, "FilesSize : " + files.size() + " | Operation : " + mNumOfOperations);

        return largestFiles;
    }

    /**
     * Get N largest files using priorityQueue (min-Heap)
     *
     * @param files
     * @param numberOfFiles
     * @return List of largest files
     */
    private List<File> largestFilesMinPriorityQueue(List<File> files, int numberOfFiles) {
        final PriorityQueue<File> minHeap = new PriorityQueue<>(numberOfFiles, new FileComparator());

        for (int i = 0; i < numberOfFiles; i++) {
            mNumOfOperations++;
            minHeap.add(files.get(i));

            if (i % mProgressCycle == 0) {
                updateProgress(mProgressIncrement);
            }
        }

        for (int i = numberOfFiles; i < files.size(); i++) {
            mNumOfOperations++;
            if (files.get(i).length() > minHeap.peek().length()) {
                minHeap.poll();
                minHeap.add(files.get(i));
            }

            if (i % mProgressCycle == 0) {
                updateProgress(mProgressIncrement);
            }
        }

        Iterator iterator = minHeap.iterator();
        files.clear();

        while (iterator.hasNext()) {
            mNumOfOperations++;
            files.add((File) iterator.next());
        }

        return files;
    }


    private void setupProgressVariables(int size) {
        mNumOfOperations = 0;
        mProgressCycle = 1;
        mProgressIncrement = 1;

        if ((size) > mMaxProgress) {
            mProgressCycle = (int) Math.ceil((size) / (double) (mMaxProgress));
        } else if ((size) < mMaxProgress) {
            mProgressIncrement = (int) Math.ceil((mMaxProgress) / (double) (size));
        }
    }

    /**
     * Serialize and save files
     *
     * @param files
     * @return
     */
    private boolean saveFiles(List<File> files) {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput(SERIALIZED_FILES_FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(files);
            os.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Helper class for comparing File objects
     */
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
