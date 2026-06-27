package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SingGramConfig;
import org.telegram.messenger.SingGramDownloadStats;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class SingGramDownloadStatusActivity extends BaseFragment {

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramDownloadStatus));
        actionBar.setAllowOverlayTitle(true);
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        SingGramDownloadStats.Snapshot snapshot = SingGramDownloadStats.getSnapshot();

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        ((FrameLayout) fragmentView).addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(28));
        scrollView.addView(container, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        addHeroCard(context, container, snapshot);

        addHeader(context, container, LocaleController.getString(R.string.SingGramDownloadStatusLive));
        LinearLayout liveSection = addSection(context, container);
        addInfoCell(context, liveSection, LocaleController.getString(R.string.SingGramDownloadStatusActive), LocaleController.formatString(R.string.SingGramDownloadStatusActiveValue, snapshot.activeCount, speedValue(snapshot.speedBytesPerSecond)));
        addDivider(context, liveSection);
        addInfoCell(context, liveSection, LocaleController.getString(R.string.SingGramDownloadCenterThroughput), speedValue(snapshot.speedBytesPerSecond));
        addDivider(context, liveSection);
        addInfoCell(context, liveSection, LocaleController.getString(R.string.SingGramDownloadCenterTracked), LocaleController.formatPluralString("items", snapshot.items.size()));

        addHeader(context, container, LocaleController.getString(R.string.SingGramDownloadCenterQueue));
        LinearLayout queueSection = addSection(context, container);
        addInfoCell(context, queueSection, LocaleController.getString(R.string.SingGramDownloadBoost), boostValue(snapshot));
        addDivider(context, queueSection);
        addInfoCell(context, queueSection, LocaleController.getString(R.string.SingGramDownloadAutoReason), autoReasonValue(snapshot));
        addDivider(context, queueSection);
        addInfoCell(context, queueSection, LocaleController.getString(R.string.SingGramDownloadStatusConcurrency), concurrencyValue());
        addDivider(context, queueSection);
        addInfoCell(context, queueSection, LocaleController.getString(R.string.SingGramDownloadCenterLimits), LocaleController.getString(R.string.SingGramDownloadBoostFootnote));

        addHeader(context, container, LocaleController.getString(R.string.SingGramDownloadBoostMode));
        LinearLayout modeSection = addSection(context, container);
        addBoostModeCell(context, modeSection, LocaleController.getString(R.string.SingGramDownloadBoostOff), LocaleController.getString(R.string.SingGramDownloadThreadsDefault), -1);
        addDivider(context, modeSection);
        addBoostModeCell(context, modeSection, LocaleController.getString(R.string.SingGramDownloadBoostAuto), LocaleController.getString(R.string.SingGramDownloadAutoThreads), 3);
        addDivider(context, modeSection);
        addBoostModeCell(context, modeSection, LocaleController.getString(R.string.SingGramDownloadBoostBalanced), LocaleController.getString(R.string.SingGramDownloadBoostBalancedInfo), 0);
        addDivider(context, modeSection);
        addBoostModeCell(context, modeSection, LocaleController.getString(R.string.SingGramDownloadBoostAggressive), LocaleController.getString(R.string.SingGramDownloadBoostAggressiveInfo), 1);
        addDivider(context, modeSection);
        addBoostModeCell(context, modeSection, LocaleController.getString(R.string.SingGramDownloadBoostMaximum), LocaleController.getString(R.string.SingGramDownloadBoostMaximumInfo), 2);

        addHeader(context, container, LocaleController.getString(R.string.SingGramDownloadStatusRecent));
        LinearLayout recentSection = addSection(context, container);
        if (snapshot.items.isEmpty()) {
            addInfoCell(context, recentSection, LocaleController.getString(R.string.SingGramDownloadStatusEmpty), "");
        } else {
            boolean added = false;
            for (SingGramDownloadStats.ItemSnapshot item : snapshot.items) {
                if (added) {
                    addDivider(context, recentSection);
                }
                addInfoCell(context, recentSection, shortFileName(item.fileName), itemValue(item));
                added = true;
            }
        }

        addHeader(context, container, LocaleController.getString(R.string.SingGramDownloadCenterActions));
        LinearLayout actionsSection = addSection(context, container);
        addActionCell(context, actionsSection, LocaleController.getString(R.string.SingGramDownloadStatusRefresh), LocaleController.getString(R.string.SingGramDownloadStatusRefreshInfo), true, v -> refresh());
        addDivider(context, actionsSection);
        addActionCell(context, actionsSection, LocaleController.getString(R.string.SingGramDownloadStatusCopy), LocaleController.getString(R.string.SingGramDownloadStatusCopyInfo), true, v -> copyStatus());
        addDivider(context, actionsSection);
        addActionCell(context, actionsSection, LocaleController.getString(R.string.SingGramDownloadCenterClearRecent), LocaleController.getString(R.string.SingGramDownloadCenterClearRecentInfo), !snapshot.items.isEmpty(), v -> clearRecent());

        addInfo(context, container, LocaleController.getString(R.string.SingGramDownloadStatusInfo));
        return fragmentView;
    }

    private void addHeroCard(Context context, LinearLayout container, SingGramDownloadStats.Snapshot snapshot) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(16));
        card.setBackground(downloadHeroBackground());
        container.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 4));

        TextView eyebrow = new TextView(context);
        eyebrow.setText(LocaleController.getString(R.string.SingGramDownloadCenter));
        eyebrow.setTextColor(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), 0.90f));
        eyebrow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        eyebrow.setTypeface(AndroidUtilities.bold());
        eyebrow.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        eyebrow.setIncludeFontPadding(false);
        card.addView(eyebrow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText(snapshot.activeCount > 0 ? LocaleController.getString(R.string.SingGramDownloadCenterActiveTitle) : LocaleController.getString(R.string.SingGramDownloadCenterIdleTitle));
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        title.setIncludeFontPadding(false);
        title.setPadding(0, AndroidUtilities.dp(8), 0, 0);
        card.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText(LocaleController.formatString(R.string.SingGramDownloadCenterHeroInfo, snapshot.activeCount, speedValue(snapshot.speedBytesPerSecond), downloadLevelName()));
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        subtitle.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        subtitle.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(14));
        card.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        addHeroMetric(context, row, LocaleController.getString(R.string.SingGramDownloadStatusActive), String.valueOf(snapshot.activeCount));
        addHeroMetric(context, row, LocaleController.getString(R.string.SingGramDownloadCenterSpeed), speedValue(snapshot.speedBytesPerSecond));
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

    private GradientDrawable downloadHeroBackground() {
        int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
        int white = Theme.getColor(Theme.key_windowBackgroundWhite);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {
                Theme.multAlpha(accent, SingGramConfig.isLiquidGlassEnabled() ? 0.24f : 0.14f),
                Theme.multAlpha(white, SingGramConfig.isLiquidGlassEnabled() ? 0.92f : 0.84f),
                Theme.multAlpha(0xFF18A999, SingGramConfig.isLiquidGlassEnabled() ? 0.18f : 0.10f)
        });
        drawable.setCornerRadius(AndroidUtilities.dp(SingGramConfig.isLiquidGlassEnabled() ? 18 : 12));
        drawable.setStroke(AndroidUtilities.dp(1), Theme.multAlpha(accent, SingGramConfig.isLiquidGlassEnabled() ? 0.20f : 0.12f));
        return drawable;
    }

    private String boostValue(SingGramDownloadStats.Snapshot snapshot) {
        String state = LocaleController.getString(SingGramConfig.isDownloadBoostEnabled() ? R.string.SingGramCommandPaletteOn : R.string.SingGramCommandPaletteOff);
        return LocaleController.formatString(R.string.SingGramDownloadStatusSummary, state, downloadLevelName(), snapshot.activeCount, speedValue(snapshot.speedBytesPerSecond));
    }

    private String concurrencyValue() {
        return LocaleController.formatString(
                R.string.SingGramDownloadStatusConcurrencyValue,
                SingGramConfig.getBoostedSmallQueueMaxActiveOperations(5),
                SingGramConfig.getBoostedLargeQueueMaxActiveOperations(2),
                SingGramConfig.getBoostedDownloadRequestCount(4)
        );
    }

    private String itemValue(SingGramDownloadStats.ItemSnapshot item) {
        String state;
        if (item.completed) {
            state = LocaleController.getString(R.string.SingGramDownloadStatusDone);
        } else if (item.failed) {
            state = LocaleController.getString(R.string.SingGramDownloadStatusFailed);
        } else if (item.active) {
            state = LocaleController.getString(R.string.SingGramDownloadStatusActiveState);
        } else {
            state = LocaleController.getString(R.string.SingGramDownloadStatusRecentState);
        }
        return LocaleController.formatString(R.string.SingGramDownloadStatusItemValue, state, progressValue(item), speedValue(item.active ? item.speedBytesPerSecond : 0));
    }

    private String progressValue(SingGramDownloadStats.ItemSnapshot item) {
        if (item.totalSize > 0) {
            int progress = (int) Math.min(100, Math.max(0, item.downloadedSize * 100L / item.totalSize));
            return AndroidUtilities.formatFileSize(item.downloadedSize) + " / " + AndroidUtilities.formatFileSize(item.totalSize) + " (" + progress + "%)";
        }
        return AndroidUtilities.formatFileSize(item.downloadedSize);
    }

    private String speedValue(long bytesPerSecond) {
        return AndroidUtilities.formatFileSize(Math.max(0, bytesPerSecond)) + "/s";
    }

    private String downloadLevelName() {
        int level = SingGramConfig.getEffectiveDownloadBoostLevel();
        if (level <= 0) {
            return LocaleController.getString(R.string.SingGramDownloadBoostBalanced);
        }
        if (level >= 2) {
            return LocaleController.getString(R.string.SingGramDownloadBoostMaximum);
        }
        return LocaleController.getString(R.string.SingGramDownloadBoostAggressive);
    }

    private String autoReasonValue(SingGramDownloadStats.Snapshot snapshot) {
        if (!SingGramConfig.isDownloadBoostEnabled()) {
            return LocaleController.getString(R.string.SingGramDownloadAutoReasonOff);
        }
        if (!SingGramConfig.isDownloadBoostAutoEnabled()) {
            return LocaleController.formatString(R.string.SingGramDownloadAutoReasonManual, downloadLevelName());
        }
        if (snapshot.activeCount >= 3 || snapshot.speedBytesPerSecond >= 4L * 1024L * 1024L) {
            return LocaleController.formatString(R.string.SingGramDownloadAutoReasonValue, downloadLevelName(), snapshot.activeCount, speedValue(snapshot.speedBytesPerSecond), LocaleController.getString(R.string.SingGramDownloadAutoReasonHigh));
        }
        if (snapshot.activeCount >= 2 || snapshot.speedBytesPerSecond >= 1024L * 1024L) {
            return LocaleController.formatString(R.string.SingGramDownloadAutoReasonValue, downloadLevelName(), snapshot.activeCount, speedValue(snapshot.speedBytesPerSecond), LocaleController.getString(R.string.SingGramDownloadAutoReasonMedium));
        }
        return LocaleController.formatString(R.string.SingGramDownloadAutoReasonValue, downloadLevelName(), snapshot.activeCount, speedValue(snapshot.speedBytesPerSecond), LocaleController.getString(R.string.SingGramDownloadAutoReasonLow));
    }

    private void addBoostModeCell(Context context, LinearLayout container, String text, String value, int mode) {
        boolean selected;
        if (mode < 0) {
            selected = !SingGramConfig.isDownloadBoostEnabled();
        } else if (mode == 3) {
            selected = SingGramConfig.isDownloadBoostEnabled() && SingGramConfig.isDownloadBoostAutoEnabled();
        } else {
            selected = SingGramConfig.isDownloadBoostEnabled() && !SingGramConfig.isDownloadBoostAutoEnabled() && SingGramConfig.getDownloadBoostLevel() == mode;
        }
        String displayValue = selected ? LocaleController.getString(R.string.SingGramCurrentSelection) + " / " + value : value;
        addActionCell(context, container, text, displayValue, true, v -> setBoostMode(mode));
    }

    private void setBoostMode(int mode) {
        if (mode < 0) {
            SingGramConfig.setDownloadBoostEnabled(false);
        } else {
            SingGramConfig.setDownloadBoostEnabled(true);
            SingGramConfig.setDownloadBoostAutoEnabled(mode == 3);
            if (mode >= 0 && mode <= 2) {
                SingGramConfig.setDownloadBoostLevel(mode);
            }
        }
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramDownloadBoostChanged), Toast.LENGTH_SHORT).show();
        refresh();
    }

    private String shortFileName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return LocaleController.getString(R.string.SingGramDownloadStatusUnknownFile);
        }
        int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        String value = slash >= 0 && slash + 1 < fileName.length() ? fileName.substring(slash + 1) : fileName;
        if (value.length() > 52) {
            return "..." + value.substring(value.length() - 49);
        }
        return value;
    }

    private void copyStatus() {
        SingGramDownloadStats.Snapshot snapshot = SingGramDownloadStats.getSnapshot();
        StringBuilder builder = new StringBuilder();
        builder.append("SingGram download status\n");
        builder.append("boost: ").append(SingGramConfig.isDownloadBoostEnabled()).append('\n');
        builder.append("boost_auto: ").append(SingGramConfig.isDownloadBoostAutoEnabled()).append('\n');
        builder.append("boost_level: ").append(SingGramConfig.getDownloadBoostLevel()).append('\n');
        builder.append("boost_effective_level: ").append(SingGramConfig.getEffectiveDownloadBoostLevel()).append('\n');
        builder.append("boost_reason: ").append(autoReasonValue(snapshot)).append('\n');
        builder.append("active: ").append(snapshot.activeCount).append('\n');
        builder.append("speed_bps: ").append(snapshot.speedBytesPerSecond).append('\n');
        builder.append("small_queue: ").append(SingGramConfig.getBoostedSmallQueueMaxActiveOperations(5)).append('\n');
        builder.append("large_queue: ").append(SingGramConfig.getBoostedLargeQueueMaxActiveOperations(2)).append('\n');
        builder.append("request_count: ").append(SingGramConfig.getBoostedDownloadRequestCount(4)).append('\n');
        for (SingGramDownloadStats.ItemSnapshot item : snapshot.items) {
            builder.append(item.fileName).append(" | ").append(item.downloadedSize).append('/').append(item.totalSize).append(" | ").append(item.speedBytesPerSecond).append('\n');
        }
        AndroidUtilities.addToClipboard(builder.toString());
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramDownloadStatusCopied), Toast.LENGTH_SHORT).show();
    }

    private void refresh() {
        removeSelfFromStack();
        presentFragment(new SingGramDownloadStatusActivity());
    }

    private void clearRecent() {
        SingGramDownloadStats.clear();
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramDownloadCenterCleared), Toast.LENGTH_SHORT).show();
        refresh();
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
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValue(text, value, true, false);
        cell.setEnabled(false);
        cell.setAlpha(0.86f);
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addActionCell(Context context, LinearLayout container, String text, String value, boolean enabled, View.OnClickListener listener) {
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValue(text, value, true, false);
        cell.setEnabled(enabled);
        cell.setAlpha(enabled ? 1.0f : 0.58f);
        if (enabled && listener != null) {
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            cell.setOnClickListener(listener);
        }
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addDivider(Context context, LinearLayout container) {
        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 16, 0, 16, 0));
    }

    private void addInfo(Context context, LinearLayout container, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(10), AndroidUtilities.dp(24), 0);
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        return themeDescriptions;
    }
}
