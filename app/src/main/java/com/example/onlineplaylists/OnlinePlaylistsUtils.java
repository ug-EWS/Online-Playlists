package com.example.onlineplaylists;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
