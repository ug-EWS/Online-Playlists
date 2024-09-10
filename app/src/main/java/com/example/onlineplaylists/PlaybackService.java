package com.example.onlineplaylists;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentCallbacks;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.Objects;
import java.util.Random;

import javax.security.auth.callback.Callback;

public class PlaybackService extends Service {
    public class ServiceBinder extends Binder {
        PlaybackService getService() {
           return PlaybackService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    private final IBinder binder = new ServiceBinder();

    private NotificationManager notificationManager;
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer youTubePlayer;
    private RemoteViews remoteViews;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private Playlist playlist;
    private int currentPlaylistIndex;
    private int currentVideoIndex;
    private int currentSecond;
    private boolean isPlaying = false;
    private boolean shuffle;
    private boolean isStarting = false;
    private boolean isReady = false;
    private String title = "Online Playlists";

    @Override
    public void onCreate() {
        Log.i("start", "Service created");
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("def", "Playback", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(false);
            Uri uri = Uri.parse(("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.silent));
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(uri, audioAttributes);
            channel.enableLights(false);
            notificationManager.createNotificationChannel(channel);
        }
        sp = getSharedPreferences("OnlinePlaylists", MODE_PRIVATE);
        spe = sp.edit();
        youTubePlayerView = new YouTubePlayerView(this);
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer _youTubePlayer) {
                isReady = true;
                youTubePlayer = _youTubePlayer;
                if(isStarting) initializePlayer();
                super.onReady(youTubePlayer);
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.ENDED) {
                    playOtherVideo();
                } else {
                    isPlaying = state == PlayerConstants.PlayerState.PLAYING;
                }
                super.onStateChange(youTubePlayer, state);
            }

            @Override
            public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float second) {
                currentSecond = (int)second;
                spe.putInt("currentSecond", currentSecond).commit();
                super.onCurrentSecond(youTubePlayer, second);
            }
        });
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(R.string.app_name, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(R.string.app_name, getNotification());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("start", "Service started");
        if (intent != null) {
            if (intent.getAction() == null) {

            } else {
                String action = intent.getAction();
                switch (action) {
                    case ACTION_START_SERVICE:
                        playlist = new Playlist().fromJson(intent.getStringExtra("playlist"));
                        currentPlaylistIndex = sp.getInt("currentPlaylistIndex", 0);
                        currentVideoIndex = sp.getInt("currentVideoIndex", 0);
                        currentSecond = sp.getInt("currentSecond", 0);
                        spe.putBoolean("serviceRunning", true).commit();
                        isStarting = true;
                        if (isReady) initializePlayer();
                        break;
                    case ACTION_PLAY:
                        play();
                        break;
                    case ACTION_CLOSE:
                        stopSelf();
                        break;
                    case ACTION_PAUSE:
                        pause();
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
        notificationManager.cancel(R.string.app_name);
    }

    private Notification getNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification notification = new Notification.Builder(this, "def")
                    .setSmallIcon(R.drawable.baseline_smart_display_24)
                    .setTicker(getString(R.string.app_name))  // the status text
                    .setContentTitle(getString(R.string.app_name))  // the label of the entry
                    .setContentText("Yükleniyor...")
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(getPendingIntent())
                    .setCustomContentView(getRemoteViews())
                    .build();
            return notification;
        } else {
            return new Notification();
        }
    }

    private @NonNull RemoteViews getRemoteViews() {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.playback_notification);
        views.setOnClickPendingIntent(R.id.play, getIntentFor(ACTION_PLAY));
        views.setOnClickPendingIntent(R.id.off, getIntentFor(ACTION_CLOSE));
        views.setOnClickPendingIntent(R.id.pause, getIntentFor(ACTION_PAUSE));
        views.setOnClickPendingIntent(R.id.previous, getIntentFor(ACTION_PREV));
        views.setOnClickPendingIntent(R.id.next, getIntentFor(ACTION_NEXT));
        views.setImageViewResource(R.id.icon, R.drawable.baseline_smart_display_24);
        views.setImageViewResource(R.id.play, isPlaying ? R.drawable.baseline_play_arrow_24 : R.drawable.baseline_pause_24);
        views.setImageViewResource(R.id.off, R.drawable.baseline_stop_24);
        views.setImageViewResource(R.id.pause, R.drawable.baseline_forward_5_24);
        views.setImageViewResource(R.id.previous, R.drawable.baseline_skip_previous_24);
        views.setImageViewResource(R.id.next, R.drawable.baseline_skip_next_24);
        views.setTextViewText(R.id.title, title);
        return views;
    }

    private @NonNull PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private @NonNull PendingIntent getIntentFor(String action) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private final String ACTION_START_SERVICE = "com.opl.ACTION_START_SERVICE";
    private final String ACTION_PLAY = "com.opl.ACTION_PLAY";
    private final String ACTION_CLOSE = "com.opl.ACTION_CLOSE";
    private final String ACTION_PAUSE = "com.opl.ACTION_PAUSE";
    private final String ACTION_PREV = "com.opl.ACTION_PREV";
    private final String ACTION_NEXT = "com.opl.ACTION_NEXT";

    private void initializePlayer() {
        youTubePlayer.loadVideo(playlist.getVideoAt(currentVideoIndex).id, currentSecond);
        title = playlist.getVideoAt(currentVideoIndex).title;
        startForegroundService();
    }

    private void play(){
        if (isPlaying) youTubePlayer.pause(); else youTubePlayer.play();
        startForegroundService();
    }

    private void pause() {
        youTubePlayer.seekTo(currentSecond + 5);
    }

    private void playPrevious() {
        currentVideoIndex = currentVideoIndex == 0 ? playlist.getLength() - 1 : currentVideoIndex - 1;
        changeVideo();
    }

    private void playNext() {
        currentVideoIndex = currentVideoIndex == playlist.getLength() - 1 ? 0 : currentVideoIndex + 1;
        changeVideo();
    }

    private void playOtherVideo() {
        if (shuffle) {
            Random random = new Random();
            int index = -1;
            do {
                index = random.nextInt(playlist.getLength());
            } while (index == currentVideoIndex);
            currentVideoIndex = index;
            changeVideo();
        } else {
            playNext();
        }
    }

    private void changeVideo() {
        YouTubeVideo video = playlist.getVideoAt(currentVideoIndex);
        youTubePlayer.loadVideo(video.id, video.musicStartSeconds);
        title = video.title;
        startForegroundService();
        spe.putInt("currentVideoIndex", currentVideoIndex).commit();
    }
}
