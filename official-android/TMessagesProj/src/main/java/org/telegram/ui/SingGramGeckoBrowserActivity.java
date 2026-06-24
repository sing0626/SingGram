package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebRequestError;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SingGramConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class SingGramGeckoBrowserActivity extends BasePermissionsActivity {

    private static final String EXTRA_URL = "url";
    private static final int GECKO_MIN_SDK = Build.VERSION_CODES.O;

    private GeckoSession session;
    private GeckoView geckoView;
    private TextView titleView;
    private TextView urlView;
    private ProgressBar progressBar;
    private ImageView backButton;
    private ImageView forwardButton;
    private String currentUrl;
    private boolean canGoBack;
    private boolean canGoForward;

    public static boolean openIfEnabled(Context context, String url) {
        if (context == null || TextUtils.isEmpty(url) || !SingGramConfig.shouldUseGeckoBrowser() || Build.VERSION.SDK_INT < GECKO_MIN_SDK) {
            return false;
        }
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return false;
        }
        try {
            Intent intent = new Intent(context, SingGramGeckoBrowserActivity.class);
            intent.putExtra(EXTRA_URL, url);
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_URL);
        if (TextUtils.isEmpty(url)) {
            finish();
            return;
        }
        currentUrl = url;
        configureWindow();
        setContentView(createContentView());
        openSession(url);
    }

    private void configureWindow() {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        int background = Theme.getColor(Theme.key_windowBackgroundWhite);
        int text = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
        window.setStatusBarColor(background);
        window.setNavigationBarColor(background);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && AndroidUtilities.computePerceivedBrightness(background) > 0.721f) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0));
        }
        getWindow().getDecorView().setBackgroundColor(background);
    }

    private View createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(6), dp(6), dp(6), dp(4));
        bar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        root.addView(bar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, dp(58)));

        ImageView closeButton = addIconButton(bar, R.drawable.ic_ab_back, v -> finish());

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.CENTER_VERTICAL);
        titleView = new TextView(this);
        titleView.setText(LocaleController.getString(R.string.SingGramAIBrowser));
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        urlView = new TextView(this);
        urlView.setText(currentUrl);
        urlView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        urlView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        urlView.setSingleLine(true);
        urlView.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(urlView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        bar.addView(titles, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1, Gravity.CENTER_VERTICAL, 4, 0, 4, 0));

        addIconButton(bar, R.drawable.msg_reset, v -> {
            if (session != null) {
                session.reload();
            }
        });
        addIconButton(bar, R.drawable.msg_share, v -> shareCurrentUrl());
        addIconButton(bar, R.drawable.msg_openin, v -> Browser.openInExternalBrowser(this, currentUrl, true));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        root.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, dp(2)));

        FrameLayout content = new FrameLayout(this);
        geckoView = new GeckoView(this);
        content.addView(geckoView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        root.addView(content, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        backButton = addIconButton(nav, R.drawable.msg_arrow_back, v -> {
            if (session != null && canGoBack) {
                session.goBack();
            }
        });
        forwardButton = addIconButton(nav, R.drawable.msg_arrow_forward, v -> {
            if (session != null && canGoForward) {
                session.goForward();
            }
        });
        root.addView(nav, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, dp(52)));
        updateNavigationButtons();
        return root;
    }

    private ImageView addIconButton(LinearLayout parent, int icon, View.OnClickListener listener) {
        ImageView button = new ImageView(this);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setImageResource(icon);
        button.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.SRC_IN));
        button.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        button.setOnClickListener(listener);
        parent.addView(button, LayoutHelper.createLinear(dp(46), dp(46)));
        return button;
    }

    private void openSession(String url) {
        try {
            GeckoRuntime runtime = GeckoRuntime.getDefault(this);
            session = new GeckoSession();
            session.setContentDelegate(new GeckoSession.ContentDelegate() {
                @Override
                public void onTitleChange(GeckoSession session, String title) {
                    titleView.setText(TextUtils.isEmpty(title) ? LocaleController.getString(R.string.SingGramAIBrowser) : title);
                }

                @Override
                public void onCrash(GeckoSession session) {
                    Toast.makeText(SingGramGeckoBrowserActivity.this, LocaleController.getString(R.string.SingGramBrowserEngineFallback), Toast.LENGTH_LONG).show();
                    finish();
                }
            });
            session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
                @Override
                public void onLocationChange(GeckoSession session, String url, java.util.List<GeckoSession.PermissionDelegate.ContentPermission> perms, Boolean hasUserGesture) {
                    currentUrl = url;
                    urlView.setText(url);
                }

                @Override
                public void onCanGoBack(GeckoSession session, boolean canGoBack) {
                    SingGramGeckoBrowserActivity.this.canGoBack = canGoBack;
                    updateNavigationButtons();
                }

                @Override
                public void onCanGoForward(GeckoSession session, boolean canGoForward) {
                    SingGramGeckoBrowserActivity.this.canGoForward = canGoForward;
                    updateNavigationButtons();
                }

                @Override
                public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession session, GeckoSession.NavigationDelegate.LoadRequest request) {
                    if (request == null || TextUtils.isEmpty(request.uri)) {
                        return GeckoResult.allow();
                    }
                    Uri uri = Uri.parse(request.uri);
                    String scheme = uri.getScheme();
                    if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                        return GeckoResult.allow();
                    }
                    Browser.openInExternalBrowser(SingGramGeckoBrowserActivity.this, request.uri, true);
                    return GeckoResult.deny();
                }

                @Override
                public GeckoResult<String> onLoadError(GeckoSession session, String uri, WebRequestError error) {
                    return GeckoResult.fromValue(null);
                }
            });
            session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
                @Override
                public void onPageStart(GeckoSession session, String url) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(8);
                }

                @Override
                public void onProgressChange(GeckoSession session, int progress) {
                    progressBar.setProgress(progress);
                }

                @Override
                public void onPageStop(GeckoSession session, boolean success) {
                    progressBar.setProgress(100);
                    progressBar.setVisibility(View.GONE);
                }
            });
            session.open(runtime);
            geckoView.setSession(session);
            session.loadUri(url);
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(this, LocaleController.getString(R.string.SingGramBrowserEngineFallback), Toast.LENGTH_LONG).show();
            Browser.openInTelegramBrowser(this, url, null);
            finish();
        }
    }

    private void updateNavigationButtons() {
        if (backButton != null) {
            backButton.setAlpha(canGoBack ? 1.0f : 0.35f);
        }
        if (forwardButton != null) {
            forwardButton.setAlpha(canGoForward ? 1.0f : 0.35f);
        }
    }

    private void shareCurrentUrl() {
        if (TextUtils.isEmpty(currentUrl)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, currentUrl);
        startActivity(Intent.createChooser(intent, LocaleController.getString(R.string.ShareFile)));
    }

    @Override
    public void onBackPressed() {
        if (session != null && canGoBack) {
            session.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }
}
