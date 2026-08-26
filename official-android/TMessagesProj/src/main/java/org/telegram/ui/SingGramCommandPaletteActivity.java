package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import org.telegram.messenger.SingGramBackupBundle;
import org.telegram.messenger.SingGramChatNotesStore;
import org.telegram.messenger.SingGramConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class SingGramCommandPaletteActivity extends BaseFragment {

    private LinearLayout listSection;
    private EditTextBoldCursor searchField;

    private static class Command {
        final String title;
        final String value;
        final Runnable action;

        Command(String title, String value, Runnable action) {
            this.title = title;
            this.value = value;
            this.action = action;
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramCommandPalette));
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

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        ((FrameLayout) fragmentView).addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(28));
        scrollView.addView(container, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout searchSection = addSection(context, container);
        searchField = addSearchField(context, searchSection);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshCommands();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        addHeader(context, container, LocaleController.getString(R.string.SingGramCommandPaletteActions));
        listSection = addSection(context, container);
        refreshCommands();
        addInfo(context, container, LocaleController.getString(R.string.SingGramCommandPaletteFootnote));
        return fragmentView;
    }

    private void refreshCommands() {
        if (listSection == null) {
            return;
        }
        listSection.removeAllViews();
        String query = searchField == null ? "" : searchField.getText().toString().trim().toLowerCase();
        ArrayList<Command> commands = buildCommands();
        boolean added = false;
        for (Command command : commands) {
            String haystack = (command.title + " " + command.value).toLowerCase();
            if (!TextUtils.isEmpty(query) && !haystack.contains(query)) {
                continue;
            }
            if (added) {
                addDivider(listSection.getContext(), listSection);
            }
            addCommandCell(listSection.getContext(), listSection, command);
            added = true;
        }
        if (!added) {
            addInfoCell(listSection.getContext(), listSection, LocaleController.getString(R.string.SingGramCommandPaletteNoResults), "");
        }
    }

    private ArrayList<Command> buildCommands() {
        ArrayList<Command> commands = new ArrayList<>();
        commands.add(new Command(LocaleController.getString(R.string.SingGramFeatureHub), LocaleController.getString(R.string.SingGramFeatureHubInfo), () -> presentFragment(new SingGramFeatureHubActivity())));
        commands.add(new Command(LocaleController.getString(R.string.SingGramDoctor), LocaleController.getString(R.string.SingGramDoctorInfo), () -> presentFragment(new SingGramDoctorActivity())));
        commands.add(new Command(LocaleController.getString(R.string.SingGramAccountOverview), LocaleController.getString(R.string.SingGramAccountOverviewInfo), () -> presentFragment(new SingGramAccountOverviewActivity())));
        commands.add(new Command(LocaleController.getString(R.string.SingGramAccountProfiles), LocaleController.getString(R.string.SingGramAccountProfilesInfo), () -> presentFragment(new SingGramAccountProfilesActivity())));
        commands.add(new Command(LocaleController.getString(R.string.SingGramChatNotesAll), LocaleController.formatString(R.string.SingGramChatNotesAllCount, SingGramChatNotesStore.getNotesCount()), () -> presentFragment(new SingGramChatNotesListActivity())));
        commands.add(new Command(LocaleController.getString(R.string.SingGramWorkspace), LocaleController.getString(R.string.SingGramWorkspaceSummaryShort), () -> presentFragment(SingGramSettingsActivity.workspacePage())));
        commands.add(new Command(LocaleController.getString(R.string.SingGramCallHealth), LocaleController.getString(R.string.SingGramCallHealthSummary), () -> presentFragment(SingGramSettingsActivity.callHealthPage())));
        commands.add(new Command(LocaleController.getString(R.string.SingGramGhostManager), LocaleController.formatString(R.string.SingGramGhostManagerSummary, SingGramConfig.getGhostDialogCount(), SingGramConfig.getReadReceiptsAllowedDialogCount()), () -> presentFragment(new SingGramGhostManagerActivity())));
        commands.add(new Command(LocaleController.getString(R.string.SingGramDownloadStatus), LocaleController.getString(R.string.SingGramDownloadStatusInfo), () -> presentFragment(new SingGramDownloadStatusActivity())));
        commands.add(new Command(LocaleController.getString(R.string.SingGramLiquidGlassStudio), LocaleController.getString(R.string.SingGramLiquidGlassStudioInfo), () -> presentFragment(new SingGramLiquidGlassStudioActivity())));
        commands.add(new Command(LocaleController.getString(R.string.SingGramEventLog), LocaleController.getString(R.string.SingGramMessageProtectionInfo), () -> presentFragment(new SingGramEventLogActivity())));
        commands.add(new Command(LocaleController.getString(R.string.SingGramBackupBundle), LocaleController.getString(R.string.SingGramBackupBundleInfo), this::copyBackupBundle));
        commands.add(new Command(LocaleController.getString(R.string.SingGramGhostModeEnable), stateValue(SingGramConfig.isGhostModeEnabled()), () -> toggle(() -> SingGramConfig.setGhostModeEnabled(!SingGramConfig.isGhostModeEnabled()))));
        commands.add(new Command(LocaleController.getString(R.string.SingGramLiquidGlassEnable), stateValue(SingGramConfig.isLiquidGlassEnabled()), () -> toggle(() -> SingGramConfig.setLiquidGlassEnabled(!SingGramConfig.isLiquidGlassEnabled()))));
        commands.add(new Command(LocaleController.getString(R.string.SingGramDownloadBoost), stateValue(SingGramConfig.isDownloadBoostEnabled()), () -> toggle(() -> SingGramConfig.setDownloadBoostEnabled(!SingGramConfig.isDownloadBoostEnabled()))));
        commands.add(new Command(LocaleController.getString(R.string.SingGramAIEnableTools), stateValue(SingGramConfig.isAiEnabled()), () -> toggle(() -> SingGramConfig.setAiEnabled(!SingGramConfig.isAiEnabled()))));
        commands.add(new Command(LocaleController.getString(R.string.SingGramCrashSafeMode), stateValue(SingGramConfig.isCrashSafeModeEnabled()), () -> toggle(() -> {
            boolean enabled = !SingGramConfig.isCrashSafeModeEnabled();
            SingGramConfig.setCrashSafeModeEnabled(enabled);
            if (!enabled) {
                SingGramConfig.clearLastCrash();
            }
        })));
        return commands;
    }

    private String stateValue(boolean enabled) {
        return LocaleController.getString(enabled ? R.string.SingGramCommandPaletteOn : R.string.SingGramCommandPaletteOff);
    }

    private void toggle(Runnable action) {
        action.run();
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramSettingsSaved), Toast.LENGTH_SHORT).show();
        refreshCommands();
    }

    private void copyBackupBundle() {
        AndroidUtilities.addToClipboard(SingGramBackupBundle.exportBundle());
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramSettingsExported), Toast.LENGTH_SHORT).show();
    }

    private EditTextBoldCursor addSearchField(Context context, LinearLayout container) {
        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        editText.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        editText.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setHint(LocaleController.getString(R.string.SingGramCommandPaletteSearchHint));
        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52));
        return editText;
    }

    private void addCommandCell(Context context, LinearLayout container, Command command) {
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValue(command.title, command.value, true, false);
        cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        cell.setOnClickListener(v -> command.action.run());
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
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
        TextView textView = new TextView(context);
        textView.setText(TextUtils.isEmpty(value) ? text : text + "\n" + value);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
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
