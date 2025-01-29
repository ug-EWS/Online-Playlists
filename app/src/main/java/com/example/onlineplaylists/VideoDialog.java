package com.example.onlineplaylists;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

class VideoDialog {
    MainActivity activity;
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
    ImageView searchButton;
    int whereToAdd;

    @SuppressLint("SetJavaScriptEnabled")
    VideoDialog(MainActivity _activity) {
        activity = _activity;
        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialogDark);
        dialog = builder.create();
        dialog.setTitle(activity.getString(R.string.add_video));

        dialogView = activity.getLayoutInflater().inflate(R.layout.add_video, null);
        editText = dialogView.findViewById(R.id.editUrl);
        addButton = dialogView.findViewById(R.id.addVideoButton);
        cancelButton = dialogView.findViewById(R.id.cancelButton);
        webView = dialogView.findViewById(R.id.webView);
        backButton = dialogView.findViewById(R.id.webBackButton);
        refreshButton = dialogView.findViewById(R.id.webRefreshButton);
        forwardButton = dialogView.findViewById(R.id.webForwardButton);
        searchButton = dialogView.findViewById(R.id.searchButton);

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
        webView.loadUrl("https://m.youtube.com");

        addButton.setOnClickListener(view -> {
            String id = YouTubeVideo.getVideoIdFrom(url);
            if (activity.currentPlaylist.contains(id)) {
                activity.showMessage("Bu video zaten eklenmiş.");
            }
            else {
                title = title.replace(" - YouTube", "");
                activity.currentPlaylist.addVideoTo(new YouTubeVideo(title, id), whereToAdd);
                PlaylistAdapter a = (PlaylistAdapter) activity.playlistRecycler.getAdapter();
                a.insertItem(whereToAdd);
                dismiss();
                activity.updateNoItemsView();
            }
        });

        cancelButton.setOnClickListener(view -> dismiss());

        backButton.setOnClickListener(view -> webView.goBack());

        refreshButton.setOnClickListener(view -> webView.reload());

        forwardButton.setOnClickListener(view -> webView.goForward());

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchButton.setVisibility(s.toString().isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        searchButton.setOnClickListener(v -> search());

        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                search();
                return true;
            }
            return false;
        });

        dialog.setView(dialogView);

    }

    public void show() {
        show(0);
    }

    public void show(int _whereToAdd) {
        whereToAdd = _whereToAdd;
        webView.onResume();
        webView.reload();
        dialog.show();
    }

    private void dismiss() {
        webView.setEnabled(false);
        webView.onPause();
        webView.reload();
        dialog.dismiss();
    }

    private void search() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
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
        webView.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.VISIBLE);
    }
}