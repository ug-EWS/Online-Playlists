package com.example.onlineplaylists;

import android.animation.Animator;
import android.animation.FloatEvaluator;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class YouTube {
    YouTubePlayerView view;
    YouTubePlayer player;
    YouTubePlayerListener listener;
    IFramePlayerOptions options;
    int currentSecond, playingVideoIndex, playMode, videoDuration, startSeconds;
    MainActivity activity;
    boolean isPlaying, shuffle, areControlsVisible, remotePlaylist, shouldPlayWhenReady, autoPlay;
    ArrayList<Integer> playlistIndexes;
    LinearLayout bottomMainControls, speedControls, controls1, controls2;
    ConstraintLayout videoController;
    ImageView videoReplay, videoPlay, videoForward, videoPrevious, videoNext, setMusicStart, speed, fullscreen, speedBack;
    TextView videoCurrent, videoLength, speedText;
    SeekBar seekBar, speedBar;
    ProgressBar videoProgressBar;
    Timer timer;
    String videoId;

    private final PlayerConstants.PlaybackRate[] speeds = {
            PlayerConstants.PlaybackRate.RATE_0_25,
            PlayerConstants.PlaybackRate.RATE_0_5,
            PlayerConstants.PlaybackRate.RATE_1,
            PlayerConstants.PlaybackRate.RATE_1_5,
            PlayerConstants.PlaybackRate.RATE_2};
    private final String[] speedStrings = {"0.25x", "0.5x", "1.0x", "1.5x", "2.0x"};

    YouTube(MainActivity _activity, ViewGroup _container) {
        options = new IFramePlayerOptions.Builder().controls(0).build();
        remotePlaylist = false;
        initialize(_activity, _container);
    }

    YouTube(MainActivity _activity, ViewGroup _container, String _remoteId) {
        options = new IFramePlayerOptions.Builder().controls(0).listType("playlist").list(_remoteId).build();
        remotePlaylist = true;
        initialize(_activity, _container);
    }

    private void initialize(MainActivity _activity, ViewGroup _container) {
        activity = _activity;
        view = new YouTubePlayerView(activity);
        updateLayoutParams();
        listener = getListener();
        view.setEnableAutomaticInitialization(false);
        view.initialize(listener, options);
        playingVideoIndex = -1;
        prepareUi();
        _container.addView(view);
    }

    private YouTubePlayerListener getListener() {
        return new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer _youTubePlayer) {
                player = _youTubePlayer;
                if (shouldPlayWhenReady) playVideo(videoId, startSeconds, autoPlay);
                if (remotePlaylist) {
                    player.setShuffle(shuffle);
                    play();
                }
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer _youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.ENDED) {
                    if (!remotePlaylist) {
                        if (playMode == 1) activity.playVideo(playingVideoIndex, false);
                        if (playMode == 2 && shuffle) playRandom();
                        if (playMode == 2 && !shuffle) playNext();
                    }
                } else {
                    boolean isBuffering = state == PlayerConstants.PlayerState.BUFFERING;
                    videoProgressBar.setVisibility(isBuffering ? View.VISIBLE : View.GONE);
                    videoPlay.setVisibility(isBuffering ? View.GONE : View.VISIBLE);

                    boolean isPlayingNewValue = state == PlayerConstants.PlayerState.PLAYING;
                    if (isPlayingNewValue != isPlaying) {
                        isPlaying = isPlayingNewValue;
                        videoPlay.setImageResource(isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
                        if (isPlaying) {
                            timer = new Timer();
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    activity.runOnUiThread(() -> {
                                        if (areControlsVisible) setControlsVisibility(false);
                                    });
                                }
                            }, 600);
                        } else if (timer != null) {
                            timer.cancel();
                        }
                    }
                    activity.onStateChange(isPlaying, isBuffering);
                }
            }

            @Override
            public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float second) {
                currentSecond = (int)second;
                seekBar.setProgress(currentSecond);
                super.onCurrentSecond(youTubePlayer, second);
            }

            @Override
            public void onVideoDuration(@NonNull YouTubePlayer youTubePlayer, float duration) {
                videoDuration = (int) duration;
                seekBar.setMax(videoDuration);
                videoLength.setText(OnlinePlaylistsUtils.getHMS(videoDuration));
                super.onVideoDuration(youTubePlayer, duration);
            }

            @Override
            public void onVideoLoadedFraction(@NonNull YouTubePlayer youTubePlayer, float loadedFraction) {
                seekBar.setSecondaryProgress((int) (videoDuration * loadedFraction));
                super.onVideoLoadedFraction(youTubePlayer, loadedFraction);
            }
        };
    }

    public void prepareUi() {
        View mainView = view.inflateCustomPlayerUi(R.layout.video_controller);

        videoController = mainView.findViewById(R.id.mainLay);
        videoController.setOnClickListener(v -> setControlsVisibility(!areControlsVisible));
        controls1 = mainView.findViewById(R.id.controls1);
        controls2 = mainView.findViewById(R.id.controls2);
        videoReplay = mainView.findViewById(R.id.replay);
        videoReplay.setOnClickListener(v -> replay());
        videoPlay = mainView.findViewById(R.id.play);
        videoPlay.setOnClickListener(v -> playPause());
        videoForward = mainView.findViewById(R.id.forward);
        videoForward.setOnClickListener(v -> forward());
        videoCurrent = mainView.findViewById(R.id.current);
        videoLength = mainView.findViewById(R.id.length);
        seekBar = mainView.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                videoCurrent.setText(OnlinePlaylistsUtils.getHMS(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                player.seekTo(seekBar.getProgress());
            }
        });
        setMusicStart = mainView.findViewById(R.id.openInYoutube);
        setMusicStart.setVisibility(remotePlaylist ? View.GONE : View.VISIBLE);
        setMusicStart.setOnClickListener(v -> {
            activity.playingVideo.musicStartSeconds = seekBar.getProgress();
            activity.showMessage(R.string.music_start_point_set);
        });
        fullscreen = mainView.findViewById(R.id.fullscreen);
        fullscreen.setOnClickListener(v -> {
            boolean isFullscreen = activity.toggleFullscreen();
            fullscreen.setImageResource(isFullscreen ? R.drawable.baseline_fullscreen_exit_24 : R.drawable.baseline_fullscreen_24);
        });
        videoPrevious = mainView.findViewById(R.id.previous);
        videoPrevious.setOnClickListener(v -> playPrevious());
        videoNext = mainView.findViewById(R.id.next);
        videoNext.setOnClickListener(v -> playNext());
        speed = mainView.findViewById(R.id.speed);
        speed.setOnClickListener(v -> {
            bottomMainControls.setVisibility(View.GONE);
            speedControls.setVisibility(View.VISIBLE);
        });
        bottomMainControls = mainView.findViewById(R.id.bottomMainControls);
        speedControls = mainView.findViewById(R.id.speedControls);
        speedBack = mainView.findViewById(R.id.speedBack);
        speedBack.setOnClickListener(v -> {
            bottomMainControls.setVisibility(View.VISIBLE);
            speedControls.setVisibility(View.GONE);
        });
        speedText = mainView.findViewById(R.id.speedText);
        speedBar = mainView.findViewById(R.id.speedBar);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                player.setPlaybackRate(speeds[progress]);
                speedText.setText(speedStrings[progress]);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        videoProgressBar = mainView.findViewById(R.id.progressBar2);
    }

    private void setControlsVisibility(boolean _areControlsVisible) {
        areControlsVisible = _areControlsVisible;
        ObjectAnimator animator = ObjectAnimator.ofObject(videoController,"alpha",  new FloatEvaluator(), areControlsVisible ? 1 : 0);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {
                if (areControlsVisible) {
                    controls1.setVisibility(View.VISIBLE);
                    controls2.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                if (!areControlsVisible) {
                    controls1.setVisibility(View.GONE);
                    controls2.setVisibility(View.GONE);
                }
            }

            @Override public void onAnimationCancel(@NonNull Animator animation) {}
            @Override public void onAnimationRepeat(@NonNull Animator animation) {}
        });
        animator.start();
    }

    public void playVideo(String _videoId, int _startSeconds, boolean _autoPlay) {
        if (player == null) {
            shouldPlayWhenReady = true;
            videoId = _videoId;
            startSeconds = _startSeconds;
            autoPlay = _autoPlay;
        } else {
            if (_autoPlay) player.loadVideo(_videoId, _startSeconds);
            else  player.cueVideo(_videoId, _startSeconds);
        }
    }

    public void playPause() {
        if (isPlaying) player.pause(); else player.play();
    }

    public void pause() {
        player.pause();
    }

    public void play() {
        player.play();
    }

    public void replay() {
        player.seekTo(currentSecond - 5);
    }

    public void forward() {
        player.seekTo(currentSecond + 5);
    }

    public void playPrevious() {
        if (remotePlaylist) player.previousVideo();
        else {
            int index = playingVideoIndex == 0 ? activity.currentPlaylist.getLength() - 1 : playingVideoIndex - 1;
            activity.playVideo(index, false);
        }
    }

    public void playNext() {
        if (remotePlaylist) player.nextVideo();
        else {
            int index = playingVideoIndex == activity.currentPlaylist.getLength() - 1 ? 0 : playingVideoIndex + 1;
            activity.playVideo(index, false);
        }
    }

    public void playRandom() {
        if (playlistIndexes.isEmpty()) for (int i = 0; i < activity.currentPlaylist.getLength(); i++) playlistIndexes.add(i);
        int index = playlistIndexes.get(new Random().nextInt(playlistIndexes.size()));
        activity.playVideo(index, false);
    }

    public void setShuffle(boolean _shuffle) {
        shuffle = _shuffle;
        if (remotePlaylist) player.setShuffle(shuffle);
    }

    public void updateLayoutParams() {
        view.setLayoutParams(activity.youTubeLayoutParams);
    }
}
