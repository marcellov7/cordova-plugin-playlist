package com.rolamix.plugins.audioplayer.playlist;

import com.rolamix.plugins.audioplayer.data.AudioTrack;
import com.rolamix.plugins.audioplayer.manager.PlaylistManager;
import com.rolamix.plugins.audioplayer.notification.PlaylistNotificationProvider;

import androidx.annotation.Nullable;
import android.app.Service;
import android.content.Context;
import android.util.Log;

import com.devbrackets.android.playlistcore.api.PlaylistItem;
import com.devbrackets.android.playlistcore.data.MediaProgress;
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager;
import com.devbrackets.android.playlistcore.components.audiofocus.AudioFocusProvider;
import com.devbrackets.android.playlistcore.components.audiofocus.DefaultAudioFocusProvider;
import com.devbrackets.android.playlistcore.components.image.ImageProvider;
import com.devbrackets.android.playlistcore.components.mediacontrols.DefaultMediaControlsProvider;
import com.devbrackets.android.playlistcore.components.mediacontrols.MediaControlsProvider;
import com.devbrackets.android.playlistcore.components.mediasession.DefaultMediaSessionProvider;
import com.devbrackets.android.playlistcore.components.mediasession.MediaSessionProvider;
import com.devbrackets.android.playlistcore.components.playlisthandler.DefaultPlaylistHandler;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;

public class AudioPlaylistHandler<I extends PlaylistItem, M extends BasePlaylistManager<I>>
            extends DefaultPlaylistHandler<I, M> implements Player.Listener {

    private static final String TAG = "AudioPlaylistHandler";
    private boolean didSeekCatchup = false;

    AudioPlaylistHandler(
            Context context,
            Class<? extends Service> serviceClass,
            M playlistManager,
            ImageProvider<I> imageProvider,
            com.devbrackets.android.playlistcore.components.notification.PlaylistNotificationProvider notificationProvider,
            MediaSessionProvider mediaSessionProvider,
            MediaControlsProvider mediaControlsProvider,
            AudioFocusProvider<I> audioFocusProvider,
            @Nullable Listener<I> listener
    ) {
        super(context, serviceClass, playlistManager, imageProvider, notificationProvider,
                mediaSessionProvider, mediaControlsProvider, audioFocusProvider, listener);
        getMediaProgressPoll().setProgressPollDelay(1000);
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        super.onPlaybackStateChanged(state);
        // Handle playback state changes if needed
    }

    @Override
    public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
        ((PlaylistManager)getPlaylistManager()).setCurrentErrorTrack(getCurrentPlaylistItem());
        int currentIndex = getPlaylistManager().getCurrentPosition();
        int currentErrorCount = getSequentialErrors();

        super.onPlayerError(error);
        // Do not set startPaused to false if we are at the first item.
        if (currentIndex > 0 && currentErrorCount <= 3) {
            Log.e(TAG, "ListHandler error: setting startPaused to false");
            setStartPaused(false);
        }
    }

   @Override
    public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, @Player.DiscontinuityReason int reason) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason);
        Log.i(TAG, "onPositionDiscontinuity! " + newPosition.positionMs);
        getCurrentMediaProgress().update(newPosition.positionMs, getPlaybackSpeed(), getDuration());
        onProgressUpdated(getCurrentMediaProgress());
        didSeekCatchup = false;
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        super.onPlaybackStateChanged(playbackState);
        Log.i("AudioPlaylistHandler", "onPlaybackStateChanged: " + playbackState);
        // This is called when a single item completes playback.
        // For now, the superclass does the right thing, but we may need to override.
        if (playbackState == Player.STATE_ENDED) {
            onCompletion(getCurrentMediaItem());
        }
    }

    @Override
    public void play() {
        if (!didSeekCatchup) {
            I track = getCurrentPlaylistItem();
            // For streams, immediately seek to 0, which for a stream actually means
            // "start at the current location in the stream when you play again"
            // Without this, the stream buffer grows out of control, and worse, playback
            // continues where you paused. Accidentally pause for 12 hours? Yeah, you just
            // blew out the memory on your device (or forced the player to skip)
            if (((PlaylistManager) getPlaylistManager()).getResetStreamOnPause()) {
                if (track instanceof AudioTrack && ((AudioTrack) track).getIsStream()) {
                    performSeek(0, false);
                }
            }
        }

        didSeekCatchup = true;
        setPlayingBeforeSeek(true);
        super.play();
    }

    @Override
    public void pause(boolean temporary) {
        super.pause(temporary);

        if (!didSeekCatchup) {
            I track = getCurrentPlaylistItem();
            // For streams, immediately seek to 0, which for a stream actually means
            // "start at the current location in the stream when you play again"
            // Without this, the stream buffer grows out of control, and worse, playback
            // continues where you paused. Accidentally pause for 12 hours? Yeah, you just
            // blew out the memory on your device (or forced the player to skip)
            if (((PlaylistManager) getPlaylistManager()).getResetStreamOnPause()) {
                if (track instanceof AudioTrack && ((AudioTrack) track).getIsStream()) {
                    performSeek(0, false);
                }
            }
        }
        didSeekCatchup = true;
    }

    public static class Builder<I extends PlaylistItem, M extends BasePlaylistManager<I>> {

        Context context;
        Class<? extends Service> serviceClass;
        M playlistManager;
        ImageProvider<I> imageProvider;

        com.devbrackets.android.playlistcore.components.notification.PlaylistNotificationProvider notificationProvider = null;
        MediaSessionProvider mediaSessionProvider = null;
        MediaControlsProvider mediaControlsProvider = null;
        AudioFocusProvider<I> audioFocusProvider = null;
        Listener<I> listener;

        public Builder(Context context, Class<? extends Service> serviceClass,
                       M playlistManager, ImageProvider<I> imageProvider, Listener<I> listener) {
            this.context = context;
            this.serviceClass = serviceClass;
            this.playlistManager = playlistManager;
            this.imageProvider = imageProvider;
            this.listener = listener;
        }

        public AudioPlaylistHandler<I, M> build() {
            return new AudioPlaylistHandler<>(context,
                serviceClass,
                playlistManager,
                imageProvider,
                notificationProvider != null ? notificationProvider : new PlaylistNotificationProvider(context),
                mediaSessionProvider != null ? mediaSessionProvider : new DefaultMediaSessionProvider(context, serviceClass),
                mediaControlsProvider != null ? mediaControlsProvider : new DefaultMediaControlsProvider(context),
                audioFocusProvider != null ? audioFocusProvider : new DefaultAudioFocusProvider<>(context),
                listener);
        }
    }
}