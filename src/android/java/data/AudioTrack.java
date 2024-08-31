package com.rolamix.plugins.audioplayer.data;

import com.rolamix.plugins.audioplayer.manager.PlaylistManager;
import com.devbrackets.android.playlistcore.annotation.SupportedMediaType;
import com.devbrackets.android.playlistcore.api.PlaylistItem;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.*;

public class AudioTrack implements PlaylistItem {

    private final JSONObject config;
    private float bufferPercentFloat = 0f;
    private int bufferPercent = 0;
    private long duration = 0;

    public AudioTrack(@NonNull JSONObject config) {
        this.config = config;
    }

    public JSONObject toDict() {
      JSONObject info = new JSONObject();
      try {
          info.put("trackId", getTrackId());
          info.put("isStream", getIsStream());
          info.put("assetUrl", getMediaUrl());
          info.put("albumArt", getThumbnailUrl());
          info.put("artist", getArtist());
          info.put("album", getAlbum());
          info.put("title", getTitle());
      } catch (JSONException e) {
          // I can think of no reason this would ever fail
      }
      return info;
    }

    @Override
    public long getId() {
        if (getTrackId() == null) { return 0; }
        return getTrackId().hashCode();
    }

    public boolean getIsStream() {
        return this.config.optBoolean("isStream", false);
    }

    @Nullable
    public String getTrackId() {
      String trackId = this.config.optString("trackId");
      if (trackId.equals("")) { return null; }
      return trackId;
    }

    @Override
    public boolean getDownloaded() {
        return false;
    }

    @Override
    public String getDownloadedMediaUri() {
        return null;
    }

    @Override
    @SupportedMediaType
    public int getMediaType() {
        return PlaylistManager.AUDIO;
    }

    @Override
    public String getMediaUrl() {
      return this.config.optString("assetUrl", "");
    }

    @Override
    public String getThumbnailUrl() {
      String albumArt = this.config.optString("albumArt");
      if (albumArt.equals("")) { return null; }
      return albumArt;
    }

    @Override
    public String getArtworkUrl() {
        return getThumbnailUrl();
    }

    @Override
    public String getTitle() {
        return this.config.optString("title");
    }

    @Override
    public String getAlbum() {
        return this.config.optString("album");
    }

    @Override
    public String getArtist() {
        return this.config.optString("artist");
    }

    public float getBufferPercentFloat() {
      return bufferPercentFloat;
    }

    public void setBufferPercentFloat(float buff) {
      bufferPercentFloat = Math.min(Math.max(bufferPercentFloat, buff), 1f);
    }

    public int getBufferPercent() {
      return bufferPercent;
    }

    public void setBufferPercent(int buff) {
      bufferPercent = Math.max(bufferPercent, buff);
    }

    public long getDuration() {
      return duration;
    }

    public void setDuration(long dur) {
      duration = Math.max(duration, dur);
    }
}