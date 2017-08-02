package com.eset.tomasovych.filip.bigfileseset.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eset.tomasovych.filip.bigfileseset.R;
import com.eset.tomasovych.filip.bigfileseset.Utils.FilesScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {


    private Context mContext;
    private List<File> mFiles;

    public List<File> getFiles() {
        return new ArrayList<>(mFiles);
    }

    public FileListAdapter(Context mContext, List<File> mFiles) {
        this.mContext = mContext;
        this.mFiles = mFiles;
    }

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.file_list_item, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FileViewHolder holder, int position) {
        holder.fileName.setText(mFiles.get(position).getName());
        holder.filePath.setText(mFiles.get(position).getAbsolutePath());
        holder.fileSize.setText(FilesScanner.formatFileSize(mFiles.get(position).length()));
    }

    public void swapFiles(List<File> files) {
        mFiles = files;
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        if (mFiles != null) {
            return mFiles.size();
        }

        return 0;
    }

    public class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName;
        TextView filePath;
        TextView fileSize;

        public FileViewHolder(View itemView) {
            super(itemView);

            fileName = (TextView) itemView.findViewById(R.id.tv_file_name);
            filePath = (TextView) itemView.findViewById(R.id.tv_file_path);
            fileSize = (TextView) itemView.findViewById(R.id.tv_file_size);
        }
    }
}
