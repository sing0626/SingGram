package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
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

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SingGramConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class SingGramLiquidGlassStudioActivity extends BaseFragment {

    private int thicknessDp;
    private int intensityPermille;
    private int indexPermille;
    private TextView previewView;
    private TextView summaryView;

    private interface IntSetter {
        void set(int value);
    }

    @Override
    public View createView(Context context) {
        thicknessDp = SingGramConfig.getLiquidGlassThicknessDp();
        intensityPermille = SingGramConfig.getLiquidGlassIntensityPermille();
        indexPermille = SingGramConfig.getLiquidGlassIndexPermille();

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramLiquidGlassStudio));
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

        LinearLayout section = addSection(context, container);
        previewView = new TextView(context);
        previewView.setGravity(Gravity.CENTER);
        previewView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        previewView.setTypeface(AndroidUtilities.bold());
        previewView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        previewView.setText(LocaleController.getString(R.string.SingGramLiquidGlassPreview));
        section.addView(previewView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 92, 14, 14, 14, 10));

        summaryView = new TextView(context);
        summaryView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        summaryView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        summaryView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        summaryView.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), AndroidUtilities.dp(4));
        section.addView(summaryView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        addSlider(context, section, LocaleController.getString(R.string.SingGramLiquidGlassThickness), 4, 32, thicknessDp, value -> {
            thicknessDp = value;
            updatePreview();
        });
        addDivider(context, section);
        addSlider(context, section, LocaleController.getString(R.string.SingGramLiquidGlassIntensity), 250, 1200, intensityPermille, value -> {
            intensityPermille = value;
            updatePreview();
        });
        addDivider(context, section);
        addSlider(context, section, LocaleController.getString(R.string.SingGramLiquidGlassRefraction), 1000, 2200, indexPermille, value -> {
            indexPermille = value;
            updatePreview();
        });

        addButton(context, section, LocaleController.getString(R.string.SingGramSaveButton), true, v -> save());
        addButton(context, section, LocaleController.getString(R.string.SingGramLiquidGlassResetStudio), false, v -> reset());
        addInfo(context, container, LocaleController.getString(R.string.SingGramLiquidGlassStudioInfo));
        updatePreview();
        return fragmentView;
    }

    private void save() {
        SingGramConfig.setLiquidGlassEnabled(true);
        SingGramConfig.setLiquidGlassCustomEnabled(true);
        SingGramConfig.setLiquidGlassThicknessDp(thicknessDp);
        SingGramConfig.setLiquidGlassIntensityPermille(intensityPermille);
        SingGramConfig.setLiquidGlassIndexPermille(indexPermille);
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramLiquidGlassChanged), Toast.LENGTH_SHORT).show();
    }

    private void reset() {
        SingGramConfig.resetLiquidGlassStudio();
        removeSelfFromStack();
        presentFragment(new SingGramLiquidGlassStudioActivity());
    }

    private void updatePreview() {
        if (summaryView != null) {
            summaryView.setText(LocaleController.formatString(R.string.SingGramLiquidGlassStudioSummary, thicknessDp, intensityPermille, indexPermille));
        }
        if (previewView != null) {
            int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
            float alpha = Math.max(0.08f, Math.min(0.22f, intensityPermille / 6000.0f));
            previewView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(Math.max(8, Math.min(20, thicknessDp))), Theme.multAlpha(accent, alpha)));
        }
    }

    private LinearLayout addSection(Context context, LinearLayout container) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(section, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 0));
        return section;
    }

    private void addSlider(Context context, LinearLayout container, String title, int min, int max, int value, IntSetter setter) {
        LinearLayout sliderContainer = new LinearLayout(context);
        sliderContainer.setOrientation(LinearLayout.VERTICAL);
        sliderContainer.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        container.addView(sliderContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView label = new TextView(context);
        label.setText(title + ": " + value);
        label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        label.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        sliderContainer.addView(label, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        SeekBar seekBar = new SeekBar(context);
        seekBar.setMax(max - min);
        seekBar.setProgress(Math.max(0, Math.min(value - min, max - min)));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newValue = min + progress;
                label.setText(title + ": " + newValue);
                setter.set(newValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sliderContainer.addView(seekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42));
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
