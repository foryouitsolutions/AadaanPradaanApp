package com.foryouitsolutions.aadaanpradaan;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.Nullable;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Status;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.EXTRA_STREAM;

public final class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {


    @NonNull
    private final List<DownloadData> downloads = new ArrayList<>();
    @NonNull
    private final ActionListener actionListener;

    public interface ActionListener {

        void onPauseDownload(int id);

        void onResumeDownload(int id);

        void onRemoveDownload(int id);

        void onRetryDownload(int id);


    }

    FileAdapter(@NonNull final ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.actionButton.setOnClickListener(null);
        holder.actionButton.setEnabled(true);

        final DownloadData downloadData = downloads.get(position);
        String url = "";
        if (downloadData.download != null) {
            url = downloadData.download.getUrl();
        }
        final Uri uri = Uri.parse(url);
        final Status status = downloadData.download.getStatus();
        final Context context = holder.itemView.getContext();

        String view_file_name = downloadData.download.getFile();
        try {
            view_file_name = URLDecoder.decode(view_file_name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        view_file_name = view_file_name.substring(view_file_name.lastIndexOf('/') + 1);
        holder.titleTextView.setText(view_file_name);
        holder.statusTextView.setText(getStatusString(status));

        int progress = downloadData.download.getProgress();
        if (progress == -1) { // Download progress is undermined at the moment.
            progress = 0;
        }
        holder.progressBar.setProgress(progress);
        holder.progressTextView.setText(context.getString(R.string.percent_progress, progress));


        if (downloadData.eta == -1) {
            holder.timeRemainingTextView.setText("");
        } else {
            holder.timeRemainingTextView.setText(Utils.getETAString(context, downloadData.eta));
        }

        if (downloadData.downloadedBytesPerSecond == 0) {
            holder.downloadedBytesPerSecondTextView.setText("");
        } else {
            holder.downloadedBytesPerSecondTextView.setText(Utils.getDownloadSpeedString(context, downloadData.downloadedBytesPerSecond));
        }
        holder.actionButton2.setOnClickListener(view -> {
            Toast.makeText(context, "Removed ", Toast.LENGTH_LONG).show();
            actionListener.onRemoveDownload(downloadData.download.getId());
            return;
        });

        switch (status) {
            case COMPLETED: {
                //holder.actionButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_clear_24,0,0,0);
                holder.actionButton.setText(R.string.view);
                holder.actionButton.setOnClickListener(view -> {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                        Toast.makeText(context, "Downloaded Path:" + downloadData.download.getFile(), Toast.LENGTH_LONG).show();
//                        return;
//                    }
                    final Uri uri1 = Uri.parse(downloadData.download.getFile());
                    final Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType(getMimeType(uri1, context));
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    share.putExtra(EXTRA_STREAM, uri1);
                    context.startActivity(Intent.createChooser(share, "Share file"));
                });


                break;
            }
            case FAILED: {
                holder.actionButton.setText(R.string.retry);
                holder.actionButton.setOnClickListener(view -> {
                    holder.actionButton.setEnabled(false);
                    actionListener.onRetryDownload(downloadData.download.getId());
                });
                break;
            }
            case PAUSED: {
                holder.actionButton.setText(R.string.resume);
                holder.actionButton.setOnClickListener(view -> {
                    holder.actionButton.setEnabled(false);
                    actionListener.onResumeDownload(downloadData.download.getId());
                });
                break;
            }
            case DOWNLOADING:
            case QUEUED: {
                holder.actionButton.setText(R.string.pause);
                holder.actionButton.setOnClickListener(view -> {
                    holder.actionButton.setEnabled(false);
                    actionListener.onPauseDownload(downloadData.download.getId());
                });
                break;
            }
            case ADDED: {
                holder.actionButton.setText(R.string.download);
                holder.actionButton.setOnClickListener(view -> {
                    holder.actionButton.setEnabled(false);
                    actionListener.onResumeDownload(downloadData.download.getId());
                });
                break;
            }
            default: {
                break;
            }
        }

        //Set delete action
        holder.itemView.setOnLongClickListener(v -> {
            final Uri uri12 = Uri.parse(downloadData.download.getUrl());
            new AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.delete_title, uri12.getLastPathSegment()))
                    .setPositiveButton(R.string.delete, (dialog, which) -> actionListener.onRemoveDownload(downloadData.download.getId()))
                    .setNegativeButton(R.string.cancel, null)
                    .show();

            return true;
        });


    }

    public String getMimeType(Uri uri, Context context) {
        String mimeType = null;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }

    public List<DownloadData> getDownloads() {
        return downloads;
    }

    public void addDownload(@NonNull final Download download) {
        boolean found = false;
        DownloadData data = null;
        int dataPosition = -1;
        for (int i = 0; i < downloads.size(); i++) {
            final DownloadData downloadData = downloads.get(i);
            if (downloadData.id == download.getId()) {
                data = downloadData;
                dataPosition = i;
                found = true;
                break;
            }
        }
        if (!found) {
            final DownloadData downloadData = new DownloadData();
            downloadData.id = download.getId();
            downloadData.download = download;
            downloads.add(downloadData);
            notifyItemInserted(downloads.size() - 1); // on new download update update adapter
        } else {
            data.download = download;
            notifyItemChanged(dataPosition);
        }
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }

    public void update(@NonNull final Download download, long eta, long downloadedBytesPerSecond) {
        for (int position = 0; position < downloads.size(); position++) {
            final DownloadData downloadData = downloads.get(position);
            if (downloadData.id == download.getId()) {
                switch (download.getStatus()) {
                    case REMOVED:
                    case DELETED: {
                        downloads.remove(position);
                        notifyItemRemoved(position);
                        break;
                    }
                    default: {
                        downloadData.download = download;
                        downloadData.eta = eta;
                        downloadData.downloadedBytesPerSecond = downloadedBytesPerSecond;
                        notifyItemChanged(position);
                    }
                }
                return;
            }
        }
    }

    private String getStatusString(Status status) {
        switch (status) {
            case COMPLETED:
                return "Done";
            case DOWNLOADING:
                return "Downloading";
            case FAILED:
                return "Error";
            case PAUSED:
                return "Paused";
            case QUEUED:
                return "Waiting in Queue";
            case REMOVED:
                return "Removed";
            case NONE:
                return "Not Queued";
            default:
                return "Unknown";
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView titleTextView;
        final TextView statusTextView;
        public final ProgressBar progressBar;
        public final TextView progressTextView;
        public final Button actionButton;
        public final Button actionButton2;
        final TextView timeRemainingTextView;
        final TextView downloadedBytesPerSecondTextView;

        // public ProgressBar progressBarCircle;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            statusTextView = itemView.findViewById(R.id.status_TextView);
            progressBar = itemView.findViewById(R.id.progressBar);
            //progressBarCircle = itemView.findViewById(R.id.progressbarcircle);
            actionButton = itemView.findViewById(R.id.actionButton);
            actionButton2 = itemView.findViewById(R.id.actionButton2);
            progressTextView = itemView.findViewById(R.id.progress_TextView);
            timeRemainingTextView = itemView.findViewById(R.id.remaining_TextView);
            downloadedBytesPerSecondTextView = itemView.findViewById(R.id.downloadSpeedTextView);
        }

    }

    public static class DownloadData {
        public int id;
        @Nullable
        public Download download;
        long eta = -1;
        long downloadedBytesPerSecond = 0;

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            if (download == null) {
                return "";
            }
            return download.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof DownloadData && ((DownloadData) obj).id == id;
        }
    }

}
