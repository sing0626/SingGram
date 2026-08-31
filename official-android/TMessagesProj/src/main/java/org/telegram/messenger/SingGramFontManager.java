package org.telegram.messenger;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import org.telegram.ui.ActionBar.Theme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Stores and applies an app-only font without requiring an OEM system font installation. */
public final class SingGramFontManager {

    private static final String PREFS_NAME = "singgram_font";
    private static final String KEY_PATH = "path";
    private static final String KEY_NAME = "name";
    private static final String FONT_DIR = "singgram_fonts";
    private static final long MAX_FONT_BYTES = 50L * 1024L * 1024L;

    public static final class ImportResult {
        public final boolean success;
        public final String error;

        private ImportResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
    }

    private static final Map<TextView, Integer> appliedViews = new WeakHashMap<>();
    private static final Map<View, Boolean> observedRoots = new WeakHashMap<>();
    private static boolean installed;
    private static boolean initialized;
    private static int generation;
    private static Typeface customTypeface;
    private static String customFontName = "";
    private static String customFontPath = "";

    private SingGramFontManager() {
    }

    public static synchronized void install(Application application) {
        ensureInitialized(application);
        if (installed) {
            return;
        }
        installed = true;
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, android.os.Bundle savedInstanceState) {
                applyToActivity(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                applyToActivity(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, android.os.Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

    public static synchronized boolean isEnabled() {
        ensureInitialized(ApplicationLoader.applicationContext);
        return customTypeface != null;
    }

    public static synchronized String getDisplayName() {
        ensureInitialized(ApplicationLoader.applicationContext);
        return customTypeface == null ? "" : customFontName;
    }

    public static synchronized Typeface getCustomTypeface() {
        ensureInitialized(ApplicationLoader.applicationContext);
        return customTypeface;
    }

    public static synchronized Typeface getTypefaceForAsset(String assetPath) {
        ensureInitialized(ApplicationLoader.applicationContext);
        if (customTypeface == null || isMonospaceAsset(assetPath)) {
            return null;
        }
        String path = assetPath == null ? "" : assetPath.toLowerCase(Locale.US);
        int style = path.contains("italic") ? Typeface.ITALIC : Typeface.NORMAL;
        if (path.contains("medium") || path.contains("bold") || path.contains("rextrabold")) {
            style |= Typeface.BOLD;
        }
        return Typeface.create(customTypeface, style);
    }

    public static ImportResult importFont(Context context, Uri uri) {
        if (context == null || uri == null) {
            return new ImportResult(false, "read");
        }
        String displayName = getDisplayName(context, uri);
        String extension = extensionOf(displayName);
        if (!isSupportedExtension(extension)) {
            return new ImportResult(false, "unsupported");
        }

        File directory = ApplicationLoader.getFilesDirFixed(FONT_DIR);
        if (directory == null) {
            return new ImportResult(false, "storage");
        }
        File temporary = new File(directory, "global_font.tmp");
        File destination = new File(directory, "global_font." + extension);
        long copied = 0;
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(temporary, false)) {
            if (input == null) {
                return new ImportResult(false, "read");
            }
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                copied += count;
                if (copied > MAX_FONT_BYTES) {
                    temporary.delete();
                    return new ImportResult(false, "too_large");
                }
                output.write(buffer, 0, count);
            }
            output.getFD().sync();
        } catch (Exception e) {
            FileLog.e(e);
            temporary.delete();
            return new ImportResult(false, "read");
        }

        Typeface candidate;
        try {
            candidate = Typeface.createFromFile(temporary);
        } catch (Exception e) {
            FileLog.e(e);
            temporary.delete();
            return new ImportResult(false, "invalid");
        }
        if (candidate == null) {
            temporary.delete();
            return new ImportResult(false, "invalid");
        }

        if (destination.exists() && !destination.delete()) {
            temporary.delete();
            return new ImportResult(false, "storage");
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete();
            return new ImportResult(false, "storage");
        }
        synchronized (SingGramFontManager.class) {
            clearOldFontFiles(directory, destination);
            customTypeface = candidate;
            customFontPath = destination.getAbsolutePath();
            customFontName = displayName;
            initialized = true;
            preferences(context).edit().putString(KEY_PATH, customFontPath).putString(KEY_NAME, customFontName).apply();
            fontChanged();
        }
        return new ImportResult(true, "");
    }

    public static synchronized void resetFont(Context context) {
        if (context == null) {
            return;
        }
        File directory = ApplicationLoader.getFilesDirFixed(FONT_DIR);
        if (directory != null) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        customTypeface = null;
        customFontName = "";
        customFontPath = "";
        initialized = true;
        preferences(context).edit().remove(KEY_PATH).remove(KEY_NAME).apply();
        fontChanged();
    }

    public static void applyToActivity(Activity activity) {
        if (activity == null || !isEnabled() || activity.getWindow() == null) {
            return;
        }
        final View root = activity.getWindow().getDecorView();
        if (root == null) {
            return;
        }
        applyToViewTree(root);
        synchronized (SingGramFontManager.class) {
            if (observedRoots.containsKey(root)) {
                return;
            }
            observedRoots.put(root, true);
        }
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (isEnabled()) {
                    applyToViewTree(root);
                }
            }
        });
    }

    private static void ensureInitialized(Context context) {
        if (initialized || context == null) {
            return;
        }
        initialized = true;
        SharedPreferences preferences = preferences(context);
        customFontPath = preferences.getString(KEY_PATH, "");
        customFontName = preferences.getString(KEY_NAME, "");
        if (TextUtils.isEmpty(customFontPath)) {
            return;
        }
        try {
            File fontFile = new File(customFontPath);
            if (fontFile.isFile()) {
                customTypeface = Typeface.createFromFile(fontFile);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (customTypeface == null) {
            customFontPath = "";
            customFontName = "";
            preferences.edit().remove(KEY_PATH).remove(KEY_NAME).apply();
        }
    }

    private static void fontChanged() {
        generation++;
        appliedViews.clear();
        AndroidUtilities.invalidateTypefaceCache();
        refreshThemePaints();
    }

    private static void applyToViewTree(View view) {
        if (view instanceof TextView) {
            applyToTextView((TextView) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyToViewTree(group.getChildAt(i));
            }
        }
    }

    private static void applyToTextView(TextView textView) {
        Typeface custom = getCustomTypeface();
        if (custom == null || isMonospaceTypeface(textView.getTypeface())) {
            return;
        }
        synchronized (SingGramFontManager.class) {
            Integer appliedGeneration = appliedViews.get(textView);
            if (appliedGeneration != null && appliedGeneration == generation) {
                return;
            }
            appliedViews.put(textView, generation);
        }
        Typeface current = textView.getTypeface();
        int style = current == null ? Typeface.NORMAL : current.getStyle();
        textView.setTypeface(Typeface.create(custom, style));
    }

    private static void refreshThemePaints() {
        try {
            for (Field field : Theme.class.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                applyToThemeValue(field.get(null), field.getName());
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void applyToThemeValue(Object value, String fieldName) {
        if (value instanceof TextPaint) {
            if (isMonospaceAsset(fieldName)) {
                return;
            }
            Typeface base = getCustomTypeface();
            if (base == null) {
                base = isBoldField(fieldName) ? AndroidUtilities.bold() : Typeface.DEFAULT;
            }
            ((TextPaint) value).setTypeface(Typeface.create(base, isBoldField(fieldName) ? Typeface.BOLD : Typeface.NORMAL));
        } else if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                applyToThemeValue(Array.get(value, i), fieldName);
            }
        }
    }

    private static boolean isBoldField(String fieldName) {
        String name = fieldName.toLowerCase(Locale.US);
        return name.contains("bold") || name.contains("namepaint") || name.contains("title") || name.contains("button") || name.contains("admin");
    }

    private static boolean isMonospaceAsset(String value) {
        String name = value == null ? "" : value.toLowerCase(Locale.US);
        return name.contains("mono") || name.contains("code");
    }

    private static boolean isMonospaceTypeface(Typeface typeface) {
        return typeface != null && typeface.toString().toLowerCase(Locale.US).contains("mono");
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String getDisplayName(Context context, Uri uri) {
        String name = null;
        try (Cursor cursor = context.getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    name = cursor.getString(column);
                }
            }
        } catch (Exception ignored) {
        }
        if (TextUtils.isEmpty(name)) {
            name = uri.getLastPathSegment();
        }
        return TextUtils.isEmpty(name) ? "custom-font.ttf" : name;
    }

    private static String extensionOf(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.US);
    }

    private static boolean isSupportedExtension(String extension) {
        return "ttf".equals(extension) || "otf".equals(extension) || "ttc".equals(extension) || "otc".equals(extension);
    }

    private static void clearOldFontFiles(File directory, File current) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.equals(current)) {
                file.delete();
            }
        }
    }
}
