package com.eset.tomasovych.filip.bigfileseset.ui;

import android.content.Context;
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
import java.util.HashMap;
import java.util.List;

public class DirectoryListAdapter extends RecyclerView.Adapter<DirectoryListAdapter.DirectoryViewHolder> {

    private Context mContext;
    private List<File> mFiles;
    private boolean mIsDirectoryChooser;
    public HashMap<String, Boolean> directoriesStateMap;


    public DirectoryListAdapter(Context context, List<File> mFiles, boolean isDirectoryChooser) {
        this.mContext = context;
        this.mFiles = mFiles;
        this.mIsDirectoryChooser = isDirectoryChooser;
        directoriesStateMap = new HashMap<>();
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
        holder.directoryName.setText(mFiles.get(position).getName());
        holder.directoryPath.setText(mFiles.get(position).getAbsolutePath());
        holder.directoryPath.setTag(mFiles.get(position).getAbsolutePath());

        // Check if directory is empty
        if (mFiles.get(position).isDirectory() && mFiles.get(position).list().length == 0) {
            holder.directoryIcon.setImageResource(R.drawable.ic_folder_empty);
        } else {
            holder.directoryIcon.setImageResource(R.drawable.ic_folder);
        }

        if (!mIsDirectoryChooser) {
            holder.directorySelectedCheckBox.setVisibility(View.VISIBLE);

            holder.directorySelectedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    directoriesStateMap.put(mFiles.get(position).getAbsolutePath(), b);
                }
            });
        }
    }

    // change displayed files
    public void swapFiles(List<File> files) {
        mFiles = files;
        notifyDataSetChanged();
    }

    public void addFiles(List<File> files) {
        if (mFiles == null) {
            swapFiles(files);
            return;
        }

        if (files != null && files.size() > 0) {
            mFiles.addAll(files);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        if (mFiles != null) {
            return mFiles.size();
        }

        return 0;
    }

    class DirectoryViewHolder extends RecyclerView.ViewHolder {

        TextView directoryName;
        TextView directoryPath;
        ImageView directoryIcon;
        CheckBox directorySelectedCheckBox;

        DirectoryViewHolder(View itemView) {
            super(itemView);
            directoryName = (TextView) itemView.findViewById(R.id.tv_directory_name);
            directoryPath = (TextView) itemView.findViewById(R.id.tv_directory_path);
            directoryIcon = (ImageView) itemView.findViewById(R.id.iv_folder_ic);
            directorySelectedCheckBox = (CheckBox) itemView.findViewById(R.id.cb_directory_selected);

            if (!mIsDirectoryChooser) {
                itemView.setOnClickListener(null);
            }
        }
    }
}
