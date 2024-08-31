package com.rolamix.plugins.audioplayer.playlist;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import com.rolamix.plugins.audioplayer.data.AudioTrack;
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class AudioApi extends BaseMediaApi implements Player.Listener {
    @NonNull
    private ExoPlayer exoPlayer;
    private DefaultDataSourceFactory dataSourceFactory;
    private Handler handler = new Handler();

    private ReentrantLock errorListenersLock = new ReentrantLock(true);
    private ArrayList<WeakReference<Player.Listener>> errorListeners = new ArrayList<>();

    public AudioApi(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.exoPlayer = new ExoPlayer.Builder(appContext).build();
        this.dataSourceFactory = new DefaultDataSourceFactory(appContext, Util.getUserAgent(appContext, "RmxAudioPlayer"));

        exoPlayer.addListener(this);
    }

    public void addErrorListener(Player.Listener listener) {
        errorListenersLock.lock();
        errorListeners.add(new WeakReference<>(listener));
        errorListenersLock.unlock();
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
    public boolean getHandlesOwnAudioFocus() {
        return false;
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
        errorListenersLock.lock();
        for(WeakReference<Player.Listener> listener : errorListeners) {
            if (listener.get() != null) {
                listener.get().onPlayerError(error);
            }
        }
        errorListenersLock.unlock();
    }
}