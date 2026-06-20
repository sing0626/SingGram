package org.telegram.messenger;

import android.text.TextUtils;

public class SingGramBackupBundle {

    public static String exportBundle() {
        StringBuilder builder = new StringBuilder();
        builder.append("SingGram backup bundle v3\n");
        builder.append("sg.backup.version: 3\n");
        builder.append("sg.ai.enabled: ").append(SingGramConfig.isAiEnabled()).append('\n');
        builder.append("sg.ai.base_url: ").append(SingGramConfig.getAiBaseUrl()).append('\n');
        builder.append("sg.ai.model: ").append(SingGramConfig.getAiModel()).append('\n');
        builder.append("sg.ai.prefer_cantonese: ").append(SingGramConfig.shouldAiPreferCantonese()).append('\n');
        builder.append("sg.ai.context_menu: ").append(SingGramConfig.isAiContextMenuEnabled()).append('\n');
        builder.append("sg.ai.translate: ").append(SingGramConfig.isAiTranslateActionEnabled()).append('\n');
        builder.append("sg.ai.insert_result: ").append(SingGramConfig.isAiInsertResultEnabled()).append('\n');
        builder.append("sg.ai.reply_ideas: ").append(SingGramConfig.isQuickReplyIdeasEnabled()).append('\n');
        builder.append("sg.ghost.enabled: ").append(SingGramConfig.isGhostModeEnabled()).append('\n');
        builder.append("sg.ghost.selected_chats_only: ").append(SingGramConfig.isGhostSelectedChatsOnly()).append('\n');
        builder.append("sg.ghost.dialog_ids: ").append(SingGramConfig.exportGhostDialogIds()).append('\n');
        builder.append("sg.ghost.read_receipt_allowed_ids: ").append(SingGramConfig.exportReadReceiptAllowedDialogIds()).append('\n');
        builder.append("sg.ghost.disable_read_receipts: ").append(SingGramConfig.isDisableReadReceiptsEnabled()).append('\n');
        builder.append("sg.ghost.hide_typing: ").append(SingGramConfig.isHideTypingStatusEnabled()).append('\n');
        builder.append("sg.privacy.hide_phone: ").append(SingGramConfig.shouldHidePhoneInSettings()).append('\n');
        builder.append("sg.protection.keep_deleted: ").append(SingGramConfig.shouldKeepDeletedMessages()).append('\n');
        builder.append("sg.protection.keep_edits: ").append(SingGramConfig.shouldKeepOriginalEdits()).append('\n');
        builder.append("sg.liquid_glass.enabled: ").append(SingGramConfig.isLiquidGlassEnabled()).append('\n');
        builder.append("sg.liquid_glass.strong: ").append(SingGramConfig.isLiquidGlassStrongEnabled()).append('\n');
        builder.append("sg.liquid_glass.level: ").append(SingGramConfig.getLiquidGlassLevel()).append('\n');
        builder.append("sg.liquid_glass.custom: ").append(SingGramConfig.isLiquidGlassCustomEnabled()).append('\n');
        builder.append("sg.liquid_glass.thickness_dp: ").append(SingGramConfig.getLiquidGlassThicknessDp()).append('\n');
        builder.append("sg.liquid_glass.intensity_permille: ").append(SingGramConfig.getLiquidGlassIntensityPermille()).append('\n');
        builder.append("sg.liquid_glass.index_permille: ").append(SingGramConfig.getLiquidGlassIndexPermille()).append('\n');
        builder.append("sg.download_boost.enabled: ").append(SingGramConfig.isDownloadBoostEnabled()).append('\n');
        builder.append("sg.download_boost.level: ").append(SingGramConfig.getDownloadBoostLevel()).append('\n');
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            builder.append("sg.account_profile.").append(account).append(".label: ").append(SingGramConfig.getAccountProfileLabel(account)).append('\n');
            builder.append("sg.account_profile.").append(account).append(".group: ").append(SingGramConfig.getAccountProfileGroup(account)).append('\n');
            builder.append("sg.account_profile.").append(account).append(".color: ").append(SingGramConfig.getAccountProfileColor(account)).append('\n');
        }
        builder.append(SingGramChatNotesStore.exportAllNotesForBundle());
        builder.append("sg.crash_safe.enabled: ").append(SingGramConfig.isCrashSafeModeEnabled()).append('\n');
        builder.append("sg.diagnostics.visible: ").append(SingGramConfig.isDiagnosticsEnabled()).append('\n');
        return builder.toString();
    }

    public static boolean importBundle(String text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        boolean imported = false;
        for (String line : text.split("\\n")) {
            int index = line.indexOf(':');
            if (index <= 0) {
                continue;
            }
            String key = line.substring(0, index).trim();
            String value = line.substring(index + 1).trim();
            if (SingGramChatNotesStore.importBundleLine(key, value)) {
                imported = true;
            } else if ("AI base URL".equals(key) || "sg.ai.base_url".equals(key)) {
                SingGramConfig.setAiBaseUrl(value);
                imported = true;
            } else if ("AI model".equals(key) || "sg.ai.model".equals(key)) {
                SingGramConfig.setAiModel(value);
                imported = true;
            } else if ("AI tools".equals(key) || "sg.ai.enabled".equals(key)) {
                SingGramConfig.setAiEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("AI prefer Cantonese".equals(key) || "sg.ai.prefer_cantonese".equals(key)) {
                SingGramConfig.setAiPreferCantonese(Boolean.parseBoolean(value));
                imported = true;
            } else if ("Liquid Glass".equals(key) || "sg.liquid_glass.enabled".equals(key)) {
                SingGramConfig.setLiquidGlassEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("Liquid Glass strong".equals(key) || "sg.liquid_glass.strong".equals(key)) {
                SingGramConfig.setLiquidGlassStrongEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("sg.liquid_glass.level".equals(key)) {
                imported |= importInt(value, SingGramConfig::setLiquidGlassLevel);
            } else if ("sg.liquid_glass.custom".equals(key)) {
                SingGramConfig.setLiquidGlassCustomEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("sg.liquid_glass.thickness_dp".equals(key)) {
                imported |= importInt(value, SingGramConfig::setLiquidGlassThicknessDp);
            } else if ("sg.liquid_glass.intensity_permille".equals(key)) {
                imported |= importInt(value, SingGramConfig::setLiquidGlassIntensityPermille);
            } else if ("sg.liquid_glass.index_permille".equals(key)) {
                imported |= importInt(value, SingGramConfig::setLiquidGlassIndexPermille);
            } else if ("sg.download_boost.enabled".equals(key)) {
                SingGramConfig.setDownloadBoostEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("sg.download_boost.level".equals(key)) {
                imported |= importInt(value, SingGramConfig::setDownloadBoostLevel);
            } else if ("Hide phone".equals(key) || "sg.privacy.hide_phone".equals(key)) {
                SingGramConfig.setHidePhoneInSettings(Boolean.parseBoolean(value));
                imported = true;
            } else if ("Ghost mode".equals(key) || "sg.ghost.enabled".equals(key)) {
                SingGramConfig.setGhostModeEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("sg.ghost.selected_chats_only".equals(key)) {
                SingGramConfig.setGhostSelectedChatsOnly(Boolean.parseBoolean(value));
                imported = true;
            } else if ("sg.ghost.dialog_ids".equals(key)) {
                SingGramConfig.importGhostDialogIds(value);
                imported = true;
            } else if ("sg.ghost.read_receipt_allowed_ids".equals(key)) {
                SingGramConfig.importReadReceiptAllowedDialogIds(value);
                imported = true;
            } else if ("Disable read receipts".equals(key) || "sg.ghost.disable_read_receipts".equals(key)) {
                SingGramConfig.setDisableReadReceiptsEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("Hide typing status".equals(key) || "sg.ghost.hide_typing".equals(key)) {
                SingGramConfig.setHideTypingStatusEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("Keep deleted messages".equals(key) || "sg.protection.keep_deleted".equals(key)) {
                SingGramConfig.setKeepDeletedMessages(Boolean.parseBoolean(value));
                imported = true;
            } else if ("Keep original edits".equals(key) || "sg.protection.keep_edits".equals(key)) {
                SingGramConfig.setKeepOriginalEdits(Boolean.parseBoolean(value));
                imported = true;
            } else if ("AI context menu".equals(key) || "sg.ai.context_menu".equals(key)) {
                SingGramConfig.setAiContextMenuEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("AI translate action".equals(key) || "sg.ai.translate".equals(key)) {
                SingGramConfig.setAiTranslateActionEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("AI insert result".equals(key) || "sg.ai.insert_result".equals(key)) {
                SingGramConfig.setAiInsertResultEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("Quick reply ideas".equals(key) || "sg.ai.reply_ideas".equals(key)) {
                SingGramConfig.setQuickReplyIdeasEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("sg.crash_safe.enabled".equals(key)) {
                SingGramConfig.setCrashSafeModeEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if ("Diagnostics".equals(key) || "sg.diagnostics.visible".equals(key)) {
                SingGramConfig.setDiagnosticsEnabled(Boolean.parseBoolean(value));
                imported = true;
            } else if (key.startsWith("sg.account_profile.") && key.endsWith(".label")) {
                int account = parseProfileAccount(key);
                if (account >= 0) {
                    SingGramConfig.setAccountProfileLabel(account, value);
                    imported = true;
                }
            } else if (key.startsWith("sg.account_profile.") && key.endsWith(".group")) {
                int account = parseProfileAccount(key);
                if (account >= 0) {
                    SingGramConfig.setAccountProfileGroup(account, value);
                    imported = true;
                }
            } else if (key.startsWith("sg.account_profile.") && key.endsWith(".color")) {
                int account = parseProfileAccount(key);
                if (account >= 0) {
                    imported |= importInt(value, color -> SingGramConfig.setAccountProfileColor(account, color));
                }
            }
        }
        return imported;
    }

    private static boolean importInt(String value, IntSetter setter) {
        try {
            setter.set(Integer.parseInt(value));
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    private static int parseProfileAccount(String key) {
        try {
            String prefix = "sg.account_profile.";
            int start = prefix.length();
            int end = key.indexOf('.', start);
            if (end <= start) {
                return -1;
            }
            int account = Integer.parseInt(key.substring(start, end));
            return account >= 0 && account < UserConfig.MAX_ACCOUNT_COUNT ? account : -1;
        } catch (Exception ignore) {
            return -1;
        }
    }

    private interface IntSetter {
        void set(int value);
    }
}
