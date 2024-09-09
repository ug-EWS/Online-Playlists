package com.example.onlineplaylists;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerUtils;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private YouTubePlayer youTubePlayer;

    private YouTubePlayerView youTubePlayerView;
    private LinearLayout layout;
    private LinearLayout list;
    private ImageView icon;
    private TextView titleText;
    private FloatingActionButton addButton;
    private ImageView options;
    private ImageView settings;
    private RecyclerView recycler;
    private PopupMenu settingsPopupMenu;
    private LinearLayout musicController;
    private TextView musicTitle;
    private ImageView replayButton;
    private ImageView playButton;
    private ImageView forwardButton;
    private ConstraintLayout videoController;
    private LinearLayout controls1;
    private LinearLayout controls2;
    private ImageView videoReplay;
    private ImageView videoPlay;
    private ImageView videoForward;
    private TextView videoCurrent;
    private TextView videoLength;
    private SeekBar seekBar;

    private ListOfPlaylists listOfPlaylists;

    private Playlist currentPlaylist, playingPlaylist;
    private YouTubeVideo playingVideo;

    private int currentPlaylistIndex = -1,
            playingPlaylistIndex = -1,
            playingVideoIndex = -1,
            cutPlaylist = -1,
            cutVideo = -1,
            playMode = 0,
            currentSecond = 0,
            videoDuration = 0;

    private PlaylistDialog playlistDialog;
    private VideoDialog videoDialog;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private boolean viewMode, isPortrait, isPlaying, cut, shuffle, musicMode, showController, serviceRunning, areControlsVisible;

    private final Context context = MainActivity.this;
    private PlaybackService playbackService;

    private Timer timer;

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

        playlistDialog = new PlaylistDialog(-1);
        videoDialog = new VideoDialog();
        initializeUi();
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isPortrait && isPlaying) youTubePlayer.pause(); else if(viewMode) setViewMode(false); else finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(onBackPressedCallback);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        isPortrait = true;
        updateLayout();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (Objects.equals(intent.getAction(), Intent.ACTION_VIEW) && intent.getData() != null)
            importPlaylist(getIntent().getData());
        checkBatteryOptimizationSettings();
    }

    private void checkBatteryOptimizationSettings() {
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + "YOUR_PACKAGE_NAME"));
                context.startActivity(intent);

            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        updateLayout();
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
        intent.putExtra("playlist", currentPlaylist.getJson());
        spe.putInt("currentPlaylistIndex", playingPlaylistIndex);
        spe.putInt("currentVideoIndex", playingVideoIndex);
        spe.putInt("currentSecond", currentSecond);
        spe.commit();
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
        openPlaylist(cpi);
        playVideo(cvi, cs);
    }

    private void initializeUi() {
        youTubePlayerView = findViewById(R.id.youTubePlayerView);
        layout = findViewById(R.id.layoutMain);
        list = findViewById(R.id.list);
        icon = findViewById(R.id.icon);
        titleText = findViewById(R.id.titleText);
        addButton = findViewById(R.id.addButton);
        options = findViewById(R.id.options);
        settings = findViewById(R.id.settings);
        recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        setViewMode(false);

        AbstractYouTubePlayerListener listener = new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer _youTubePlayer) {
                youTubePlayer = _youTubePlayer;
                if(serviceRunning) continuePlayback();
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer _youTubePlayer, @NonNull PlayerConstants.PlayerState state){
                if (state == PlayerConstants.PlayerState.ENDED) {
                    if (playMode == 1) playVideo(playingVideoIndex, 0);
                    if (playMode == 2 && shuffle) playVideo(getRandomVideoIndex(), 0);
                    if (playMode == 2 && !shuffle) playVideo(playingVideoIndex == currentPlaylist.getLength()-1 ? 0 : playingVideoIndex + 1, 0);
                } else {
                    boolean isPlayingNewValue = state == PlayerConstants.PlayerState.PLAYING;
                    if (isPlayingNewValue != isPlaying) {
                        isPlaying = isPlayingNewValue;
                        playButton.setImageResource(isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
                        videoPlay.setImageResource(isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
                        updateLayout();
                        if(isPlaying) {
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (areControlsVisible) videoController.performClick();
                                    }
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
                seekBar.setSecondaryProgress((int) (videoDuration / loadedFraction));
                super.onVideoLoadedFraction(youTubePlayer, loadedFraction);
            }
        };

        youTubePlayerView.setEnableAutomaticInitialization(false);
        IFramePlayerOptions options = new IFramePlayerOptions.Builder().controls(0).build();
        youTubePlayerView.initialize(listener, options);
        View mainView = youTubePlayerView.inflateCustomPlayerUi(R.layout.video_controller);

        addButton.setOnClickListener(view -> {
            if (viewMode) videoDialog.show(0); else playlistDialog.show(0);
        });

        icon.setOnClickListener(view -> setViewMode(false));

        settingsPopupMenu = getSettingsPopupMenu();
        settings.setOnClickListener(view -> settingsPopupMenu.show());

        musicController = findViewById(R.id.musicController);
        musicTitle = findViewById(R.id.musicTitle);
        replayButton = findViewById(R.id.replayButton);
        replayButton.setOnClickListener(v -> controllerReplay());
        playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener(v -> controllerPlayPause());
        forwardButton = findViewById(R.id.forwardButton);
        forwardButton.setOnClickListener(v -> controllerForward());
        videoController = mainView.findViewById(R.id.mainLay);
        videoController.setOnClickListener(v -> {
            areControlsVisible = !areControlsVisible;
            videoController.animate().alpha(areControlsVisible ? 1 : 0);
            videoReplay.setClickable(areControlsVisible);
            videoPlay.setClickable(areControlsVisible);
            videoForward.setClickable(areControlsVisible);
            seekBar.setEnabled(areControlsVisible);
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
    }

    private @NonNull PopupMenu getPlaylistPopupMenu(View anchor, boolean current, int index) {
        PopupMenu menu = new PopupMenu(context, anchor);
        menu.inflate(R.menu.playlist_options);
        for (int i = 0; i < 3; i++) menu.getMenu().getItem(i).setVisible(!current);
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
            if (itemIndex == R.id.move) {
                return true;
            }
            if (itemIndex == R.id.paste) {
                if (cutPlaylist != index) {
                    if (cutPlaylist == playingPlaylistIndex && cutVideo == playingVideoIndex && cut){
                        closePlayer();
                    }
                    listOfPlaylists.moveVideo(cutPlaylist, cutVideo, index, cut);
                    cutPlaylist = -1;
                    cutVideo = -1;

                    if (current) {
                        PlaylistAdapter a = (PlaylistAdapter) recycler.getAdapter();
                        a.insertItem(0);
                    }
                }
                return true;
            }
            if (itemIndex == R.id.edit) {
                new PlaylistDialog(index).show();
                return true;
            }
            if (itemIndex == R.id.share) {
                sharePlaylist(listOfPlaylists.getPlaylistAt(index));
                return true;
            }
            if (itemIndex == R.id.delete) {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this, R.style.Theme_OnlinePlaylistsDialogDark);
                b.setTitle(listOfPlaylists.getPlaylistAt(index).title);
                b.setMessage(getString(R.string.delete_playlist_alert));
                b.setPositiveButton(getString(R.string.dialog_button_delete) , ((dialog, which) -> {
                    listOfPlaylists.removePlaylist(index);
                    if(!current) {
                        ListOfPlaylistsAdapter a = (ListOfPlaylistsAdapter) recycler.getAdapter();
                        a.removeItem(index);
                    }
                    dialog.dismiss();
                }));
                b.setNegativeButton(getString(R.string.dialog_button_no), ((dialog, which) -> dialog.dismiss()));
                b.create().show();
                return true;
            }
            return false;
        });
        return menu;
    }

    private @NonNull PopupMenu getVideoPopupMenu(View anchor, int index) {
        PopupMenu menu = new PopupMenu(context, anchor);
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
            if (itemIndex == R.id.move) {
                return true;
            }
            if (itemIndex == R.id.cut) {
                cutPlaylist = currentPlaylistIndex;
                cutVideo = index;
                cut = true;
                showMessage(getString(R.string.cut_to_clipboard));
                return true;
            }
            if (itemIndex == R.id.copy) {
                cutPlaylist = currentPlaylistIndex;
                cutVideo = index;
                cut = false;
                showMessage(getString(R.string.copy_to_clipboard));
                return true;
            }
            if (itemIndex == R.id.openInYouTube) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(currentPlaylist.getVideoAt(index).getVideoUrl()));
                startActivity(i);
                return true;
            }
            if (itemIndex == R.id.delete) {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this, R.style.Theme_OnlinePlaylistsDialogDark);
                b.setTitle(currentPlaylist.getVideoAt(index).title);
                b.setMessage(getString(R.string.delete_video_alert));
                b.setPositiveButton(getString(R.string.dialog_button_delete), ((dialog, which) -> {
                    currentPlaylist.removeVideo(index);
                    PlaylistAdapter a = (PlaylistAdapter)recycler.getAdapter();
                    a.removeItem(index);
                    dialog.dismiss();
                }));
                b.setNegativeButton(getString(R.string.dialog_button_no), ((dialog, which) -> dialog.dismiss()));
                b.create().show();
                return true;
            }
            return false;
        });
        return menu;
    }

    private @NonNull PopupMenu getSettingsPopupMenu() {
        PopupMenu popupMenu = new PopupMenu(context, settings, Gravity.TOP);
        popupMenu.inflate(R.menu.options);
        Menu menu = popupMenu.getMenu();
        if (playMode == 0) menu.findItem(R.id.doNothing).setChecked(true);
        if (playMode == 1) menu.findItem(R.id.replayButton).setChecked(true);
        if (playMode == 2) menu.findItem(R.id.playNextVideo).setChecked(true);
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
                musicMode = !musicMode;
                item.setChecked(musicMode);
                updateLayout();
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
                .apply();
    }

    private void importPlaylist(Uri uri) {
        Log.d("PlaylistImporter", uri.toString());
        try {
            String filePath = uri.getPath();
            if (filePath.startsWith("/document/primary:"))
                filePath = filePath.replace("/document/primary:", "storage/emulated/0/");
            Log.i("filePath", "importPlaylist: ".concat(filePath));
            listOfPlaylists.addPlaylist(new Playlist().fromJson(OnlinePlaylistsUtils.readFile(filePath)));
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Oynatma listesi içe aktarılamadı");
        }
    }

    private void sharePlaylist(Playlist playlist) {
        String folderPath = Objects.requireNonNull(getExternalFilesDir(null)).getAbsolutePath();
        folderPath = folderPath.concat("/Exports");
        File folder = new File(folderPath);
        if (!folder.exists()) folder.mkdirs();

        String content = playlist.getJson();
        String filePath = folderPath.concat("/").concat(playlist.title).concat(".opl");
        OnlinePlaylistsUtils.writeFile(filePath, content);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath));
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void openPlaylist(int index) {
        currentPlaylistIndex = index;
        currentPlaylist = listOfPlaylists.getPlaylistAt(index);
        PopupMenu currentPlaylistOptions = getPlaylistPopupMenu(options, true, currentPlaylistIndex);
        options.setOnClickListener(view -> {
            currentPlaylistOptions.getMenu().getItem(3).setEnabled(cutPlaylist != -1);
            currentPlaylistOptions.show();
        });
        setViewMode(true);
    }

    private void playVideo(int index, int startSecond) {
        if (youTubePlayer == null) {
            showMessage(getString(R.string.player_not_ready));
        } else {
            int oldPosition = playingVideoIndex;
            playingPlaylistIndex = currentPlaylistIndex;
            playingPlaylist = currentPlaylist;
            playingVideoIndex = index;
            playingVideo = currentPlaylist.getVideoAt(index);
            //YouTubePlayerUtils.loadOrCueVideo(youTubePlayer, getLifecycle(), playingVideo.id, startSecond);
            youTubePlayer.loadVideo(playingVideo.id, startSecond);
            musicTitle.setText(playingVideo.title);
            showController = true;
            updateLayout();
            if (oldPosition != -1) recycler.getAdapter().notifyItemChanged(oldPosition);
            recycler.getAdapter().notifyItemChanged(index);
        }
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

    private int getRandomVideoIndex() {
        int i;
        do {
            i = new Random().nextInt(playingPlaylist.getLength());
        } while (i == playingVideoIndex);
        return i;
    }

    private void closePlayer() {
        playingPlaylistIndex = -1;
        playingVideoIndex = -1;
        youTubePlayer.pause();
        showController = false;
        updateLayout();
    }
    class PlaylistDialog {
        AlertDialog.Builder builder;
        AlertDialog dialog;
        View dialogView;
        EditText editText;
        LinearLayout iconSelector;
        Playlist toEdit;
        int whereToAdd;
        int selectedIcon;
        Integer[] icons = {
                R.drawable.baseline_featured_play_list_24,
                R.drawable.baseline_favorite_24,
                R.drawable.baseline_library_music_24,
                R.drawable.baseline_videogame_asset_24,
                R.drawable.baseline_movie_creation_24};

        PlaylistDialog(int _forPlaylist) {

            builder = new AlertDialog.Builder(context, R.style.Theme_OnlinePlaylistsDialogDark);
            dialogView = getLayoutInflater().inflate(R.layout.add_playlist, null);
            editText = dialogView.findViewById(R.id.editText);
            iconSelector = dialogView.findViewById(R.id.iconSelector);
            View.OnClickListener onClickListener = view -> {
                selectedIcon = iconSelector.indexOfChild(view);
                for (int i = 0; i < 5; i++) {
                    iconSelector.getChildAt(i).setBackgroundColor(i == selectedIcon ? getResources().getColor(R.color.soft_red): Color.TRANSPARENT);
                }
            };
            for (int i = 0; i < 5; i++){
                iconSelector.getChildAt(i).setOnClickListener(onClickListener);
            }
            builder.setNegativeButton(getString(R.string.dialog_button_cancel), (dialog, which) -> dialog.dismiss());
            builder.setView(dialogView);
            editText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                    return true;
                }
                return false;
            });
            boolean _newPlaylist = _forPlaylist == -1;
            builder.setTitle(getString(_newPlaylist ? R.string.add_playlist : R.string.edit_playlist));
            if(_newPlaylist) {
                iconSelector.getChildAt(0).setBackgroundColor(getResources().getColor(R.color.soft_red));
                builder.setPositiveButton(getString(R.string.dialog_button_add), (dialog, which) -> {
                    String text = editText.getText().toString();
                    if (text.isEmpty()) text = editText.getHint().toString();
                    listOfPlaylists.addPlaylistTo(new Playlist(text, icons[selectedIcon]), whereToAdd);
                    ListOfPlaylistsAdapter a = (ListOfPlaylistsAdapter) recycler.getAdapter();
                    a.insertItem(whereToAdd);
                    dialog.dismiss();
                });
            } else {
                toEdit = listOfPlaylists.getPlaylistAt(_forPlaylist);
                editText.setText(toEdit.title);
                selectedIcon = Arrays.asList(icons).indexOf(toEdit.icon);
                iconSelector.getChildAt(selectedIcon).setBackgroundColor(getResources().getColor(R.color.soft_red));
                builder.setPositiveButton(getString(R.string.dialog_button_apply), (dialog, which) -> {
                    String text = editText.getText().toString();
                    toEdit.title = text;
                    toEdit.icon = icons[selectedIcon];
                    recycler.getAdapter().notifyItemChanged(_forPlaylist);
                });
            }

            dialog = builder.create();
        }
        public void show() {
            dialog.show();
        }
        public void show(int _whereToAdd) {
            whereToAdd = _whereToAdd;
            dialog.show();
        }
    }

    class VideoDialog {
        AlertDialog.Builder builder;
        AlertDialog dialog;
        String url;
        String title;
        View dialogView;
        EditText editText;
        Button addButton;
        Button cancelButton;
        WebView webView;
        ImageView backButton;
        ImageView refreshButton;
        ImageView forwardButton;
        int whereToAdd;

        @SuppressLint("SetJavaScriptEnabled")
        VideoDialog() {
            builder = new AlertDialog.Builder(context, R.style.Theme_OnlinePlaylistsDialogDark);
            dialog = builder.create();
            dialog.setTitle(getString(R.string.add_video));

            dialogView = getLayoutInflater().inflate(R.layout.add_video, null);
            editText = dialogView.findViewById(R.id.editUrl);
            addButton = dialogView.findViewById(R.id.addVideoButton);
            cancelButton = dialogView.findViewById(R.id.cancelButton);
            webView = dialogView.findViewById(R.id.webView);
            backButton = dialogView.findViewById(R.id.webBackButton);
            refreshButton = dialogView.findViewById(R.id.webRefreshButton);
            forwardButton = dialogView.findViewById(R.id.webForwardButton);

            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new WebViewClient());
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onReceivedTitle(WebView v, String t) {
                    super.onReceivedTitle(v, t);
                    url = webView.getUrl();
                    addButton.setVisibility(url.contains("watch") ? View.VISIBLE : View.GONE);
                    title = t;
                    backButton.setVisibility(webView.canGoBack() ? View.VISIBLE : View.GONE);
                    forwardButton.setVisibility(webView.canGoForward() ? View.VISIBLE : View.GONE);
                }
            });

            addButton.setOnClickListener(view -> {
                String id = YouTubeVideo.getVideoIdFrom(url);
                title = title.replace(" - YouTube","");
                currentPlaylist.addVideoTo(new YouTubeVideo(title, id), whereToAdd);
                PlaylistAdapter a = (PlaylistAdapter) recycler.getAdapter();
                a.insertItem(whereToAdd);
                dialog.dismiss();
            });

            cancelButton.setOnClickListener(view -> dialog.dismiss());

            backButton.setOnClickListener(view -> webView.goBack());

            refreshButton.setOnClickListener(view -> webView.reload());

            forwardButton.setOnClickListener(view -> webView.goForward());

            editText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    String text = editText.getText().toString();
                    if (text.contains("/watch?") && text.indexOf("http") == 0) {
                        webView.loadUrl(text);
                    }
                    else if (text.length() == 11) {
                        webView.loadUrl("https://m.youtube.com/watch?v=".concat(text));
                    }
                    else {
                        webView.loadUrl("https://m.youtube.com/results?search_query=".concat(text));
                    }
                    return true;
                }
                return false;
            });

            dialog.setView(dialogView);

        }
        public void show(int _whereToAdd) {
            whereToAdd = _whereToAdd;
            dialog.show();
            webView.loadUrl("https://www.youtube.com");
        }
    }
    private void showMessage(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private void setViewMode(boolean mode) {
        viewMode = mode;
        titleText.setText(viewMode? currentPlaylist.title : getString(R.string.app_name));
        recycler.setAdapter(viewMode? new PlaylistAdapter() : new ListOfPlaylistsAdapter());
        icon.setImageResource(viewMode ? R.drawable.baseline_arrow_back_24 : R.drawable.baseline_smart_display_24);
        icon.setClickable(viewMode);
        options.setVisibility(mode ? View.VISIBLE : View.GONE);
        settings.setVisibility(mode ? View.GONE : View.VISIBLE);
    }

    private void setMusicMode(boolean _musicMode) {
        musicMode = _musicMode;
        updateLayout();
    }

    private void updateLayout() {
        musicController.setVisibility(showController && musicMode ? View.VISIBLE : View.GONE);
        youTubePlayerView.setVisibility(showController && !musicMode ? View.VISIBLE : View.GONE);
        if (isPortrait || youTubePlayerView.getVisibility() == View.GONE) {
            layout.setOrientation(LinearLayout.VERTICAL);
            OnlinePlaylistsUtils.setDimensions(context, youTubePlayerView, MATCH_PARENT, 240, 0);
            OnlinePlaylistsUtils.setDimensions(context, list, MATCH_PARENT, WRAP_CONTENT, 1);
            list.setVisibility(View.VISIBLE);
            addButton.show();
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } else {
            layout.setOrientation(LinearLayout.HORIZONTAL);
            OnlinePlaylistsUtils.setDimensions(context, youTubePlayerView, isPlaying ? MATCH_PARENT : 480, MATCH_PARENT, 0);
            OnlinePlaylistsUtils.setDimensions(context, list, WRAP_CONTENT, MATCH_PARENT, 1);
            list.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
            if (isPlaying) addButton.hide(); else addButton.show();
            getWindow().getDecorView().setSystemUiVisibility(isPlaying ? View.SYSTEM_UI_FLAG_FULLSCREEN : View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    class PlaylistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = getLayoutInflater();
            View itemView = inf.inflate(R.layout.video_item, parent, false);
            return new RecyclerView.ViewHolder(itemView) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View itemView = holder.itemView;
            int pos = holder.getAdapterPosition();

            ImageView thumbnail = itemView.findViewById(R.id.videoThumbnail);
            TextView title = itemView.findViewById(R.id.videoTitle);
            ImageView options = itemView.findViewById(R.id.videoOptions);

            setItemOnClickListener(itemView, pos);
            YouTubeVideo thisVideo = currentPlaylist.getVideoAt(pos);
            title.setText(thisVideo.title);
            title.setTextColor(currentPlaylistIndex == playingPlaylistIndex && playingVideoIndex == pos ? Color.GREEN : Color.WHITE);
            Glide.with(MainActivity.this).load(thisVideo.getThumbnailUrl()).into(thumbnail);

            PopupMenu popupMenu = getVideoPopupMenu(options, pos);
            options.setOnClickListener(view -> popupMenu.show());
        }

        @Override
        public int getItemCount() {
            return currentPlaylist.getLength();
        }
        public void insertItem(int index) {
            if(playingVideoIndex != -1 && index <= playingVideoIndex && currentPlaylistIndex == playingPlaylistIndex) {
                playingVideoIndex++;
                playingVideo = currentPlaylist.getVideoAt(playingVideoIndex);
            }
            if (cutVideo != -1 && index <= cutVideo && currentPlaylistIndex == cutPlaylist) {
                cutVideo++;
            }
            this.notifyItemInserted(index);
            this.notifyItemRangeChanged(index, listOfPlaylists.getLength()-index);
        }
        public void removeItem(int index) {
            if(playingVideoIndex != -1 && currentPlaylistIndex == playingPlaylistIndex) {
                if(index < playingVideoIndex) {
                    playingVideoIndex--;
                    playingVideo = currentPlaylist.getVideoAt(playingVideoIndex);
                }
                if(index == playingVideoIndex) {
                    closePlayer();
                }
            }
            if(cutVideo != -1 && currentPlaylistIndex == cutPlaylist) {
                if (index < cutVideo) {
                    cutVideo--;
                }
                if (index == cutVideo) {
                    cutPlaylist = -1;
                    cutVideo = -1;
                }
            }
            this.notifyItemRemoved(index);
            this.notifyItemRangeChanged(index, listOfPlaylists.getLength()-index);
        }
        private void setItemOnClickListener(View v, int position) {
            v.setOnClickListener(view -> playVideo(position, 0));
        }
    }

    class ListOfPlaylistsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = getLayoutInflater();
            View itemView = inf.inflate(R.layout.playlist_item, parent, false);
            return new RecyclerView.ViewHolder(itemView) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View itemView = holder.itemView;
            int pos = holder.getAdapterPosition();

            ImageView icon = itemView.findViewById(R.id.playlistIcon);
            TextView title = itemView.findViewById(R.id.playlistTitle);
            ImageView options = itemView.findViewById(R.id.playlistOptions);

            setItemOnClickListener(itemView, pos);

            icon.setImageResource(listOfPlaylists.getPlaylistAt(pos).icon);
            title.setText(listOfPlaylists.getPlaylistAt(pos).title);
            title.setTextColor(playingPlaylistIndex == pos ? Color.GREEN : Color.WHITE);

            PopupMenu popupMenu = getPlaylistPopupMenu(options, false, pos);

            options.setOnClickListener(view -> {
                popupMenu.getMenu().getItem(3).setEnabled(cutPlaylist != -1);
                popupMenu.show();});
        }

        @Override
        public int getItemCount() {
            return listOfPlaylists.getLength();
        }
        public void insertItem(int index) {
            if(playingPlaylistIndex != -1 && index <= playingPlaylistIndex) {
                playingPlaylistIndex++;
                playingPlaylist = listOfPlaylists.getPlaylistAt(playingPlaylistIndex);
            }
            if (cutPlaylist != -1 && index <= cutPlaylist) {
                cutPlaylist++;
            }
            this.notifyItemInserted(index);
            this.notifyItemRangeChanged(index, listOfPlaylists.getLength()-index);
        }
        public void removeItem(int index) {
            if(playingPlaylistIndex != -1) {
                if(index < playingPlaylistIndex) {
                    playingPlaylistIndex--;
                    playingPlaylist = listOfPlaylists.getPlaylistAt(playingPlaylistIndex);
                }
                if(index == playingPlaylistIndex) {
                    closePlayer();
                }
            }
            if(cutPlaylist != -1) {
                if (index < cutPlaylist) {
                    cutPlaylist--;
                }
                if (index == cutPlaylist) {
                    cutPlaylist = -1;
                    cutVideo = -1;
                }
            }
            this.notifyItemRemoved(index);
            this.notifyItemRangeChanged(index, listOfPlaylists.getLength()-index);
        }
        private void setItemOnClickListener(View v, int position) {
            v.setOnClickListener(view -> openPlaylist(position));
        }
    }
}