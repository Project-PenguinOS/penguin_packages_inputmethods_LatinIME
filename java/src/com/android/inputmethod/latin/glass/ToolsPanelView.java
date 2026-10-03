package com.android.inputmethod.latin.glass;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.inputmethod.latin.R;

public final class ToolsPanelView extends ScrollView {
    private static final int COLUMNS = 4;

    private static final int[][] TOOLS = {
        { R.id.glass_toolbar_clipboard, R.drawable.ic_f7_doc_on_clipboard,
                R.string.glass_toolbar_clipboard },
        { R.id.glass_toolbar_edit, R.drawable.ic_f7_text_cursor,
                R.string.glass_toolbar_text_editing },
        { R.id.glass_bar_emoji, R.drawable.ic_f7_smiley, R.string.spoken_description_emoji },
        { R.id.glass_bar_mic, R.drawable.ic_f7_mic, R.string.spoken_description_mic },
        { R.id.glass_toolbar_one_handed, R.drawable.ic_f7_hand_point_right,
                R.string.glass_toolbar_one_handed },
        { R.id.glass_toolbar_floating, R.drawable.ic_f7_rectangle_on_rectangle_angled,
                R.string.glass_toolbar_floating },
        { R.id.glass_toolbar_theme, R.drawable.ic_f7_paintbrush, R.string.glass_toolbar_theme },
        { R.id.glass_toolbar_settings, R.drawable.ic_f7_gear_alt,
                R.string.glass_toolbar_settings },
    };

    public ToolsPanelView(final Context context, final PanelHost host) {
        super(context);
        setFillViewport(true);
        setOverScrollMode(OVER_SCROLL_NEVER);
        final GridLayout grid = new GridLayout(context);
        grid.setColumnCount(COLUMNS);
        final int pad = GlassUi.dp(context, 8);
        grid.setPadding(pad, pad, pad, pad);
        final ColorStateList tint = ColorStateList.valueOf(context.getColor(R.color.glass_icon));
        for (final int[] tool : TOOLS) {
            final LinearLayout tile = new LinearLayout(context);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setBackground(GlassUi.cardBackground(context));
            tile.setContentDescription(context.getString(tool[2]));
            tile.setHapticFeedbackEnabled(false);
            tile.setSoundEffectsEnabled(false);
            final ImageView icon = new ImageView(context);
            icon.setImageResource(tool[1]);
            icon.setImageTintList(tint);
            final int size = GlassUi.dp(context, 26);
            tile.addView(icon, new LinearLayout.LayoutParams(size, size));
            final TextView label = GlassUi.label(context, context.getString(tool[2]), 12, false);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            final LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelLp.topMargin = GlassUi.dp(context, 6);
            tile.addView(label, labelLp);
            final int id = tool[0];
            tile.setOnClickListener(v -> {
                host.performFeedback(v);
                host.onToolAction(id);
            });
            final GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            lp.height = GlassUi.dp(context, 84);
            final int m = GlassUi.dp(context, 4);
            lp.setMargins(m, m, m, m);
            grid.addView(tile, lp);
        }
        addView(grid);
    }
}
