package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SingGramConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class SingGramAccountProfilesActivity extends BaseFragment {

    private EditTextBoldCursor labelField;
    private EditTextBoldCursor groupField;
    private int colorIndex;
    private TextView colorLabel;

    @Override
    public View createView(Context context) {
        int account = UserConfig.selectedAccount;
        colorIndex = SingGramConfig.getAccountProfileColor(account);

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramAccountProfiles));
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

        addHeader(context, container, LocaleController.getString(R.string.SingGramAccountProfilesCurrent));
        LinearLayout editorSection = addSection(context, container);
        addInfoCell(context, editorSection, accountDisplayName(account), currentProfileSummary(account));
        addDivider(context, editorSection);
        labelField = addField(context, editorSection, LocaleController.getString(R.string.SingGramAccountProfileLabel), LocaleController.getString(R.string.SingGramAccountProfileLabelHint), SingGramConfig.getAccountProfileLabel(account));
        addDivider(context, editorSection);
        groupField = addField(context, editorSection, LocaleController.getString(R.string.SingGramAccountProfileGroup), LocaleController.getString(R.string.SingGramAccountProfileGroupHint), SingGramConfig.getAccountProfileGroup(account));
        addDivider(context, editorSection);
        addColorSlider(context, editorSection, colorIndex);
        addButton(context, editorSection, LocaleController.getString(R.string.Save), true, v -> {
            save();
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAccountProfilesSaved), Toast.LENGTH_SHORT).show();
            removeSelfFromStack();
            presentFragment(new SingGramAccountProfilesActivity());
        });
        addButton(context, editorSection, LocaleController.getString(R.string.ClearButton), false, v -> {
            labelField.setText("");
            groupField.setText("");
            colorIndex = 0;
            save();
            removeSelfFromStack();
            presentFragment(new SingGramAccountProfilesActivity());
        });

        addHeader(context, container, LocaleController.getString(R.string.SingGramAccountProfilesAll));
        LinearLayout listSection = addSection(context, container);
        boolean added = false;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!AccountInstance.getInstance(a).getUserConfig().isClientActivated()) {
                continue;
            }
            if (added) {
                addDivider(context, listSection);
            }
            addProfileCell(context, listSection, a);
            added = true;
        }
        if (!added) {
            addInfoCell(context, listSection, LocaleController.getString(R.string.SingGramAccountProfilesEmpty), "");
        }
        addButton(context, listSection, LocaleController.getString(R.string.SingGramCopyAccountProfiles), false, v -> copyProfiles());

        addInfo(context, container, LocaleController.getString(R.string.SingGramAccountProfilesInfo));
        return fragmentView;
    }

    @Override
    public void onFragmentDestroy() {
        save();
        super.onFragmentDestroy();
    }

    private void save() {
        int account = UserConfig.selectedAccount;
        if (labelField != null) {
            SingGramConfig.setAccountProfileLabel(account, labelField.getText().toString());
        }
        if (groupField != null) {
            SingGramConfig.setAccountProfileGroup(account, groupField.getText().toString());
        }
        SingGramConfig.setAccountProfileColor(account, colorIndex);
    }

    private void copyProfiles() {
        save();
        String text = SingGramConfig.exportAccountProfiles();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAccountProfilesEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        AndroidUtilities.addToClipboard(text);
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.TextCopied), Toast.LENGTH_SHORT).show();
    }

    private void addProfileCell(Context context, LinearLayout container, int account) {
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValue(accountDisplayName(account), currentProfileSummary(account), true, false);
        cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private String accountDisplayName(int account) {
        TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
        String name = user == null ? "Account " + (account + 1) : UserObject.getUserName(user);
        return LocaleController.formatString(R.string.SingGramAccountProfileAccount, account + 1, name);
    }

    private String currentProfileSummary(int account) {
        String label = SingGramConfig.getAccountProfileLabel(account);
        String group = SingGramConfig.getAccountProfileGroup(account);
        int color = SingGramConfig.getAccountProfileColor(account);
        if (TextUtils.isEmpty(label) && TextUtils.isEmpty(group) && color == 0) {
            return LocaleController.getString(R.string.SingGramAccountProfileUnset);
        }
        return LocaleController.formatString(R.string.SingGramAccountProfileSummary, TextUtils.isEmpty(label) ? "-" : label, TextUtils.isEmpty(group) ? "-" : group, color);
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

    private EditTextBoldCursor addField(Context context, LinearLayout container, String label, String hint, String value) {
        LinearLayout fieldContainer = new LinearLayout(context);
        fieldContainer.setOrientation(LinearLayout.VERTICAL);
        fieldContainer.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(9));
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
        editText.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        editText.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.setIncludeFontPadding(false);
        editText.setHint(hint);
        editText.setText(value == null ? "" : value);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        fieldContainer.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 34));
        return editText;
    }

    private void addColorSlider(Context context, LinearLayout container, int value) {
        LinearLayout sliderContainer = new LinearLayout(context);
        sliderContainer.setOrientation(LinearLayout.VERTICAL);
        sliderContainer.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        container.addView(sliderContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        colorLabel = new TextView(context);
        colorLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        colorLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        colorLabel.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        sliderContainer.addView(colorLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        SeekBar seekBar = new SeekBar(context);
        seekBar.setMax(7);
        seekBar.setProgress(Math.max(0, Math.min(value, 7)));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                colorIndex = progress;
                updateColorLabel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sliderContainer.addView(seekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42));
        updateColorLabel();
    }

    private void updateColorLabel() {
        if (colorLabel != null) {
            colorLabel.setText(LocaleController.formatString(R.string.SingGramAccountProfileColor, colorIndex));
        }
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
