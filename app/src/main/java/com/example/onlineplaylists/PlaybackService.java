package com.example.onlineplaylists;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class PlaybackService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private NotificationManager notificationManager;
    private MediaSession mediaSession;
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer youTubePlayer;
    private Handler handler;
    private MediaMetadata.Builder mediaMetadata;
    private PlaybackState.Builder playbackState;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private Playlist playlist;
    private int currentVideoIndex;
    private int currentSecond;
    private boolean isPlaying = false;
    private boolean shuffle;
    private boolean isStarting = false;
    private boolean isReady = false;
    private String title = "Yükleniyor...";
    private ArrayList<Integer> playlistIndexes;
    private boolean timerSet;
    private long timerMs;

    private final String ACTION_START_SERVICE = "com.opl.ACTION_START_SERVICE";
    private final String ACTION_PLAY = "com.opl.ACTION_PLAY";
    private final String ACTION_CLOSE = "com.opl.ACTION_CLOSE";
    private final String ACTION_FORWARD = "com.opl.ACTION_FORWARD";
    private final String ACTION_PREV = "com.opl.ACTION_PREV";
    private final String ACTION_NEXT = "com.opl.ACTION_NEXT";
    private final String ACTION_CONTINUE = "com.opl.ACTION_CONTINUE";

    private final int SERVICE_NOTIFICATION = 101;
    private final int TIMEOUT_NOTIFICATION = 102;

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("def", "Playback", NotificationManager.IMPORTANCE_LOW);
        channel.enableVibration(false);
        channel.setSound(null, null);
        channel.enableLights(false);
        notificationManager.createNotificationChannel(channel);

        mediaSession = new MediaSession(this, "Online Playlists");
        mediaMetadata = new MediaMetadata.Builder();
        playbackState = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY_PAUSE
                        | PlaybackState.ACTION_SEEK_TO
                        | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackState.ACTION_SKIP_TO_NEXT
                        | PlaybackState.ACTION_STOP
                        | PlaybackState.ACTION_FAST_FORWARD);
        mediaSession.setPlaybackState(playbackState.build());
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                youTubePlayer.play();
                super.onPlay();
            }

            @Override
            public void onPause() {
                youTubePlayer.pause();
                super.onPause();
            }

            @Override
            public void onSkipToNext() {
                playNext();
                super.onSkipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                playPrevious();
                super.onSkipToPrevious();
            }

            @Override
            public void onFastForward() {
                youTubePlayer.seekTo(currentSecond + 5);
                super.onFastForward();
            }

            @Override
            public void onStop() {
                super.onStop();
                stopSelf();
            }

            @Override
            public void onSeekTo(long pos) {
                youTubePlayer.seekTo((float) (pos/1000));
                super.onSeekTo(pos);
            }
        });

        sp = getSharedPreferences("OnlinePlaylists", MODE_PRIVATE);
        spe = sp.edit();

        playlistIndexes = new ArrayList<>();

        youTubePlayerView = new YouTubePlayerView(this);
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer _youTubePlayer) {
                isReady = true;
                youTubePlayer = _youTubePlayer;
                if (isStarting) initializePlayer();
                super.onReady(youTubePlayer);
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.ENDED) {
                    playOtherVideo();
                } else {
                    boolean isPlayingBeforeValue = isPlaying;
                    isPlaying = state == PlayerConstants.PlayerState.PLAYING;
                    if (isPlaying != isPlayingBeforeValue) startForegroundService();
                    boolean isBuffering = state == PlayerConstants.PlayerState.BUFFERING;
                    playbackState.setState(isPlaying ? PlaybackState.STATE_PLAYING
                            : isBuffering ? PlaybackState.STATE_BUFFERING
                            : PlaybackState.STATE_PAUSED
                            , currentSecond * 1000L
                            , 1);
                    mediaSession.setPlaybackState(playbackState.build());
                }
                super.onStateChange(youTubePlayer, state);
            }

            @Override
            public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float second) {
                currentSecond = (int) second;
                spe.putInt("currentSecond", currentSecond).commit();
                playbackState.setState(PlaybackState.STATE_PLAYING , currentSecond * 1000L, 1);
                mediaSession.setPlaybackState(playbackState.build());
                super.onCurrentSecond(youTubePlayer, second);
            }

            @Override
            public void onVideoDuration(@NonNull YouTubePlayer youTubePlayer, float duration) {
                mediaMetadata.putLong(MediaMetadata.METADATA_KEY_DURATION, (long) (duration * 1000L));
                mediaSession.setMetadata(mediaMetadata.build());
                super.onVideoDuration(youTubePlayer, duration);
            }
        });
        handler = new Handler(getMainLooper());
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(SERVICE_NOTIFICATION, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(SERVICE_NOTIFICATION, getNotification());
        }
    }

    private void setTimer() {
        if (timerSet) {
            long afterMillis = timerMs - Calendar.getInstance().getTimeInMillis();
            if (afterMillis > 0) {
                handler = new Handler(getMainLooper());
                handler.postDelayed(() -> {
                    youTubePlayer.pause();
                    timerSet = false;
                    spe.putBoolean("timerSet", false);
                }, afterMillis);
            } else {
                timerSet = false;
                spe.putBoolean("timerSet", false);
                setTimer();
            }
        } else if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction() != null) {
                String action = intent.getAction();
                switch (action) {
                    case ACTION_START_SERVICE:
                        playlist = new Playlist().fromJson(intent.getStringExtra("playlist"));
                        currentVideoIndex = sp.getInt("currentVideoIndex", 0);
                        currentSecond = sp.getInt("currentSecond", 0);
                        shuffle = sp.getBoolean("shuffle", false);
                        timerSet = sp.getBoolean("timerSet", false);
                        timerMs = sp.getLong("timerMs", 0);
                        spe.putBoolean("serviceRunning", true).commit();
                        isStarting = true;
                        if (isReady) initializePlayer();
                        setTimer();
                        break;
                    case ACTION_PLAY:
                        play();
                        break;
                    case ACTION_CLOSE:
                        notificationManager.cancel(TIMEOUT_NOTIFICATION);
                        stopSelf();
                        break;
                    case ACTION_FORWARD:
                        forward();
                        break;
                    case ACTION_PREV:
                        playPrevious();
                        break;
                    case ACTION_NEXT:
                        playNext();
                        break;
                    default:
                        break;
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        youTubePlayerView.release();
        spe.putBoolean("serviceRunning", false).commit();
        notificationManager.cancel(SERVICE_NOTIFICATION);
        timerSet = false;
        setTimer();
    }

    private Notification getNotification() {
        Notification notification;
        title = playlist.getVideoAt(currentVideoIndex).title;
        Bitmap bitmap = null;
        try {
            bitmap = Glide.with(this).asBitmap().load(playlist.getVideoAt(currentVideoIndex).getThumbnailUrl()).submit().get();
            mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
            mediaSession.setMetadata(mediaMetadata.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notification = new Notification.Builder(this, "def")
                    .setSmallIcon(R.drawable.baseline_smart_display_24)
                    .setTicker(getString(R.string.app_name))  // the status text
                    .setContentTitle(title)  // the label of the entry
                    .setContentText(playlist.title)
                    .setShowWhen(false)
                    .setContentIntent(getPendingIntent())
                    .setDeleteIntent(getIntentFor(ACTION_CLOSE))
                    .setColorized(true)
                    .setStyle(new Notification.DecoratedMediaCustomViewStyle()
                            .setMediaSession(mediaSession.getSessionToken()))
                    .setAutoCancel(false)
                    .build();
        } else {
            notification = new Notification.Builder(this, "def")
                    .setSmallIcon(R.drawable.baseline_smart_display_24)
                    .setTicker(getString(R.string.app_name))  // the status text
                    .setContentTitle(title)  // the label of the entry
                    .setContentText(playlist.title)
                    .setShowWhen(false)
                    .setContentIntent(getPendingIntent())
                    .setDeleteIntent(getIntentFor(ACTION_CLOSE))
                    .setColorized(true)
                    .setStyle(new Notification.DecoratedMediaCustomViewStyle()
                            .setShowActionsInCompactView(0, 1, 2)
                            .setMediaSession(mediaSession.getSessionToken()))
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(this, R.drawable.baseline_skip_previous_24),
                            "Önceki",
                            getIntentFor(ACTION_PREV)).build())
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(this, isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24),
                            isPlaying ? "Duraklat" : "Çal",
                            getIntentFor(ACTION_PLAY)).build())
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(this, R.drawable.baseline_skip_next_24),
                            "Sonraki",
                            getIntentFor(ACTION_NEXT)).build())
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(this, R.drawable.baseline_forward_5_24),
                            "İleri atla",
                            getIntentFor(ACTION_FORWARD)).build())
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(this, R.drawable.baseline_stop_24),
                            "Durdur",
                            getIntentFor(ACTION_CLOSE)).build())
                    .setAutoCancel(false)
                    .setLargeIcon(bitmap)
                    .build();
        }
        return notification;
    }

    private @NonNull PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private @NonNull PendingIntent getIntentFor(String action) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void initializePlayer() {
        changeVideo(false);
    }

    private void play() {
        if (isPlaying) youTubePlayer.pause();
        else youTubePlayer.play();
    }

    private void forward() {
        youTubePlayer.seekTo(currentSecond + 5);
    }

    private void playPrevious() {
        currentVideoIndex = currentVideoIndex == 0 ? playlist.getLength() - 1 : currentVideoIndex - 1;
        changeVideo(true);
    }

    private void playNext() {
        currentVideoIndex = currentVideoIndex == playlist.getLength() - 1 ? 0 : currentVideoIndex + 1;
        changeVideo(true);
    }

    private void playRandom() {
        if (playlistIndexes.isEmpty())
            for (int i = 0; i < playlist.getLength(); i++) playlistIndexes.add(i);
        currentVideoIndex = playlistIndexes.get(new Random().nextInt(playlistIndexes.size()));
        changeVideo(true);
    }

    private void playOtherVideo() {
        if (shuffle) playRandom();
        else playNext();
    }

    private void changeVideo(boolean startFrom) {
        YouTubeVideo video = playlist.getVideoAt(currentVideoIndex);
        youTubePlayer.loadVideo(video.id, startFrom ? video.musicStartSeconds : currentSecond);
        playlistIndexes.remove((Integer) currentVideoIndex);
        startForegroundService();
        title = video.title;
        spe.putInt("currentVideoIndex", currentVideoIndex).commit();
        mediaMetadata.putLong(MediaMetadata.METADATA_KEY_DURATION, -1L);
        mediaSession.setMetadata(mediaMetadata.build());
    }
}