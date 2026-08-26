package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SingGramBotAuth;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

/** First-class Bot token login. The token stays only in memory for the authorization request. */
public class SingGramBotLoginActivity extends BaseFragment {

    private EditTextBoldCursor tokenField;
    private TextView connectButton;
    private boolean connecting;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramLoginBotAccount));
        actionBar.setAllowOverlayTitle(true);
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1 && !connecting) {
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
        container.setPadding(0, AndroidUtilities.dp(28), 0, AndroidUtilities.dp(28));
        scrollView.addView(container, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        addIntroduction(context, container);
        addHeader(context, container, LocaleController.getString(R.string.SingGramBotToken));
        LinearLayout form = addSection(context, container);
        tokenField = createTokenField(context);
        form.addView(tokenField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, 16, 0, 16, 0));
        addDivider(context, form);
        connectButton = createConnectButton(context);
        form.addView(connectButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52, 16, 8, 16, 12));

        addInfo(context, container, LocaleController.getString(R.string.SingGramBotLoginDialogInfo));
        updateConnectButton();
        return fragmentView;
    }

    @Override
    public void onFragmentDestroy() {
        if (tokenField != null) {
            tokenField.setText("");
        }
        super.onFragmentDestroy();
    }

    private void addIntroduction(Context context, LinearLayout container) {
        TextView title = new TextView(context);
        title.setText(LocaleController.getString(R.string.SingGramLoginBotAccount));
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(Gravity.CENTER);
        title.setIncludeFontPadding(false);
        container.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 0, 24, 0));

        TextView subtitle = new TextView(context);
        subtitle.setText(LocaleController.getString(R.string.SingGramLoginBotAccountInfo));
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        subtitle.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(24));
        container.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 0, 24, 0));
    }

    private EditTextBoldCursor createTokenField(Context context) {
        EditTextBoldCursor field = new EditTextBoldCursor(context);
        field.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        field.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        field.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        field.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        field.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setHint(LocaleController.getString(R.string.SingGramBotTokenHint));
        field.setContentDescription(LocaleController.getString(R.string.SingGramBotToken));
        field.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
        field.setBackgroundColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateConnectButton();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        return field;
    }

    private TextView createConnectButton(Context context) {
        TextView button = new TextView(context);
        button.setText(LocaleController.getString(R.string.SingGramBotConnect));
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        button.setTypeface(AndroidUtilities.bold());
        button.setGravity(Gravity.CENTER);
        button.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), accent, Theme.multAlpha(accent, 0.82f)));
        button.setContentDescription(LocaleController.getString(R.string.SingGramBotConnect));
        button.setOnClickListener(v -> connectBot());
        return button;
    }

    private void updateConnectButton() {
        if (connectButton == null) {
            return;
        }
        boolean valid = !connecting && tokenField != null && SingGramBotAuth.isValidToken(tokenField.getText().toString().trim());
        connectButton.setEnabled(valid);
        connectButton.setAlpha(valid ? 1.0f : 0.5f);
    }

    private void connectBot() {
        if (connecting || tokenField == null) {
            return;
        }
        String token = tokenField.getText().toString().trim();
        if (!SingGramBotAuth.isValidToken(token)) {
            tokenField.setErrorText(LocaleController.getString(R.string.SingGramBotTokenInvalid));
            return;
        }
        tokenField.setErrorText(null);
        tokenField.setText("");
        tokenField.clearFocus();
        connecting = true;
        updateConnectButton();

        AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.setCanCancel(false);
        progress.show();
        SingGramBotAuth.login(token, new SingGramBotAuth.Callback() {
            @Override
            public void onSuccess(int account, TLRPC.User bot) {
                dismissProgress(progress);
                connecting = false;
                updateConnectButton();
                showSuccess(account, bot);
            }

            @Override
            public void onError(String error) {
                dismissProgress(progress);
                connecting = false;
                updateConnectButton();
                showFailure(error);
            }
        });
    }

    private void showSuccess(int account, TLRPC.User bot) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.SingGramBotConnected));
        builder.setMessage(LocaleController.formatString(R.string.SingGramBotConnectedInfo, UserObject.getUserName(bot), account + 1));
        builder.setPositiveButton(LocaleController.getString(R.string.SingGramBotSwitch), (dialog, which) -> openAuthorizedAccount(account));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showFailure(String error) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.SingGramBotLoginFailed));
        builder.setMessage(LocaleController.formatString(R.string.SingGramBotLoginFailedInfo, error));
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        showDialog(builder.create());
    }

    private void openAuthorizedAccount(int account) {
        if (LaunchActivity.instance != null) {
            LaunchActivity.instance.openAuthorizedAccount(account);
        } else {
            presentFragment(new MainTabsActivity(), true);
        }
    }

    private void dismissProgress(AlertDialog progress) {
        try {
            progress.dismiss();
        } catch (Throwable ignore) {
        }
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
        textView.setTypeface(AndroidUtilities.bold());
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(8));
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
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(10), AndroidUtilities.dp(24), 0);
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> descriptions = new ArrayList<>();
        descriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        return descriptions;
    }
}
