package com.rolamix.plugins.audioplayer.playlist;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import com.rolamix.plugins.audioplayer.data.AudioTrack;
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager;

import org.jetbrains.annotations.NotNull;

public class AudioApi extends BaseMediaApi implements Player.Listener {
    @NonNull
    private ExoPlayer exoPlayer;
    private DataSource.Factory dataSourceFactory;

    public AudioApi(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.exoPlayer = new ExoPlayer.Builder(appContext).build();
        this.dataSourceFactory = new DefaultDataSourceFactory(appContext, 
                Util.getUserAgent(appContext, "RmxAudioPlayer"));

        exoPlayer.addListener(this);
    }

    @Override
    public boolean isPlaying() {
        return exoPlayer.isPlaying();
    }

    @Override
    public void play() {
        exoPlayer.play();
    }

    @Override
    public void pause() {
        exoPlayer.pause();
    }

    @Override
    public void stop() {
        exoPlayer.stop();
    }

    @Override
    public void reset() {
        exoPlayer.stop();
        exoPlayer.clearMediaItems();
    }

    @Override
    public void release() {
        exoPlayer.release();
    }

    @Override
    public void setVolume(@FloatRange(from = 0.0, to = 1.0) float left, @FloatRange(from = 0.0, to = 1.0) float right) {
        exoPlayer.setVolume((left + right) / 2);
    }

    @Override
    public void seekTo(@IntRange(from = 0L) long milliseconds) {
        exoPlayer.seekTo(milliseconds);
    }

    public void setPlaybackSpeed(@FloatRange(from = 0.0, to = 1.0) float speed) {
        exoPlayer.setPlaybackSpeed(speed);
    }

    @Override
    public boolean handlesItem(@NotNull AudioTrack item) {
        return item.getMediaType() == BasePlaylistManager.AUDIO;
    }

    @Override
    public void playItem(@NotNull AudioTrack item) {
        Uri uri = Uri.parse(item.getDownloaded() ? item.getDownloadedMediaUri() : item.getMediaUrl());
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri));
        exoPlayer.setMediaSource(mediaSource);
        exoPlayer.prepare();
    }

    @Override
    public long getCurrentPosition() {
        return exoPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return exoPlayer.getDuration();
    }

    @Override
    public int getBufferedPercent() {
        return exoPlayer.getBufferedPercentage();
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        switch (state) {
            case Player.STATE_READY:
                onPrepared();
                break;
            case Player.STATE_ENDED:
                onCompletion();
                break;
        }
    }

    @Override
    public void onPlayerError(@NonNull com.google.android.exoplayer2.PlaybackException error) {
        onError(error);
    }
}