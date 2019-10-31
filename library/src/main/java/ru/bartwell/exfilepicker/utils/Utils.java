package ru.bartwell.exfilepicker.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import ru.bartwell.exfilepicker.R;

/**
 * Created by BArtWell on 22.11.2015.
 */
public class Utils {

    private final static String CACHE_DIR_PATH_PART = "/Android";

    @NonNull
    public static LinkedHashMap<String, String> getNamedExternalStoragePaths(@NonNull Context context) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (String path : getExternalStoragePaths(context)) {
            result.put(path, getFriendlyName(context, path));
        }
        return result;
    }

    @NonNull
    public static List<String> getExternalStoragePaths(@NonNull Context context) {
        ArrayList<String> paths = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            for (File externalCacheDir : context.getExternalCacheDirs())
                paths.add(externalCacheDir.getPath().split(CACHE_DIR_PATH_PART)[0]);
        } else {
            paths.add(context.getExternalCacheDir().getPath().split(CACHE_DIR_PATH_PART)[0]);
        }

        return paths;
    }

    private static String getFriendlyName(Context context, String path) {
        File file = new File(path);
        String name = file.getName();
        if (name.equals("0")) // emulated/0/ i.e. root
            return context.getString(R.string.efp__internal_storage);
        if (name.length() == 9 && name.charAt(4) == '-') {
            StorageManager storageManager;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                storageManager = context.getSystemService(StorageManager.class);
                StorageVolume volume = storageManager.getStorageVolume(file);
                if (volume != null)
                    return context.getString(R.string.efp__external_storage_from_description, volume.getDescription(context), name);
            }
            return context.getString(R.string.efp__external_storage, name);
        }
        return name;
    }

    @NonNull
    public static String getFileExtension(@NonNull String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index == -1) return "";
        return fileName.substring(index + 1, fileName.length()).toLowerCase(Locale.getDefault());
    }

    @NonNull
    public static String getHumanReadableFileSize(@NonNull Context context, long size) {
        String[] units = context.getResources().getStringArray(R.array.efp__size_units);
        for (int i = units.length - 1; i >= 0; i--) {
            if (size >= Math.pow(1024, i)) {
                return Math.round((size / Math.pow(1024, i))) + " " + units[i];
            }
        }
        return size + " " + units[0];
    }

    public static int attrToResId(@NonNull Context context, @AttrRes int attr) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        return a.getResourceId(0, 0);
    }
}
