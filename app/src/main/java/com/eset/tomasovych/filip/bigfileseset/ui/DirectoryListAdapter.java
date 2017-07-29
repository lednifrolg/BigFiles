package com.eset.tomasovych.filip.bigfileseset.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.eset.tomasovych.filip.bigfileseset.R;

import java.io.File;

public class DirectoryListAdapter extends RecyclerView.Adapter<DirectoryListAdapter.DirectoryViewHolder> {

    private Context mContext;
    private File[] mFiles;

    public DirectoryListAdapter(Context context, File[] mFiles) {
        this.mContext = context;
        this.mFiles = mFiles;
    }

    @Override
    public DirectoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.directory_list_item, parent, false);
        return new DirectoryViewHolder(view);
    }

    // set up Directory list items
    @Override
    public void onBindViewHolder(DirectoryViewHolder holder, int position) {
        holder.directoryName.setText(mFiles[position].getName());
        holder.directoryPath.setText(mFiles[position].getAbsolutePath());
        holder.directoryPath.setTag(mFiles[position].getAbsolutePath());

        // Check if directory is empty
        if (mFiles[position].isDirectory() && mFiles[position].list().length == 0) {
            holder.directoryIcon.setImageResource(R.drawable.ic_folder_empty);
        } else {
            holder.directoryIcon.setImageResource(R.drawable.ic_folder);
        }
    }

    // change displayed files
    public void swapFiles(File[] files) {
        mFiles = files;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (mFiles != null)
            return mFiles.length;

        return 0;
    }

    public class DirectoryViewHolder extends RecyclerView.ViewHolder {

        TextView directoryName;
        TextView directoryPath;
        ImageView directoryIcon;

        public DirectoryViewHolder(View itemView) {
            super(itemView);
            directoryName = (TextView) itemView.findViewById(R.id.tv_directory_name);
            directoryPath = (TextView) itemView.findViewById(R.id.tv_directory_path);
            directoryIcon = (ImageView) itemView.findViewById(R.id.iv_folder_ic);
        }
    }
}
