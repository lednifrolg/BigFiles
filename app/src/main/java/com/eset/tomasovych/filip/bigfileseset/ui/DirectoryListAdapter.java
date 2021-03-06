package com.eset.tomasovych.filip.bigfileseset.ui;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.eset.tomasovych.filip.bigfileseset.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DirectoryListAdapter extends RecyclerView.Adapter<DirectoryListAdapter.DirectoryViewHolder> {

    private Context mContext;
    public List<File> mDirs;
    private boolean mIsDirectoryChooser;
    private HashMap<String, Boolean> mDirectoriesStateMap;


    public DirectoryListAdapter(Context context, List<File> mFiles, boolean isDirectoryChooser) {
        this.mContext = context;
        this.mDirs = mFiles;
        this.mIsDirectoryChooser = isDirectoryChooser;
        mDirectoriesStateMap = new HashMap<>();
    }

    @Override
    public DirectoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.directory_list_item, parent, false);
        return new DirectoryViewHolder(view);
    }

    // set up Directory list items
    @Override
    public void onBindViewHolder(DirectoryViewHolder holder, final int position) {
        holder.directoryName.setText(mDirs.get(position).getName());
        holder.directoryPath.setText(mDirs.get(position).getAbsolutePath());
        holder.directoryPath.setTag(mDirs.get(position).getAbsolutePath());
        holder.itemView.setTag(position);
        holder.selectDirectoryFab.setTag(mDirs.get(position).getAbsolutePath());

        // Check if directory is empty
        if (mDirs.get(position).isDirectory() && mDirs.get(position).list().length == 0) {
            holder.directoryIcon.setImageResource(R.drawable.ic_folder_empty);
        } else {
            holder.directoryIcon.setImageResource(R.drawable.ic_folder);
        }

        if (!mIsDirectoryChooser) {
            holder.directorySelectedCheckBox.setVisibility(View.VISIBLE);
            holder.selectDirectoryFab.setVisibility(View.GONE);

            holder.directorySelectedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    mDirectoriesStateMap.put(mDirs.get(position).getAbsolutePath(), b);
                }
            });
        }
    }

    // change displayed Directories
    public void swapDirs(List<File> files) {
        mDirs = files;
        notifyDataSetChanged();
    }

    public void addDir(File dir) {
        if (mDirs == null) {
            List<File> dirs = new ArrayList<>();
            dirs.add(dir);
            swapDirs(new ArrayList<File>(dirs));
            return;
        }

        if (dir != null) {
            mDirs.add(dir);
            notifyDataSetChanged();
        }
    }

    public HashMap<String, Boolean> getDirectoriesStateMap() {
        return (HashMap<String, Boolean>) mDirectoriesStateMap.clone();
    }

    @Override
    public int getItemCount() {
        if (mDirs != null) {
            return mDirs.size();
        }

        return 0;
    }

    class DirectoryViewHolder extends RecyclerView.ViewHolder {

        TextView directoryName;
        TextView directoryPath;
        ImageView directoryIcon;
        CheckBox directorySelectedCheckBox;
        FloatingActionButton selectDirectoryFab;

        DirectoryViewHolder(View itemView) {
            super(itemView);
            directoryName = (TextView) itemView.findViewById(R.id.tv_directory_name);
            directoryPath = (TextView) itemView.findViewById(R.id.tv_directory_path);
            directoryIcon = (ImageView) itemView.findViewById(R.id.iv_folder_ic);

            directorySelectedCheckBox = (CheckBox) itemView.findViewById(R.id.cb_directory_selected);
            selectDirectoryFab = (FloatingActionButton) itemView.findViewById(R.id.fab_select_directory);

            if (!mIsDirectoryChooser) {
                itemView.setOnClickListener(null);
            }
        }
    }
}
