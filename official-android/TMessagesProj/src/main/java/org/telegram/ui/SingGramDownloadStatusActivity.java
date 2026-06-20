package org.telegram.ui;

import android.content.Context;
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

        addHeader(context, container, LocaleController.getString(R.string.SingGramDownloadStatusLive));
        LinearLayout liveSection = addSection(context, container);
        addInfoCell(context, liveSection, LocaleController.getString(R.string.SingGramDownloadStatusActive), LocaleController.formatString(R.string.SingGramDownloadStatusActiveValue, snapshot.activeCount, speedValue(snapshot.speedBytesPerSecond)));
        addDivider(context, liveSection);
        addActionCell(context, liveSection, LocaleController.getString(R.string.SingGramDownloadStatusRefresh), LocaleController.getString(R.string.SingGramDownloadStatusRefreshInfo), true, v -> refresh());
        addDivider(context, liveSection);
        addActionCell(context, liveSection, LocaleController.getString(R.string.SingGramDownloadStatusCopy), LocaleController.getString(R.string.SingGramDownloadStatusCopyInfo), true, v -> copyStatus());

        addHeader(context, container, LocaleController.getString(R.string.SingGramDownloadBoost));
        LinearLayout boostSection = addSection(context, container);
        addInfoCell(context, boostSection, LocaleController.getString(R.string.SingGramDownloadBoost), boostValue(snapshot));
        addDivider(context, boostSection);
        addInfoCell(context, boostSection, LocaleController.getString(R.string.SingGramDownloadStatusConcurrency), concurrencyValue());

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

        addInfo(context, container, LocaleController.getString(R.string.SingGramDownloadStatusInfo));
        return fragmentView;
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
        int level = SingGramConfig.getDownloadBoostLevel();
        if (level <= 0) {
            return LocaleController.getString(R.string.SingGramDownloadBoostBalanced);
        }
        if (level >= 2) {
            return LocaleController.getString(R.string.SingGramDownloadBoostMaximum);
        }
        return LocaleController.getString(R.string.SingGramDownloadBoostAggressive);
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
        builder.append("boost_level: ").append(SingGramConfig.getDownloadBoostLevel()).append('\n');
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
