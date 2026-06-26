package org.telegram.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
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
    private LinearLayout contentContainer;

    private interface BooleanSetter {
        void set(boolean enabled);
    }

    public SingGramSettingsActivity() {
        this(MODE_HOME);
    }

    private SingGramSettingsActivity(int mode) {
        this.mode = mode;
    }

    public static SingGramSettingsActivity accountsPage() {
        return new SingGramSettingsActivity(MODE_ACCOUNTS);
    }

    public static SingGramSettingsActivity privacyPage() {
        return new SingGramSettingsActivity(MODE_PRIVACY);
    }

    public static SingGramSettingsActivity aiPage() {
        return new SingGramSettingsActivity(MODE_AI);
    }

    public static SingGramSettingsActivity downloadsPage() {
        return new SingGramSettingsActivity(MODE_DOWNLOADS);
    }

    public static SingGramSettingsActivity appearancePage() {
        return new SingGramSettingsActivity(MODE_APPEARANCE);
    }

    public static SingGramSettingsActivity diagnosticsPage() {
        return new SingGramSettingsActivity(MODE_DIAGNOSTICS);
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
        contentContainer = container;

        buildContent(context, container);
        maybeShowFeatureHubIntro();
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
        addActionCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadBoostMode), downloadBoostModeValue(), true, v -> showDownloadBoostModeDialog());
        addDivider(context, downloadSection);
        addActionCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadAutoThreads), downloadThreadsValue(), false, null);
        addDivider(context, downloadSection);
        addActionCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadStatus), downloadStatusValue(), true, v -> presentFragment(new SingGramDownloadStatusActivity()));
        addInfo(context, container, LocaleController.getString(R.string.SingGramDownloadBoostFootnote));

        addHeader(context, container, LocaleController.getString(R.string.SingGramAI));
        LinearLayout aiSection = addSection(context, container);

        aiEnabledCell = addSwitchCell(context, aiSection, LocaleController.getString(R.string.SingGramAIEnableTools), SingGramConfig.isAiEnabled(), false);
        aiEnabledCell.setOnClickListener(v -> {
            boolean enabled = !aiEnabledCell.isChecked();
            aiEnabledCell.setChecked(enabled);
            SingGramConfig.setAiEnabled(enabled);
            rebuildSettingsPage();
        });
        if (!SingGramConfig.isAiEnabled()) {
            addDivider(context, aiSection);
            addActionCell(context, aiSection, LocaleController.getString(R.string.SingGramAISettingsCollapsed), LocaleController.getString(R.string.SingGramAISettingsCollapsedInfo), false, null);
        } else {
            addDivider(context, aiSection);
            addSwitchSetting(context, aiSection, LocaleController.getString(R.string.SingGramAIPreferCantonese), LocaleController.getString(R.string.SingGramAIPreferCantoneseInfo), SingGramConfig.shouldAiPreferCantonese(), SingGramConfig::setAiPreferCantonese, false);
            addDivider(context, aiSection);
            addActionCell(context, aiSection, LocaleController.getString(R.string.SingGramAIProvider), aiProviderValue(), true, v -> showAiProviderDialog());
            addDivider(context, aiSection);
            baseUrlField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAIBaseUrl), LocaleController.getString(R.string.SingGramAIBaseUrlHint), SingGramConfig.getAiBaseUrl(), false);
            addDivider(context, aiSection);
            apiKeyField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAIApiKey), "sk-...", SingGramConfig.getAiApiKey(), false, true);
            addDivider(context, aiSection);
            modelField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAIModel), SingGramConfig.DEFAULT_AI_MODEL, SingGramConfig.getAiModel(), false);
            addDivider(context, aiSection);
            addActionCell(context, aiSection, LocaleController.getString(R.string.SingGramAIChooseModel), LocaleController.getString(R.string.SingGramAIChooseModelInfo), true, v -> fetchAndChooseModel());
            addDivider(context, aiSection);
            systemPromptField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAISystemPrompt), LocaleController.getString(R.string.SingGramAISystemPromptHint), SingGramConfig.getAiSystemPrompt(), true);
            addIconActionCell(context, aiSection, LocaleController.getString(R.string.SingGramSaveButton), LocaleController.getString(R.string.SingGramAIBaseUrlAutoV1Info), R.drawable.msg_copy, 0xFF36A7F2, 0xFF2D7FE6, true, v -> {
                saveSettings();
                Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramSettingsSaved), Toast.LENGTH_SHORT).show();
            });
            addDivider(context, aiSection);
            addIconActionCell(context, aiSection, LocaleController.getString(R.string.SingGramAISaveProvider), LocaleController.getString(R.string.SingGramAISaveProviderInfo), R.drawable.menu_browser_bookmarks, 0xFF8A7CFF, 0xFF5267E8, true, v -> saveCurrentAiProvider());
            addDivider(context, aiSection);
            addIconActionCell(context, aiSection, LocaleController.getString(R.string.SingGramAITestConnection), LocaleController.getString(R.string.SingGramAITestConnectionInfo), R.drawable.settings_features, 0xFF35C46A, 0xFF168DDF, true, v -> testNewApiConnection());
        }

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
        addActionCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassMode), liquidGlassLevelName(SingGramConfig.getLiquidGlassLevel()), true, v -> showLiquidGlassModeDialog());
        addDivider(context, appearanceSection);
        addActionCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassStudio), liquidGlassStudioValue(), true, v -> presentFragment(new SingGramLiquidGlassStudioActivity()));
        addInfo(context, container, LocaleController.getString(R.string.SingGramLiquidGlassInfo));

        if (SingGramConfig.isAiEnabled()) {
            addHeader(context, container, LocaleController.getString(R.string.SingGramAITestLab));
            LinearLayout testSection = addSection(context, container);
            inputField = addField(context, testSection, LocaleController.getString(R.string.SingGramAIInput), LocaleController.getString(R.string.SingGramAIInputHint), "", true);
            addDivider(context, testSection);
            addAiTestActionGrid(context, testSection);

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

        addHeader(context, container, LocaleController.getString(R.string.SingGramDiagnostics));
        LinearLayout diagnosticsSection = addSection(context, container);
        addSwitchSetting(context, diagnosticsSection, LocaleController.getString(R.string.SingGramCrashSafeMode), crashSafeModeValue(), SingGramConfig.isCrashSafeModeEnabled(), enabled -> {
            SingGramConfig.setCrashSafeModeEnabled(enabled);
            if (!enabled) {
                SingGramConfig.clearLastCrash();
            }
        }, false);
        addDivider(context, diagnosticsSection);
        addActionCell(context, diagnosticsSection, LocaleController.getString(R.string.SingGramCrashRecovery), LocaleController.getString(R.string.SingGramCrashRecoveryInfo), true, v -> applyCrashRecovery());
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
        addIconActionCell(context, toolsSection, LocaleController.getString(R.string.SingGramFeatureHub), LocaleController.getString(R.string.SingGramFeatureHubInfo), R.drawable.settings_features, 0xFF4B8DFF, 0xFF23B9C9, true, v -> presentFragment(new SingGramFeatureHubActivity()));
        addDivider(context, toolsSection);
        addIconActionCell(context, toolsSection, LocaleController.getString(R.string.SingGramCommandPalette), LocaleController.getString(R.string.SingGramCommandPaletteInfo), R.drawable.premium_ai_editor, 0xFF23B9C9, 0xFF2684E8, true, v -> presentFragment(new SingGramCommandPaletteActivity()));
        addDivider(context, toolsSection);
        addIconActionCell(context, toolsSection, LocaleController.getString(R.string.SingGramChatNotesAll), LocaleController.formatString(R.string.SingGramChatNotesAllCount, SingGramChatNotesStore.getNotesCount()), R.drawable.msg_addbio, 0xFF55CA47, 0xFF27B434, true, v -> presentFragment(new SingGramChatNotesListActivity()));
        addDivider(context, toolsSection);
        addIconActionCell(context, toolsSection, LocaleController.getString(R.string.SingGramDownloadCenter), downloadStatusValue(), R.drawable.settings_data, 0xFF40B7FF, 0xFF168BDE, true, v -> presentFragment(new SingGramDownloadStatusActivity()));
        addDivider(context, toolsSection);
        addIconActionCell(context, toolsSection, LocaleController.getString(R.string.SingGramUpdates), updateSummaryValue(), R.drawable.settings_features, 0xFF4EA5F6, 0xFF3577E5, true, v -> presentFragment(new SingGramUpdateActivity()));
        addDivider(context, toolsSection);
        addIconActionCell(context, toolsSection, LocaleController.getString(R.string.SingGramAIBrowser), browserEngineValue(), R.drawable.settings_language, 0xFF23B9C9, 0xFF617CFF, true, v -> presentFragment(SingGramSettingsActivity.aiPage()));
        addDivider(context, toolsSection);
        addIconActionCell(context, toolsSection, LocaleController.getString(R.string.SingGramCrashRecovery), crashSafeModeValue(), R.drawable.settings_power, 0xFFFF8B3D, 0xFFE45644, true, v -> applyCrashRecovery());
        addDivider(context, toolsSection);
        addIconActionCell(context, toolsSection, LocaleController.getString(R.string.SingGramDoctor), LocaleController.getString(R.string.SingGramDoctorInfo), R.drawable.settings_power, 0xFFFF8B3D, 0xFFE45644, true, v -> presentFragment(new SingGramDoctorActivity()));

        addHeader(context, container, LocaleController.getString(R.string.SingGramSettingsCategories));
        LinearLayout categoriesSection = addSection(context, container);
        addIconActionCell(context, categoriesSection, LocaleController.getString(R.string.SingGramAccount), categoryValue(LocaleController.getString(R.string.SingGramAccountSummary), accountBadgeValue()), R.drawable.settings_account, 0xFF4EA5F6, 0xFF3577E5, true, v -> presentFragment(new SingGramSettingsActivity(MODE_ACCOUNTS)));
        addDivider(context, categoriesSection);
        addIconActionCell(context, categoriesSection, LocaleController.getString(R.string.SingGramPrivacy), categoryValue(LocaleController.getString(R.string.SingGramPrivacySummary), privacyBadgeValue()), R.drawable.settings_privacy, 0xFF55CA47, 0xFF27B434, true, v -> presentFragment(new SingGramSettingsActivity(MODE_PRIVACY)));
        addDivider(context, categoriesSection);
        addIconActionCell(context, categoriesSection, LocaleController.getString(R.string.SingGramAI), categoryValue(LocaleController.getString(R.string.SingGramAiSummary), aiBadgeValue()), R.drawable.premium_ai_editor, 0xFF23B9C9, 0xFF2684E8, true, v -> presentFragment(new SingGramSettingsActivity(MODE_AI)));
        addDivider(context, categoriesSection);
        addIconActionCell(context, categoriesSection, LocaleController.getString(R.string.SingGramDownload), categoryValue(LocaleController.getString(R.string.SingGramDownloadSummary), downloadBadgeValue()), R.drawable.settings_data, 0xFF40B7FF, 0xFF168BDE, true, v -> presentFragment(new SingGramSettingsActivity(MODE_DOWNLOADS)));
        addDivider(context, categoriesSection);
        addIconActionCell(context, categoriesSection, LocaleController.getString(R.string.SingGramAppearance), categoryValue(LocaleController.getString(R.string.SingGramAppearanceSummary), appearanceBadgeValue()), R.drawable.settings_chat, 0xFFB659FF, 0xFF617CFF, true, v -> presentFragment(new SingGramSettingsActivity(MODE_APPEARANCE)));
        addDivider(context, categoriesSection);
        addIconActionCell(context, categoriesSection, LocaleController.getString(R.string.SingGramDiagnostics), categoryValue(LocaleController.getString(R.string.SingGramDiagnosticsSummary), diagnosticsBadgeValue()), R.drawable.settings_devices, 0xFF8A98A7, 0xFF5D6C7B, true, v -> presentFragment(new SingGramSettingsActivity(MODE_DIAGNOSTICS)));
        addInfo(context, container, LocaleController.getString(R.string.SingGramSettingsHomeInfo));
    }

    private void maybeShowFeatureHubIntro() {
        if (mode != MODE_HOME || !SingGramConfig.shouldShowFeatureHubIntro()) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            if (getParentActivity() == null || !SingGramConfig.shouldShowFeatureHubIntro()) {
                return;
            }
            SingGramConfig.markFeatureHubIntroShown();
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString(R.string.SingGramFeatureIntroTitle));
            builder.setMessage(LocaleController.getString(R.string.SingGramFeatureIntroMessage));
            builder.setPositiveButton(LocaleController.getString(R.string.SingGramFeatureIntroOpen), (dialog, which) -> presentFragment(new SingGramFeatureHubActivity()));
            builder.setNegativeButton(LocaleController.getString(R.string.OK), null);
            showDialog(builder.create());
        }, 450);
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
        addAiHeroCard(context, container);

        addHeader(context, container, LocaleController.getString(R.string.SingGramAINewApiConnection));
        LinearLayout aiSection = addSection(context, container);
        aiEnabledCell = addSwitchCell(context, aiSection, LocaleController.getString(R.string.SingGramAIEnableTools), SingGramConfig.isAiEnabled(), false);
        aiEnabledCell.setOnClickListener(v -> {
            boolean enabled = !aiEnabledCell.isChecked();
            aiEnabledCell.setChecked(enabled);
            SingGramConfig.setAiEnabled(enabled);
            rebuildSettingsPage();
        });
        if (!SingGramConfig.isAiEnabled()) {
            addDivider(context, aiSection);
            addActionCell(context, aiSection, LocaleController.getString(R.string.SingGramAISettingsCollapsed), LocaleController.getString(R.string.SingGramAISettingsCollapsedInfo), false, null);
            addInfo(context, container, LocaleController.getString(R.string.SingGramAIInfo));
            return;
        }
        addDivider(context, aiSection);
        addSwitchSetting(context, aiSection, LocaleController.getString(R.string.SingGramAIPreferCantonese), LocaleController.getString(R.string.SingGramAIPreferCantoneseInfo), SingGramConfig.shouldAiPreferCantonese(), SingGramConfig::setAiPreferCantonese, false);
        addDivider(context, aiSection);
        addActionCell(context, aiSection, LocaleController.getString(R.string.SingGramAIProvider), aiProviderValue(), true, v -> showAiProviderDialog());
        addDivider(context, aiSection);
        baseUrlField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAIBaseUrl), LocaleController.getString(R.string.SingGramAIBaseUrlHint), SingGramConfig.getAiBaseUrl(), false);
        addDivider(context, aiSection);
        apiKeyField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAIApiKey), "sk-...", SingGramConfig.getAiApiKey(), false, true);
        addDivider(context, aiSection);
        modelField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAIModel), SingGramConfig.DEFAULT_AI_MODEL, SingGramConfig.getAiModel(), false);
        addDivider(context, aiSection);
        addActionCell(context, aiSection, LocaleController.getString(R.string.SingGramAIChooseModel), LocaleController.getString(R.string.SingGramAIChooseModelInfo), true, v -> fetchAndChooseModel());
        addDivider(context, aiSection);
        systemPromptField = addField(context, aiSection, LocaleController.getString(R.string.SingGramAISystemPrompt), LocaleController.getString(R.string.SingGramAISystemPromptHint), SingGramConfig.getAiSystemPrompt(), true);
        addIconActionCell(context, aiSection, LocaleController.getString(R.string.SingGramSaveButton), LocaleController.getString(R.string.SingGramAIBaseUrlAutoV1Info), R.drawable.msg_copy, 0xFF36A7F2, 0xFF2D7FE6, true, v -> {
            saveSettings();
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramSettingsSaved), Toast.LENGTH_SHORT).show();
        });
        addDivider(context, aiSection);
        addIconActionCell(context, aiSection, LocaleController.getString(R.string.SingGramAISaveProvider), LocaleController.getString(R.string.SingGramAISaveProviderInfo), R.drawable.menu_browser_bookmarks, 0xFF8A7CFF, 0xFF5267E8, true, v -> saveCurrentAiProvider());
        addDivider(context, aiSection);
        addIconActionCell(context, aiSection, LocaleController.getString(R.string.SingGramAITestConnection), LocaleController.getString(R.string.SingGramAITestConnectionInfo), R.drawable.settings_features, 0xFF35C46A, 0xFF168DDF, true, v -> testNewApiConnection());
        addInfo(context, container, LocaleController.getString(R.string.SingGramAIInfo));

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

        addHeader(context, container, LocaleController.getString(R.string.SingGramAIBrowser));
        LinearLayout browserSection = addSection(context, container);
        addActionCell(context, browserSection, LocaleController.getString(R.string.SingGramBrowserEngine), browserEngineValue(), true, v -> showBrowserEngineDialog());
        addInfo(context, container, LocaleController.getString(R.string.SingGramBrowserEngineInfo));

        addHeader(context, container, LocaleController.getString(R.string.SingGramAITestLab));
        LinearLayout testSection = addSection(context, container);
        inputField = addField(context, testSection, LocaleController.getString(R.string.SingGramAIInput), LocaleController.getString(R.string.SingGramAIInputHint), "", true);
        addDivider(context, testSection);
        addAiTestActionGrid(context, testSection);

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
        addDownloadHeroCard(context, container);
        addHeader(context, container, LocaleController.getString(R.string.SingGramDownload));
        LinearLayout downloadSection = addSection(context, container);
        addActionCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadBoostMode), downloadBoostModeValue(), true, v -> showDownloadBoostModeDialog());
        addDivider(context, downloadSection);
        addActionCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadAutoThreads), downloadThreadsValue(), false, null);
        addDivider(context, downloadSection);
        addActionCell(context, downloadSection, LocaleController.getString(R.string.SingGramDownloadStatus), downloadStatusValue(), true, v -> presentFragment(new SingGramDownloadStatusActivity()));
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
        addActionCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassMode), liquidGlassLevelName(SingGramConfig.getLiquidGlassLevel()), true, v -> showLiquidGlassModeDialog());
        addDivider(context, appearanceSection);
        addActionCell(context, appearanceSection, LocaleController.getString(R.string.SingGramLiquidGlassStudio), liquidGlassStudioValue(), true, v -> presentFragment(new SingGramLiquidGlassStudioActivity()));
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
        addActionCell(context, diagnosticsSection, LocaleController.getString(R.string.SingGramCrashRecovery), LocaleController.getString(R.string.SingGramCrashRecoveryInfo), true, v -> applyCrashRecovery());
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
            baseUrlField.setText(SingGramConfig.getAiBaseUrl());
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

    private void rebuildSettingsPage() {
        if (contentContainer == null || getParentActivity() == null) {
            return;
        }
        contentContainer.removeAllViews();
        buildContent(getParentActivity(), contentContainer);
    }

    private String aiProviderValue() {
        String provider = SingGramConfig.getAiProviderSummary();
        if (TextUtils.isEmpty(provider)) {
            return LocaleController.getString(R.string.SingGramAIProviderNone);
        }
        return provider;
    }

    private void saveCurrentAiProvider() {
        saveSettings();
        if (SingGramConfig.saveCurrentAiProvider()) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAIProviderSaved), Toast.LENGTH_SHORT).show();
            rebuildSettingsPage();
        } else {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAIConfigureError), Toast.LENGTH_LONG).show();
        }
    }

    private void showAiProviderDialog() {
        ArrayList<SingGramConfig.AiProvider> providers = SingGramConfig.getAiProviders();
        if (providers.isEmpty()) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAIProviderEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] items = new CharSequence[providers.size()];
        for (int i = 0; i < providers.size(); i++) {
            SingGramConfig.AiProvider provider = providers.get(i);
            items[i] = provider.name + "  ·  " + provider.model;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.SingGramAIProvider));
        builder.setItems(items, (dialog, which) -> {
            SingGramConfig.applyAiProvider(providers.get(which));
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAIProviderApplied), Toast.LENGTH_SHORT).show();
            rebuildSettingsPage();
        });
        showDialog(builder.create());
    }

    private void fetchAndChooseModel() {
        saveSettings();
        AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.setCanCancel(false);
        progressDialog.show();
        SingGramAiClient.fetchModels(new SingGramAiClient.ModelsCallback() {
            @Override
            public void onResult(ArrayList<String> models) {
                try {
                    progressDialog.dismiss();
                } catch (Exception ignore) {

                }
                showModelDialog(models);
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

    private void showModelDialog(ArrayList<String> models) {
        if (models == null || models.isEmpty()) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAIModelsEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] items = new CharSequence[models.size()];
        for (int i = 0; i < models.size(); i++) {
            items[i] = models.get(i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.SingGramAIChooseModel));
        builder.setItems(items, (dialog, which) -> {
            String model = models.get(which);
            SingGramConfig.setAiModel(model);
            if (modelField != null) {
                modelField.setText(model);
            }
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAIModelSelected), Toast.LENGTH_SHORT).show();
        });
        showDialog(builder.create());
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

    private String categoryValue(String summary, String badge) {
        if (TextUtils.isEmpty(badge)) {
            return summary;
        }
        if (TextUtils.isEmpty(summary)) {
            return badge;
        }
        return summary + "\n" + badge;
    }

    private String accountBadgeValue() {
        return LocaleController.formatString(R.string.SingGramMaxAccounts100Info, UserConfig.getActivatedAccountsCount(), UserConfig.MAX_ACCOUNT_COUNT);
    }

    private String privacyBadgeValue() {
        return LocaleController.formatString(
                R.string.SingGramFeatureHubPrivacyValue,
                stateValue(SingGramConfig.isGhostModeEnabled()),
                SingGramConfig.getGhostDialogCount(),
                SingGramEventLog.getEventCount()
        );
    }

    private String aiBadgeValue() {
        String state = stateValue(SingGramConfig.isAiEnabled());
        if (TextUtils.isEmpty(SingGramConfig.getAiBaseUrl()) || TextUtils.isEmpty(SingGramConfig.getAiApiKey())) {
            return state + " / " + LocaleController.getString(R.string.SingGramCategoryNeedsSetup);
        }
        return state + " / " + SingGramConfig.getAiModel();
    }

    private String downloadBadgeValue() {
        String state = stateValue(SingGramConfig.isDownloadBoostEnabled());
        return state + " / " + downloadStatusValue();
    }

    private String appearanceBadgeValue() {
        return liquidGlassStatusValue();
    }

    private String diagnosticsBadgeValue() {
        SingGramPushDiagnostics.Snapshot snapshot = SingGramPushDiagnostics.getSnapshot();
        String push = SingGramPushDiagnostics.summary(snapshot);
        if (SingGramConfig.getLastCrashTime() > 0) {
            return push + " / " + LocaleController.getString(R.string.SingGramCrashSafeMode);
        }
        return push;
    }

    private String stateValue(boolean enabled) {
        return LocaleController.getString(enabled ? R.string.SingGramStateOn : R.string.SingGramStateOff);
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

    private void applyCrashRecovery() {
        SingGramConfig.applyCrashRecoveryPreset();
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramCrashRecoveryDone), Toast.LENGTH_SHORT).show();
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

    private String browserEngineValue() {
        return SingGramConfig.getBrowserEngine() == SingGramConfig.BROWSER_ENGINE_GECKOVIEW
                ? LocaleController.getString(R.string.SingGramBrowserEngineGecko)
                : LocaleController.getString(R.string.SingGramBrowserEngineSystem);
    }

    private void showBrowserEngineDialog() {
        String[] items = new String[] {
                LocaleController.getString(R.string.SingGramBrowserEngineSystem),
                LocaleController.getString(R.string.SingGramBrowserEngineGecko)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.SingGramBrowserEngine));
        builder.setItems(items, (dialog, which) -> {
            SingGramConfig.setBrowserEngine(which == 1 ? SingGramConfig.BROWSER_ENGINE_GECKOVIEW : SingGramConfig.BROWSER_ENGINE_SYSTEM_WEBVIEW);
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramBrowserEngineChanged), Toast.LENGTH_SHORT).show();
            if (getParentActivity() != null) {
                createView(getParentActivity());
            }
        });
        showDialog(builder.create());
    }

    private void showDownloadBoostModeDialog() {
        String[] items = new String[] {
                LocaleController.getString(R.string.SingGramDownloadBoostOff),
                LocaleController.getString(R.string.SingGramDownloadBoostAuto),
                LocaleController.getString(R.string.SingGramDownloadBoostBalanced),
                LocaleController.getString(R.string.SingGramDownloadBoostAggressive),
                LocaleController.getString(R.string.SingGramDownloadBoostMaximum)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.SingGramDownloadBoostMode));
        builder.setItems(items, (dialog, which) -> {
            if (which == 0) {
                SingGramConfig.setDownloadBoostEnabled(false);
                SingGramConfig.setDownloadBoostAutoEnabled(false);
            } else if (which == 1) {
                SingGramConfig.setDownloadBoostEnabled(true);
                SingGramConfig.setDownloadBoostAutoEnabled(true);
            } else {
                SingGramConfig.setDownloadBoostEnabled(true);
                SingGramConfig.setDownloadBoostAutoEnabled(false);
                SingGramConfig.setDownloadBoostLevel(which - 2);
            }
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramDownloadBoostChanged), Toast.LENGTH_SHORT).show();
            rebuildSettingsPage();
        });
        showDialog(builder.create());
    }

    private void showLiquidGlassModeDialog() {
        String[] items = new String[] {
                LocaleController.getString(R.string.SingGramLiquidGlassSoft),
                LocaleController.getString(R.string.SingGramLiquidGlassStandard),
                LocaleController.getString(R.string.SingGramLiquidGlassStrong)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.SingGramLiquidGlassMode));
        builder.setItems(items, (dialog, which) -> {
            SingGramConfig.setLiquidGlassEnabled(true);
            SingGramConfig.setLiquidGlassLevel(which);
            if (liquidGlassCell != null) {
                liquidGlassCell.setChecked(true);
            }
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramLiquidGlassChanged), Toast.LENGTH_SHORT).show();
            rebuildSettingsPage();
        });
        showDialog(builder.create());
    }

    private String downloadStatusValue() {
        SingGramDownloadStats.Snapshot snapshot = SingGramDownloadStats.getSnapshot();
        return LocaleController.formatString(R.string.SingGramDownloadStatusActiveValue, snapshot.activeCount, AndroidUtilities.formatFileSize(snapshot.speedBytesPerSecond) + "/s");
    }

    private String downloadBoostModeValue() {
        if (!SingGramConfig.isDownloadBoostEnabled()) {
            return LocaleController.getString(R.string.SingGramDownloadBoostOff);
        }
        if (SingGramConfig.isDownloadBoostAutoEnabled()) {
            return LocaleController.formatString(R.string.SingGramDownloadBoostAutoValue, LocaleController.getString(R.string.SingGramDownloadBoostAuto), downloadBoostLevelName(SingGramConfig.getEffectiveDownloadBoostLevel()));
        }
        return downloadBoostLevelName(SingGramConfig.getDownloadBoostLevel());
    }

    private String downloadBoostLevelName(int level) {
        if (level <= 0) {
            return LocaleController.getString(R.string.SingGramDownloadBoostBalanced);
        } else if (level == 1) {
            return LocaleController.getString(R.string.SingGramDownloadBoostAggressive);
        }
        return LocaleController.getString(R.string.SingGramDownloadBoostMaximum);
    }

    private String downloadThreadsValue() {
        if (!SingGramConfig.isDownloadBoostEnabled()) {
            return LocaleController.getString(R.string.SingGramDownloadThreadsDefault);
        }
        int small = SingGramConfig.getBoostedSmallQueueMaxActiveOperations(6);
        int large = SingGramConfig.getBoostedLargeQueueMaxActiveOperations(2);
        int requests = SingGramConfig.getBoostedDownloadRequestCount(8);
        return LocaleController.formatString(R.string.SingGramDownloadThreadsValue, small, large, requests);
    }

    private void addDownloadHeroCard(Context context, LinearLayout container) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(16));
        card.setBackground(downloadHeroBackground());
        container.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 4));

        TextView eyebrow = new TextView(context);
        eyebrow.setText(LocaleController.getString(R.string.SingGramDownload));
        eyebrow.setTextColor(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), 0.90f));
        eyebrow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        eyebrow.setTypeface(AndroidUtilities.bold());
        eyebrow.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        eyebrow.setIncludeFontPadding(false);
        card.addView(eyebrow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText(SingGramConfig.isDownloadBoostEnabled() ? LocaleController.getString(R.string.SingGramDownloadBoostReady) : LocaleController.getString(R.string.SingGramDownloadBoostIdle));
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        title.setIncludeFontPadding(false);
        title.setPadding(0, AndroidUtilities.dp(8), 0, 0);
        card.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText(LocaleController.formatString(R.string.SingGramDownloadBoostHeroInfo, downloadBoostModeValue(), downloadThreadsValue()));
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        subtitle.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        subtitle.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(14));
        card.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        SingGramDownloadStats.Snapshot snapshot = SingGramDownloadStats.getSnapshot();
        addAiHeroMetric(context, row, LocaleController.getString(R.string.SingGramDownloadCenterSpeed), AndroidUtilities.formatFileSize(snapshot.speedBytesPerSecond) + "/s");
        addAiHeroMetric(context, row, LocaleController.getString(R.string.SingGramDownloadCenterTracked), String.valueOf(snapshot.activeCount));
    }

    private GradientDrawable downloadHeroBackground() {
        int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
        int white = Theme.getColor(Theme.key_windowBackgroundWhite);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {
                Theme.multAlpha(accent, 0.20f),
                Theme.multAlpha(white, SingGramConfig.isLiquidGlassEnabled() ? 0.92f : 0.84f),
                Theme.multAlpha(0xFF35C46A, 0.12f)
        });
        drawable.setCornerRadius(AndroidUtilities.dp(SingGramConfig.isLiquidGlassEnabled() ? 18 : 12));
        drawable.setStroke(AndroidUtilities.dp(1), Theme.multAlpha(accent, 0.18f));
        return drawable;
    }

    private void addDownloadBoostLevelCell(Context context, LinearLayout container, String text, String value, int level) {
        String displayValue = SingGramConfig.getDownloadBoostLevel() == level ? LocaleController.getString(R.string.SingGramCurrentSelection) : value;
        addActionCell(context, container, text, displayValue, true, v -> {
            SingGramConfig.setDownloadBoostEnabled(true);
            SingGramConfig.setDownloadBoostAutoEnabled(false);
            SingGramConfig.setDownloadBoostLevel(level);
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramDownloadBoostChanged), Toast.LENGTH_SHORT).show();
        });
    }

    private void addAiHeroCard(Context context, LinearLayout container) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(16));
        card.setBackground(aiHeroBackground());
        container.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 4));

        TextView eyebrow = new TextView(context);
        eyebrow.setText(LocaleController.getString(R.string.SingGramAINewApiConnection));
        eyebrow.setTextColor(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), 0.90f));
        eyebrow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        eyebrow.setTypeface(AndroidUtilities.bold());
        eyebrow.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        eyebrow.setIncludeFontPadding(false);
        card.addView(eyebrow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText(aiHeroTitle());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        title.setIncludeFontPadding(false);
        title.setPadding(0, AndroidUtilities.dp(8), 0, 0);
        card.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText(aiHeroSubtitle());
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        subtitle.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        subtitle.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(14));
        card.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        addAiHeroMetric(context, row, LocaleController.getString(R.string.SingGramAIStatusEnabled), stateValue(SingGramConfig.isAiEnabled()));
        addAiHeroMetric(context, row, LocaleController.getString(R.string.SingGramAIStatusModel), TextUtils.isEmpty(SingGramConfig.getAiModel()) ? SingGramConfig.DEFAULT_AI_MODEL : SingGramConfig.getAiModel());
    }

    private void addAiHeroMetric(Context context, LinearLayout row, String label, String value) {
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
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        valueView.setTypeface(AndroidUtilities.bold());
        valueView.setSingleLine(true);
        valueView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        metric.addView(valueView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private GradientDrawable aiHeroBackground() {
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

    private String aiHeroTitle() {
        if (!SingGramConfig.isAiEnabled()) {
            return LocaleController.getString(R.string.SingGramAIHeroDisabled);
        }
        if (!SingGramConfig.isAiConfigured()) {
            return LocaleController.getString(R.string.SingGramAIHeroNeedsSetup);
        }
        return LocaleController.getString(R.string.SingGramAIHeroReady);
    }

    private String aiHeroSubtitle() {
        if (!SingGramConfig.isAiConfigured()) {
            return LocaleController.getString(R.string.SingGramAIHeroNeedsSetupInfo);
        }
        String baseUrl = SingGramConfig.getAiBaseUrl();
        String model = TextUtils.isEmpty(SingGramConfig.getAiModel()) ? SingGramConfig.DEFAULT_AI_MODEL : SingGramConfig.getAiModel();
        return LocaleController.formatString(R.string.SingGramAIHeroReadyInfo, model, baseUrl);
    }

    private View addIconActionCell(Context context, LinearLayout container, String text, String value, int icon, int colorTop, int colorBottom, boolean enabled, View.OnClickListener listener) {
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.HORIZONTAL);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setMinimumHeight(AndroidUtilities.dp(72));
        cell.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(11), AndroidUtilities.dp(18), AndroidUtilities.dp(11));
        cell.setEnabled(enabled);
        cell.setAlpha(enabled ? 1.0f : 0.58f);

        FrameLayout iconLayout = new FrameLayout(context);
        GradientDrawable iconBackground = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] { colorTop, colorBottom });
        iconBackground.setCornerRadius(AndroidUtilities.dp(11));
        iconLayout.setBackground(iconBackground);

        ImageView iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setImageResource(icon);
        iconView.setColorFilter(Color.WHITE);
        iconLayout.addView(iconView, LayoutHelper.createFrame(24, 24, Gravity.CENTER));

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(text);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setSingleLine(false);
        titleView.setMaxLines(2);
        titleView.setIncludeFontPadding(false);
        titleView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (!TextUtils.isEmpty(value)) {
            TextView subtitleView = new TextView(context);
            subtitleView.setText(value);
            subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subtitleView.setSingleLine(false);
            subtitleView.setMaxLines(3);
            subtitleView.setIncludeFontPadding(false);
            subtitleView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
            subtitleView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            textLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 5, 0, 0));
        }

        if (LocaleController.isRTL) {
            cell.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL, 0, 0, 16, 0));
            cell.addView(iconLayout, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        } else {
            cell.addView(iconLayout, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL | Gravity.LEFT));
            cell.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL, 16, 0, 0, 0));
        }

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
        return addField(context, container, label, hint, value, multiline, false);
    }

    private EditTextBoldCursor addField(Context context, LinearLayout container, String label, String hint, String value, boolean multiline, boolean password) {
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
            editText.setInputType(password
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
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

    private void addAiTestActionGrid(Context context, LinearLayout container) {
        LinearLayout row1 = addButtonRow(context, container);
        addAiTestActionCard(context, row1, LocaleController.getString(R.string.SingGramAISummarize), R.drawable.msg_message_s, v -> runAction(SingGramAiClient.ACTION_SUMMARIZE));
        addAiTestActionCard(context, row1, LocaleController.getString(R.string.SingGramAITranslate), R.drawable.msg_translate, v -> runAction(SingGramAiClient.ACTION_TRANSLATE_ZH_HANT));

        LinearLayout row2 = addButtonRow(context, container);
        addAiTestActionCard(context, row2, LocaleController.getString(R.string.SingGramAIRewriteCantonese), R.drawable.msg_language, v -> runAction(SingGramAiClient.ACTION_REWRITE_YUE));
        addAiTestActionCard(context, row2, LocaleController.getString(R.string.SingGramAIReplyIdeas), R.drawable.msg_discussion, v -> runAction(SingGramAiClient.ACTION_REPLY_SUGGESTIONS));

        LinearLayout row3 = addButtonRow(context, container);
        addAiTestActionCard(context, row3, LocaleController.getString(R.string.SingGramAIShorten), R.drawable.menu_feature_simple, v -> runAction(SingGramAiClient.ACTION_SHORTEN));
        addAiTestActionCard(context, row3, LocaleController.getString(R.string.SingGramAIExplain), R.drawable.msg_info, v -> runAction(SingGramAiClient.ACTION_EXPLAIN));

        LinearLayout row4 = addButtonRow(context, container);
        addAiTestActionCard(context, row4, LocaleController.getString(R.string.SingGramAICleanCopy), R.drawable.msg_copy, v -> runAction(SingGramAiClient.ACTION_CLEAN_COPY));
        addAiTestActionCard(context, row4, LocaleController.getString(R.string.SingGramAIExtractTasks), R.drawable.msg_work, v -> runAction(SingGramAiClient.ACTION_EXTRACT_TASKS));

        LinearLayout row5 = addButtonRow(context, container);
        addAiTestActionCard(context, row5, LocaleController.getString(R.string.SingGramAITranslateCantonese), R.drawable.menu_feature_translate, v -> runAction(SingGramAiClient.ACTION_TRANSLATE_YUE));
        addAiTestActionCard(context, row5, LocaleController.getString(R.string.SingGramAIPasteClipboard), R.drawable.input_keyboard, v -> pasteClipboard());
    }

    private void addAiTestActionCard(Context context, LinearLayout row, String text, int icon, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setMinimumHeight(AndroidUtilities.dp(52));
        card.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        card.setBackground(Theme.createRadSelectorDrawable(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), 0.08f), Theme.getColor(Theme.key_listSelector), 8, 8));
        card.setOnClickListener(listener);

        ImageView iconView = new ImageView(context);
        iconView.setImageResource(icon);
        iconView.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(iconView, LayoutHelper.createLinear(22, 22, Gravity.CENTER_VERTICAL));

        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setSingleLine(false);
        textView.setMaxLines(2);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setIncludeFontPadding(false);
        card.addView(textView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        row.addView(card, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 4, 0, 4, 0));
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
