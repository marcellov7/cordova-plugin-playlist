package com.rolamix.plugins.audioplayer.manager;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ArrayList;
import android.app.Application;
import android.util.Log;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;

import com.devbrackets.android.playlistcore.api.PlaylistItem;
import com.devbrackets.android.playlistcore.data.MediaProgress;
import com.devbrackets.android.playlistcore.manager.ListPlaylistManager;
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;

import com.rolamix.plugins.audioplayer.PlaylistItemOptions;
import com.rolamix.plugins.audioplayer.TrackRemovalItem;
import com.rolamix.plugins.audioplayer.data.AudioTrack;
import com.rolamix.plugins.audioplayer.playlist.AudioApi;
import com.rolamix.plugins.audioplayer.service.MediaService;

public class PlaylistManager extends ListPlaylistManager<AudioTrack> implements Player.Listener {

    private static final String TAG = "PlaylistManager";
    private List<AudioTrack> AudioTracks = new ArrayList<>();

    private boolean mediaServiceStarted = false;
    private float volumeLeft = 1.0f;
    private float volumeRight = 1.0f;
    private float playbackSpeed = 1.0f;
    private boolean loop = false;
    private boolean shouldStopPlaylist = false;
    private boolean previousInvoked = false;
    private boolean nextInvoked = false;
    private AudioTrack currentErrorTrack;

    private boolean resetStreamOnPause = true;

    private WeakReference<MediaControlsListener> mediaControlsListener = new WeakReference<>(null);
    private WeakReference<Player.Listener> errorListener = new WeakReference<>(null);
    private WeakReference<ExoPlayer> currentMediaPlayer = new WeakReference<>(null);

    public PlaylistManager(Application application) {
        super(application, MediaService.class);
        this.setParameters(AudioTracks, -1);
    }

    public void onMediaServiceInit(boolean hasInit) {
        mediaServiceStarted = hasInit;
    }

    public void onMediaPlayerChanged(ExoPlayer currentMediaPlayer) {
        if (this.currentMediaPlayer.get() != null) {
            this.currentMediaPlayer.get().removeListener(this);
        }
        this.currentMediaPlayer = new WeakReference<>(currentMediaPlayer);
        if (currentMediaPlayer != null) {
            currentMediaPlayer.addListener(this);
            if (mediaServiceStarted) {
                setVolume(volumeLeft, volumeRight);
                setPlaybackSpeed(playbackSpeed);
            }
        }
    }

    public void setErrorListener(Player.Listener listener) {
        errorListener = new WeakReference<>(listener);
    }

    public void setMediaControlsListener(MediaControlsListener listener) {
        mediaControlsListener = new WeakReference<>(listener);
    }

    public boolean getResetStreamOnPause() {
        return resetStreamOnPause;
    }

    public void setResetStreamOnPause(boolean val) {
        resetStreamOnPause = val;
    }

    public AudioTrack getCurrentErrorTrack() {
        return currentErrorTrack;
    }

    public void setCurrentErrorTrack(@Nullable PlaylistItem errorItem) {
        currentErrorTrack = (AudioTrack)errorItem;
    }

    public boolean isPlaying() {
        ExoPlayer player = currentMediaPlayer.get();
        return player != null && player.isPlaying();
    }

    @Override
    public void onPlayerError(@NonNull com.google.android.exoplayer2.PlaybackException error) {
        Log.i(TAG, "onPlayerError: " + error.toString());
        Player.Listener listener = errorListener.get();
        if (listener != null) {
            listener.onPlayerError(error);
        }
    }

    private boolean isShouldStopPlaylist() {
        return shouldStopPlaylist;
    }

    public void setShouldStopPlaylist(boolean shouldStopPlaylist) {
        this.shouldStopPlaylist = shouldStopPlaylist;
    }

    @Override
    public boolean isNextAvailable() {
        boolean isAtEnd = getCurrentPosition() + 1 >= getItemCount();
        boolean isConstrained = getCurrentPosition() + 1 >= 0 && getCurrentPosition() + 1 < getItemCount();

        if (isAtEnd) {
            return loop;
        }
        return isConstrained;
    }

    @Override
    public AudioTrack getCurrentItem() {
        boolean isAtEnd = getCurrentPosition() + 1 == getItemCount();
        boolean isConstrained = getCurrentPosition() >= 0 && getCurrentPosition() < getItemCount();

        if (isAtEnd && isShouldStopPlaylist()) {
            return null;
        }
        if (isConstrained) {
            return getItem(getCurrentPosition());
        }
        return null;
    }

    @Override
    public AudioTrack previous() {
        setCurrentPosition(Math.max(0, getCurrentPosition() - 1));
        AudioTrack prevItem = getCurrentItem();

        if (!previousInvoked) {
            Log.i(TAG, "PlaylistManager.previous: invoked via service.");
            MediaControlsListener listener = mediaControlsListener.get();
            if (listener != null) {
                listener.onPrevious(prevItem, getCurrentPosition());
            }
        }

        previousInvoked = false;
        return prevItem;
    }

    @Override
    public AudioTrack next() {
        if (isNextAvailable()) {
            setCurrentPosition(Math.min(getCurrentPosition() + 1, getItemCount()));
        } else {
            if (loop) {
                setCurrentPosition(BasePlaylistManager.INVALID_POSITION);
            } else {
                setShouldStopPlaylist(true);
                raiseAndCheckOnNext();
                return null;
            }
        }

        raiseAndCheckOnNext();
        return getCurrentItem();
    }

    private void raiseAndCheckOnNext() {
        AudioTrack nextItem = getCurrentItem();
        if (!nextInvoked) {
            Log.i(TAG, "PlaylistManager.next: invoked via service.");
            MediaControlsListener listener = mediaControlsListener.get();
            if (listener != null) {
                listener.onNext(nextItem, getCurrentPosition());
            }
        }
        nextInvoked = false;
    }

    public void setAllItems(List<AudioTrack> items, PlaylistItemOptions options) {
        clearItems();
        addAllItems(items);
        setCurrentPosition(0);

        long seekStart = 0;
        if (options.getRetainPosition()) {
            if (options.getPlayFromPosition() > 0) {
                seekStart = options.getPlayFromPosition();
            } else {
                MediaProgress progress = getCurrentProgress();
                if (progress != null) {
                    seekStart = progress.getPosition();
                }
            }
        }

        String idStart = null;
        if (options.getRetainPosition()) {
            if (options.getPlayFromId() != null) {
                idStart = options.getPlayFromId();
            }
        }
        if (idStart != null && !"".equals((idStart))) {
            int code = idStart.hashCode();
            setCurrentItem(code);
        }

        beginPlayback(seekStart, options.getStartPaused());
    }

    public void addItem(AudioTrack item) {
        if (item == null) { return; }
        AudioTracks.add(item);
        setItems(AudioTracks);
    }

    public void addAllItems(List<AudioTrack> items) {
        AudioTrack currentItem = getCurrentItem();
        AudioTracks.addAll(items);
        setItems(AudioTracks);
        setCurrentPosition(AudioTracks.indexOf(currentItem));
    }

    public AudioTrack removeItem(int index, @Nullable String itemId) {
        boolean wasPlaying = this.isPlaying();
        ExoPlayer player = currentMediaPlayer.get();
        if (player != null) {
            player.pause();
        }
        int currentPosition = getCurrentPosition();
        AudioTrack currentItem = getCurrentItem();
        AudioTrack foundItem = null;
        boolean removingCurrent = false;

        int resolvedIndex = resolveItemPosition(index, itemId);
        if (resolvedIndex >= 0) {
            foundItem = AudioTracks.get(resolvedIndex);
            if (foundItem == currentItem) {
                removingCurrent = true;
            }
            AudioTracks.remove(resolvedIndex);
        }

        setItems(AudioTracks);
        setCurrentPosition(removingCurrent ? currentPosition : AudioTracks.indexOf(currentItem));
        this.beginPlayback(0, !wasPlaying);

        return foundItem;
    }

    public ArrayList<AudioTrack> removeAllItems(ArrayList<TrackRemovalItem> items) {
        ArrayList<AudioTrack> removedTracks = new ArrayList<>();
        boolean wasPlaying = this.isPlaying();
        ExoPlayer player = currentMediaPlayer.get();
        if (player != null) {
            player.pause();
        }
        int currentPosition = getCurrentPosition();
        AudioTrack currentItem = getCurrentItem();
        boolean removingCurrent = false;

        for (TrackRemovalItem item : items) {
            int resolvedIndex = resolveItemPosition(item.trackIndex, item.trackId);
            if (resolvedIndex >= 0) {
                AudioTrack foundItem = AudioTracks.get(resolvedIndex);
                if (foundItem == currentItem) {
                    removingCurrent = true;
                }
                removedTracks.add(foundItem);
                AudioTracks.remove(resolvedIndex);
            }
        }

        setItems(AudioTracks);
        setCurrentPosition(removingCurrent ? currentPosition : AudioTracks.indexOf(currentItem));
        this.beginPlayback(0, !wasPlaying);

        return removedTracks;
    }

    public void clearItems() {
        ExoPlayer player = currentMediaPlayer.get();
        if (player != null) {
            player.stop();
        }
        AudioTracks.clear();
        setItems(AudioTracks);
        setCurrentPosition(BasePlaylistManager.INVALID_POSITION);
    }

    private int resolveItemPosition(int trackIndex, String trackId) {
        int resolvedPosition = -1;
        if (trackIndex >= 0 && trackIndex < AudioTracks.size()) {
            resolvedPosition = trackIndex;
        } else if (trackId != null && !"".equals(trackId)) {
            int itemPos = getPositionForItem(trackId.hashCode());
            if (itemPos != BasePlaylistManager.INVALID_POSITION) {
                resolvedPosition = itemPos;
            }
        }
        return resolvedPosition;
    }

    public boolean getLoop() {
        return loop;
    }

    public void setLoop(boolean newLoop) {
        loop = newLoop;
    }

    public float getVolumeLeft() {
        return volumeLeft;
    }

    public float getVolumeRight() {
        return volumeRight;
    }

    public void setVolume(@FloatRange(from = 0.0, to = 1.0) float left, @FloatRange(from = 0.0, to = 1.0) float right) {
        volumeLeft = left;
        volumeRight = right;

        ExoPlayer player = currentMediaPlayer.get();
        if (player != null) {
            Log.i("PlaylistManager", "setVolume completing with volume = " + left);
            player.setVolume((volumeLeft + volumeRight) / 2);
        }
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(@FloatRange(from = 0.0, to = 1.0) float speed) {
        playbackSpeed = speed;
        ExoPlayer player = currentMediaPlayer.get();
        if (player != null) {
            Log.i("PlaylistManager", "setPlaybackSpeed completing with speed = " + speed);
            player.setPlaybackSpeed(playbackSpeed);
        }
    }

    public void beginPlayback(@IntRange(from = 0) long seekPosition, boolean startPaused) {
        super.play(seekPosition, startPaused);
        try {
            setVolume(volumeLeft, volumeRight);
            setPlaybackSpeed(playbackSpeed);
        } catch (Exception e) {
            Log.w(TAG, "beginPlayback: Error setting volume or playback speed: " + e.getMessage());
        }
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            case Player.STATE_IDLE:
                // The player does not have any media to play
                break;
            case Player.STATE_BUFFERING:
                // The player is buffering (loading the content)
                break;
            case Player.STATE_READY:
                // The player is able to immediately play
                break;
            case Player.STATE_ENDED:
                // The player has finished playing the media
                break;
        }
    }

    @Override
    public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
        // Handle position discontinuity (e.g., when seeking or switching tracks)
    }

    // Add any other methods from Player.Listener that you need to implement
}