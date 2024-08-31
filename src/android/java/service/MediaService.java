package com.rolamix.plugins.audioplayer.service;

import __PACKAGE_NAME__.MainApplication;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayer;
import com.devbrackets.android.playlistcore.components.playlisthandler.PlaylistHandler;
import com.devbrackets.android.playlistcore.service.BasePlaylistService;

import com.rolamix.plugins.audioplayer.data.AudioTrack;
import com.rolamix.plugins.audioplayer.manager.PlaylistManager;
import com.rolamix.plugins.audioplayer.playlist.AudioApi;
import com.rolamix.plugins.audioplayer.playlist.AudioPlaylistHandler;

public class MediaService extends BasePlaylistService<AudioTrack, PlaylistManager> {

    @Override
    public void onCreate() {
        super.onCreate();
        // Adds the audio player implementation, otherwise there's nothing to play media with
        ExoPlayer exoPlayer = new ExoPlayer.Builder(getApplicationContext()).build();
        AudioApi newAudio = new AudioApi(getApplicationContext(), exoPlayer);
        newAudio.addErrorListener(getPlaylistManager());
        getPlaylistManager().getMediaPlayers().add(newAudio);
        getPlaylistManager().onMediaServiceInit(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Releases and clears all the MediaPlayers
        for (ExoPlayer player : getPlaylistManager().getMediaPlayers()) {
            player.release();
        }

        getPlaylistManager().getMediaPlayers().clear();
    }

    @NonNull
    @Override
    protected PlaylistManager getPlaylistManager() {
        return ((MainApplication)getApplicationContext()).getPlaylistManager();
    }

    @NonNull
    @Override
    public PlaylistHandler<AudioTrack> newPlaylistHandler() {
        MediaImageProvider imageProvider = new MediaImageProvider(getApplicationContext(), new MediaImageProvider.OnImageUpdatedListener() {
            @Override
            public void onImageUpdated() {
                getPlaylistHandler().updateMediaControls();
            }
        });

        AudioPlaylistHandler.Listener<AudioTrack> listener = new AudioPlaylistHandler.Listener<AudioTrack>() {
            @Override
            public void onMediaPlayerChanged(ExoPlayer oldPlayer, ExoPlayer newPlayer) {
                getPlaylistManager().onMediaPlayerChanged(newPlayer);
            }

            @Override
            public void onItemSkipped(AudioTrack item) {
                // We don't need to do anything with this right now
                // The PluginManager receives notifications of the current item changes.
            }
        };

        return new AudioPlaylistHandler.Builder<>(
                getApplicationContext(),
                getClass(),
                getPlaylistManager(),
                imageProvider,
                listener
        ).build();
    }
}