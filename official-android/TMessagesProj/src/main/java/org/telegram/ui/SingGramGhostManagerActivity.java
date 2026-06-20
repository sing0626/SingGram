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
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SingGramConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class SingGramGhostManagerActivity extends BaseFragment {

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramGhostManager));
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

        addHeader(context, container, LocaleController.getString(R.string.SingGramGhostManagerSelected));
        LinearLayout selectedSection = addSection(context, container);
        addDialogList(context, selectedSection, sortedIds(SingGramConfig.getGhostDialogIdSnapshot()), false);

        addHeader(context, container, LocaleController.getString(R.string.SingGramGhostManagerReadExceptions));
        LinearLayout readSection = addSection(context, container);
        addDialogList(context, readSection, sortedIds(SingGramConfig.getReadReceiptAllowedDialogIdSnapshot()), true);

        addHeader(context, container, LocaleController.getString(R.string.SingGramGhostManagerActions));
        LinearLayout actionSection = addSection(context, container);
        addActionCell(context, actionSection, LocaleController.getString(R.string.SingGramCopyGhostChats), LocaleController.getString(R.string.SingGramCopyGhostChatsInfo), SingGramConfig.getGhostDialogCount() > 0, v -> copyGhostDialogIds());
        addDivider(context, actionSection);
        addActionCell(context, actionSection, LocaleController.getString(R.string.SingGramClearGhostChats), LocaleController.getString(R.string.SingGramClearGhostChatsInfo), SingGramConfig.getGhostDialogCount() > 0, v -> confirmClear(false));
        addDivider(context, actionSection);
        addActionCell(context, actionSection, LocaleController.getString(R.string.SingGramGhostManagerCopyReadExceptions), LocaleController.getString(R.string.SingGramGhostManagerCopyReadExceptionsInfo), SingGramConfig.getReadReceiptsAllowedDialogCount() > 0, v -> copyReadExceptions());
        addDivider(context, actionSection);
        addActionCell(context, actionSection, LocaleController.getString(R.string.SingGramGhostManagerClearReadExceptions), LocaleController.getString(R.string.SingGramGhostManagerClearReadExceptionsInfo), SingGramConfig.getReadReceiptsAllowedDialogCount() > 0, v -> confirmClear(true));

        addInfo(context, container, LocaleController.getString(R.string.SingGramGhostManagerInfo));
        return fragmentView;
    }

    private void addDialogList(Context context, LinearLayout container, ArrayList<String> ids, boolean readException) {
        if (ids.isEmpty()) {
            addInfoCell(context, container, readException ? LocaleController.getString(R.string.SingGramGhostManagerReadExceptionsEmpty) : LocaleController.getString(R.string.SingGramGhostChatsEmpty), "");
            return;
        }
        boolean added = false;
        for (String id : ids) {
            if (added) {
                addDivider(context, container);
            }
            long dialogId = parseDialogId(id);
            addDialogCell(context, container, dialogId, id, readException);
            added = true;
        }
    }

    private void addDialogCell(Context context, LinearLayout container, long dialogId, String rawId, boolean readException) {
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValue(dialogTitle(dialogId, rawId), LocaleController.formatString(R.string.SingGramGhostManagerDialogValue, rawId), true, false);
        cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        cell.setOnClickListener(v -> showRemoveDialog(dialogId, rawId, readException));
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void showRemoveDialog(long dialogId, String rawId, boolean readException) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(dialogTitle(dialogId, rawId));
        builder.setMessage(LocaleController.getString(readException ? R.string.SingGramGhostManagerRemoveReadExceptionInfo : R.string.SingGramGhostManagerRemoveGhostInfo));
        builder.setPositiveButton(LocaleController.getString(R.string.SingGramGhostManagerRemove), (dialog, which) -> {
            if (dialogId != 0) {
                if (readException) {
                    SingGramConfig.setReadReceiptsAllowedForDialog(dialogId, false);
                } else {
                    SingGramConfig.setGhostModeForDialog(dialogId, false);
                }
            }
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramSettingsSaved), Toast.LENGTH_SHORT).show();
            refresh();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
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

    private void copyReadExceptions() {
        String ids = SingGramConfig.exportReadReceiptAllowedDialogIds();
        if (TextUtils.isEmpty(ids)) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramGhostManagerReadExceptionsEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        AndroidUtilities.addToClipboard(ids);
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramGhostManagerReadExceptionsCopied), Toast.LENGTH_SHORT).show();
    }

    private void confirmClear(boolean readExceptions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(readExceptions ? R.string.SingGramGhostManagerClearReadExceptions : R.string.SingGramClearGhostChats));
        builder.setMessage(LocaleController.getString(readExceptions ? R.string.SingGramGhostManagerClearReadExceptionsInfo : R.string.SingGramClearGhostChatsInfo));
        builder.setPositiveButton(LocaleController.getString(R.string.ClearButton), (dialog, which) -> {
            if (readExceptions) {
                SingGramConfig.importReadReceiptAllowedDialogIds("");
            } else {
                SingGramConfig.importGhostDialogIds("");
            }
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramSettingsSaved), Toast.LENGTH_SHORT).show();
            refresh();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void refresh() {
        removeSelfFromStack();
        presentFragment(new SingGramGhostManagerActivity());
    }

    private ArrayList<String> sortedIds(Set<String> ids) {
        ArrayList<String> result = new ArrayList<>(ids);
        Collections.sort(result, (a, b) -> Long.compare(parseDialogId(a), parseDialogId(b)));
        return result;
    }

    private long parseDialogId(String id) {
        if (TextUtils.isEmpty(id)) {
            return 0;
        }
        try {
            return Long.parseLong(id);
        } catch (Exception ignore) {
            return 0;
        }
    }

    private String dialogTitle(long dialogId, String rawId) {
        if (dialogId != 0) {
            String name = DialogObject.getName(UserConfig.selectedAccount, dialogId);
            if (!TextUtils.isEmpty(name)) {
                return name;
            }
        }
        return rawId;
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
