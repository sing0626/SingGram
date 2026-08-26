package org.telegram.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.DateFormat;
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
import org.telegram.messenger.SingGramChatNotesStore;
import org.telegram.messenger.SingGramWorkspaceConfig;
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
import java.util.Calendar;

public class SingGramChatNotesActivity extends BaseFragment {

    private final long dialogId;
    private final int account;
    private EditTextBoldCursor tagsField;
    private EditTextBoldCursor reminderField;
    private EditTextBoldCursor noteField;
    private EditTextBoldCursor groupKeywordsField;

    public SingGramChatNotesActivity(long dialogId) {
        this(dialogId, UserConfig.selectedAccount);
    }

    public SingGramChatNotesActivity(long dialogId, int account) {
        this.dialogId = dialogId;
        this.account = account;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramChatNotes));
        actionBar.setAllowOverlayTitle(true);
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    save();
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

        LinearLayout section = addSection(context, container);
        addInfoCell(context, section, LocaleController.getString(R.string.SingGramChatNotesDialog), String.valueOf(dialogId));
        addDivider(context, section);
        tagsField = addField(context, section, LocaleController.getString(R.string.SingGramChatNotesTags), LocaleController.getString(R.string.SingGramChatNotesTagsHint), SingGramChatNotesStore.getTags(dialogId), false);
        addDivider(context, section);
        reminderField = addField(context, section, LocaleController.getString(R.string.SingGramChatNotesReminder), LocaleController.getString(R.string.SingGramChatNotesReminderHint), SingGramChatNotesStore.getReminder(dialogId), false);
        addDivider(context, section);
        noteField = addField(context, section, LocaleController.getString(R.string.SingGramChatNotesNote), LocaleController.getString(R.string.SingGramChatNotesNoteHint), SingGramChatNotesStore.getNote(dialogId), true);

        addButton(context, section, LocaleController.getString(R.string.Save), true, v -> {
            save();
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramChatNotesSaved), Toast.LENGTH_SHORT).show();
        });
        addButton(context, section, LocaleController.getString(R.string.Copy), false, v -> copy());
        addButton(context, section, LocaleController.getString(R.string.ClearButton), false, v -> confirmClear());

        addHeader(context, container, LocaleController.getString(R.string.SingGramFollowUp));
        LinearLayout followUpSection = addSection(context, container);
        addInfoCell(context, followUpSection, LocaleController.getString(R.string.SingGramFollowUp), followUpSummary());
        addButton(context, followUpSection, LocaleController.getString(R.string.SingGramFollowUpSet), false, v -> chooseFollowUpDate());
        addButton(context, followUpSection, LocaleController.getString(SingGramChatNotesStore.isFollowUpComplete(dialogId) ? R.string.SingGramFollowUpReopen : R.string.SingGramFollowUpComplete), false, v -> toggleFollowUpComplete());
        if (SingGramChatNotesStore.getFollowUpDueAt(dialogId) > 0) {
            addButton(context, followUpSection, LocaleController.getString(R.string.SingGramFollowUpClear), false, v -> clearFollowUp());
        }

        addHeader(context, container, LocaleController.getString(R.string.SingGramChatControls));
        LinearLayout policySection = addSection(context, container);
        addToggle(context, policySection, LocaleController.getString(R.string.SingGramPriorityNotifications), LocaleController.getString(R.string.SingGramPriorityNotificationsInfo), SingGramWorkspaceConfig.isPriorityDialog(account, dialogId), enabled -> SingGramWorkspaceConfig.setPriorityDialog(account, dialogId, enabled));
        addDivider(context, policySection);
        addToggle(context, policySection, LocaleController.getString(R.string.SingGramSensitiveChat), LocaleController.getString(R.string.SingGramSensitiveChatInfo), SingGramWorkspaceConfig.isSensitiveDialog(account, dialogId), enabled -> SingGramWorkspaceConfig.setSensitiveDialog(account, dialogId, enabled));
        if (DialogObject.isChatDialog(dialogId)) {
            addDivider(context, policySection);
            addToggle(context, policySection, LocaleController.getString(R.string.SingGramGroupFocusChat), LocaleController.getString(R.string.SingGramGroupFocusChatInfo), SingGramWorkspaceConfig.isWatchedGroup(account, dialogId), enabled -> SingGramWorkspaceConfig.setWatchedGroup(account, dialogId, enabled));
            addDivider(context, policySection);
            groupKeywordsField = addField(context, policySection, LocaleController.getString(R.string.SingGramGroupKeywords), LocaleController.getString(R.string.SingGramGroupKeywordsHint), SingGramWorkspaceConfig.getGroupKeywords(account, dialogId), false);
        }

        addInfo(context, container, LocaleController.getString(R.string.SingGramChatNotesInfo));
        return fragmentView;
    }

    @Override
    public void onFragmentDestroy() {
        save();
        super.onFragmentDestroy();
    }

    private void save() {
        if (tagsField != null) {
            SingGramChatNotesStore.setTags(dialogId, tagsField.getText().toString());
        }
        if (reminderField != null) {
            SingGramChatNotesStore.setReminder(dialogId, reminderField.getText().toString());
        }
        if (noteField != null) {
            SingGramChatNotesStore.setNote(dialogId, noteField.getText().toString());
        }
        if (groupKeywordsField != null) {
            SingGramWorkspaceConfig.setGroupKeywords(account, dialogId, groupKeywordsField.getText().toString());
        }
    }

    private void copy() {
        save();
        if (!SingGramChatNotesStore.hasNotes(dialogId)) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramChatNotesEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        AndroidUtilities.addToClipboard(SingGramChatNotesStore.exportNote(dialogId));
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.TextCopied), Toast.LENGTH_SHORT).show();
    }

    private void confirmClear() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.SingGramChatNotesClear));
        builder.setMessage(LocaleController.getString(R.string.SingGramChatNotesClearInfo));
        builder.setPositiveButton(LocaleController.getString(R.string.ClearButton), (dialog, which) -> {
            SingGramChatNotesStore.clear(dialogId);
            if (tagsField != null) {
                tagsField.setText("");
            }
            if (reminderField != null) {
                reminderField.setText("");
            }
            if (noteField != null) {
                noteField.setText("");
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void chooseFollowUpDate() {
        Calendar calendar = Calendar.getInstance();
        long currentDueAt = SingGramChatNotesStore.getFollowUpDueAt(dialogId);
        if (currentDueAt > System.currentTimeMillis()) {
            calendar.setTimeInMillis(currentDueAt);
        }
        DatePickerDialog dialog = new DatePickerDialog(getParentActivity(), (view, year, month, day) -> chooseFollowUpTime(year, month, day), calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 60_000L);
        dialog.show();
    }

    private void chooseFollowUpTime(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        long currentDueAt = SingGramChatNotesStore.getFollowUpDueAt(dialogId);
        if (currentDueAt > System.currentTimeMillis()) {
            calendar.setTimeInMillis(currentDueAt);
        }
        TimePickerDialog dialog = new TimePickerDialog(getParentActivity(), (view, hourOfDay, minute) -> {
            Calendar dueAt = Calendar.getInstance();
            dueAt.set(year, month, day, hourOfDay, minute, 0);
            dueAt.set(Calendar.MILLISECOND, 0);
            if (dueAt.getTimeInMillis() <= System.currentTimeMillis()) {
                Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramFollowUpFutureOnly), Toast.LENGTH_SHORT).show();
                return;
            }
            save();
            SingGramChatNotesStore.setFollowUpDueAt(dialogId, account, dueAt.getTimeInMillis());
            reload();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), DateFormat.is24HourFormat(getParentActivity()));
        dialog.show();
    }

    private void toggleFollowUpComplete() {
        long dueAt = SingGramChatNotesStore.getFollowUpDueAt(dialogId);
        if (dueAt <= 0) {
            chooseFollowUpDate();
            return;
        }
        save();
        SingGramChatNotesStore.setFollowUpComplete(dialogId, !SingGramChatNotesStore.isFollowUpComplete(dialogId));
        reload();
    }

    private void clearFollowUp() {
        SingGramChatNotesStore.setFollowUpDueAt(dialogId, 0);
        reload();
    }

    private String followUpSummary() {
        long dueAt = SingGramChatNotesStore.getFollowUpDueAt(dialogId);
        if (dueAt <= 0) {
            return LocaleController.getString(R.string.SingGramFollowUpNotSet);
        }
        String due = LocaleController.formatDateTime(dueAt / 1000, true);
        if (SingGramChatNotesStore.isFollowUpComplete(dialogId)) {
            return LocaleController.formatString(R.string.SingGramFollowUpCompletedValue, due);
        }
        if (dueAt <= System.currentTimeMillis()) {
            return LocaleController.formatString(R.string.SingGramFollowUpOverdueValue, due);
        }
        return LocaleController.formatString(R.string.SingGramFollowUpDueValue, due);
    }

    private void reload() {
        removeSelfFromStack();
        presentFragment(new SingGramChatNotesActivity(dialogId, account));
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

    private void addToggle(Context context, LinearLayout container, String title, String value, boolean checked, ToggleSetter setter) {
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValueAndCheck(title, value, checked, true, false);
        cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        cell.setOnClickListener(v -> {
            boolean enabled = !cell.isChecked();
            cell.setChecked(enabled);
            setter.set(enabled);
        });
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private interface ToggleSetter {
        void set(boolean enabled);
    }

    private void addInfoCell(Context context, LinearLayout container, String text, String value) {
        TextView textView = new TextView(context);
        textView.setText(text + "\n" + value);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
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
            editText.setMinLines(5);
            editText.setMaxLines(12);
            editText.setSingleLine(false);
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            fieldContainer.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 150));
        } else {
            editText.setSingleLine(true);
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            fieldContainer.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 34));
        }
        return editText;
    }

    private void addButton(Context context, LinearLayout container, String text, boolean primary, View.OnClickListener listener) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        button.setTypeface(AndroidUtilities.bold());
        button.setIncludeFontPadding(false);
        if (primary) {
            button.setTextColor(Color.WHITE);
            button.setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed), 8, 8));
        } else {
            int accentColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
            button.setTextColor(accentColor);
            button.setBackground(Theme.createRadSelectorDrawable(Theme.multAlpha(accentColor, 0.10f), Theme.getColor(Theme.key_listSelector), 8, 8));
        }
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setOnClickListener(listener);
        container.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 14, 10, 14, 0));
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
