package com.example.onlineplaylists;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class OnlinePlaylistsUtils {
    public static int dpToPx(Context c, int dp) {
        DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        int pixels = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
        return pixels;
    }

    public static void setDimensions(Context c, View v, int width, int height, int weight) {
        if (width > 0) width = dpToPx(c, width);
        if (height > 0) height = dpToPx(c, height);
        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) v.getLayoutParams();
        p.width = width;
        p.height = height;
        p.weight = weight;
        v.setLayoutParams(p);
    }

    public static String readFile(Context c, Uri uri) {
        try {
            InputStream in = c.getContentResolver().openInputStream(uri);
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append(line).append('\n');
            }
           return total.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void writeFileDep(String filePath, String content) {
        File file = new File(filePath);
        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileWriter fw = null;
        try {
            fw = new FileWriter(file, false);
            fw.write(content);
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fw != null) fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeFile(Context c, Uri uri, String content) {
        try {
            ParcelFileDescriptor fd = c.getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream fos = new FileOutputStream(fd.getFileDescriptor());
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            fos.close();
            fd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Intent getCreateIntent(String fileName) {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, fileName);
        return i;
    }

    public static Intent getShareIntent(Uri uri) {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_SEND);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_STREAM, uri);
        return i;
    }

    public static String getHMS(int seconds) {
        int hour = seconds/3600;
        int minute = (seconds % 3600) / 60;
        int second = seconds % 60;
        return (hour == 0 ? "" : String.valueOf(hour).concat(":"))
                .concat(minute < 10 ? "0" : "")
                .concat(String.valueOf(minute).concat(":"))
                .concat(second < 10 ? "0" : "")
                .concat(String.valueOf(second));
    }

    public static boolean isConnected(Context c) {
        ConnectivityManager cm = ((ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE));
        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        assert nc != null;
        return nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    public static void showMessageDialog(Context c,
                                         int title,
                                         String message,
                                         int positiveButtonText,
                                         DialogInterface.OnClickListener onButtonClick,
                                         int negativeButtonText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c, R.style.Theme_OnlinePlaylistsDialog);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveButtonText, onButtonClick);
        builder.setNegativeButton(negativeButtonText, null);
        builder.create().show();
    }

    public static void showMessageDialog(Context c,
                                         String title,
                                         int message,
                                         int positiveButtonText,
                                         DialogInterface.OnClickListener onButtonClick,
                                         int negativeButtonText,
                                         DialogInterface.OnCancelListener onCancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c, R.style.Theme_OnlinePlaylistsDialog);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveButtonText, onButtonClick);
        builder.setNegativeButton(negativeButtonText, ((dialog, which) -> dialog.cancel()));
        builder.setOnCancelListener(onCancelListener);
        builder.create().show();
    }
}