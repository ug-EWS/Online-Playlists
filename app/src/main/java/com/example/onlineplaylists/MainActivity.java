package com.example.onlineplaylists;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class MainActivity extends AppCompatActivity {
    YouTube youTube;
    private YouTube youTubeTemp;

    private LinearLayout layout, list, musicController, toolbar, searchBar, youTubeContainer;
    private CoordinatorLayout coordinatorLayout;
    private ImageView icon, options, settings, selectAllButton, removeButton, addToPlaylistButton,
            replayButton, playButton, forwardButton,
            mergeButton, cancelSearchButton, findUpButton, findDownButton, searchButton;
    private TextView musicTitle, noPlaylistsText, noVideosText;
    private EditText searchEditText;
    private FloatingActionButton addButton;
    private ProgressBar progressBar;
    TextView titleText;
    RecyclerView listOfPlaylistsRecycler, playlistRecycler;
    ViewGroup.LayoutParams youTubeLayoutParams;

    ListOfPlaylistsAdapter listOfPlaylistsAdapter;
    PlaylistAdapter playlistAdapter;
    ItemTouchHelper listOfPlaylistsItemTouchHelper, playlistItemTouchHelper;

    ListOfPlaylists listOfPlaylists;
    Playlist currentPlaylist, playingPlaylist, sharedPlaylist;
    YouTubeVideo playingVideo;

    int currentPlaylistIndex = -1,
            playingPlaylistIndex = -1,
            foundItemIndex = -1,
            foundAtStart = -1,
            foundAtEnd = -1,
            theme = 0;

    private long timerMs;

    private PlaylistDialog playlistDialog;
    private VideoDialog videoDialog;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    boolean viewMode, listSortMode, selectionMode, searchMode, darkMode;
    boolean isPortrait, serviceRunning;
    private boolean isFullscreen, musicMode, showController,
            musicControlsMode, timerSet;

    ArrayList<Integer> selectedItems;

    private final Context context = MainActivity.this;

    private Handler handler;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        sp = getSharedPreferences("OnlinePlaylists", MODE_PRIVATE);
        spe = sp.edit();
        listOfPlaylists = new ListOfPlaylists().fromJson(sp.getString("playlists", "[]"));

        musicMode = sp.getBoolean("musicMode", false);
        musicControlsMode = sp.getBoolean("musicControlsMode", true);
        theme = sp.getInt("theme", 0);

        playlistDialog = new PlaylistDialog(this, -1);
        videoDialog = new VideoDialog(this);

        initializeUi();
        youTube = new YouTube(this, youTubeContainer);
        youTube.playMode = sp.getInt("playMode", 2);
        youTube.setShuffle(sp.getBoolean("shuffle", false));
        youTube.playlistIndexes = new ArrayList<>();
        youTubeTemp = null;

        selectedItems = new ArrayList<>();

        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                back();
            }
        };
        getOnBackPressedDispatcher().addCallback(onBackPressedCallback);
        setMusicControlsMode(musicControlsMode);
        setOrientation(true);
        setFullscreen(false);
        setControllerVisibility(false);

        activityResultLauncher = getActivityResultLauncher();
    }

    private void back() {
        if (isFullscreen) {
            setFullscreen(false);
        } else if (searchMode) {
            setSearchMode(false);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                importPlaylist(getIntent().getData());
            } else {
                String[] permissions = {READ_EXTERNAL_STORAGE};
                requestPermissions(permissions, 101);
            }
        }
        if (Objects.equals(intent.getAction(), "themeChange")) {
            if (sp.getBoolean("changing", false)) {
                int ppi = intent.getIntExtra("ppi", -1);
                int pvi = intent.getIntExtra("pvi", -1);
                int cs = intent.getIntExtra("cs", 0);
                int cpi = intent.getIntExtra("cpi", -1);
                boolean play = intent.getBooleanExtra("play", true);
                if (ppi != -1) {
                    openPlaylist(ppi);
                    if (!currentPlaylist.remote) {
                        if (pvi != -1) playVideo(pvi, cs, true, play);
                        setViewMode(false);
                        if (cpi != -1) openPlaylist(cpi);
                        spe.putBoolean("changing", false);
                    }
                }
            }
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
                showMessage(getString(R.string.grant_permission));
            } else {
                importPlaylist(getIntent().getData());
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        darkMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                newConfig.isNightModeActive() :
                (newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (theme == 0) setAppTheme(0);
        setOrientation(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
        boolean darkModeSaved = sp.contains("darkMode");
        boolean darkModeChanged = darkMode != sp.getBoolean("darkMode", true);
        spe.putBoolean("darkMode", darkMode).commit();
        if (theme == 0 && (!darkModeSaved || darkModeChanged)) setAppTheme(0);
        super.onConfigurationChanged(newConfig);
    }

    private ActivityResultLauncher<Intent> getActivityResultLauncher() {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult()
                , (result) -> {
                    Uri uri = result.getData().getData();
                    OnlinePlaylistsUtils.writeFile(context, uri, sharedPlaylist.getJson());
                    startActivity(Intent.createChooser(OnlinePlaylistsUtils.getShareIntent(uri), getString(R.string.share)));
                });
    }

    @Override
    protected void onResume() {
        serviceRunning = sp.getBoolean("serviceRunning", false);
        if (serviceRunning && youTube.player != null) {
            continuePlayback();
        }
        timerSet = sp.getBoolean("timerSet", false);
        timerMs = sp.getLong("timerMs", 0);
        setTimer();
        super.onResume();
    }

    @Override
    protected void onPause() {
        refreshDatabase();
        if (playingPlaylist != null) {
            boolean isPlaying = false;
            if (playingPlaylist.remote) {
                if (youTubeTemp != null) {
                    isPlaying = youTubeTemp.isPlaying;
                    if (isPlaying) youTubeTemp.pause();
                }
            } else {
                isPlaying = youTube.isPlaying;
                if (isPlaying) youTube.pause();
            }
            if (isPlaying) {
                timerSet = false;
                setTimer();
                startPlaybackService();
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        youTube.view.release();
        if (youTubeTemp != null) youTubeTemp.view.release();
    }

    private void startPlaybackService() {
        Intent intent = new Intent(MainActivity.this, PlaybackService.class);
        intent.setAction("com.opl.ACTION_START_SERVICE");
        intent.putExtra("playlist", playingPlaylist.getJson());
        spe.putInt("currentPlaylistIndex", playingPlaylistIndex)
                .putInt("currentVideoIndex", youTube.playingVideoIndex)
                .putInt("currentSecond", youTube.currentSecond)
                .commit();
        startService(intent);
    }

    private void stopPlaybackService() {
        stopService(new Intent(MainActivity.this, PlaybackService.class));
    }

    void startTimer(long millis) {
        timerSet = true;
        timerMs = Calendar.getInstance().getTimeInMillis() + millis;
        setTimer();
        showMessage(String.format(getString(R.string.timer_set), (int)(millis / 60000)));
    }

    private void setTimer() {
        if (timerSet) {
            long afterMillis = timerMs - Calendar.getInstance().getTimeInMillis();
            if (afterMillis > 0) {
                handler = new Handler(getMainLooper());
                handler.postDelayed(() -> {
                    youTube.pause();
                    timerSet = false;
                }, afterMillis);
            } else {
                timerSet = false;
                setTimer();
            }
        } else if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    void continuePlayback() {
        stopPlaybackService();
        int cpi = sp.getInt("currentPlaylistIndex", 0);
        int cvi = sp.getInt("currentVideoIndex", 0);
        int cs = sp.getInt("currentSecond", 0);
        boolean play = sp.getBoolean("playing", true);
        openPlaylist(cpi, cvi);
        if (!currentPlaylist.remote) playVideo(cvi, cs, true, play);
        spe.putBoolean("serviceRunning", false);
    }

    private void initializeUi() {
        youTubeContainer = findViewById(R.id.youTubeContainer);
        youTubeLayoutParams = youTubeContainer.getLayoutParams();
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
        mergeButton = findViewById(R.id.merge);
        listOfPlaylistsRecycler = findViewById(R.id.listOfPlaylistsRecycler);
        listOfPlaylistsRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listOfPlaylistsAdapter = new ListOfPlaylistsAdapter(this);
        listOfPlaylistsRecycler.setAdapter(listOfPlaylistsAdapter);
        listOfPlaylistsItemTouchHelper = new ItemTouchHelper(new ItemMoveCallback(listOfPlaylistsAdapter));
        listOfPlaylistsItemTouchHelper.attachToRecyclerView(listOfPlaylistsRecycler);
        playlistRecycler = findViewById(R.id.playlistRecycler);
        playlistRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        addButton.setOnClickListener(view -> {
            if (viewMode) videoDialog.show(); else playlistDialog.show();
        });
        icon.setOnClickListener(view -> back());
        settings.setOnClickListener(view -> getSettingsPopupMenu().show());
        options.setOnClickListener(v ->
                (viewMode ? getPlaylistPopupMenu(options, true, currentPlaylistIndex)
                        : getListOfPlaylistsPopupMenu()).show());
        musicController = findViewById(R.id.musicController);
        musicTitle = findViewById(R.id.musicTitle);
        replayButton = findViewById(R.id.replayButton);
        replayButton.setOnClickListener(v -> {if (musicControlsMode) getPlayingYouTube().replay(); else getPlayingYouTube().playPrevious();});
        playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener(v -> getPlayingYouTube().playPause());
        progressBar = findViewById(R.id.progressBar);
        forwardButton = findViewById(R.id.forwardButton);
        forwardButton.setOnClickListener(v -> {if (musicControlsMode) getPlayingYouTube().forward(); else getPlayingYouTube().playNext();});

        noPlaylistsText = findViewById(R.id.noPlaylists);
        noVideosText = findViewById(R.id.noVideos);
        toolbar = findViewById(R.id.toolbar);
        searchBar = findViewById(R.id.searchBar);
        cancelSearchButton = findViewById(R.id.cancelSearchButton);
        cancelSearchButton.setOnClickListener(v -> setSearchMode(false));
        searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                findItem(false, true);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        findUpButton = findViewById(R.id.findUpButton);
        findUpButton.setOnClickListener(v -> findItem(true, false));
        findDownButton = findViewById(R.id.findDownButton);
        findDownButton.setOnClickListener(v -> findItem(false, false));
        searchButton = findViewById(R.id.search);
        searchButton.setOnClickListener(v -> setSearchMode(true));

        selectAllButton.setOnClickListener(v -> selectAllItems());
        removeButton.setOnClickListener(v -> removeItems());
        addToPlaylistButton.setOnClickListener(v -> addItemsToPlaylist());
        mergeButton.setOnClickListener(v -> mergeItems());
        selectAllButton.setTooltipText(getText(R.string.select_all));
        removeButton.setTooltipText(getText(R.string.delete));
        addToPlaylistButton.setTooltipText(getText(R.string.add_to_playlist));
        mergeButton.setTooltipText(getText(R.string.merge));

        coordinatorLayout = findViewById(R.id.coordinatorLayout);

        ViewCompat.setOnApplyWindowInsetsListener(coordinatorLayout, (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets insets2 = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(insets1.left, 0, insets1.right, insets2.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(addButton, (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) addButton.getLayoutParams();
            layoutParams.bottomMargin = layoutParams.topMargin + insets1.bottom;
            addButton.setLayoutParams(layoutParams);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.statusBarBackground), (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
            layoutParams.height = insets1.top;
            v.setLayoutParams(layoutParams);
            return insets;
        });
        setViewMode(false);
    }

    private YouTube getPlayingYouTube() {
        return playingPlaylist.remote ? youTubeTemp : youTube;
    }

    void onStateChange(boolean isPlaying, boolean isBuffering) {
        progressBar.setVisibility(isBuffering ? View.VISIBLE : View.GONE);
        playButton.setVisibility(isBuffering ? View.GONE : View.VISIBLE);
        playButton.setImageResource(isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
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
            if (itemId == R.id.settings) {
                getSettingsPopupMenu().show();
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
        menu.getMenu().getItem(8).setVisible(current);
        menu.getMenu().getItem(6).setVisible(forPlaylist.remote);
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
            if (itemIndex == R.id.openInYoutube) {
                openPlaylistInYouTube(forPlaylist);
                return true;
            }
            if (itemIndex == R.id.delete) {
                removePlaylist(index);
                updateNoItemsView();
                return true;
            }
            if (itemIndex == R.id.settings) {
                getSettingsPopupMenu().show();
                return true;
            }
            return false;
        });
        return menu;
    }

    void removePlaylist(int index) {
        OnlinePlaylistsUtils.showMessageDialog(
                context,
                listOfPlaylists.getPlaylistAt(index).title,
                R.string.delete_playlist_alert,
                R.string.dialog_button_delete,
                (dialog, which) -> {
                    listOfPlaylists.removePlaylist(index);
                    listOfPlaylistsAdapter.removeItem(index);
                    updateNoItemsView();
                },
                R.string.dialog_button_no,
                dialog -> listOfPlaylistsAdapter.notifyItemChanged(index)
                );
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
        OnlinePlaylistsUtils.showMessageDialog(
                context,
                currentPlaylist.getVideoAt(index).title,
                R.string.delete_video_alert,
                R.string.dialog_button_delete,
                (dialog, which) -> {
                    currentPlaylist.removeVideo(index);
                    playlistAdapter.removeItem(index);
                    updateNoItemsView();
                },
                R.string.dialog_button_no,
                dialog -> playlistAdapter.notifyItemChanged(index)
                );
    }

    private @NonNull PopupMenu getSettingsPopupMenu() {
        PopupMenu popupMenu = new PopupMenu(context, options, Gravity.TOP);
        popupMenu.inflate(R.menu.options);
        Menu menu = popupMenu.getMenu();
        if (youTube.playMode == 0) menu.findItem(R.id.doNothing).setChecked(true);
        if (youTube.playMode == 1) menu.findItem(R.id.replayButton).setChecked(true);
        if (youTube.playMode == 2) menu.findItem(R.id.playNextVideo).setChecked(true);
        if (theme == 0) menu.findItem(R.id.autoTheme).setChecked(true);
        if (theme == 1) menu.findItem(R.id.lightTheme).setChecked(true);
        if (theme == 2) menu.findItem(R.id.darkTheme).setChecked(true);
        menu.findItem(R.id.autoDisabled).setEnabled(timerSet);
        menu.findItem(R.id.auto10Min).setEnabled(!timerSet);
        menu.findItem(R.id.auto30Min).setEnabled(!timerSet);
        menu.findItem(R.id.auto1Hour).setEnabled(!timerSet);
        menu.findItem(R.id.custom).setEnabled(!timerSet);
        if (musicControlsMode) menu.findItem(R.id.rewindForward).setChecked(true);
        else menu.findItem(R.id.prevNext).setChecked(true);
        menu.findItem(R.id.shuffleMode).setChecked(youTube.shuffle);
        menu.findItem(R.id.musicMode).setChecked(musicMode);
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemIndex = item.getItemId();
            if (itemIndex == R.id.doNothing) {
                youTube.playMode = 0;
                item.setChecked(true);
                return false;
            }
            if (itemIndex == R.id.replayButton) {
                youTube.playMode = 1;
                item.setChecked(true);
                return false;
            }
            if (itemIndex == R.id.playNextVideo) {
                youTube.playMode = 2;
                item.setChecked(true);
                return false;
            }
            if (itemIndex == R.id.shuffleMode) {
                youTube.setShuffle(!youTube.shuffle);
                item.setChecked(youTube.shuffle);
                return false;
            }
            if (itemIndex == R.id.musicMode) {
                setMusicMode(!musicMode);
                item.setChecked(musicMode);
                musicTitle.setSelected(musicMode);
                return false;
            }
            if (itemIndex == R.id.autoDisabled) {
                timerSet = false;
                setTimer();
                showMessage(getString(R.string.timer_disabled));
                return false;
            }
            if (itemIndex == R.id.auto10Min) {
                startTimer(10000);
                return false;
            }
            if (itemIndex == R.id.auto30Min) {
                startTimer(1800000);
                return false;
            }
            if (itemIndex == R.id.auto1Hour) {
                startTimer(3600000);
                return false;
            }
            if (itemIndex == R.id.custom) {
                new AutoShutDownDialog(this).show();
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

            if (itemIndex == R.id.lightTheme) {
                item.setChecked(true);
                setAppTheme(1);
                return true;
            }

            if (itemIndex == R.id.darkTheme) {
                item.setChecked(true);
                setAppTheme(2);
                return true;
            }

            if (itemIndex == R.id.autoTheme) {
                item.setChecked(true);
                setAppTheme(0);
                return true;
            }

            if (itemIndex == R.id.about) {
                return true;
            }
            return true;
        });
        return popupMenu;
    }

    private void refreshDatabase() {
        spe.putInt("playMode", youTube.playMode)
                .putBoolean("shuffle", youTube.shuffle)
                .putBoolean("musicMode", musicMode)
                .putBoolean("musicControlsMode", musicControlsMode)
                .putBoolean("timerSet", timerSet)
                .putLong("timerMs", timerMs)
                .putInt("theme", theme)
                .commit();
        spe.putString("playlists", listOfPlaylists.getJson()).apply();
    }

    private void importPlaylist(Uri uri) {
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
        sharedPlaylist = playlist;
        String fileName = playlist.title.replace("/", "_".concat(".json"));
        activityResultLauncher.launch(OnlinePlaylistsUtils.getCreateIntent(fileName));
    }

    void openPlaylist(int index) {
        openPlaylist(index, index == playingPlaylistIndex ? youTube.playingVideoIndex : 0);
    }

    private void openPlaylist(int index, int scroll) {
        currentPlaylistIndex = index;
        currentPlaylist = listOfPlaylists.getPlaylistAt(index);
        if (currentPlaylist.remote) {
            if (currentPlaylistIndex == playingPlaylistIndex && playingPlaylistIndex != -1) youTubeTemp.playPause();
            else {
                if (youTubeTemp != null) {
                    youTubeContainer.removeView(youTubeTemp.view);
                    youTubeTemp = null;
                }
                int oldPosition = playingPlaylistIndex;
                youTubeTemp = new YouTube(this, youTubeContainer, currentPlaylist.remoteId);
                youTube.view.setVisibility(View.GONE);
                youTubeTemp.view.setVisibility(View.VISIBLE);
                youTubeTemp.shuffle = youTube.shuffle;
                playingPlaylistIndex = currentPlaylistIndex;
                playingPlaylist = currentPlaylist;
                if (oldPosition != -1) listOfPlaylistsAdapter.notifyItemChanged(oldPosition);
                listOfPlaylistsAdapter.notifyItemChanged(playingPlaylistIndex);
                setControllerVisibility(true);
                musicTitle.setText(currentPlaylist.title);
            }
        } else {
            setViewMode(true);
            playlistRecycler.scrollToPosition(scroll);
        }
    }

    void playVideo(int index, boolean switchPlaylist) {
        playVideo(index, musicMode ? currentPlaylist.getVideoAt(index).musicStartSeconds : 0, switchPlaylist, true);
    }

    private void playVideo(int index, int startSecond, boolean switchPlaylist, boolean autoPlay) {
        if (youTube.player == null) {
            showMessage(OnlinePlaylistsUtils.isConnected(context) ?
                    getString(R.string.player_not_ready) :
                    "Lütfen internet bağlantınızı kontrol edin.");
        } else {
            if (youTubeTemp != null) {
                youTubeContainer.removeView(youTubeTemp.view);
                youTubeTemp = null;
            }
            youTube.view.setVisibility(View.VISIBLE);
            if (!switchPlaylist && playingPlaylistIndex == currentPlaylistIndex) switchPlaylist = true;

            int oldPosition = youTube.playingVideoIndex;

            youTube.playlistIndexes.remove((Integer) index);

            if (switchPlaylist) {
                playingPlaylistIndex = currentPlaylistIndex;
                playingPlaylist = currentPlaylist;
            }

            youTube.playingVideoIndex = index;
            playingVideo = playingPlaylist.getVideoAt(index);
            musicTitle.setText(playingVideo.title);
            setControllerVisibility(true);

            if (switchPlaylist) {
                if (oldPosition != -1) playlistRecycler.getAdapter().notifyItemChanged(oldPosition);
                playlistRecycler.getAdapter().notifyItemChanged(index);
            }

            if (autoPlay) youTube.player.loadVideo(playingVideo.id, startSecond);
            else youTube.player.cueVideo(playingVideo.id, startSecond);
        }
    }

    private void openVideoInYoutube(YouTubeVideo video) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getVideoUrl()));
        startActivity(i);
    }

    private void openPlaylistInYouTube(Playlist playlist) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(playlist.getPlaylistUrl()));
        startActivity(i);
    }

    void closePlayer() {
        playingPlaylistIndex = -1;
        youTube.playingVideoIndex = -1;
        youTube.player.pause();
        setControllerVisibility(false);
    }

    void showMessage(int resId) {
        showMessage(getString(resId));
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
            currentPlaylistIndex = -1;
        }
        updateToolbar();
        updateNoItemsView();
    }

    void setSelectionMode(boolean _selectionMode) {
        selectionMode = _selectionMode;
        (viewMode ? playlistAdapter : listOfPlaylistsAdapter).notifyDataSetChanged();
        updateToolbar();
    }

    void setListSortMode(boolean _listSortMode) {
        listSortMode = _listSortMode;
        (viewMode ? playlistAdapter : listOfPlaylistsAdapter).notifyDataSetChanged();
        updateToolbar();
    }

    void setSearchMode(boolean _searchMode) {
        searchMode = _searchMode;
        foundItemIndex = -1;
        (viewMode ? playlistAdapter : listOfPlaylistsAdapter).notifyDataSetChanged();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (searchMode) imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
        else imm.hideSoftInputFromWindow(searchBar.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        updateToolbar();
    }

    void updateToolbar() {
        toolbar.setVisibility(searchMode ? View.GONE : View.VISIBLE);
        searchBar.setVisibility(searchMode ? View.VISIBLE : View.GONE);
        titleText.setText(
                listSortMode && viewMode ? getString(R.string.sort_videos)
                        : listSortMode ? getString(R.string.sort_playlists)
                        : selectionMode ? String.format(getString(R.string.multi_choose), selectedItems.size())
                        : viewMode ? currentPlaylist.title
                        : getString(R.string.app_name));
        icon.setImageResource(selectionMode ? R.drawable.baseline_close_24
                : viewMode || listSortMode ? R.drawable.baseline_arrow_back_24
                : R.drawable.baseline_smart_display_24);
        icon.setClickable(viewMode || selectionMode || listSortMode);
        options.setVisibility(selectionMode || listSortMode ? View.GONE : View.VISIBLE);
        //settings.setVisibility(selectionMode || listSortMode ? View.GONE : View.VISIBLE);
        selectAllButton.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        removeButton.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        addToPlaylistButton.setVisibility(selectionMode && viewMode ? View.VISIBLE : View.GONE);
        mergeButton.setVisibility(selectionMode && !viewMode ? View.VISIBLE : View.GONE);
        searchButton.setVisibility(selectionMode || listSortMode ? View.GONE : View.VISIBLE);
        addButton.setVisibility(selectionMode || listSortMode || searchMode ? View.GONE : View.VISIBLE);
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
        if (selectedItems.size() == 1) {
            int index = selectedItems.get(0);
            if (viewMode) removeVideo(index); else removePlaylist(index);
        } else OnlinePlaylistsUtils.showMessageDialog(context,
                R.string.multi_remove_title,
                String.format(getString(R.string.multi_remove_prompt), selectedItems.size()),
                R.string.dialog_button_delete,
                (dialog, which) -> {
                    if (viewMode) {
                        currentPlaylist.removeVideos(selectedItems);
                        if (selectedItems.contains(youTube.playingVideoIndex)) closePlayer();
                    }
                    else {
                        listOfPlaylists.removePlaylists(selectedItems);
                        if (selectedItems.contains(playingPlaylistIndex)) closePlayer();
                    }
                    showMessage(getString(R.string.removed));
                    selectedItems.clear();
                    setSelectionMode(false);
                    updateNoItemsView();
                },
                R.string.dialog_button_no);
    }

    private void addItemsToPlaylist() {
        (selectedItems.size() == 1 ?
                new ManagePlaylistsDialog(this, selectedItems.get(0)) :
                new ManagePlaylistsDialog(this, selectedItems))
                .show();
    }

    private void mergeItems() {
        if (selectedItems.size() < 2)
            showMessage(getString(R.string.choose_more_than_one));
        else if (listOfPlaylists.hasRemote(selectedItems))
            showMessage("YouTube oynatma listeleri birleştirilemez.");
        else
            OnlinePlaylistsUtils.showMessageDialog(
                    context,
                    R.string.merge_title,
                    String.format(getString(R.string.merge_message), selectedItems.size()),
                    R.string.dialog_button_merge,
                    (dialog, which) -> {
                        String s = listOfPlaylists.mergePlaylists(selectedItems);
                        selectedItems.clear();
                        setSelectionMode(false);
                        showMessage(String.format(getString(R.string.merge_success), s));
                    },
                    R.string.dialog_button_no);
    }

    private void findItem(boolean up, boolean _new) {
        int f = foundItemIndex;
        int i = _new ? -1 : foundItemIndex;
        String query = searchEditText.getText().toString().toLowerCase();
        Function<Integer, String> title =
                viewMode ? (index) -> currentPlaylist.getVideoAt(index).title
                        : (index) -> listOfPlaylists.getPlaylistAt(index).title;
        Supplier<Integer> length =
                viewMode ? () -> currentPlaylist.getLength() : () -> listOfPlaylists.getLength();
        if (!query.isEmpty()) {
            while (up && i > 0 || !up && i < length.get() - 1) {
                if (up) i--;
                else i++;
                String title1 = title.apply(i).toLowerCase();
                if (title1.contains(query)) {
                    foundItemIndex = i;
                    foundAtStart = title1.indexOf(query);
                    foundAtEnd = foundAtStart + query.length();
                    break;
                }
            }
            (viewMode ? playlistAdapter : listOfPlaylistsAdapter).notifyItemChanged(f);
            (viewMode ? playlistAdapter : listOfPlaylistsAdapter).notifyItemChanged(foundItemIndex);
            (viewMode ? playlistRecycler : listOfPlaylistsRecycler).scrollToPosition(foundItemIndex);
        }
    }

    private void setMusicControlsMode(boolean _musicControlsMode) {
        musicControlsMode = _musicControlsMode;
        replayButton.setImageResource(musicControlsMode ? R.drawable.baseline_replay_5_24 : R.drawable.baseline_skip_previous_24);
        forwardButton.setImageResource(musicControlsMode ? R.drawable.baseline_forward_5_24 : R.drawable.baseline_skip_next_24);
    }

    boolean toggleFullscreen() {
        setFullscreen(!isFullscreen);
        return isFullscreen;
    }

    void setFullscreen(boolean _fullscreen) {
        isFullscreen = _fullscreen;

        setRequestedOrientation(isFullscreen ? ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_USER);
        list.setVisibility(isFullscreen ? View.GONE : View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController windowInsetsController = getWindow().getInsetsController();
            assert windowInsetsController != null;
            windowInsetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            if (isFullscreen) {
                addButton.hide();
                windowInsetsController.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());
            } else {
                addButton.show();
                windowInsetsController.show(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());
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
            youTubeLayoutParams = OnlinePlaylistsUtils.setDimensions(context, youTubeContainer, isFullscreen ? MATCH_PARENT : WRAP_CONTENT, MATCH_PARENT, 1);
            youTube.updateLayoutParams();
            if (youTubeTemp != null) youTubeTemp.updateLayoutParams();
        }
    }

    private void setOrientation(boolean _isPortrait) {
        isPortrait = _isPortrait;
        if (isPortrait) {
            layout.setOrientation(LinearLayout.VERTICAL);
            youTubeLayoutParams = OnlinePlaylistsUtils.setDimensions(context, youTubeContainer, MATCH_PARENT, WRAP_CONTENT, 0);
            OnlinePlaylistsUtils.setDimensions(context, list, MATCH_PARENT, WRAP_CONTENT, 1);
        } else {
            layout.setOrientation(musicMode ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            youTubeLayoutParams = OnlinePlaylistsUtils.setDimensions(context, youTubeContainer, isFullscreen ? MATCH_PARENT : WRAP_CONTENT, MATCH_PARENT, 1);
            OnlinePlaylistsUtils.setDimensions(context, list, musicMode || !showController ? MATCH_PARENT : 300, MATCH_PARENT, 0);
        }
        youTube.updateLayoutParams();
        if (youTubeTemp != null) youTubeTemp.updateLayoutParams();
        if (viewMode) playlistAdapter.notifyDataSetChanged();
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
        youTubeContainer.setVisibility(showController && !musicMode ? View.VISIBLE : View.GONE);
        if (!isPortrait) OnlinePlaylistsUtils.setDimensions(context, list, musicMode || !showController ? MATCH_PARENT : 300, MATCH_PARENT, 0);
    }

    private void setAppTheme(int _theme) {
        theme = _theme;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ((UiModeManager) getSystemService(UI_MODE_SERVICE)).setApplicationNightMode(theme);
        else AppCompatDelegate.setDefaultNightMode(theme == 0 ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : theme);
        Intent i = new Intent(this, MainActivity.class);
        spe.putBoolean("changing", true);
        i.setAction("themeChange");
        i.putExtra("ppi", playingPlaylistIndex);
        i.putExtra("pvi", youTube.playingVideoIndex);
        i.putExtra("cpi", currentPlaylistIndex);
        i.putExtra("cs", youTube.currentSecond);
        if (playingPlaylistIndex != -1) i.putExtra("play", getPlayingYouTube().isPlaying);
        startActivity(i);
        finish();
    }
}