package com.example.ytdl;

import android.graphics.drawable.Drawable;

public class VideoItem {
    private Drawable Thumbnail;
    private String Title;
    private String Channel;
    private String VideoId;

    public void setVideoId(String VideoId) {
        this.VideoId = VideoId;
    }

    public String getVideoId() {
        return VideoId;
    }

    public Drawable getThumbnail() {
        return Thumbnail;
    }

    public void setThumbnail(Drawable Thumbnail) {
        this.Thumbnail = Thumbnail;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String Title) {
        this.Title = Title;
    }

    public String getChannel()
    {
        return Channel;
    }

    public void setChannel(String Channel) {
        this.Channel = Channel;
    }

}
