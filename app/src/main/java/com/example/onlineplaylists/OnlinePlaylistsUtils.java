package com.example.onlineplaylists;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

public class OnlinePlaylistsUtils {
    public static int dpToPx(Context c, int dp) {
        DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        int pixels = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
        return pixels;
    }

    public static void setDimensions(Context c, View v, int width, int height, int weight) {
        if (width > 0) width = dpToPx(c, width);
        if (height > 0) height = dpToPx(c, height);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
        p.weight = weight;
        v.setLayoutParams(p);
    }

    public static String readFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            StringBuilder sb = new StringBuilder();
            FileReader fr = null;
            try {
                fr = new FileReader(file);

                char[] buff = new char[1024];
                int length = 0;

                while ((length = fr.read(buff)) > 0) {
                    sb.append(new String(buff, 0, length));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fr != null) {
                    try {
                        fr.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    public static void writeFile(String filePath, String content) {
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

    public static void exportPlaylist(Context c, Playlist p) {

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
}
