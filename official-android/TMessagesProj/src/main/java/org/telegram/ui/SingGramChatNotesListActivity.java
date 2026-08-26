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
import org.telegram.messenger.SingGramChatNotesStore;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class SingGramChatNotesListActivity extends BaseFragment {

    private LinearLayout listSection;
    private EditTextBoldCursor searchField;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramChatNotesAll));
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
                refreshNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        addHeader(context, container, LocaleController.formatString(R.string.SingGramChatNotesAllCount, SingGramChatNotesStore.getNotesCount()));
        listSection = addSection(context, container);
        refreshNotes();

        LinearLayout actions = addSection(context, container);
        addButton(context, actions, LocaleController.getString(R.string.SingGramChatNotesCopyAll), v -> copyAll());
        addInfo(context, container, LocaleController.getString(R.string.SingGramChatNotesAllInfo));
        return fragmentView;
    }

    private void refreshNotes() {
        if (listSection == null) {
            return;
        }
        listSection.removeAllViews();
        ArrayList<Long> dialogIds = SingGramChatNotesStore.getNotedDialogIds();
        String query = searchField == null ? "" : searchField.getText().toString().trim().toLowerCase();
        boolean added = false;
        for (int i = 0; i < dialogIds.size(); i++) {
            long dialogId = dialogIds.get(i);
            if (!matchesQuery(dialogId, query)) {
                continue;
            }
            if (added) {
                addDivider(listSection.getContext(), listSection);
            }
            addNoteCell(listSection.getContext(), listSection, dialogId);
            added = true;
        }
        if (!added) {
            addInfoCell(listSection.getContext(), listSection, TextUtils.isEmpty(query) ? LocaleController.getString(R.string.SingGramChatNotesEmpty) : LocaleController.getString(R.string.SingGramChatNotesNoResults), "");
        }
    }

    private boolean matchesQuery(long dialogId, String query) {
        if (TextUtils.isEmpty(query)) {
            return true;
        }
        String haystack = dialogId + " "
                + SingGramChatNotesStore.getTags(dialogId) + " "
                + SingGramChatNotesStore.getReminder(dialogId) + " "
                + SingGramChatNotesStore.getFollowUpDueAt(dialogId) + " "
                + SingGramChatNotesStore.getNote(dialogId);
        return haystack.toLowerCase().contains(query);
    }

    private void addNoteCell(Context context, LinearLayout container, long dialogId) {
        TextView textView = new TextView(context);
        textView.setText(formatNote(dialogId));
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        textView.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        textView.setOnClickListener(v -> presentFragment(new SingGramChatNotesActivity(dialogId, UserConfig.selectedAccount)));
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private CharSequence formatNote(long dialogId) {
        StringBuilder builder = new StringBuilder();
        builder.append(LocaleController.formatString(R.string.SingGramChatNotesDialogValue, String.valueOf(dialogId)));
        String tags = SingGramChatNotesStore.getTags(dialogId);
        String reminder = SingGramChatNotesStore.getReminder(dialogId);
        String note = SingGramChatNotesStore.getNote(dialogId);
        long followUpDueAt = SingGramChatNotesStore.getFollowUpDueAt(dialogId);
        if (!TextUtils.isEmpty(tags)) {
            builder.append('\n').append(tags);
        }
        if (!TextUtils.isEmpty(reminder)) {
            builder.append('\n').append(reminder);
        }
        if (followUpDueAt > 0) {
            builder.append('\n').append(LocaleController.getString(SingGramChatNotesStore.isFollowUpComplete(dialogId) ? R.string.SingGramFollowUpCompleted : R.string.SingGramFollowUp));
            builder.append(": ").append(LocaleController.formatDateTime(followUpDueAt / 1000, true));
        }
        if (!TextUtils.isEmpty(note)) {
            String singleLine = note.replace('\n', ' ').trim();
            builder.append('\n').append(singleLine.length() > 96 ? singleLine.substring(0, 96) + "..." : singleLine);
        }
        return builder;
    }

    private void copyAll() {
        if (SingGramChatNotesStore.getNotesCount() == 0) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramChatNotesEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        AndroidUtilities.addToClipboard(SingGramChatNotesStore.exportAllNotesText());
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.TextCopied), Toast.LENGTH_SHORT).show();
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
        editText.setHint(LocaleController.getString(R.string.SingGramChatNotesSearchHint));
        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52));
        return editText;
    }

    private LinearLayout addSection(Context context, LinearLayout container) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(section, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, AndroidUtilities.dp(12)));
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
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(6), AndroidUtilities.dp(24), AndroidUtilities.dp(8));
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

    private void addButton(Context context, LinearLayout container, String text, View.OnClickListener listener) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        button.setTypeface(AndroidUtilities.bold());
        button.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        button.setBackground(Theme.createRadSelectorDrawable(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), 0.10f), Theme.getColor(Theme.key_listSelector), 8, 8));
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setOnClickListener(listener);
        container.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 14, 10, 14, 10));
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
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(2), AndroidUtilities.dp(24), 0);
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        return themeDescriptions;
    }
}
