package com.example.onlineplaylists;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    YouTubePlayer youTubePlayer;

    private CoordinatorLayout coordinatorLayout;
    private YouTubePlayerView youTubePlayerView;
    private LinearLayout layout, list, musicController, bottomMainControls, speedControls;
    private ConstraintLayout videoController;
    private ImageView icon, options, settings, selectAllButton, removeButton, addToPlaylistButton,
            replayButton, playButton, forwardButton, videoReplay, videoPlay, videoForward,
            openInYouTube, speed, fullscreen, speedBack, videoPrevious, videoNext;
    private TextView musicTitle, videoCurrent, videoLength, speedText, noPlaylistsText, noVideosText;
    private FloatingActionButton addButton;
    private SeekBar seekBar, speedBar;
    private RecyclerView listOfPlaylistsRecycler;
    private PopupMenu settingsPopupMenu, listOfPlaylistsPopupMenu;
    TextView titleText;
    RecyclerView playlistRecycler;

    ListOfPlaylistsAdapter listOfPlaylistsAdapter;
    PlaylistAdapter playlistAdapter;
    ItemTouchHelper listOfPlaylistsItemTouchHelper, playlistItemTouchHelper;

    ListOfPlaylists listOfPlaylists;
    Playlist currentPlaylist, playingPlaylist;
    YouTubeVideo playingVideo;

    int currentPlaylistIndex = -1,
            playingPlaylistIndex = -1,
            playingVideoIndex = -1,
            playMode = 0,
            currentSecond = 0,
            videoDuration = 0,
            autoShutDown = 0;

    private PlaylistDialog playlistDialog;
    private VideoDialog videoDialog;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    boolean viewMode, isPlaying, showThumbnails, listSortMode, selectionMode;
    private boolean isPortrait, isFullscreen, shuffle, musicMode, showController, serviceRunning,
            areControlsVisible, musicControlsMode;

    private ArrayList<Integer> playlistIndexes;
    ArrayList<Integer> selectedItems;

    private final Context context = MainActivity.this;

    private Timer timer;
    private Uri uri;
    Vibrator vibrator;

    private PlayerConstants.PlaybackRate[] speeds = {
            PlayerConstants.PlaybackRate.RATE_0_25,
            PlayerConstants.PlaybackRate.RATE_0_5,
            PlayerConstants.PlaybackRate.RATE_1,
            PlayerConstants.PlaybackRate.RATE_1_5,
            PlayerConstants.PlaybackRate.RATE_2};
    private String[] speedStrings = {"0.25x", "0.5x", "1.0x", "1.5x", "2.0x"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences("OnlinePlaylists", MODE_PRIVATE);
        spe = sp.edit();
        listOfPlaylists = new ListOfPlaylists().fromJson(sp.getString("playlists", "[]"));
        playMode = sp.getInt("playMode", 2);
        shuffle = sp.getBoolean("shuffle", false);
        musicMode = sp.getBoolean("musicMode", false);
        autoShutDown = sp.getInt("autoShutDown", 0);
        musicControlsMode = sp.getBoolean("musicControlsMode", true);

        playlistDialog = new PlaylistDialog(this, -1);
        videoDialog = new VideoDialog(this);

        playlistIndexes = new ArrayList<>();
        selectedItems = new ArrayList<>();

        initializeUi();
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                back();
            }
        };
        getOnBackPressedDispatcher().addCallback(onBackPressedCallback);
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        areControlsVisible = true;
        setMusicControlsMode(musicControlsMode);
        setOrientation(true);
        setFullscreen(false);
        setControllerVisibility(false);
    }

    private void back() {
        if (isFullscreen) {
            setFullscreen(false);
        } else if (listSortMode) {
            setListSortMode(false);
        } else if (selectionMode) {
            setSelectionMode(false);
        } else if (viewMode) {
            setViewMode(false);
        } else {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (Objects.equals(intent.getAction(), Intent.ACTION_VIEW) && intent.getData() != null) {
                String[] permissions = {READ_EXTERNAL_STORAGE};
                requestPermissions(permissions, 101);
        }
        checkBatteryOptimizationSettings();
    }

    private void checkBatteryOptimizationSettings() {
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            context.startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults[0] == PERMISSION_DENIED) {
                showMessage("İzin olmadan oynatma listeleri içe aktarılamaz.");
            } else {
                uri = getIntent().getData();
                importPlaylist();
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        setOrientation(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        serviceRunning = sp.getBoolean("serviceRunning", false);
        if (serviceRunning && youTubePlayer != null) {
            continuePlayback();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        refreshDatabase();
        if (playingVideoIndex != -1 && isPlaying) {
            youTubePlayer.pause();
            startPlaybackService();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        youTubePlayerView.release();
    }

    private void startPlaybackService() {
        Intent intent = new Intent(MainActivity.this, PlaybackService.class);
        intent.setAction("com.opl.ACTION_START_SERVICE");
        intent.putExtra("playlist", playingPlaylist.getJson());
        spe.putInt("currentPlaylistIndex", playingPlaylistIndex)
                .putInt("currentVideoIndex", playingVideoIndex)
                .putInt("currentSecond", currentSecond)
                .commit();
        startService(intent);
    }

    private void stopPlaybackService() {
        stopService(new Intent(MainActivity.this, PlaybackService.class));
    }

    private void continuePlayback() {
        stopPlaybackService();
        int cpi = sp.getInt("currentPlaylistIndex", 0);
        int cvi = sp.getInt("currentVideoIndex", 0);
        int cs = sp.getInt("currentSecond", 0);
        openPlaylist(cpi, cvi);
        playVideo(cvi, cs, true);
        spe.putBoolean("serviceRunning", false);
    }

    private void initializeUi() {
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        youTubePlayerView = findViewById(R.id.youTubePlayerView);
        layout = findViewById(R.id.layoutMain);
        list = findViewById(R.id.list);
        icon = findViewById(R.id.icon);
        titleText = findViewById(R.id.titleText);
        addButton = findViewById(R.id.addButton);
        options = findViewById(R.id.options);
        settings = findViewById(R.id.settings);
        selectAllButton = findViewById(R.id.selectAll);
        removeButton = findViewById(R.id.remove);
        addToPlaylistButton = findViewById(R.id.addToPlaylist);
        listOfPlaylistsRecycler = findViewById(R.id.listOfPlaylistsRecycler);
        listOfPlaylistsRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listOfPlaylistsAdapter = new ListOfPlaylistsAdapter(this);
        listOfPlaylistsRecycler.setAdapter(listOfPlaylistsAdapter);
        listOfPlaylistsItemTouchHelper = new ItemTouchHelper(new ItemMoveCallback(listOfPlaylistsAdapter));
        listOfPlaylistsItemTouchHelper.attachToRecyclerView(listOfPlaylistsRecycler);
        playlistRecycler = findViewById(R.id.playlistRecycler);
        playlistRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));

        AbstractYouTubePlayerListener listener = new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer _youTubePlayer) {
                youTubePlayer = _youTubePlayer;
                if (serviceRunning) continuePlayback();
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer _youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.ENDED) {
                    if (playMode == 1) playVideo(playingVideoIndex, false);
                    if (playMode == 2 && shuffle) playRandom();
                    if (playMode == 2 && !shuffle) playNext();
                } else {
                    boolean isPlayingNewValue = state == PlayerConstants.PlayerState.PLAYING;
                    if (isPlayingNewValue != isPlaying) {
                        isPlaying = isPlayingNewValue;
                        playButton.setImageResource(isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
                        videoPlay.setImageResource(isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
                        //updateLayout();
                        if (isPlaying) {
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(() -> {
                                    if (areControlsVisible) videoController.performClick();
                                });
                            }
                        }, 600);
                        } else if (timer != null) {
                            timer.cancel();
                        }
                    }
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

        youTubePlayerView.setEnableAutomaticInitialization(false);
        IFramePlayerOptions iFramePlayerOptions = new IFramePlayerOptions.Builder().controls(0).build();
        youTubePlayerView.initialize(listener, iFramePlayerOptions);
        View mainView = youTubePlayerView.inflateCustomPlayerUi(R.layout.video_controller);

        addButton.setOnClickListener(view -> {
            if (viewMode) videoDialog.show(); else playlistDialog.show();
        });

        icon.setOnClickListener(view -> back());

        settingsPopupMenu = getSettingsPopupMenu();
        settings.setOnClickListener(view -> settingsPopupMenu.show());

        listOfPlaylistsPopupMenu = getListOfPlaylistsPopupMenu();
        options.setOnClickListener(v ->
                (viewMode ? getPlaylistPopupMenu(options, true, currentPlaylistIndex) : listOfPlaylistsPopupMenu).show());

        musicController = findViewById(R.id.musicController);
        musicTitle = findViewById(R.id.musicTitle);
        replayButton = findViewById(R.id.replayButton);
        replayButton.setOnClickListener(v -> {if (musicControlsMode) controllerReplay(); else playPrevious();});
        playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener(v -> controllerPlayPause());
        forwardButton = findViewById(R.id.forwardButton);
        forwardButton.setOnClickListener(v -> {if (musicControlsMode) controllerForward(); else playNext();});
        videoController = mainView.findViewById(R.id.mainLay);
        videoController.setOnClickListener(v -> {
            areControlsVisible = !areControlsVisible;
            videoController.animate().alpha(areControlsVisible ? 1 : 0);

            videoReplay.setClickable(areControlsVisible);
            videoPlay.setClickable(areControlsVisible);
            videoForward.setClickable(areControlsVisible);
            openInYouTube.setClickable(areControlsVisible);
            fullscreen.setClickable(areControlsVisible);
            speed.setClickable(areControlsVisible);
            speedBack.setClickable(areControlsVisible);
            videoPrevious.setClickable(areControlsVisible);
            videoNext.setClickable(areControlsVisible);

            seekBar.setEnabled(areControlsVisible);
            speedBar.setEnabled(areControlsVisible);
        });
        videoReplay = mainView.findViewById(R.id.replay);
        videoReplay.setOnClickListener(v -> controllerReplay());
        videoPlay = mainView.findViewById(R.id.play);
        videoPlay.setOnClickListener(v -> controllerPlayPause());
        videoForward = mainView.findViewById(R.id.forward);
        videoForward.setOnClickListener(v -> controllerForward());
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
                youTubePlayer.seekTo(seekBar.getProgress());
            }
        });
        openInYouTube = mainView.findViewById(R.id.openInYoutube);
        openInYouTube.setOnClickListener(v -> {
            playingVideo.musicStartSeconds = currentSecond;
            showMessage(getString(R.string.music_start_point_set));
        });
        fullscreen = mainView.findViewById(R.id.fullscreen);
        fullscreen.setOnClickListener(v -> setFullscreen(!isFullscreen));
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
                youTubePlayer.setPlaybackRate(speeds[progress]);
                speedText.setText(speedStrings[progress]);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        noPlaylistsText = findViewById(R.id.noPlaylists);
        noVideosText = findViewById(R.id.noVideos);

        selectAllButton.setOnClickListener(v -> selectAllItems());
        removeButton.setOnClickListener(v -> removeItems());
        addToPlaylistButton.setOnClickListener(v -> addItemsToPlaylist());

        setViewMode(false);
    }

    @NonNull PopupMenu getListOfPlaylistsPopupMenu() {
        PopupMenu menu = new PopupMenu(context, options);
        menu.inflate(R.menu.list_of_playlists_options);
        menu.getMenu().getItem(0).setEnabled(!listOfPlaylists.isEmpty());
        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.sort) {
                setListSortMode(true);
                return true;
            }
            return false;
        });
        return menu;
    }

    @NonNull PopupMenu getPlaylistPopupMenu(View anchor, boolean current, int index) {
        PopupMenu menu = new PopupMenu(context, anchor);
        Playlist forPlaylist = listOfPlaylists.getPlaylistAt(index);
        menu.inflate(R.menu.playlist_options);
        menu.getMenu().getItem(0).setVisible(!current);
        menu.getMenu().getItem(1).setVisible(!current);
        menu.getMenu().getItem(3).setVisible(current);
        if (current) menu.getMenu().getItem(3).setEnabled(!currentPlaylist.isEmpty());
        menu.setOnMenuItemClickListener(item -> {
            int itemIndex = item.getItemId();
            if (itemIndex == R.id.addToTop) {
                playlistDialog.show(index);
                return true;
            }
            if (itemIndex == R.id.addToBottom) {
                playlistDialog.show(index + 1);
                return true;
            }
            if (itemIndex == R.id.sort) {
                setListSortMode(true);
                return true;
            }
            if (itemIndex == R.id.edit) {
                new PlaylistDialog(this, index).show();
                return true;
            }
            if (itemIndex == R.id.share) {
                sharePlaylist(forPlaylist);
                return true;
            }
            if (itemIndex == R.id.delete) {
                removePlaylist(index);
                updateNoItemsView();
                return true;
            }
            return false;
        });
        return menu;
    }

    void removePlaylist(int index) {
        AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this, R.style.Theme_OnlinePlaylistsDialogDark);
        b.setTitle(listOfPlaylists.getPlaylistAt(index).title);
        b.setMessage(getString(R.string.delete_playlist_alert));
        b.setPositiveButton(getString(R.string.dialog_button_delete) , ((dialog, which) -> {
            listOfPlaylists.removePlaylist(index);
            listOfPlaylistsAdapter.removeItem(index);
            updateNoItemsView();
        }));
        b.setNegativeButton(getString(R.string.dialog_button_no), ((dialog, which) -> {
            listOfPlaylistsAdapter.notifyItemChanged(index);
        }));
        b.create().show();
    }

    @NonNull PopupMenu getVideoPopupMenu(View anchor, int index) {
        PopupMenu menu = new PopupMenu(context, anchor);
        YouTubeVideo forVideo = currentPlaylist.getVideoAt(index);
        menu.inflate(R.menu.video_options);
        menu.setOnMenuItemClickListener(item -> {
            int itemIndex = item.getItemId();
            if (itemIndex == R.id.addToTop) {
                videoDialog.show(index);
                return true;
            }
            if (itemIndex == R.id.addToBottom) {
                videoDialog.show(index + 1);
                return true;
            }
            if (itemIndex == R.id.openInYouTube) {
                openVideoInYoutube(forVideo);
                return true;
            }
            if (itemIndex == R.id.delete) {
                removeVideo(index);
                return true;
            }
            if (itemIndex == R.id.addToPlaylist) {
                new ManagePlaylistsDialog(this, index).show();
                return true;
            }
            return false;
        });
        return menu;
    }

    void removeVideo(int index) {
        AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this, R.style.Theme_OnlinePlaylistsDialogDark);
        b.setTitle(currentPlaylist.getVideoAt(index).title);
        b.setMessage(getString(R.string.delete_video_alert));
        b.setPositiveButton(getString(R.string.dialog_button_delete), ((dialog, which) -> {
            currentPlaylist.removeVideo(index);
            playlistAdapter.removeItem(index);
            updateNoItemsView();
        }));
        b.setNegativeButton(getString(R.string.dialog_button_no), ((dialog, which) -> {
            playlistAdapter.notifyItemChanged(index);
        }));
        b.create().show();
    }

    private @NonNull PopupMenu getSettingsPopupMenu() {
        PopupMenu popupMenu = new PopupMenu(context, settings, Gravity.TOP);
        popupMenu.inflate(R.menu.options);
        Menu menu = popupMenu.getMenu();
        if (playMode == 0) menu.findItem(R.id.doNothing).setChecked(true);
        if (playMode == 1) menu.findItem(R.id.replayButton).setChecked(true);
        if (playMode == 2) menu.findItem(R.id.playNextVideo).setChecked(true);
        if (autoShutDown == 0) menu.findItem(R.id.autoDisabled).setChecked(true);
        if (autoShutDown == 1) menu.findItem(R.id.auto10Min).setChecked(true);
        if (autoShutDown == 2) menu.findItem(R.id.auto30Min).setChecked(true);
        if (autoShutDown == 3) menu.findItem(R.id.auto1Hour).setChecked(true);
        if (musicControlsMode) menu.findItem(R.id.rewindForward).setChecked(true);
        else menu.findItem(R.id.prevNext).setChecked(true);
        menu.findItem(R.id.shuffleMode).setChecked(shuffle);
        menu.findItem(R.id.musicMode).setChecked(musicMode);
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemIndex = item.getItemId();
            if (itemIndex == R.id.doNothing) {
                playMode = 0;
                item.setChecked(true);
                return false;
            }
            if (itemIndex == R.id.replayButton) {
                playMode = 1;
                item.setChecked(true);
                return false;
            }
            if (itemIndex == R.id.playNextVideo) {
                playMode = 2;
                item.setChecked(true);
                return false;
            }
            if (itemIndex == R.id.shuffleMode) {
                shuffle = !shuffle;
                item.setChecked(shuffle);
                return false;
            }
            if (itemIndex == R.id.musicMode) {
                setMusicMode(!musicMode);
                item.setChecked(musicMode);
                musicTitle.setSelected(musicMode);
                return false;
            }
            if (itemIndex == R.id.autoDisabled) {
                autoShutDown = 0;
                item.setChecked(true);
                return false;
            }
            if (itemIndex == R.id.auto10Min) {
                autoShutDown = 1;
                item.setChecked(true);
                return false;
            }
            if (itemIndex == R.id.auto30Min) {
                autoShutDown = 2;
                item.setChecked(true);
                return false;
            }
            if (itemIndex == R.id.auto1Hour) {
                autoShutDown = 3;
                item.setChecked(true);
                return false;
            }

            if (itemIndex == R.id.rewindForward) {
                setMusicControlsMode(true);
                item.setChecked(true);
                return false;
            }

            if (itemIndex == R.id.prevNext) {
                setMusicControlsMode(false);
                item.setChecked(true);
                return false;
            }

            if (itemIndex == R.id.about) {
                return true;
            }
            return true;
        });
        return popupMenu;
    }

    private void refreshDatabase() {
        spe.putString("playlists", listOfPlaylists.getJson())
                .putInt("playMode", playMode)
                .putBoolean("shuffle", shuffle)
                .putBoolean("musicMode", musicMode)
                .putInt("autoShutDown", autoShutDown)
                .putBoolean("musicControlsMode", musicControlsMode)
                .apply();
    }

    private void importPlaylist() {
        try {
            listOfPlaylists.addPlaylist(new Playlist().fromJson(OnlinePlaylistsUtils.readFile(this, uri)));
            showMessage(getString(R.string.import_success));
            listOfPlaylistsRecycler.scrollToPosition(0);
            listOfPlaylistsAdapter.notifyItemInserted(0);
            updateNoItemsView();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage(getString(R.string.import_fail));
        }
    }

    private void sharePlaylist(Playlist playlist) {
        String folderPath = Objects.requireNonNull(getExternalFilesDir(null)).getAbsolutePath();
        folderPath = folderPath.concat("/Exports");
        File folder = new File(folderPath);
        if (!folder.exists()) folder.mkdirs();

        String content = playlist.getJson();
        String fileName = playlist.title.replace("/", "_");
        String filePath = folderPath.concat("/").concat(fileName).concat(".opl");
        OnlinePlaylistsUtils.writeFile(filePath, content);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath));
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    void openPlaylist(int index) {
        openPlaylist(index, 0);
    }

    private void openPlaylist(int index, int scroll) {
        currentPlaylistIndex = index;
        currentPlaylist = listOfPlaylists.getPlaylistAt(index);
        setViewMode(true);
        playlistRecycler.scrollToPosition(scroll);
    }

    void playVideo(int index, boolean switchPlaylist) {
        playVideo(index, musicMode ? currentPlaylist.getVideoAt(index).musicStartSeconds : 0, switchPlaylist);
    }

    private void playVideo(int index, int startSecond, boolean switchPlaylist) {
        if (youTubePlayer == null) {
            showMessage(getString(R.string.player_not_ready));
        } else {
            if (!switchPlaylist && playingPlaylistIndex == currentPlaylistIndex) switchPlaylist = true;

            int oldPosition = playingVideoIndex;

            playlistIndexes.remove((Integer) index);

            if (switchPlaylist) {
                playingPlaylistIndex = currentPlaylistIndex;
                playingPlaylist = currentPlaylist;
            }

            playingVideoIndex = index;
            playingVideo = playingPlaylist.getVideoAt(index);
            musicTitle.setText(playingVideo.title);
            setControllerVisibility(true);

            if (switchPlaylist) {
                if (oldPosition != -1) playlistRecycler.getAdapter().notifyItemChanged(oldPosition);
                playlistRecycler.getAdapter().notifyItemChanged(index);
            }

            youTubePlayer.loadVideo(playingVideo.id, startSecond);
        }
    }

    private void openVideoInYoutube(YouTubeVideo video){
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getVideoUrl()));
        startActivity(i);
    }

    private void controllerPlayPause() {
        if (isPlaying) youTubePlayer.pause(); else youTubePlayer.play();
    }

    private void controllerReplay() {
        youTubePlayer.seekTo(currentSecond - 5);
    }

    private void controllerForward() {
        youTubePlayer.seekTo(currentSecond + 5);
    }

    private void playPrevious() {
        int index = playingVideoIndex == 0 ? currentPlaylist.getLength()-1 : playingVideoIndex - 1;
        playVideo(index, false);
    }

    private void playNext() {
        int index = playingVideoIndex == currentPlaylist.getLength()-1 ? 0 : playingVideoIndex + 1;
        playVideo(index, false);
    }

    private void playRandom() {
        if (playlistIndexes.isEmpty()) for (int i = 0; i < currentPlaylist.getLength(); i++) playlistIndexes.add(i);
        int index = playlistIndexes.get(new Random().nextInt(playlistIndexes.size()));
        playVideo(index, false);
    }

    void closePlayer() {
        playingPlaylistIndex = -1;
        playingVideoIndex = -1;
        youTubePlayer.pause();
        setControllerVisibility(false);
    }

    void showMessage(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private void setViewMode(boolean mode) {
        viewMode = mode;

        listOfPlaylistsRecycler.setVisibility(View.GONE);
        noPlaylistsText.setVisibility(View.GONE);
        playlistRecycler.setVisibility(View.GONE);
        noVideosText.setVisibility(View.GONE);

        if (mode) {
            if (playlistAdapter == null) {
                playlistAdapter = new PlaylistAdapter(this);
                playlistRecycler.setAdapter(playlistAdapter);
                playlistItemTouchHelper = new ItemTouchHelper(new ItemMoveCallback(playlistAdapter));
                playlistItemTouchHelper.attachToRecyclerView(playlistRecycler);
            } else {
                playlistAdapter.notifyDataSetChanged();
            }
        } else {
            if (listOfPlaylistsAdapter != null) {
                listOfPlaylistsAdapter.notifyDataSetChanged();
            }
        }
        updateToolbar();
        updateNoItemsView();
    }

    void setSelectionMode(boolean _selectionMode) {
        selectionMode = _selectionMode;
        if (viewMode) playlistAdapter.notifyDataSetChanged();
        else listOfPlaylistsAdapter.notifyDataSetChanged();
        updateToolbar();
    }

    void setListSortMode(boolean _listSortMode) {
        listSortMode = _listSortMode;
        if (viewMode) playlistAdapter.notifyDataSetChanged();
        else listOfPlaylistsAdapter.notifyDataSetChanged();
        updateToolbar();
    }

    void updateToolbar() {
        int selectedItemCount = 0;
        if (selectionMode) selectedItemCount = selectedItems.size();
        titleText.setText(
                listSortMode && viewMode ? "Videoları sırala"
                        : listSortMode ? "Oynatma listelerini sırala"
                        : selectionMode ? String.valueOf(selectedItemCount).concat(" öge seçildi")
                        : viewMode ? currentPlaylist.title
                        : getString(R.string.app_name));
        icon.setImageResource(selectionMode ? R.drawable.baseline_close_24
                : viewMode || listSortMode ? R.drawable.baseline_arrow_back_24
                : R.drawable.baseline_smart_display_24);
        icon.setClickable(viewMode || selectionMode || listSortMode);
        options.setVisibility(selectionMode || listSortMode ? View.GONE : View.VISIBLE);
        settings.setVisibility(selectionMode || listSortMode ? View.GONE : View.VISIBLE);
        selectAllButton.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        removeButton.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        addToPlaylistButton.setVisibility(selectionMode && viewMode ? View.VISIBLE : View.GONE);
        addButton.setVisibility(selectionMode || listSortMode ? View.GONE : View.VISIBLE);
    }

    void updateNoItemsView() {
        if (viewMode) {
            boolean noVideos = currentPlaylist.isEmpty();
            playlistRecycler.setVisibility(noVideos ? View.GONE : View.VISIBLE);
            noVideosText.setVisibility(noVideos ? View.VISIBLE : View.GONE);
        } else {
            boolean noPlaylists = listOfPlaylists.isEmpty();
            listOfPlaylistsRecycler.setVisibility(noPlaylists ? View.GONE : View.VISIBLE);
            noPlaylistsText.setVisibility(noPlaylists ? View.VISIBLE : View.GONE);
        }
    }

    private void selectAllItems() {
        selectedItems.clear();
        int length;
        if (viewMode) length = currentPlaylist.getLength(); else length = listOfPlaylists.getLength();
        for (int i = 0; i < length; i++) selectedItems.add(i);
        if (viewMode) playlistAdapter.notifyDataSetChanged();
        else listOfPlaylistsAdapter.notifyDataSetChanged();
        updateToolbar();
    }

    private void removeItems() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_OnlinePlaylistsDialogDark);
        builder.setTitle("Birden çok öge sil");
        builder.setMessage(String.valueOf(selectedItems.size()).concat(" öge silinsin mi?"));
        builder.setPositiveButton(R.string.dialog_button_delete, (dialog, which) -> {
            if (viewMode) {
                currentPlaylist.removeVideos(selectedItems);
                if (selectedItems.contains(playingVideoIndex)) closePlayer();
            }
            else {
                listOfPlaylists.removePlaylists(selectedItems);
                if (selectedItems.contains(playingPlaylistIndex)) closePlayer();
            }
            selectedItems.clear();
            setSelectionMode(false);
            updateNoItemsView();
        });
        builder.setNegativeButton(R.string.dialog_button_no, null);
        builder.create().show();
    }

    private void addItemsToPlaylist() {
        new ManagePlaylistsDialog(this, selectedItems).show();
    }

    private void setMusicControlsMode(boolean _musicControlsMode) {
        musicControlsMode = _musicControlsMode;
        replayButton.setImageResource(musicControlsMode ? R.drawable.baseline_replay_5_24 : R.drawable.baseline_skip_previous_24);
        forwardButton.setImageResource(musicControlsMode ? R.drawable.baseline_forward_5_24 : R.drawable.baseline_skip_next_24);
    }

    private void setFullscreen(boolean _fullscreen) {
        isFullscreen = _fullscreen;
        fullscreen.setImageResource(isFullscreen ? R.drawable.baseline_fullscreen_exit_24 : R.drawable.baseline_fullscreen_24);
        setRequestedOrientation(isFullscreen ? ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_USER);
        list.setVisibility(isFullscreen ? View.GONE : View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            coordinatorLayout.setFitsSystemWindows(!_fullscreen);
            getWindow().getInsetsController().setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            if (isFullscreen) {
                addButton.hide();
                getWindow().getInsetsController().hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());
            } else {
                addButton.show();
                getWindow().getInsetsController().show(WindowInsets.Type.systemBars()|WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(isFullscreen ? View.SYSTEM_UI_FLAG_FULLSCREEN : View.SYSTEM_UI_FLAG_VISIBLE);
            if (isFullscreen) {
                addButton.hide();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                addButton.show();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
        if (!isPortrait) {
            OnlinePlaylistsUtils.setDimensions(context, youTubePlayerView, isFullscreen ? MATCH_PARENT : WRAP_CONTENT, MATCH_PARENT, 1);
        }
    }

    private void setOrientation(boolean _isPortrait) {
        isPortrait = _isPortrait;
        if (isPortrait) {
            layout.setOrientation(LinearLayout.VERTICAL);
            OnlinePlaylistsUtils.setDimensions(context, youTubePlayerView, MATCH_PARENT, WRAP_CONTENT, 0);
            OnlinePlaylistsUtils.setDimensions(context, list, MATCH_PARENT, WRAP_CONTENT, 1);
        } else {
            layout.setOrientation(musicMode ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            OnlinePlaylistsUtils.setDimensions(context, youTubePlayerView, isFullscreen ? MATCH_PARENT : WRAP_CONTENT, MATCH_PARENT, 1);
            OnlinePlaylistsUtils.setDimensions(context, list, musicMode ? MATCH_PARENT : 300, MATCH_PARENT, 0);
        }
        setShowThumbnails();
    }

    private void setControllerVisibility(boolean _showController) {
        showController = _showController;
        updateController();
    }

    private void setMusicMode(boolean _musicMode) {
        musicMode = _musicMode;
        if (!isPortrait) layout.setOrientation(musicMode ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        updateController();
    }

    private void updateController() {
        musicController.setVisibility(showController && musicMode ? View.VISIBLE : View.GONE);
        youTubePlayerView.setVisibility(showController && !musicMode ? View.VISIBLE : View.GONE);
        if (!isPortrait) OnlinePlaylistsUtils.setDimensions(context, list, musicMode ? MATCH_PARENT : 300, MATCH_PARENT, 0);
        setShowThumbnails();
    }

    private void setShowThumbnails() {
        boolean showThumbnailsNewValue = !(!isPortrait && showController && !musicMode);
        if (showThumbnails != showThumbnailsNewValue) {
            showThumbnails = showThumbnailsNewValue;
            if (viewMode) playlistRecycler.getAdapter().notifyDataSetChanged();
        }
    }
}