package org.telegram.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
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
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SingGramAiClient;
import org.telegram.messenger.SingGramBackupBundle;
import org.telegram.messenger.SingGramChatNotesStore;
import org.telegram.messenger.SingGramConfig;
import org.telegram.messenger.SingGramDownloadStats;
import org.telegram.messenger.SingGramEventLog;
import org.telegram.messenger.SingGramPushDiagnostics;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class SingGramSettingsActivity extends BaseFragment {

    private static final int MODE_HOME = 0;
    private static final int MODE_ACCOUNTS = 1;
    private static final int MODE_PRIVACY = 2;
    private static final int MODE_AI = 3;
    private static final int MODE_DOWNLOADS = 4;
    private static final int MODE_APPEARANCE = 5;
    private static final int MODE_DIAGNOSTICS = 6;

    private final int mode;

    private EditTextBoldCursor baseUrlField;
    private EditTextBoldCursor apiKeyField;
    private EditTextBoldCursor modelField;
    private EditTextBoldCursor systemPromptField;
    private EditTextBoldCursor inputField;
    private TextView resultView;
    private TextCheckCell aiEnabledCell;
    private TextCheckCell liquidGlassCell;

    private interface BooleanSetter {
        void set(boolean enabled);
    }

    public SingGramSettingsActivity() {
        this(MODE_HOME);
    }

    private SingGramSettingsActivity(int mode) {
        this.mode = mode;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(getTitle());
        actionBar.setAllowOverlayTitle(true);
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    saveSettings();
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        ((FrameLayout) fragmentView).addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(28));
        scrollView.addView(container, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        buildContent(context, container);
        if (fragmentView != null) {
            return fragmentView;
        }

        addHeader(context, container, LocaleController.getString(R.string.SingGramAccount));
        LinearLayout accountSection = addSection(context, container);
        addActionCell(context, accountSection, LocaleController.getString(R.string.SingGramCommandPalette), LocaleController.getString(R.string.SingGramCommandPaletteInfo), true, v -> presentFragment(new SingGramCommandPaletteActivity()));
        addDivider(context, accountSection);
        addActionCell(context, accountSection, LocaleController.getString(R.string.SingGramDoctor), LocaleController.getString(R.string.SingGramDoctorInfo), true, v -> presentFragment(new SingGramDoctorActivity()));
        addDivider(context, accountSection);
        addActionCell(context, accountSection, LocaleController.getString(R.string.SingGramAccountOverview), LocaleController.getString(R.string.SingGramAccountOverviewInfo), true, v -> presentFragment(new SingGramAccountOverviewActivity()));
        addDivider(context, accountSection);
        addActionCell(context, accountSection, LocaleController.getString(R.string.SingGramMaxAccounts100), LocaleController.formatString(R.string.SingGramMaxAccounts100Info, UserConfig.getActivatedAccountsCount(), UserConfig.MAX_ACCOUNT_COUNT), false, null);
        addDivider(context, accountSection);
        addActionCell(context, accountSection, LocaleController.getString(R.string.SingGramAccountProfiles), LocaleController.formatString(R.string.SingGramAccountProfilesCount, UserConfig.getActivatedAccountsCount()), true, v -> presentFragment(new SingGramAccountProfilesActivity()));
        addDivider(context, accountSection);
        addActionCell(context, accountSection, LocaleController.getString(R.string.SingGramImportSettings), LocaleController.getString(R.string.SingGramBackupImportInfo), true, v -> importSingGramSettings());
        addDivider(context, accountSection);
        addActionCell(context, accountSection, LocaleController.getString(R.string.SingGramBackupBundle), LocaleController.getString(R.string.SingGramBackupBundleInfo), true, v -> exportSingGramSettings());
        addInfo(context, container, LocaleController.getString(R.string.SingGramAccountInfo));

        addHeader(context, container, LocaleController.getString(R.string.SingGramGhostMode));
        LinearLayout privacySection = addSection(context, container);
        addSwitchSetting(context, privacySection, LocaleController.getString(R.string.SingGramGhostModeEnable), LocaleController.getString(R.string.SingGramGhostModeEnableInfo), SingGramConfig.isGhostModeEnabled(), SingGramConfig::setGhostModeEnabled, false);
        addDivider(context, privacySection);
        addSwitchSetting(context, privacySection, LocaleController.getString(R.string.SingGramGhostSelectedChatsOnly), LocaleController.getString(R.string.SingGramGhostSelectedChatsOnlyInfo), SingGramConfig.isGhostSelectedChatsOnly(), SingGramConfig::setGhostSelectedChatsOnly, false);
        addDivider(context, privacySection);
        addActionCell(context, privacySection, LocaleController.getString(R.string.SingGramGhostManager), LocaleController.formatString(R.string.SingGramGhostManagerSummary, SingGramConfig.getGhostDialogCount(), SingGramConfig.getReadReceiptsAllowedDialogCount()), true, v -> presentFragment(new SingGramGhostManagerActivity()));
        addDivider(context, privacySection);
        addSwitchSetting(context, privacySection, LocaleController.getString(R.string.SingGramDisableReadReceipts), LocaleController.getString(R.string.SingGramDisableReadReceiptsInfo), SingGramConfig.isDisableReadReceiptsEnabled(), SingGramConfig::setDisableReadReceiptsEnabled, false);
        addDivider(context, privacySection);
        addSwitchSetting(context, privacySection, LocaleController.getString(R.string.SingGramHideTypingStatus), LocaleController.getString(R.string.SingGramHideTypingStatusInfo), SingGramConfig.isHideTypingStatusEnabled(), SingGramConfig::setHideTypingStatusEnabled, false);
        addDivider(context, privacySection);
        addSwitchSetting(context, privacySection, LocaleController.getString(R.string.SingGramHidePhoneInSettings), LocaleController.getString(R.string.SingGramHidePhoneInSettingsInfo), SingGramConfig.shouldHidePhoneInSettings(), SingGramConfig::setHidePhoneInSettings, false);
        addInfo(context, container, LocaleController.getString(R.string.SingGramGhostModeInfo));

        addHeader(context, container, LocaleController.getString(R.string.SingGramMessageProtection));
        LinearLayout protectionSection = addSection(context, container);
        addSwitchSetting(context, protectionSection, LocaleController.getString(R.string.SingGramKeepDeletedMessages), LocaleController.getString(R.string.SingGramKeepDeletedMessagesInfo), SingGramConfig.shouldKeepDeletedMessages(), SingGramConfig::setKeepDeletedMessages, false);
        addDivider(context, protectionSection);
        addSwitchSetting(context, protectionSection, LocaleController.getString(R.string.SingGramKeepOriginalEdits), LocaleController.getString(R.string.SingGramKeepOriginalEditsInfo), SingGramConfig.shouldKeepOriginalEdits(), SingGramConfig::setKeepOriginalEdits, false);
        addDivider(context, protectionSection);
        addActionCell(context, protectionSection, LocaleController.getString(R.string.SingGramEventLog), LocaleController.formatString(R.string.SingGramEventLogCount, SingGramEventLog.getEventCount()), true, v -> presentFragment(new SingGramEventLogActivity()));
        addInfo(context, container, LocaleController.getString(R.string.SingGramMessageProtectionInfo));

        addHeader(context, container, LocaleController.getString(R.string.SingGramChatTools));
        LinearLayout chatSection = addSection(context, container);
        addSwitchSetting(context, chatSection, LocaleController.getString(R.string.SingGramAIContextMenu), LocaleController.getString(R.string.SingGramAIContextMenuInfo), SingGramConfig.isAiContextMenuEnabled(), SingGramConfig::setAiContextMenuEnabled, false);
        addDivider(context, chatSection);
        addSwitchSetting(context, chatSection, LocaleController.getString(R.string.SingGramAITranslateAction), LocaleController.getString(R.string.SingGramAITranslateActionInfo), SingGramConfig.isAiTranslateActionEnabled(), SingGramConfig::setAiTranslateActionEnabled, false);
        addDivider(context, chatSection);
        addSwitchSetting(context, chatSection, LocaleController.getString(R.string.SingGramAIReplyIdeasSwitch), LocaleController.getString(R.string.SingGramAIReplyIdeasSwitchInfo), SingGramConfig.isQuickReplyIdeasEnabled(), SingGramConfig::setQuickReplyIdeasEnabled, false);
        addDivider(context, chatSection);
        addSwitchSetting(context, chatSection, LocaleController.getString(R.string.SingGramAIInsertResultSwitch), LocaleController.getString(R.string.SingGramAIInsertResultSwitchInfo), SingGramConfig.isAiInsertResultEnabled(), SingGramConfig::setAiInsertResultEnabled, false);
        addDivider(context, chatSection);
        addActionCell(context, chatSection, LocaleController.getString(R.string.SingGramChatNotesAll), LocaleController.formatString(R.string.SingGramChatNotesAllCount, SingGramChatNotesStore.getNotesCount()), true, v -> presentFragment(new SingGramChatNotesListActivity()));

        addHeader(context, container, LocaleController.getString(R.string.SingGramDownload));
        LinearLayout downloadSection = addSection(context, container);
        addSwitchSetting(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadBoost), LocaleController.getString(R.string.SingGramDownloadBoostInfo), SingGramConfig.isDownloadBoostEnabled(), SingGramConfig::setDownloadBoostEnabled, false);
        addDivider(context, downloadSection);
        addActionCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadStatus), downloadStatusValue(), true, v -> presentFragment(new SingGramDownloadStatusActivity()));
        addDivider(context, downloadSection);
        addDownloadBoostLevelCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadBoostBalanced), LocaleController.getString(R.string.SingGramDownloadBoostBalancedInfo), 0);
        addDivider(context, downloadSection);
        addDownloadBoostLevelCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadBoostAggressive), LocaleController.getString(R.string.SingGramDownloadBoostAggressiveInfo), 1);
        addDivider(context, downloadSection);
        addDownloadBoostLevelCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadBoostMaximum), LocaleController.getString(R.string.SingGramDownloadBoostMaximumInfo), 2);
        addInfo(context, container, LocaleController.getString(R.string.SingGramDownloadBoostFootnote));

        addHeader(context, container, LocaleController.getString(R.string.SingGramAI));
        LinearLayout aiSection = addSection(context, container);

        aiEnabledCell = addSwitchCell(context, aiSection, LocaleController.getString(R.string.SingGramAIEnableTools), SingGramConfig.isAiEnabled(), false);
        aiEnabledCell.setOnClickListener(v -> {
            boolean enabled = !aiEnabledCell.isChecked();
            aiEnabledCell.setChecked(enabled);
            SingGramConfig.setAiEnabled(enabled);
        });
        addDivider(context, aiSection);
        addSwitchSetting(context, aiSection, LocaleController.getString(R.string.SingGramAIPreferCantonese), LocaleController.getString(R.string.SingGramAIPreferCantoneseInfo), SingGramConfig.shouldAiPreferCantonese(), SingGramConfig::setAiPreferCantonese, false);
        addDivider(context, aiSection);

        baseUrlField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAIBaseUrl), "https://your-newapi.example.com", SingGramConfig.getAiBaseUrl(), false);
        addDivider(context, aiSection);
        apiKeyField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAIApiKey), "sk-...", SingGramConfig.getAiApiKey(), false);
        addDivider(context, aiSection);
        modelField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAIModel), SingGramConfig.DEFAULT_AI_MODEL, SingGramConfig.getAiModel(), false);
        addDivider(context, aiSection);
        systemPromptField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAISystemPrompt), LocaleController.getString(R.string.SingGramAISystemPromptHint), SingGramConfig.getAiSystemPrompt(), true);

        addButton(context, aiSection, LocaleController.getString(R.string.SingGramSaveButton), true, v -> {
            saveSettings();
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramSettingsSaved), Toast.LENGTH_SHORT).show();
        });
        addButton(context, aiSection, LocaleController.getString(R.string.SingGramAITestConnection), false, v -> testNewApiConnection());

        addInfo(context, container, LocaleController.getString(R.string.SingGramAIInfo));

        addHeader(context, container, LocaleController.getString(R.string.SingGramAppearance));
        LinearLayout appearanceSection = addSection(context, container);
        liquidGlassCell = addSwitchCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassEnable), SingGramConfig.isLiquidGlassEnabled(), false);
        liquidGlassCell.setOnClickListener(v -> {
            boolean enabled = !liquidGlassCell.isChecked();
            liquidGlassCell.setChecked(enabled);
            SingGramConfig.setLiquidGlassEnabled(enabled);
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramLiquidGlassChanged), Toast.LENGTH_SHORT).show();
        });
        addDivider(context, appearanceSection);
        addLiquidGlassLevelCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassSoft), LocaleController.getString(R.string.SingGramLiquidGlassSoftInfo), 0);
        addDivider(context, appearanceSection);
        addLiquidGlassLevelCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassStandard), LocaleController.getString(R.string.SingGramLiquidGlassStandardInfo), 1);
        addDivider(context, appearanceSection);
        addLiquidGlassLevelCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassStrong), LocaleController.getString(R.string.SingGramLiquidGlassStrongInfo), 2);
        addDivider(context, appearanceSection);
        addActionCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassStudio), liquidGlassStudioValue(), true, v -> presentFragment(new SingGramLiquidGlassStudioActivity()));
        addInfo(context, container, LocaleController.getString(R.string.SingGramLiquidGlassInfo));

        addHeader(context, container, LocaleController.getString(R.string.SingGramAITestLab));
        LinearLayout testSection = addSection(context, container);
        inputField = addField(context, testSection, LocaleController.getString(R.string.SingGramAIInput), LocaleController.getString(R.string.SingGramAIInputHint), "", true);
        addDivider(context, testSection);

        LinearLayout buttonRow1 = addButtonRow(context, testSection);
        addSmallButton(context, buttonRow1, LocaleController.getString(R.string.SingGramAISummarize), v -> runAction(SingGramAiClient.ACTION_SUMMARIZE));
        addSmallButton(context, buttonRow1, LocaleController.getString(R.string.SingGramAITranslate), v -> runAction(SingGramAiClient.ACTION_TRANSLATE_ZH_HANT));

        LinearLayout buttonRow2 = addButtonRow(context, testSection);
        addSmallButton(context, buttonRow2, LocaleController.getString(R.string.SingGramAIRewriteCantonese), v -> runAction(SingGramAiClient.ACTION_REWRITE_YUE));
        addSmallButton(context, buttonRow2, LocaleController.getString(R.string.SingGramAIReplyIdeas), v -> runAction(SingGramAiClient.ACTION_REPLY_SUGGESTIONS));

        LinearLayout buttonRow3 = addButtonRow(context, testSection);
        addSmallButton(context, buttonRow3, LocaleController.getString(R.string.SingGramAIShorten), v -> runAction(SingGramAiClient.ACTION_SHORTEN));
        addSmallButton(context, buttonRow3, LocaleController.getString(R.string.SingGramAIExplain), v -> runAction(SingGramAiClient.ACTION_EXPLAIN));

        LinearLayout buttonRow4 = addButtonRow(context, testSection);
        addSmallButton(context, buttonRow4, LocaleController.getString(R.string.SingGramAICleanCopy), v -> runAction(SingGramAiClient.ACTION_CLEAN_COPY));
        addSmallButton(context, buttonRow4, LocaleController.getString(R.string.SingGramAIExtractTasks), v -> runAction(SingGramAiClient.ACTION_EXTRACT_TASKS));

        LinearLayout buttonRow5 = addButtonRow(context, testSection);
        addSmallButton(context, buttonRow5, LocaleController.getString(R.string.SingGramAITranslateCantonese), v -> runAction(SingGramAiClient.ACTION_TRANSLATE_YUE));
        addSmallButton(context, buttonRow5, LocaleController.getString(R.string.SingGramAIPasteClipboard), v -> pasteClipboard());

        resultView = new TextView(context);
        resultView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        resultView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        resultView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        resultView.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        resultView.setMinHeight(AndroidUtilities.dp(96));
        resultView.setText(LocaleController.getString(R.string.SingGramAIResultPlaceholder));
        resultView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), 0.10f)));
        testSection.addView(resultView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 12, 12, 4));

        LinearLayout resultRow = addButtonRow(context, testSection);
        addSmallButton(context, resultRow, LocaleController.getString(R.string.SingGramCopyButton), v -> copyResult());
        addSmallButton(context, resultRow, LocaleController.getString(R.string.SingGramClearButton), v -> resultView.setText(LocaleController.getString(R.string.SingGramAIResultPlaceholder)));

        addHeader(context, container, LocaleController.getString(R.string.SingGramDiagnostics));
        LinearLayout diagnosticsSection = addSection(context, container);
        addSwitchSetting(context, diagnosticsSection, LocaleController.getString(R.string.SingGramCrashSafeMode), crashSafeModeValue(), SingGramConfig.isCrashSafeModeEnabled(), enabled -> {
            SingGramConfig.setCrashSafeModeEnabled(enabled);
            if (!enabled) {
                SingGramConfig.clearLastCrash();
            }
        }, false);
        addDivider(context, diagnosticsSection);
        addSwitchSetting(context, diagnosticsSection, LocaleController.getString(R.string.SingGramDiagnosticsVisible), LocaleController.getString(R.string.SingGramDiagnosticsVisibleInfo), SingGramConfig.isDiagnosticsEnabled(), SingGramConfig::setDiagnosticsEnabled, false);
        if (SingGramConfig.isDiagnosticsEnabled()) {
            addDivider(context, diagnosticsSection);
            addButton(context, diagnosticsSection, LocaleController.getString(R.string.SingGramExportLogs), false, v -> ProfileActivity.sendLogs(getParentActivity(), false));
            addButton(context, diagnosticsSection, LocaleController.getString(R.string.SingGramExportLastLogs), false, v -> ProfileActivity.sendLogs(getParentActivity(), true));
        }

        return fragmentView;
    }

    @Override
    public void onFragmentDestroy() {
        saveSettings();
        super.onFragmentDestroy();
    }

    private String getTitle() {
        switch (mode) {
            case MODE_ACCOUNTS:
                return LocaleController.getString(R.string.SingGramAccount);
            case MODE_PRIVACY:
                return LocaleController.getString(R.string.SingGramPrivacy);
            case MODE_AI:
                return LocaleController.getString(R.string.SingGramAI);
            case MODE_DOWNLOADS:
                return LocaleController.getString(R.string.SingGramDownload);
            case MODE_APPEARANCE:
                return LocaleController.getString(R.string.SingGramAppearance);
            case MODE_DIAGNOSTICS:
                return LocaleController.getString(R.string.SingGramDiagnostics);
            case MODE_HOME:
            default:
                return LocaleController.getString(R.string.SingGramSettingsTitle);
        }
    }

    private void buildContent(Context context, LinearLayout container) {
        switch (mode) {
            case MODE_ACCOUNTS:
                buildAccountsPage(context, container);
                break;
            case MODE_PRIVACY:
                buildPrivacyPage(context, container);
                break;
            case MODE_AI:
                buildAiPage(context, container);
                break;
            case MODE_DOWNLOADS:
                buildDownloadsPage(context, container);
                break;
            case MODE_APPEARANCE:
                buildAppearancePage(context, container);
                break;
            case MODE_DIAGNOSTICS:
                buildDiagnosticsPage(context, container);
                break;
            case MODE_HOME:
            default:
                buildHomePage(context, container);
                break;
        }
    }

    private void buildHomePage(Context context, LinearLayout container) {
        addHeader(context, container, LocaleController.getString(R.string.SingGramQuickSettings));
        LinearLayout quickSection = addSection(context, container);
        addSwitchSetting(context, quickSection, LocaleController.getString(R.string.SingGramGhostModeEnable), LocaleController.getString(R.string.SingGramGhostModeEnableInfo), SingGramConfig.isGhostModeEnabled(), SingGramConfig::setGhostModeEnabled, false);
        addDivider(context, quickSection);
        addSwitchSetting(context, quickSection, LocaleController.getString(R.string.SingGramAIEnableTools), LocaleController.getString(R.string.SingGramAIContextMenuInfo), SingGramConfig.isAiEnabled(), SingGramConfig::setAiEnabled, false);
        addDivider(context, quickSection);
        addSwitchSetting(context, quickSection, LocaleController.getString(R.string.SingGramDownloadBoost), LocaleController.getString(R.string.SingGramDownloadBoostInfo), SingGramConfig.isDownloadBoostEnabled(), SingGramConfig::setDownloadBoostEnabled, false);
        addDivider(context, quickSection);
        addSwitchSetting(context, quickSection, LocaleController.getString(R.string.SingGramLiquidGlassEnable), LocaleController.getString(R.string.SingGramLiquidGlassInfo), SingGramConfig.isLiquidGlassEnabled(), enabled -> {
            SingGramConfig.setLiquidGlassEnabled(enabled);
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramLiquidGlassChanged), Toast.LENGTH_SHORT).show();
        }, false);

        addHeader(context, container, LocaleController.getString(R.string.SingGramPreset));
        LinearLayout presetSection = addSection(context, container);
        addActionCell(context, presetSection, LocaleController.getString(R.string.SingGramPresetStatus), singGramPresetStatusValue(), false, null);
        addDivider(context, presetSection);
        addIconActionCell(context, presetSection, LocaleController.getString(R.string.SingGramPresetStealth), LocaleController.getString(R.string.SingGramPresetStealthInfo), R.drawable.settings_privacy, 0xFF4B8DFF, 0xFF7D4BFF, true, v -> applySingGramPreset(0));
        addDivider(context, presetSection);
        addIconActionCell(context, presetSection, LocaleController.getString(R.string.SingGramPresetDaily), LocaleController.getString(R.string.SingGramPresetDailyInfo), R.drawable.settings_features, 0xFF55CA47, 0xFF27B434, true, v -> applySingGramPreset(1));
        addDivider(context, presetSection);
        addIconActionCell(context, presetSection, LocaleController.getString(R.string.SingGramPresetPerformance), LocaleController.getString(R.string.SingGramPresetPerformanceInfo), R.drawable.settings_data, 0xFFFF8B3D, 0xFFE45644, true, v -> applySingGramPreset(2));

        addHeader(context, container, LocaleController.getString(R.string.SingGramTools));
        LinearLayout toolsSection = addSection(context, container);
        addIconActionCell(context, toolsSection, LocaleController.getString(R.string.SingGramCommandPalette), LocaleController.getString(R.string.SingGramCommandPaletteInfo), R.drawable.premium_ai_editor, 0xFF23B9C9, 0xFF2684E8, true, v -> presentFragment(new SingGramCommandPaletteActivity()));
        addDivider(context, toolsSection);
        addIconActionCell(context, toolsSection, LocaleController.getString(R.string.SingGramChatNotesAll), LocaleController.formatString(R.string.SingGramChatNotesAllCount, SingGramChatNotesStore.getNotesCount()), R.drawable.msg_addbio, 0xFF55CA47, 0xFF27B434, true, v -> presentFragment(new SingGramChatNotesListActivity()));
        addDivider(context, toolsSection);
        addIconActionCell(context, toolsSection, LocaleController.getString(R.string.SingGramUpdates), updateSummaryValue(), R.drawable.settings_features, 0xFF4EA5F6, 0xFF3577E5, true, v -> presentFragment(new SingGramUpdateActivity()));
        addDivider(context, toolsSection);
        addIconActionCell(context, toolsSection, LocaleController.getString(R.string.SingGramDoctor), LocaleController.getString(R.string.SingGramDoctorInfo), R.drawable.settings_power, 0xFFFF8B3D, 0xFFE45644, true, v -> presentFragment(new SingGramDoctorActivity()));

        addHeader(context, container, LocaleController.getString(R.string.SingGramSettingsCategories));
        LinearLayout categoriesSection = addSection(context, container);
        addIconActionCell(context, categoriesSection, LocaleController.getString(R.string.SingGramAccount), LocaleController.getString(R.string.SingGramAccountSummary), R.drawable.settings_account, 0xFF4EA5F6, 0xFF3577E5, true, v -> presentFragment(new SingGramSettingsActivity(MODE_ACCOUNTS)));
        addIconActionCell(context, categoriesSection, LocaleController.getString(R.string.SingGramPrivacy), LocaleController.getString(R.string.SingGramPrivacySummary), R.drawable.settings_privacy, 0xFF55CA47, 0xFF27B434, true, v -> presentFragment(new SingGramSettingsActivity(MODE_PRIVACY)));
        addIconActionCell(context, categoriesSection, LocaleController.getString(R.string.SingGramAI), LocaleController.getString(R.string.SingGramAiSummary), R.drawable.premium_ai_editor, 0xFF23B9C9, 0xFF2684E8, true, v -> presentFragment(new SingGramSettingsActivity(MODE_AI)));
        addIconActionCell(context, categoriesSection, LocaleController.getString(R.string.SingGramDownload), LocaleController.getString(R.string.SingGramDownloadSummary), R.drawable.settings_data, 0xFF40B7FF, 0xFF168BDE, true, v -> presentFragment(new SingGramSettingsActivity(MODE_DOWNLOADS)));
        addIconActionCell(context, categoriesSection, LocaleController.getString(R.string.SingGramAppearance), LocaleController.getString(R.string.SingGramAppearanceSummary), R.drawable.settings_chat, 0xFFB659FF, 0xFF617CFF, true, v -> presentFragment(new SingGramSettingsActivity(MODE_APPEARANCE)));
        addIconActionCell(context, categoriesSection, LocaleController.getString(R.string.SingGramDiagnostics), LocaleController.getString(R.string.SingGramDiagnosticsSummary), R.drawable.settings_devices, 0xFF8A98A7, 0xFF5D6C7B, true, v -> presentFragment(new SingGramSettingsActivity(MODE_DIAGNOSTICS)));
        addInfo(context, container, LocaleController.getString(R.string.SingGramSettingsHomeInfo));
    }

    private void buildAccountsPage(Context context, LinearLayout container) {
        addHeader(context, container, LocaleController.getString(R.string.SingGramAccount));
        LinearLayout accountSection = addSection(context, container);
        addActionCell(context, accountSection, LocaleController.getString(R.string.SingGramAccountOverview), LocaleController.getString(R.string.SingGramAccountOverviewInfo), true, v -> presentFragment(new SingGramAccountOverviewActivity()));
        addDivider(context, accountSection);
        addActionCell(context, accountSection, LocaleController.getString(R.string.SingGramMaxAccounts100), LocaleController.formatString(R.string.SingGramMaxAccounts100Info, UserConfig.getActivatedAccountsCount(), UserConfig.MAX_ACCOUNT_COUNT), false, null);
        addDivider(context, accountSection);
        addActionCell(context, accountSection, LocaleController.getString(R.string.SingGramAccountProfiles), LocaleController.formatString(R.string.SingGramAccountProfilesCount, UserConfig.getActivatedAccountsCount()), true, v -> presentFragment(new SingGramAccountProfilesActivity()));
        addDivider(context, accountSection);
        addActionCell(context, accountSection, LocaleController.getString(R.string.SingGramImportSettings), LocaleController.getString(R.string.SingGramBackupImportInfo), true, v -> importSingGramSettings());
        addDivider(context, accountSection);
        addActionCell(context, accountSection, LocaleController.getString(R.string.SingGramBackupBundle), LocaleController.getString(R.string.SingGramBackupBundleInfo), true, v -> exportSingGramSettings());
        addInfo(context, container, LocaleController.getString(R.string.SingGramAccountInfo));
    }

    private void buildPrivacyPage(Context context, LinearLayout container) {
        addHeader(context, container, LocaleController.getString(R.string.SingGramGhostMode));
        LinearLayout ghostSection = addSection(context, container);
        addSwitchSetting(context, ghostSection, LocaleController.getString(R.string.SingGramGhostModeEnable), LocaleController.getString(R.string.SingGramGhostModeEnableInfo), SingGramConfig.isGhostModeEnabled(), SingGramConfig::setGhostModeEnabled, false);
        addDivider(context, ghostSection);
        addSwitchSetting(context, ghostSection, LocaleController.getString(R.string.SingGramGhostSelectedChatsOnly), LocaleController.getString(R.string.SingGramGhostSelectedChatsOnlyInfo), SingGramConfig.isGhostSelectedChatsOnly(), SingGramConfig::setGhostSelectedChatsOnly, false);
        addDivider(context, ghostSection);
        addActionCell(context, ghostSection, LocaleController.getString(R.string.SingGramGhostManager), LocaleController.formatString(R.string.SingGramGhostManagerSummary, SingGramConfig.getGhostDialogCount(), SingGramConfig.getReadReceiptsAllowedDialogCount()), true, v -> presentFragment(new SingGramGhostManagerActivity()));
        addInfo(context, container, LocaleController.getString(R.string.SingGramGhostModeInfo));

        addHeader(context, container, LocaleController.getString(R.string.SingGramPrivacy));
        LinearLayout privacySection = addSection(context, container);
        addSwitchSetting(context, privacySection, LocaleController.getString(R.string.SingGramDisableReadReceipts), LocaleController.getString(R.string.SingGramDisableReadReceiptsInfo), SingGramConfig.isDisableReadReceiptsEnabled(), SingGramConfig::setDisableReadReceiptsEnabled, false);
        addDivider(context, privacySection);
        addSwitchSetting(context, privacySection, LocaleController.getString(R.string.SingGramHideTypingStatus), LocaleController.getString(R.string.SingGramHideTypingStatusInfo), SingGramConfig.isHideTypingStatusEnabled(), SingGramConfig::setHideTypingStatusEnabled, false);
        addDivider(context, privacySection);
        addSwitchSetting(context, privacySection, LocaleController.getString(R.string.SingGramHidePhoneInSettings), LocaleController.getString(R.string.SingGramHidePhoneInSettingsInfo), SingGramConfig.shouldHidePhoneInSettings(), SingGramConfig::setHidePhoneInSettings, false);

        addHeader(context, container, LocaleController.getString(R.string.SingGramMessageProtection));
        LinearLayout protectionSection = addSection(context, container);
        addSwitchSetting(context, protectionSection, LocaleController.getString(R.string.SingGramKeepDeletedMessages), LocaleController.getString(R.string.SingGramKeepDeletedMessagesInfo), SingGramConfig.shouldKeepDeletedMessages(), SingGramConfig::setKeepDeletedMessages, false);
        addDivider(context, protectionSection);
        addSwitchSetting(context, protectionSection, LocaleController.getString(R.string.SingGramKeepOriginalEdits), LocaleController.getString(R.string.SingGramKeepOriginalEditsInfo), SingGramConfig.shouldKeepOriginalEdits(), SingGramConfig::setKeepOriginalEdits, false);
        addDivider(context, protectionSection);
        addActionCell(context, protectionSection, LocaleController.getString(R.string.SingGramEventLog), LocaleController.formatString(R.string.SingGramEventLogCount, SingGramEventLog.getEventCount()), true, v -> presentFragment(new SingGramEventLogActivity()));
        addInfo(context, container, LocaleController.getString(R.string.SingGramMessageProtectionInfo));
    }

    private void buildAiPage(Context context, LinearLayout container) {
        addHeader(context, container, LocaleController.getString(R.string.SingGramChatTools));
        LinearLayout chatSection = addSection(context, container);
        addSwitchSetting(context, chatSection, LocaleController.getString(R.string.SingGramAIContextMenu), LocaleController.getString(R.string.SingGramAIContextMenuInfo), SingGramConfig.isAiContextMenuEnabled(), SingGramConfig::setAiContextMenuEnabled, false);
        addDivider(context, chatSection);
        addSwitchSetting(context, chatSection, LocaleController.getString(R.string.SingGramAITranslateAction), LocaleController.getString(R.string.SingGramAITranslateActionInfo), SingGramConfig.isAiTranslateActionEnabled(), SingGramConfig::setAiTranslateActionEnabled, false);
        addDivider(context, chatSection);
        addSwitchSetting(context, chatSection, LocaleController.getString(R.string.SingGramAIReplyIdeasSwitch), LocaleController.getString(R.string.SingGramAIReplyIdeasSwitchInfo), SingGramConfig.isQuickReplyIdeasEnabled(), SingGramConfig::setQuickReplyIdeasEnabled, false);
        addDivider(context, chatSection);
        addSwitchSetting(context, chatSection, LocaleController.getString(R.string.SingGramAIInsertResultSwitch), LocaleController.getString(R.string.SingGramAIInsertResultSwitchInfo), SingGramConfig.isAiInsertResultEnabled(), SingGramConfig::setAiInsertResultEnabled, false);
        addDivider(context, chatSection);
        addActionCell(context, chatSection, LocaleController.getString(R.string.SingGramChatNotesAll), LocaleController.formatString(R.string.SingGramChatNotesAllCount, SingGramChatNotesStore.getNotesCount()), true, v -> presentFragment(new SingGramChatNotesListActivity()));

        addHeader(context, container, LocaleController.getString(R.string.SingGramAI));
        LinearLayout aiSection = addSection(context, container);
        aiEnabledCell = addSwitchCell(context, aiSection, LocaleController.getString(R.string.SingGramAIEnableTools), SingGramConfig.isAiEnabled(), false);
        aiEnabledCell.setOnClickListener(v -> {
            boolean enabled = !aiEnabledCell.isChecked();
            aiEnabledCell.setChecked(enabled);
            SingGramConfig.setAiEnabled(enabled);
        });
        addDivider(context, aiSection);
        addSwitchSetting(context, aiSection, LocaleController.getString(R.string.SingGramAIPreferCantonese), LocaleController.getString(R.string.SingGramAIPreferCantoneseInfo), SingGramConfig.shouldAiPreferCantonese(), SingGramConfig::setAiPreferCantonese, false);
        addDivider(context, aiSection);
        baseUrlField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAIBaseUrl), "https://your-newapi.example.com", SingGramConfig.getAiBaseUrl(), false);
        addDivider(context, aiSection);
        apiKeyField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAIApiKey), "sk-...", SingGramConfig.getAiApiKey(), false);
        addDivider(context, aiSection);
        modelField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAIModel), SingGramConfig.DEFAULT_AI_MODEL, SingGramConfig.getAiModel(), false);
        addDivider(context, aiSection);
        systemPromptField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAISystemPrompt), LocaleController.getString(R.string.SingGramAISystemPromptHint), SingGramConfig.getAiSystemPrompt(), true);
        addButton(context, aiSection, LocaleController.getString(R.string.SingGramSaveButton), true, v -> {
            saveSettings();
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramSettingsSaved), Toast.LENGTH_SHORT).show();
        });
        addButton(context, aiSection, LocaleController.getString(R.string.SingGramAITestConnection), false, v -> testNewApiConnection());
        addInfo(context, container, LocaleController.getString(R.string.SingGramAIInfo));

        addHeader(context, container, LocaleController.getString(R.string.SingGramAITestLab));
        LinearLayout testSection = addSection(context, container);
        inputField = addField(context, testSection, LocaleController.getString(R.string.SingGramAIInput), LocaleController.getString(R.string.SingGramAIInputHint), "", true);
        addDivider(context, testSection);

        LinearLayout buttonRow1 = addButtonRow(context, testSection);
        addSmallButton(context, buttonRow1, LocaleController.getString(R.string.SingGramAISummarize), v -> runAction(SingGramAiClient.ACTION_SUMMARIZE));
        addSmallButton(context, buttonRow1, LocaleController.getString(R.string.SingGramAITranslate), v -> runAction(SingGramAiClient.ACTION_TRANSLATE_ZH_HANT));

        LinearLayout buttonRow2 = addButtonRow(context, testSection);
        addSmallButton(context, buttonRow2, LocaleController.getString(R.string.SingGramAIRewriteCantonese), v -> runAction(SingGramAiClient.ACTION_REWRITE_YUE));
        addSmallButton(context, buttonRow2, LocaleController.getString(R.string.SingGramAIReplyIdeas), v -> runAction(SingGramAiClient.ACTION_REPLY_SUGGESTIONS));

        LinearLayout buttonRow3 = addButtonRow(context, testSection);
        addSmallButton(context, buttonRow3, LocaleController.getString(R.string.SingGramAIShorten), v -> runAction(SingGramAiClient.ACTION_SHORTEN));
        addSmallButton(context, buttonRow3, LocaleController.getString(R.string.SingGramAIExplain), v -> runAction(SingGramAiClient.ACTION_EXPLAIN));

        LinearLayout buttonRow4 = addButtonRow(context, testSection);
        addSmallButton(context, buttonRow4, LocaleController.getString(R.string.SingGramAICleanCopy), v -> runAction(SingGramAiClient.ACTION_CLEAN_COPY));
        addSmallButton(context, buttonRow4, LocaleController.getString(R.string.SingGramAIExtractTasks), v -> runAction(SingGramAiClient.ACTION_EXTRACT_TASKS));

        LinearLayout buttonRow5 = addButtonRow(context, testSection);
        addSmallButton(context, buttonRow5, LocaleController.getString(R.string.SingGramAITranslateCantonese), v -> runAction(SingGramAiClient.ACTION_TRANSLATE_YUE));
        addSmallButton(context, buttonRow5, LocaleController.getString(R.string.SingGramAIPasteClipboard), v -> pasteClipboard());

        resultView = new TextView(context);
        resultView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        resultView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        resultView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        resultView.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        resultView.setMinHeight(AndroidUtilities.dp(96));
        resultView.setText(LocaleController.getString(R.string.SingGramAIResultPlaceholder));
        resultView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), 0.10f)));
        testSection.addView(resultView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 12, 12, 4));

        LinearLayout resultRow = addButtonRow(context, testSection);
        addSmallButton(context, resultRow, LocaleController.getString(R.string.SingGramCopyButton), v -> copyResult());
        addSmallButton(context, resultRow, LocaleController.getString(R.string.SingGramClearButton), v -> resultView.setText(LocaleController.getString(R.string.SingGramAIResultPlaceholder)));
    }

    private void buildDownloadsPage(Context context, LinearLayout container) {
        addHeader(context, container, LocaleController.getString(R.string.SingGramDownload));
        LinearLayout downloadSection = addSection(context, container);
        addSwitchSetting(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadBoost), LocaleController.getString(R.string.SingGramDownloadBoostInfo), SingGramConfig.isDownloadBoostEnabled(), SingGramConfig::setDownloadBoostEnabled, false);
        addDivider(context, downloadSection);
        addActionCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadStatus), downloadStatusValue(), true, v -> presentFragment(new SingGramDownloadStatusActivity()));
        addDivider(context, downloadSection);
        addDownloadBoostLevelCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadBoostBalanced), LocaleController.getString(R.string.SingGramDownloadBoostBalancedInfo), 0);
        addDivider(context, downloadSection);
        addDownloadBoostLevelCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadBoostAggressive), LocaleController.getString(R.string.SingGramDownloadBoostAggressiveInfo), 1);
        addDivider(context, downloadSection);
        addDownloadBoostLevelCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadBoostMaximum), LocaleController.getString(R.string.SingGramDownloadBoostMaximumInfo), 2);
        addInfo(context, container, LocaleController.getString(R.string.SingGramDownloadBoostFootnote));
    }

    private void buildAppearancePage(Context context, LinearLayout container) {
        addHeader(context, container, LocaleController.getString(R.string.SingGramAppearance));
        LinearLayout appearanceSection = addSection(context, container);
        liquidGlassCell = addSwitchSetting(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassEnable), liquidGlassStatusValue(), SingGramConfig.isLiquidGlassEnabled(), enabled -> {
            SingGramConfig.setLiquidGlassEnabled(enabled);
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramLiquidGlassChanged), Toast.LENGTH_SHORT).show();
        }, false);
        addDivider(context, appearanceSection);
        addActionCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlass), liquidGlassStatusValue(), false, null);
        addDivider(context, appearanceSection);
        addActionCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassStudio), liquidGlassStudioValue(), true, v -> presentFragment(new SingGramLiquidGlassStudioActivity()));
        addDivider(context, appearanceSection);
        addLiquidGlassLevelCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassSoft), LocaleController.getString(R.string.SingGramLiquidGlassSoftInfo), 0);
        addDivider(context, appearanceSection);
        addLiquidGlassLevelCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassStandard), LocaleController.getString(R.string.SingGramLiquidGlassStandardInfo), 1);
        addDivider(context, appearanceSection);
        addLiquidGlassLevelCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassStrong), LocaleController.getString(R.string.SingGramLiquidGlassStrongInfo), 2);
        addInfo(context, container, LocaleController.getString(R.string.SingGramLiquidGlassInfo));
    }

    private void buildDiagnosticsPage(Context context, LinearLayout container) {
        addHeader(context, container, LocaleController.getString(R.string.SingGramPushNotifications));
        LinearLayout pushSection = addSection(context, container);
        addPushDiagnosticsCells(context, pushSection);

        addHeader(context, container, LocaleController.getString(R.string.SingGramDiagnostics));
        LinearLayout diagnosticsSection = addSection(context, container);
        addSwitchSetting(context, diagnosticsSection, LocaleController.getString(R.string.SingGramCrashSafeMode), crashSafeModeValue(), SingGramConfig.isCrashSafeModeEnabled(), enabled -> {
            SingGramConfig.setCrashSafeModeEnabled(enabled);
            if (!enabled) {
                SingGramConfig.clearLastCrash();
            }
        }, false);
        addDivider(context, diagnosticsSection);
        addSwitchSetting(context, diagnosticsSection, LocaleController.getString(R.string.SingGramDiagnosticsVisible), LocaleController.getString(R.string.SingGramDiagnosticsVisibleInfo), SingGramConfig.isDiagnosticsEnabled(), SingGramConfig::setDiagnosticsEnabled, false);
        if (SingGramConfig.isDiagnosticsEnabled()) {
            addDivider(context, diagnosticsSection);
            addButton(context, diagnosticsSection, LocaleController.getString(R.string.SingGramExportLogs), false, v -> ProfileActivity.sendLogs(getParentActivity(), false));
            addButton(context, diagnosticsSection, LocaleController.getString(R.string.SingGramExportLastLogs), false, v -> ProfileActivity.sendLogs(getParentActivity(), true));
        }
        addDivider(context, diagnosticsSection);
        addActionCell(context, diagnosticsSection, LocaleController.getString(R.string.SingGramBuildInfo), buildInfoValue(), false, null);
        addDivider(context, diagnosticsSection);
        addActionCell(context, diagnosticsSection, LocaleController.getString(R.string.SingGramUpdates), updateSummaryValue(), true, v -> presentFragment(new SingGramUpdateActivity()));
        addDivider(context, diagnosticsSection);
        addActionCell(context, diagnosticsSection, LocaleController.getString(R.string.SingGramCopyDiagnostics), LocaleController.getString(R.string.SingGramDiagnosticsCopied), true, v -> copyDiagnostics());
    }

    private void saveSettings() {
        if (baseUrlField != null) {
            SingGramConfig.setAiBaseUrl(baseUrlField.getText().toString());
        }
        if (apiKeyField != null) {
            SingGramConfig.setAiApiKey(apiKeyField.getText().toString());
        }
        if (modelField != null) {
            SingGramConfig.setAiModel(modelField.getText().toString());
        }
        if (systemPromptField != null) {
            SingGramConfig.setAiSystemPrompt(systemPromptField.getText().toString());
        }
    }

    private void runAction(int action) {
        saveSettings();
        String input = inputField == null ? "" : inputField.getText().toString();
        if (TextUtils.isEmpty(input)) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAIEmptyInput), Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.setCanCancel(false);
        progressDialog.show();
        SingGramAiClient.runTextAction(action, input, new SingGramAiClient.Callback() {
            @Override
            public void onResult(String text) {
                try {
                    progressDialog.dismiss();
                } catch (Exception ignore) {

                }
                resultView.setText(text);
            }

            @Override
            public void onError(String error) {
                try {
                    progressDialog.dismiss();
                } catch (Exception ignore) {

                }
                Toast.makeText(getParentActivity(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void pasteClipboard() {
        ClipboardManager clipboardManager = (ClipboardManager) getParentActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null || !clipboardManager.hasPrimaryClip()) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramClipboardEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clipData = clipboardManager.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() == 0) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramClipboardEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence text = clipData.getItemAt(0).coerceToText(getParentActivity());
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramClipboardEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        inputField.setText(text.toString());
    }

    private void copyResult() {
        if (resultView == null || TextUtils.isEmpty(resultView.getText()) || TextUtils.equals(resultView.getText(), LocaleController.getString(R.string.SingGramAIResultPlaceholder))) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAIEmptyResponse), Toast.LENGTH_SHORT).show();
            return;
        }
        AndroidUtilities.addToClipboard(resultView.getText());
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramTextCopied), Toast.LENGTH_SHORT).show();
    }

    private void importSingGramSettings() {
        ClipboardManager clipboardManager = (ClipboardManager) getParentActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null || !clipboardManager.hasPrimaryClip()) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramImportSettingsEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clipData = clipboardManager.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() == 0) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramImportSettingsEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence clipText = clipData.getItemAt(0).coerceToText(getParentActivity());
        if (TextUtils.isEmpty(clipText)) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramImportSettingsEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        boolean imported = SingGramBackupBundle.importBundle(clipText.toString());
        Toast.makeText(getParentActivity(), LocaleController.getString(imported ? R.string.SingGramSettingsImported : R.string.SingGramImportSettingsInvalid), Toast.LENGTH_SHORT).show();
        if (imported) {
            removeSelfFromStack();
            presentFragment(new SingGramSettingsActivity());
        }
    }

    private void exportSingGramSettings() {
        AndroidUtilities.addToClipboard(SingGramBackupBundle.exportBundle());
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramSettingsExported), Toast.LENGTH_SHORT).show();
    }

    private void copyGhostDialogIds() {
        String ids = SingGramConfig.exportGhostDialogIds();
        if (TextUtils.isEmpty(ids)) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramGhostChatsEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        AndroidUtilities.addToClipboard(ids);
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramGhostChatsCopied), Toast.LENGTH_SHORT).show();
    }

    private void clearGhostDialogIds() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.SingGramClearGhostChats));
        builder.setMessage(LocaleController.getString(R.string.SingGramClearGhostChatsInfo));
        builder.setPositiveButton(LocaleController.getString(R.string.SingGramClearButton), (dialog, which) -> {
            SingGramConfig.importGhostDialogIds("");
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramSettingsSaved), Toast.LENGTH_SHORT).show();
            removeSelfFromStack();
            presentFragment(new SingGramSettingsActivity());
        });
        builder.setNegativeButton(LocaleController.getString(R.string.SingGramCancelButton), null);
        showDialog(builder.create());
    }

    private void applySingGramPreset(int preset) {
        switch (preset) {
            case 0:
                SingGramConfig.setGhostModeEnabled(true);
                SingGramConfig.setGhostSelectedChatsOnly(false);
                SingGramConfig.setDisableReadReceiptsEnabled(true);
                SingGramConfig.setHideTypingStatusEnabled(true);
                SingGramConfig.setKeepDeletedMessages(true);
                SingGramConfig.setKeepOriginalEdits(true);
                SingGramConfig.setAiEnabled(true);
                SingGramConfig.setAiContextMenuEnabled(true);
                SingGramConfig.setAiTranslateActionEnabled(true);
                SingGramConfig.setQuickReplyIdeasEnabled(true);
                SingGramConfig.setLiquidGlassEnabled(true);
                SingGramConfig.setLiquidGlassLevel(1);
                SingGramConfig.setDownloadBoostEnabled(false);
                break;
            case 2:
                SingGramConfig.setGhostModeEnabled(false);
                SingGramConfig.setKeepDeletedMessages(false);
                SingGramConfig.setKeepOriginalEdits(false);
                SingGramConfig.setAiEnabled(false);
                SingGramConfig.setDownloadBoostEnabled(true);
                SingGramConfig.setDownloadBoostLevel(2);
                SingGramConfig.setLiquidGlassEnabled(false);
                SingGramConfig.setCrashSafeModeEnabled(false);
                break;
            case 1:
            default:
                SingGramConfig.setGhostModeEnabled(false);
                SingGramConfig.setKeepDeletedMessages(true);
                SingGramConfig.setKeepOriginalEdits(true);
                SingGramConfig.setAiEnabled(true);
                SingGramConfig.setAiContextMenuEnabled(true);
                SingGramConfig.setAiTranslateActionEnabled(true);
                SingGramConfig.setQuickReplyIdeasEnabled(true);
                SingGramConfig.setDownloadBoostEnabled(true);
                SingGramConfig.setDownloadBoostLevel(1);
                SingGramConfig.setLiquidGlassEnabled(true);
                SingGramConfig.setLiquidGlassLevel(1);
                SingGramConfig.setCrashSafeModeEnabled(false);
                break;
        }
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramPresetApplied), Toast.LENGTH_SHORT).show();
        removeSelfFromStack();
        presentFragment(new SingGramSettingsActivity());
    }

    private void testNewApiConnection() {
        saveSettings();
        AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.setCanCancel(false);
        progressDialog.show();
        SingGramAiClient.testConnection(new SingGramAiClient.Callback() {
            @Override
            public void onResult(String text) {
                try {
                    progressDialog.dismiss();
                } catch (Exception ignore) {

                }
                if (resultView != null) {
                    resultView.setText(text);
                }
                Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAIConnectionOk), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                try {
                    progressDialog.dismiss();
                } catch (Exception ignore) {

                }
                Toast.makeText(getParentActivity(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String buildInfoValue() {
        String packageName = ApplicationLoader.applicationContext == null ? "com.sing.singgram" : ApplicationLoader.applicationContext.getPackageName();
        return BuildVars.BUILD_VERSION_STRING + " / " + packageName;
    }

    private String updateSummaryValue() {
        int versionCode = SingGramConfig.getLastUpdateVersionCode();
        if (versionCode <= 0) {
            return LocaleController.getString(R.string.SingGramUpdateNotChecked);
        }
        String versionName = SingGramConfig.getLastUpdateVersionName();
        String version = TextUtils.isEmpty(versionName) ? String.valueOf(versionCode) : versionName;
        String state = versionCode > SharedConfig.buildVersion()
                ? LocaleController.getString(R.string.SingGramUpdateAvailable)
                : LocaleController.getString(R.string.SingGramUpdateCurrent);
        return version + " / " + state;
    }

    private String singGramPresetStatusValue() {
        int active = 0;
        if (SingGramConfig.isGhostModeEnabled()) {
            active++;
        }
        if (SingGramConfig.isDisableReadReceiptsEnabled()) {
            active++;
        }
        if (SingGramConfig.isHideTypingStatusEnabled()) {
            active++;
        }
        if (SingGramConfig.shouldKeepDeletedMessages()) {
            active++;
        }
        if (SingGramConfig.shouldKeepOriginalEdits()) {
            active++;
        }
        if (SingGramConfig.isAiEnabled()) {
            active++;
        }
        if (SingGramConfig.isDownloadBoostEnabled()) {
            active++;
        }
        if (SingGramConfig.isLiquidGlassEnabled()) {
            active++;
        }
        String mode;
        if (SingGramConfig.isGhostModeEnabled() && SingGramConfig.shouldDisableReadReceipts() && SingGramConfig.shouldHideTypingStatus()) {
            mode = LocaleController.getString(R.string.SingGramPresetStealth);
        } else if (SingGramConfig.isDownloadBoostEnabled() && SingGramConfig.getDownloadBoostLevel() >= 2 && !SingGramConfig.isLiquidGlassEnabled()) {
            mode = LocaleController.getString(R.string.SingGramPresetPerformance);
        } else {
            mode = LocaleController.getString(R.string.SingGramPresetDaily);
        }
        return LocaleController.formatString(R.string.SingGramPresetStatusValue, active, 8, mode);
    }

    private void copyDiagnostics() {
        StringBuilder builder = new StringBuilder();
        builder.append("SingGram diagnostics\n");
        builder.append("version: ").append(BuildVars.BUILD_VERSION_STRING).append('\n');
        builder.append("debug: ").append(BuildVars.DEBUG_VERSION).append('\n');
        builder.append("package: ").append(ApplicationLoader.applicationContext == null ? "com.sing.singgram" : ApplicationLoader.applicationContext.getPackageName()).append('\n');
        builder.append("accounts: ").append(UserConfig.getActivatedAccountsCount()).append('/').append(UserConfig.MAX_ACCOUNT_COUNT).append('\n');
        builder.append("ghost_mode: ").append(SingGramConfig.isGhostModeEnabled()).append('\n');
        builder.append("ghost_selected_chats_only: ").append(SingGramConfig.isGhostSelectedChatsOnly()).append('\n');
        builder.append("ghost_selected_count: ").append(SingGramConfig.getGhostDialogCount()).append('\n');
        builder.append("anti_delete: ").append(SingGramConfig.shouldKeepDeletedMessages()).append('\n');
        builder.append("anti_edit: ").append(SingGramConfig.shouldKeepOriginalEdits()).append('\n');
        builder.append("event_log_count: ").append(SingGramEventLog.getEventCount()).append('\n');
        builder.append("liquid_glass: ").append(SingGramConfig.isLiquidGlassEnabled()).append('\n');
        builder.append("liquid_glass_level: ").append(SingGramConfig.getLiquidGlassLevel()).append('\n');
        builder.append("liquid_glass_custom: ").append(SingGramConfig.isLiquidGlassCustomEnabled()).append('\n');
        builder.append("download_boost: ").append(SingGramConfig.isDownloadBoostEnabled()).append('\n');
        builder.append("download_boost_level: ").append(SingGramConfig.getDownloadBoostLevel()).append('\n');
        builder.append("crash_safe_mode: ").append(SingGramConfig.isCrashSafeModeEnabled()).append('\n');
        builder.append("read_receipt_exceptions: ").append(SingGramConfig.getReadReceiptsAllowedDialogCount()).append('\n');
        builder.append("last_update_version_code: ").append(SingGramConfig.getLastUpdateVersionCode()).append('\n');
        builder.append("last_update_version_name: ").append(SingGramConfig.getLastUpdateVersionName()).append('\n');
        builder.append("last_update_check_time: ").append(SingGramConfig.getLastUpdateCheckTime()).append('\n');
        builder.append(SingGramPushDiagnostics.buildReport());
        AndroidUtilities.addToClipboard(builder.toString());
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramDiagnosticsCopied), Toast.LENGTH_SHORT).show();
    }

    private void addPushDiagnosticsCells(Context context, LinearLayout section) {
        SingGramPushDiagnostics.Snapshot snapshot = SingGramPushDiagnostics.getSnapshot();
        addActionCell(context, section, LocaleController.getString(R.string.SingGramPushProvider), SingGramPushDiagnostics.summary(snapshot), false, null);
        addDivider(context, section);
        addActionCell(context, section, LocaleController.getString(R.string.SingGramPushToken), snapshot.tokenStatus, false, null);
        addDivider(context, section);
        addActionCell(context, section, LocaleController.getString(R.string.SingGramPushPermissions), SingGramPushDiagnostics.permissionValue(snapshot), false, null);
        addDivider(context, section);
        addActionCell(context, section, LocaleController.getString(R.string.SingGramPushKeepAlive), SingGramPushDiagnostics.keepAliveValue(snapshot), false, null);
        addDivider(context, section);
        addActionCell(context, section, LocaleController.getString(R.string.SingGramPushAccountsRegistered), SingGramPushDiagnostics.registeredAccountsValue(snapshot), false, null);
        addDivider(context, section);
        addActionCell(context, section, LocaleController.getString(R.string.SingGramPushChannels), SingGramPushDiagnostics.channelValue(snapshot), false, null);
        addDivider(context, section);
        addActionCell(context, section, LocaleController.getString(R.string.SingGramPushRefreshToken), LocaleController.getString(R.string.SingGramPushRefreshTokenInfo), true, v -> refreshPushToken());
        addDivider(context, section);
        addActionCell(context, section, LocaleController.getString(R.string.SingGramPushResetChannels), LocaleController.getString(R.string.SingGramPushResetChannelsInfo), true, v -> resetPushChannels());
        addDivider(context, section);
        addActionCell(context, section, LocaleController.getString(R.string.SingGramDoctorRepairPush), LocaleController.getString(R.string.SingGramDoctorRepairPushInfo), true, v -> repairPush());
        addDivider(context, section);
        addActionCell(context, section, LocaleController.getString(R.string.SingGramDoctorOpenNotificationSettings), LocaleController.getString(R.string.SingGramDoctorOpenNotificationSettingsInfo), true, v -> openNotificationSettings());
    }

    private void refreshPushToken() {
        SingGramPushDiagnostics.requestTokenRefresh();
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramPushRefreshRequested), Toast.LENGTH_SHORT).show();
    }

    private void resetPushChannels() {
        SingGramPushDiagnostics.resetNotificationChannels();
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramPushChannelsReset), Toast.LENGTH_SHORT).show();
    }

    private void repairPush() {
        SingGramPushDiagnostics.repairPushSettings();
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramDoctorRepairPushDone), Toast.LENGTH_SHORT).show();
        removeSelfFromStack();
        presentFragment(new SingGramSettingsActivity(MODE_DIAGNOSTICS));
    }

    private void openNotificationSettings() {
        if (getParentActivity() == null) {
            return;
        }
        String packageName = ApplicationLoader.applicationContext == null ? "com.sing.singgram" : ApplicationLoader.applicationContext.getPackageName();
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName);
            } else {
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:" + packageName));
            }
            getParentActivity().startActivity(intent);
        } catch (Throwable ignore) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + packageName));
            getParentActivity().startActivity(intent);
        }
    }

    private String crashSafeModeValue() {
        if (SingGramConfig.getLastCrashTime() > 0) {
            String reason = SingGramConfig.getLastCrashReason();
            if (!TextUtils.isEmpty(reason)) {
                return reason;
            }
            return LocaleController.getString(R.string.SingGramCrashSafeModeAutoInfo);
        }
        return LocaleController.getString(R.string.SingGramCrashSafeModeInfo);
    }

    private String liquidGlassStudioValue() {
        if (!SingGramConfig.isLiquidGlassCustomEnabled()) {
            return LocaleController.getString(R.string.SingGramLiquidGlassStudioPreset);
        }
        return LocaleController.formatString(R.string.SingGramLiquidGlassStudioSummary, SingGramConfig.getLiquidGlassThicknessDp(), SingGramConfig.getLiquidGlassIntensityPermille(), SingGramConfig.getLiquidGlassIndexPermille());
    }

    private String liquidGlassStatusValue() {
        String enabled = LocaleController.getString(SingGramConfig.isLiquidGlassEnabled() ? R.string.SingGramStateOn : R.string.SingGramStateOff);
        String level = liquidGlassLevelName(SingGramConfig.getLiquidGlassLevel());
        if (SingGramConfig.isLiquidGlassCustomEnabled()) {
            return LocaleController.formatString(R.string.SingGramLiquidGlassStatusValueCustom, enabled, level, LocaleController.getString(R.string.SingGramDoctorCustom));
        }
        return LocaleController.formatString(R.string.SingGramLiquidGlassStatusValue, enabled, level);
    }

    private String liquidGlassLevelName(int level) {
        if (level <= 0) {
            return LocaleController.getString(R.string.SingGramLiquidGlassSoft);
        } else if (level >= 2) {
            return LocaleController.getString(R.string.SingGramLiquidGlassStrong);
        }
        return LocaleController.getString(R.string.SingGramLiquidGlassStandard);
    }

    private String downloadStatusValue() {
        SingGramDownloadStats.Snapshot snapshot = SingGramDownloadStats.getSnapshot();
        return LocaleController.formatString(R.string.SingGramDownloadStatusActiveValue, snapshot.activeCount, AndroidUtilities.formatFileSize(snapshot.speedBytesPerSecond) + "/s");
    }

    private void addDownloadBoostLevelCell(Context context, LinearLayout container, String text, String value, int level) {
        String displayValue = SingGramConfig.getDownloadBoostLevel() == level ? LocaleController.getString(R.string.SingGramCurrentSelection) : value;
        addActionCell(context, container, text, displayValue, true, v -> {
            SingGramConfig.setDownloadBoostEnabled(true);
            SingGramConfig.setDownloadBoostLevel(level);
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramDownloadBoostChanged), Toast.LENGTH_SHORT).show();
        });
    }

    private SettingsActivity.SettingCell addIconActionCell(Context context, LinearLayout container, String text, String value, int icon, int colorTop, int colorBottom, boolean enabled, View.OnClickListener listener) {
        SettingsActivity.SettingCell cell = new SettingsActivity.SettingCell(context, null);
        cell.set(colorTop, colorBottom, icon, text, value, null);
        cell.setEnabled(enabled);
        cell.setAlpha(enabled ? 1.0f : 0.58f);
        if (listener != null) {
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            cell.setOnClickListener(listener);
        }
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return cell;
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

    private LinearLayout addSection(Context context, LinearLayout container) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setClipToPadding(false);
        section.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(section, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 0));
        return section;
    }

    private TextCheckCell addSwitchCell(Context context, LinearLayout container, String text, boolean checked, boolean divider) {
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndCheck(text, checked, divider);
        cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return cell;
    }

    private TextCheckCell addSwitchSetting(Context context, LinearLayout container, String text, String value, boolean checked, BooleanSetter setter, boolean divider) {
        TextCheckCell cell = new TextCheckCell(context, 16);
        if (TextUtils.isEmpty(value)) {
            cell.setTextAndCheck(text, checked, divider);
        } else {
            cell.setTextAndValueAndCheck(text, value, checked, true, divider);
        }
        cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        cell.setOnClickListener(v -> {
            boolean enabled = !cell.isChecked();
            cell.setChecked(enabled);
            setter.set(enabled);
        });
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return cell;
    }

    private TextCheckCell addActionCell(Context context, LinearLayout container, String text, String value, boolean enabled, View.OnClickListener listener) {
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValue(text, value, true, false);
        cell.setEnabled(enabled);
        cell.setAlpha(enabled ? 1.0f : 0.58f);
        if (listener != null) {
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            cell.setOnClickListener(listener);
        }
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return cell;
    }

    private void addLiquidGlassLevelCell(Context context, LinearLayout container, String text, String value, int level) {
        String displayValue = SingGramConfig.getLiquidGlassLevel() == level ? LocaleController.getString(R.string.SingGramCurrentSelection) : value;
        addActionCell(context, container, text, displayValue, true, v -> {
            SingGramConfig.setLiquidGlassEnabled(true);
            SingGramConfig.setLiquidGlassLevel(level);
            if (liquidGlassCell != null) {
                liquidGlassCell.setChecked(true);
            }
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramLiquidGlassChanged), Toast.LENGTH_SHORT).show();
        });
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
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(2));
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private EditTextBoldCursor addField(Context context, LinearLayout container, String label, String hint, String value, boolean multiline) {
        LinearLayout fieldContainer = new LinearLayout(context);
        fieldContainer.setOrientation(LinearLayout.VERTICAL);
        fieldContainer.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(multiline ? 12 : 9));
        container.addView(fieldContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        labelView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        labelView.setIncludeFontPadding(false);
        fieldContainer.addView(labelView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        editText.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        editText.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | (multiline ? Gravity.TOP : Gravity.CENTER_VERTICAL));
        editText.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.setIncludeFontPadding(false);
        editText.setHint(hint);
        editText.setText(value == null ? "" : value);
        if (multiline) {
            editText.setMinLines(3);
            editText.setMaxLines(8);
            editText.setSingleLine(false);
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            fieldContainer.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 96));
        } else {
            editText.setSingleLine(true);
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            fieldContainer.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 34));
        }
        return editText;
    }

    private void addButton(Context context, LinearLayout container, String text, boolean primary, View.OnClickListener listener) {
        TextView button = makeButton(context, text, primary);
        button.setOnClickListener(listener);
        container.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 14, 10, 14, 12));
    }

    private LinearLayout addButtonRow(Context context, LinearLayout container) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), 0);
        container.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return row;
    }

    private void addSmallButton(Context context, LinearLayout row, String text, View.OnClickListener listener) {
        TextView button = makeButton(context, text, false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        button.setOnClickListener(listener);
        row.addView(button, LayoutHelper.createLinear(0, 40, 1f, 4, 0, 4, 0));
    }

    private TextView makeButton(Context context, String text, boolean primary) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        button.setTypeface(AndroidUtilities.bold());
        button.setIncludeFontPadding(false);
        int accentColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
        if (primary) {
            button.setTextColor(Color.WHITE);
            button.setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed), 8, 8));
        } else {
            button.setTextColor(accentColor);
            button.setBackground(Theme.createRadSelectorDrawable(Theme.multAlpha(accentColor, 0.10f), Theme.getColor(Theme.key_listSelector), 8, 8));
        }
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        return button;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        return themeDescriptions;
    }
}
