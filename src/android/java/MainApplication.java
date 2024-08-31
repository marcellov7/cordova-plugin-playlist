package com.teyuto.base;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import com.rolamix.plugins.audioplayer.manager.PlaylistManager;

import java.io.File;

public class MainApplication extends Application {
    @Nullable
    private PlaylistManager playlistManager;
    private Cache downloadCache;

    @Override
    public void onCreate() {
        super.onCreate();

        playlistManager = new PlaylistManager(this);
        configureExoPlayer();
    }

    @Nullable
    public PlaylistManager getPlaylistManager() {
        return playlistManager;
    }

    private void configureExoPlayer() {
        String userAgent = Util.getUserAgent(this, "YourAppName");
        HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setAllowCrossProtocolRedirects(true);

        // Create a default DataSource.Factory which combines the cache and the http data source
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, httpDataSourceFactory);

        // Create and set up cache
        if (downloadCache == null) {
            File cacheDir = new File(getCacheDir(), "media");
            downloadCache = new SimpleCache(cacheDir, new NoOpCacheEvictor(), null);
        }

        // Wrap the default DataSource.Factory in a CacheDataSource.Factory
        CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(downloadCache)
                .setUpstreamDataSourceFactory(dataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        // Use this cacheDataSourceFactory when creating your ExoPlayer instance
    }
}