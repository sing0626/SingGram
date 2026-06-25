package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.SingGramConfig;
import org.telegram.messenger.SingGramUpdateClient;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LineProgressView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class SingGramUpdateActivity extends BaseFragment {

    private LinearLayout contentContainer;
    private SingGramUpdateClient.UpdateInfo updateInfo;
    private boolean checking;
    private boolean downloading;
    private boolean downloadComplete;
    private long downloadedBytes;
    private long downloadTotalBytes;
    private long downloadSpeedBytesPerSecond;
    private File downloadedApkFile;
    private String lastError;
    private LineProgressView downloadProgressView;
    private TextView downloadProgressTitle;
    private TextView downloadProgressSubtitle;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramOtaCenter));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        ((FrameLayout) fragmentView).addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(28));
        scrollView.addView(contentContainer, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        buildContent(context, false);
        checkForUpdates();
        return fragmentView;
    }

    private void buildContent(Context context, boolean animated) {
        if (context == null || contentContainer == null) {
            return;
        }
        contentContainer.removeAllViews();

        addHeroCard(context, contentContainer);

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramOtaVersionMap));
        LinearLayout versionSection = addSection(context, contentContainer);
        addInfoCell(context, versionSection, LocaleController.getString(R.string.SingGramOtaInstalled), BuildVars.BUILD_VERSION_STRING + " / " + SharedConfig.buildVersion());
        addDivider(context, versionSection);
        addInfoCell(context, versionSection, LocaleController.getString(R.string.SingGramOtaLatest), latestBuildValue());
        addDivider(context, versionSection);
        addInfoCell(context, versionSection, LocaleController.getString(R.string.SingGramOtaBuildDelta), buildDeltaValue());
        addDivider(context, versionSection);
        addInfoCell(context, versionSection, LocaleController.getString(R.string.SingGramOtaLastChecked), lastCheckedValue());
        addDivider(context, versionSection);
        addInfoCell(context, versionSection, LocaleController.getString(R.string.SingGramOtaReleaseChannel), SingGramUpdateClient.DEFAULT_RELEASE_URL);

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramUpdateActions));
        LinearLayout actionSection = addSection(context, contentContainer);
        addActionRow(context, actionSection, checking ? LocaleController.getString(R.string.SingGramOtaChecking) : LocaleController.getString(R.string.SingGramUpdateCheck), LocaleController.getString(R.string.SingGramUpdateCheckInfo), R.drawable.settings_features, 0xFF4EA5F6, 0xFF3577E5, true, v -> checkForUpdates());
        addActionDivider(context, actionSection);
        addActionRow(context, actionSection, downloadButtonTitle(), downloadButtonSubtitle(), R.drawable.menu_download_round, 0xFF40B7FF, 0xFF168BDE, updateInfo != null && !TextUtils.isEmpty(updateInfo.apkUrl) && !downloading, v -> downloadLatestApk());
        addActionDivider(context, actionSection);
        addDownloadProgressCard(context, actionSection);
        addDivider(context, actionSection);
        addActionRow(context, actionSection, LocaleController.getString(R.string.SingGramOtaOpenLatest), apkValue(), R.drawable.msg_openin, 0xFF8A98A7, 0xFF5D6C7B, updateInfo != null && !TextUtils.isEmpty(updateInfo.apkUrl), v -> openApk());
        addActionDivider(context, actionSection);
        addActionRow(context, actionSection, LocaleController.getString(R.string.SingGramUpdateCopyApk), LocaleController.getString(R.string.SingGramUpdateCopyApkInfo), R.drawable.msg_copy, 0xFF23B9C9, 0xFF2684E8, updateInfo != null && !TextUtils.isEmpty(updateInfo.apkUrl), v -> copyApkUrl());
        addActionDivider(context, actionSection);
        addActionRow(context, actionSection, LocaleController.getString(R.string.SingGramUpdateOpenRelease), LocaleController.getString(R.string.SingGramUpdateOpenReleaseInfo), R.drawable.settings_channel, 0xFF55CA47, 0xFF27B434, true, v -> openReleasePage());
        addActionDivider(context, actionSection);
        addActionRow(context, actionSection, LocaleController.getString(R.string.SingGramUpdateCopyRelease), SingGramUpdateClient.DEFAULT_RELEASE_URL, R.drawable.menu_copy_s, 0xFFFF8B3D, 0xFFE45644, true, v -> copyReleaseUrl());

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramOtaReleaseManifest));
        LinearLayout manifestSection = addSection(context, contentContainer);
        addInfoCell(context, manifestSection, LocaleController.getString(R.string.SingGramUpdateStatus), statusTitle());
        addDivider(context, manifestSection);
        addInfoCell(context, manifestSection, LocaleController.getString(R.string.SingGramUpdatePublishedAt), valueOrDash(updateInfo == null ? "" : updateInfo.publishedAt));
        addDivider(context, manifestSection);
        addInfoCell(context, manifestSection, LocaleController.getString(R.string.SingGramUpdateApkSize), updateInfo != null && updateInfo.apkSizeBytes > 0 ? AndroidUtilities.formatFileSize(updateInfo.apkSizeBytes) : "-");
        addDivider(context, manifestSection);
        addInfoCell(context, manifestSection, LocaleController.getString(R.string.SingGramUpdateSha256), valueOrDash(updateInfo == null ? "" : updateInfo.sha256));

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramUpdateNotes));
        LinearLayout notesSection = addSection(context, contentContainer);
        addInfoBlock(context, notesSection, notesValue());
        addDivider(context, notesSection);
        addActionRow(context, notesSection, LocaleController.getString(R.string.SingGramUpdateCopyNotes), LocaleController.getString(R.string.SingGramUpdateCopyNotesInfo), R.drawable.msg_copy, 0xFF23B9C9, 0xFF2684E8, updateInfo != null && !TextUtils.isEmpty(updateInfo.notes), v -> copyNotes());
        addInfo(context, contentContainer, LocaleController.getString(R.string.SingGramOtaInfo));

        if (animated) {
            animateRefresh();
        }
    }

    private void addHeroCard(Context context, LinearLayout container) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(16));
        card.setBackground(liquidGlassBackground());
        container.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 4));

        TextView eyebrow = new TextView(context);
        eyebrow.setText(LocaleController.getString(R.string.SingGramOtaHeroTitle));
        eyebrow.setTextColor(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), 0.90f));
        eyebrow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        eyebrow.setTypeface(AndroidUtilities.bold());
        eyebrow.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        eyebrow.setIncludeFontPadding(false);
        card.addView(eyebrow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText(statusTitle());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        title.setIncludeFontPadding(false);
        title.setPadding(0, AndroidUtilities.dp(8), 0, 0);
        card.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText(statusSubtitle());
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        subtitle.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        subtitle.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(14));
        card.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        addHeroMetric(context, row, LocaleController.getString(R.string.SingGramOtaInstalled), String.valueOf(SharedConfig.buildVersion()));
        addHeroMetric(context, row, LocaleController.getString(R.string.SingGramOtaLatest), updateInfo == null || updateInfo.versionCode <= 0 ? "-" : String.valueOf(updateInfo.versionCode));
    }

    private void addHeroMetric(Context context, LinearLayout row, String label, String value) {
        LinearLayout metric = new LinearLayout(context);
        metric.setOrientation(LinearLayout.VERTICAL);
        metric.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        metric.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhite), 0.64f)));
        row.addView(metric, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 6, 0));

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        labelView.setSingleLine(true);
        labelView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        metric.addView(labelView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        valueView.setTypeface(AndroidUtilities.bold());
        valueView.setSingleLine(true);
        valueView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        metric.addView(valueView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private GradientDrawable liquidGlassBackground() {
        int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
        int white = Theme.getColor(Theme.key_windowBackgroundWhite);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {
                Theme.multAlpha(accent, SingGramConfig.isLiquidGlassEnabled() ? 0.24f : 0.14f),
                Theme.multAlpha(white, SingGramConfig.isLiquidGlassEnabled() ? 0.92f : 0.84f),
                Theme.multAlpha(accent, SingGramConfig.isLiquidGlassEnabled() ? 0.12f : 0.08f)
        });
        drawable.setCornerRadius(AndroidUtilities.dp(SingGramConfig.isLiquidGlassEnabled() ? 18 : 12));
        drawable.setStroke(AndroidUtilities.dp(1), Theme.multAlpha(accent, SingGramConfig.isLiquidGlassEnabled() ? 0.20f : 0.12f));
        return drawable;
    }

    private String statusTitle() {
        if (checking) {
            return LocaleController.getString(R.string.SingGramOtaChecking);
        }
        if (!TextUtils.isEmpty(lastError)) {
            return LocaleController.getString(R.string.SingGramOtaErrorTitle);
        }
        if (updateInfo == null) {
            return LocaleController.getString(R.string.SingGramUpdateNotChecked);
        }
        return updateInfo.statusText();
    }

    private String statusSubtitle() {
        if (checking) {
            return LocaleController.getString(R.string.SingGramOtaCheckingInfo);
        }
        if (!TextUtils.isEmpty(lastError)) {
            return lastError;
        }
        if (updateInfo == null) {
            return LocaleController.getString(R.string.SingGramOtaNotCheckedInfo);
        }
        if (updateInfo.hasUpdate()) {
            return LocaleController.getString(R.string.SingGramOtaAvailableInfo);
        }
        return LocaleController.getString(R.string.SingGramOtaCurrentInfo);
    }

    private String downloadButtonTitle() {
        if (downloadComplete) {
            return LocaleController.getString(R.string.SingGramOtaInstallDownloaded);
        }
        if (downloading) {
            return LocaleController.getString(R.string.SingGramOtaDownloading);
        }
        return LocaleController.getString(R.string.SingGramOtaDownloadApk);
    }

    private String downloadButtonSubtitle() {
        if (downloadComplete) {
            return LocaleController.getString(R.string.SingGramOtaInstallDownloadedInfo);
        }
        if (downloading) {
            return downloadStatusValue();
        }
        if (updateInfo != null && updateInfo.apkSizeBytes > 0) {
            return LocaleController.formatString(R.string.SingGramOtaDownloadApkInfo, AndroidUtilities.formatFileSize(updateInfo.apkSizeBytes));
        }
        return LocaleController.getString(R.string.SingGramOtaDownloadApkInfoUnknown);
    }

    private String latestBuildValue() {
        if (updateInfo == null || updateInfo.versionCode <= 0) {
            return "-";
        }
        String version = TextUtils.isEmpty(updateInfo.versionName) ? LocaleController.getString(R.string.SingGramUpdates) : updateInfo.versionName;
        return version + " / " + updateInfo.versionCode;
    }

    private String buildDeltaValue() {
        if (updateInfo == null || updateInfo.versionCode <= 0) {
            return "-";
        }
        int delta = updateInfo.versionCode - SharedConfig.buildVersion();
        if (delta > 0) {
            return LocaleController.formatString(R.string.SingGramOtaBuildsAhead, delta);
        } else if (delta < 0) {
            return LocaleController.formatString(R.string.SingGramOtaBuildsNewerInstalled, Math.abs(delta));
        }
        return LocaleController.getString(R.string.SingGramUpdateSameVersion);
    }

    private String lastCheckedValue() {
        long time = SingGramConfig.getLastUpdateCheckTime();
        if (time <= 0) {
            return LocaleController.getString(R.string.SingGramUpdateNotChecked);
        }
        return LocaleController.formatDateTime(time / 1000, true);
    }

    private String valueOrDash(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }

    private String apkValue() {
        if (updateInfo == null || TextUtils.isEmpty(updateInfo.apkUrl)) {
            return LocaleController.getString(R.string.SingGramUpdateNoApk);
        }
        return LocaleController.getString(R.string.SingGramOtaOpenLatestInfo);
    }

    private String notesValue() {
        if (updateInfo == null) {
            return LocaleController.getString(R.string.SingGramUpdateNotesEmpty);
        }
        return TextUtils.isEmpty(updateInfo.notes) ? LocaleController.getString(R.string.SingGramUpdateNotesEmpty) : updateInfo.notes;
    }

    private void checkForUpdates() {
        if (checking) {
            return;
        }
        checking = true;
        lastError = null;
        buildContent(getParentActivity(), true);
        SingGramUpdateClient.check(new SingGramUpdateClient.Callback() {
            @Override
            public void onResult(SingGramUpdateClient.UpdateInfo info) {
                checking = false;
                updateInfo = info;
                SingGramConfig.setLastUpdateCheck(info.versionCode, info.versionName);
                Toast.makeText(getParentActivity(), info.hasUpdate() ? LocaleController.getString(R.string.SingGramUpdateAvailable) : LocaleController.getString(R.string.SingGramUpdateCurrent), Toast.LENGTH_SHORT).show();
                buildContent(getParentActivity(), true);
            }

            @Override
            public void onError(String error) {
                checking = false;
                lastError = error;
                Toast.makeText(getParentActivity(), error, Toast.LENGTH_LONG).show();
                buildContent(getParentActivity(), true);
            }
        });
    }

    private void animateRefresh() {
        contentContainer.animate().cancel();
        contentContainer.setAlpha(0.0f);
        contentContainer.setTranslationY(AndroidUtilities.dp(8));
        contentContainer.animate()
                .alpha(1.0f)
                .translationY(0)
                .setDuration(190)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                .start();
    }

    private void openApk() {
        if (updateInfo == null || TextUtils.isEmpty(updateInfo.apkUrl)) {
            return;
        }
        Browser.openUrl(getParentActivity(), updateInfo.apkUrl);
    }

    private void downloadLatestApk() {
        if (updateInfo == null || TextUtils.isEmpty(updateInfo.apkUrl)) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramUpdateNoApk), Toast.LENGTH_SHORT).show();
            return;
        }
        if (downloadComplete && downloadedApkFile != null && downloadedApkFile.exists()) {
            installDownloadedApk();
            return;
        }
        if (downloading) {
            return;
        }
        downloading = true;
        downloadComplete = false;
        lastError = null;
        downloadedBytes = 0;
        downloadTotalBytes = updateInfo.apkSizeBytes;
        downloadSpeedBytesPerSecond = 0;
        downloadedApkFile = null;
        updateDownloadProgressViews();
        buildContent(getParentActivity(), true);
        final String apkUrl = updateInfo.apkUrl;
        final long manifestSize = updateInfo.apkSizeBytes;
        final String versionName = updateInfo.versionName;
        Utilities.globalQueue.postRunnable(() -> runApkDownload(apkUrl, versionName, manifestSize));
    }

    private void runApkDownload(String apkUrl, String versionName, long manifestSize) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            File dir = new File(ApplicationLoader.applicationContext.getCacheDir(), "updates");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "SingGram-" + (TextUtils.isEmpty(versionName) ? "latest" : versionName.replaceAll("[^A-Za-z0-9._-]", "_")) + ".apk");
            connection = (HttpURLConnection) new URL(apkUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setRequestProperty("Accept", "application/vnd.android.package-archive,*/*");
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new Exception("HTTP " + responseCode);
            }
            long total = connection.getContentLength();
            if (total <= 0) {
                total = manifestSize;
            }
            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[64 * 1024];
            long startTime = System.currentTimeMillis();
            long lastUiTime = startTime;
            long lastUiBytes = 0;
            long current = 0;
            postDownloadProgress(current, total, 0, false, null, null);
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                current += read;
                long now = System.currentTimeMillis();
                if (now - lastUiTime >= 400) {
                    long deltaBytes = current - lastUiBytes;
                    long deltaTime = Math.max(1, now - lastUiTime);
                    long speed = deltaBytes * 1000L / deltaTime;
                    postDownloadProgress(current, total, speed, false, null, null);
                    lastUiTime = now;
                    lastUiBytes = current;
                }
            }
            outputStream.flush();
            long elapsed = Math.max(1, System.currentTimeMillis() - startTime);
            long averageSpeed = current * 1000L / elapsed;
            postDownloadProgress(current, total, averageSpeed, true, file, null);
        } catch (Exception e) {
            FileLog.e(e);
            postDownloadProgress(downloadedBytes, downloadTotalBytes, 0, false, null, e.getMessage());
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception ignore) {

            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception ignore) {

            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void postDownloadProgress(long current, long total, long speed, boolean complete, File file, String error) {
        AndroidUtilities.runOnUIThread(() -> {
            if (!TextUtils.isEmpty(error)) {
                downloading = false;
                downloadComplete = false;
                lastError = error;
                if (getParentActivity() != null) {
                    Toast.makeText(getParentActivity(), error, Toast.LENGTH_LONG).show();
                }
                buildContent(getParentActivity(), true);
                return;
            }
            downloadedBytes = current;
            downloadTotalBytes = total > 0 ? total : downloadTotalBytes;
            downloadSpeedBytesPerSecond = speed;
            if (complete) {
                downloading = false;
                downloadComplete = true;
                downloadedApkFile = file;
                if (getParentActivity() != null) {
                    Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramOtaDownloadComplete), Toast.LENGTH_SHORT).show();
                }
                buildContent(getParentActivity(), true);
                installDownloadedApk();
            } else {
                updateDownloadProgressViews();
            }
        });
    }

    private void installDownloadedApk() {
        if (downloadedApkFile == null || !downloadedApkFile.exists() || getParentActivity() == null) {
            return;
        }
        AndroidUtilities.openForView(downloadedApkFile, downloadedApkFile.getName(), "application/vnd.android.package-archive", getParentActivity(), getResourceProvider(), false);
    }

    private void copyApkUrl() {
        if (updateInfo == null || TextUtils.isEmpty(updateInfo.apkUrl)) {
            return;
        }
        AndroidUtilities.addToClipboard(updateInfo.apkUrl);
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramUpdateApkCopied), Toast.LENGTH_SHORT).show();
    }

    private void openReleasePage() {
        Browser.openUrl(getParentActivity(), SingGramUpdateClient.DEFAULT_RELEASE_URL);
    }

    private void copyReleaseUrl() {
        AndroidUtilities.addToClipboard(SingGramUpdateClient.DEFAULT_RELEASE_URL);
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramUpdateReleaseCopied), Toast.LENGTH_SHORT).show();
    }

    private void copyNotes() {
        if (updateInfo == null || TextUtils.isEmpty(updateInfo.notes)) {
            return;
        }
        AndroidUtilities.addToClipboard(updateInfo.notes);
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramUpdateNotesCopied), Toast.LENGTH_SHORT).show();
    }

    private LinearLayout addSection(Context context, LinearLayout container) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(section, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 0));
        return section;
    }

    private void addHeader(Context context, LinearLayout container, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setIncludeFontPadding(false);
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(18), AndroidUtilities.dp(24), AndroidUtilities.dp(8));
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addInfoCell(Context context, LinearLayout container, String text, String value) {
        addPlainInfoRow(context, container, text, value);
    }

    private void addPlainInfoRow(Context context, LinearLayout container, String text, String value) {
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setMinimumHeight(AndroidUtilities.dp(60));
        cell.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(10), AndroidUtilities.dp(18), AndroidUtilities.dp(10));

        TextView titleView = new TextView(context);
        titleView.setText(text);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        titleView.setIncludeFontPadding(false);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        cell.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        valueView.setSingleLine(false);
        valueView.setMaxLines(2);
        valueView.setEllipsize(TextUtils.TruncateAt.END);
        valueView.setIncludeFontPadding(false);
        valueView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        valueView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        cell.addView(valueView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 5, 0, 0));

        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addActionRow(Context context, LinearLayout container, String text, String value, int icon, int colorTop, int colorBottom, boolean enabled, View.OnClickListener listener) {
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.HORIZONTAL);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setMinimumHeight(AndroidUtilities.dp(70));
        cell.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(11), AndroidUtilities.dp(18), AndroidUtilities.dp(11));
        cell.setAlpha(enabled ? 1.0f : 0.48f);

        FrameLayout iconLayout = new FrameLayout(context);
        GradientDrawable iconBackground = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] { colorTop, colorBottom });
        iconBackground.setCornerRadius(AndroidUtilities.dp(11));
        iconLayout.setBackground(iconBackground);

        ImageView iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setImageResource(icon);
        iconView.setColorFilter(Color.WHITE);
        iconLayout.addView(iconView, LayoutHelper.createFrame(23, 23, Gravity.CENTER));

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(text);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setIncludeFontPadding(false);
        titleView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitleView = new TextView(context);
        subtitleView.setText(value);
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleView.setSingleLine(false);
        subtitleView.setMaxLines(2);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        subtitleView.setIncludeFontPadding(false);
        subtitleView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        subtitleView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 5, 0, 0));

        ImageView arrowView = new ImageView(context);
        arrowView.setImageResource(R.drawable.msg_arrowright);
        arrowView.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon));
        arrowView.setAlpha(enabled && listener != null ? 0.62f : 0.0f);
        arrowView.setRotation(LocaleController.isRTL ? 180 : 0);

        if (LocaleController.isRTL) {
            cell.addView(arrowView, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL | Gravity.LEFT));
            cell.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL, 0, 0, 14, 0));
            cell.addView(iconLayout, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL | Gravity.RIGHT, 0, 0, 0, 0));
        } else {
            cell.addView(iconLayout, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL | Gravity.LEFT, 0, 0, 0, 0));
            cell.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL, 14, 0, 0, 0));
            cell.addView(arrowView, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        }

        if (enabled && listener != null) {
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            cell.setOnClickListener(listener);
        }
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addDownloadProgressCard(Context context, LinearLayout container) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), 0.08f)));
        container.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 12, 12, 12));

        downloadProgressTitle = new TextView(context);
        downloadProgressTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        downloadProgressTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        downloadProgressTitle.setTypeface(AndroidUtilities.bold());
        downloadProgressTitle.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        downloadProgressTitle.setIncludeFontPadding(false);
        card.addView(downloadProgressTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        downloadProgressSubtitle = new TextView(context);
        downloadProgressSubtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        downloadProgressSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        downloadProgressSubtitle.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        downloadProgressSubtitle.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        downloadProgressSubtitle.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(10));
        card.addView(downloadProgressSubtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        downloadProgressView = new LineProgressView(context);
        downloadProgressView.setBackColor(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), 0.16f));
        downloadProgressView.setProgressColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        card.addView(downloadProgressView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 6));
        updateDownloadProgressViews();
    }

    private void updateDownloadProgressViews() {
        if (downloadProgressTitle == null || downloadProgressSubtitle == null || downloadProgressView == null) {
            return;
        }
        downloadProgressTitle.setText(downloadProgressTitleValue());
        downloadProgressSubtitle.setText(downloadStatusValue());
        downloadProgressView.setProgress(downloadProgress(), true);
    }

    private String downloadProgressTitleValue() {
        if (downloadComplete) {
            return LocaleController.getString(R.string.SingGramOtaDownloadComplete);
        }
        if (downloading) {
            return LocaleController.getString(R.string.SingGramOtaDownloading);
        }
        return LocaleController.getString(R.string.SingGramOtaDownloadReady);
    }

    private String downloadStatusValue() {
        if (downloadComplete) {
            return LocaleController.getString(R.string.SingGramOtaInstallDownloadedInfo);
        }
        long total = downloadTotalBytes > 0 ? downloadTotalBytes : updateInfo == null ? 0 : updateInfo.apkSizeBytes;
        String percent = total > 0 ? String.format(Locale.US, "%.1f%%", Math.min(100.0f, downloadedBytes * 100.0f / total)) : "-";
        String size = total > 0
                ? AndroidUtilities.formatFileSize(downloadedBytes) + " / " + AndroidUtilities.formatFileSize(total)
                : AndroidUtilities.formatFileSize(downloadedBytes);
        String speed = formatMbps(downloadSpeedBytesPerSecond);
        if (downloading) {
            return LocaleController.formatString(R.string.SingGramOtaDownloadProgressValue, percent, size, speed);
        }
        return LocaleController.getString(R.string.SingGramOtaDownloadReadyInfo);
    }

    private float downloadProgress() {
        long total = downloadTotalBytes > 0 ? downloadTotalBytes : updateInfo == null ? 0 : updateInfo.apkSizeBytes;
        if (downloadComplete) {
            return 1.0f;
        }
        if (total <= 0) {
            return downloading ? 0.08f : 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, downloadedBytes / (float) total));
    }

    private String formatMbps(long bytesPerSecond) {
        if (bytesPerSecond <= 0) {
            return "0 Mbps";
        }
        return String.format(Locale.US, "%.2f Mbps", bytesPerSecond * 8.0 / 1000.0 / 1000.0);
    }

    private void addInfoBlock(Context context, LinearLayout container, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addDivider(Context context, LinearLayout container) {
        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 16, 0, 16, 0));
    }

    private void addActionDivider(Context context, LinearLayout container) {
        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 68, 0, 16, 0));
    }

    private void addInfo(Context context, LinearLayout container, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(2));
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return new ArrayList<>();
    }
}
