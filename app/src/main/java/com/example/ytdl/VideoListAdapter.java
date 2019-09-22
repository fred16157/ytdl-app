package com.example.ytdl;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class VideoListAdapter extends BaseAdapter {
    public ArrayList<VideoItem> Videos = new ArrayList<>();

    @Override
    public int getCount() {
        return Videos.size();
    }

    @Override
    public VideoItem getItem(int Pos) {
        return Videos.get(Pos);
    }

    @Override
    public long getItemId(int Pos) {
        return 0;
    }

    @Override
    public View getView(int Pos, View convertView, ViewGroup parent) {
        Context context = parent.getContext();

        if(convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_layout, parent, false);
        }

        ImageView videoImg = convertView.findViewById(R.id.videoImg);
        TextView videoTitle = convertView.findViewById(R.id.videoTitle);
        TextView videoDesc = convertView.findViewById(R.id.videoDesc);

        VideoItem video = getItem(Pos);
        videoImg.setImageDrawable(video.getThumbnail());
        videoTitle.setText(video.getTitle());
        videoDesc.setText(video.getChannel());
        return convertView;
    }

    public void addItem(Drawable videoImg, String videoTitle, String videoChannel, String videoId) {
        VideoItem video = new VideoItem();
        video.setThumbnail(videoImg);
        video.setTitle(videoTitle);
        video.setChannel(videoChannel);
        video.setVideoId(videoId);
        Videos.add(video);
    }

    public void clear()
    {
        Videos.clear();
    }
}
