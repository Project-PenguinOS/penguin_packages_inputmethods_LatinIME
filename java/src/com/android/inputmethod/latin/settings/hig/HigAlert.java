package com.android.inputmethod.latin.settings.hig;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.inputmethod.latin.R;

import java.util.ArrayList;
import java.util.List;

public final class HigAlert {

    public enum Style { DEFAULT, CANCEL, DESTRUCTIVE }

    public interface Handler {
        void onAction();
    }

    private static final class Action {
        final CharSequence title;
        final Style style;
        @Nullable final Handler handler;

        Action(CharSequence title, Style style, @Nullable Handler handler) {
            this.title = title;
            this.style = style;
            this.handler = handler;
        }
    }

    private final Context mContext;
    private final boolean mSheet;
    @Nullable private CharSequence mTitle;
    @Nullable private CharSequence mMessage;
    private final List<Action> mActions = new ArrayList<>();
    private final List<EditText> mFields = new ArrayList<>();

    private HigAlert(Context context, boolean sheet) {
        mContext = context;
        mSheet = sheet;
    }

    public static HigAlert alert(Context context) {
        return new HigAlert(context, false);
    }

    public static HigAlert actionSheet(Context context) {
        return new HigAlert(context, true);
    }

    public HigAlert title(@Nullable CharSequence title) {
        mTitle = title;
        return this;
    }

    public HigAlert message(@Nullable CharSequence message) {
        mMessage = message;
        return this;
    }

    public HigAlert action(CharSequence title, Style style, @Nullable Handler handler) {
        mActions.add(new Action(title, style, handler));
        return this;
    }

    public EditText textField(CharSequence placeholder, int inputType) {
        EditText field = new EditText(mContext);
        field.setBackgroundResource(R.drawable.hig_field_background);
        field.setHint(placeholder);
        field.setSingleLine(true);
        field.setInputType(inputType);

        field.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        field.setTextColor(mContext.getColor(R.color.label_primary));
        field.setHintTextColor(mContext.getColor(R.color.hig_tertiary_label));
        field.setPadding(dp(8), dp(6), dp(8), dp(6));
        mFields.add(field);
        return field;
    }

    public Dialog show() {
        Dialog dialog = new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(mSheet ? buildSheet(dialog) : buildAlert(dialog));
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(mSheet ? 0.4f : 0.3f);
            if (mSheet) {
                window.setGravity(Gravity.BOTTOM);
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                window.setWindowAnimations(
                        com.google.android.material.R.style.Animation_Design_BottomSheetDialog);
            } else {
                window.setLayout(dp(270), ViewGroup.LayoutParams.WRAP_CONTENT);
                if (!mFields.isEmpty()) {
                    window.setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
            }
        }

        dialog.setCanceledOnTouchOutside(mSheet);
        dialog.setCancelable(mSheet || hasCancel());
        dialog.show();
        if (!mFields.isEmpty()) {
            mFields.get(0).requestFocus();
        }
        return dialog;
    }

    private boolean hasCancel() {
        for (Action a : mActions) {
            if (a.style == Style.CANCEL) return true;
        }
        return false;
    }

    private View buildAlert(Dialog dialog) {
        LinearLayout root = new LinearLayout(mContext);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.hig_alert_background);
        root.setClipToOutline(true);

        LinearLayout content = new LinearLayout(mContext);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(19), dp(16), dp(19));
        if (mTitle != null) {
            TextView title = text(mTitle, 17, R.color.label_primary, true);
            title.setGravity(Gravity.CENTER);
            content.addView(title, matchWrap());
        }
        if (mMessage != null) {
            TextView message = text(mMessage, 13, R.color.label_primary, false);
            message.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = matchWrap();
            lp.topMargin = mTitle != null ? dp(2) : 0;
            content.addView(message, lp);
        }
        for (EditText field : mFields) {
            LinearLayout.LayoutParams lp = matchWrap();
            lp.topMargin = dp(content.getChildCount() == 0 ? 0 : 12);
            content.addView(field, lp);
        }
        root.addView(content, matchWrap());

        boolean sideBySide = mActions.size() == 2;
        LinearLayout buttons = new LinearLayout(mContext);
        buttons.setOrientation(sideBySide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        List<Action> ordered = new ArrayList<>(mActions);
        if (sideBySide) {

            ordered.sort((a, b) -> Boolean.compare(b.style == Style.CANCEL,
                    a.style == Style.CANCEL));
        } else {

            ordered.sort((a, b) -> Boolean.compare(a.style == Style.CANCEL,
                    b.style == Style.CANCEL));
        }
        for (int i = 0; i < ordered.size(); i++) {
            if (i > 0) {
                View divider = new View(mContext);
                divider.setBackgroundColor(mContext.getColor(R.color.separator));
                buttons.addView(divider, sideBySide
                        ? new LinearLayout.LayoutParams(hairline(),
                                ViewGroup.LayoutParams.MATCH_PARENT)
                        : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                hairline()));
            }
            TextView button = button(ordered.get(i), dialog, dp(44));
            buttons.addView(button, sideBySide
                    ? new LinearLayout.LayoutParams(0, dp(44), 1f)
                    : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(44)));
        }
        View top = new View(mContext);
        top.setBackgroundColor(mContext.getColor(R.color.separator));
        root.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                hairline()));
        root.addView(buttons, matchWrap());
        return root;
    }

    private View buildSheet(Dialog dialog) {
        LinearLayout root = new LinearLayout(mContext);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8), 0, dp(8), dp(8));

        LinearLayout group = sheetGroup();
        if (mTitle != null || mMessage != null) {
            LinearLayout header = new LinearLayout(mContext);
            header.setOrientation(LinearLayout.VERTICAL);
            header.setPadding(dp(16), dp(14), dp(16), dp(14));
            if (mTitle != null) {
                TextView title = text(mTitle, 13, R.color.label_secondary, true);
                title.setGravity(Gravity.CENTER);
                header.addView(title, matchWrap());
            }
            if (mMessage != null) {
                TextView message = text(mMessage, 13, R.color.label_secondary, false);
                message.setGravity(Gravity.CENTER);
                header.addView(message, matchWrap());
            }
            group.addView(header, matchWrap());
        }
        Action cancel = null;
        for (Action action : mActions) {
            if (action.style == Style.CANCEL) {
                cancel = action;
                continue;
            }
            if (group.getChildCount() > 0) {
                addHairline(group);
            }
            group.addView(button(action, dialog, dp(57)), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(57)));
        }
        root.addView(group, matchWrap());
        if (cancel != null) {
            LinearLayout cancelGroup = sheetGroup();
            cancelGroup.addView(button(cancel, dialog, dp(57)), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(57)));
            LinearLayout.LayoutParams lp = matchWrap();
            lp.topMargin = dp(8);
            root.addView(cancelGroup, lp);
        }
        return root;
    }

    private LinearLayout sheetGroup() {
        LinearLayout group = new LinearLayout(mContext);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackgroundResource(R.drawable.hig_sheet_group_background);
        group.setClipToOutline(true);
        return group;
    }

    private TextView button(Action action, Dialog dialog, int height) {
        TextView button = text(action.title, mSheet ? 20 : 17,
                action.style == Style.DESTRUCTIVE ? R.color.system_red : R.color.hig_accent,
                action.style == Style.CANCEL);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(height);
        button.setSingleLine(true);
        button.setBackgroundResource(R.drawable.hig_cell_background);
        button.setOnClickListener(v -> {
            dialog.dismiss();
            if (action.handler != null) {
                action.handler.onAction();
            }
        });
        return button;
    }

    private void addHairline(LinearLayout group) {
        View line = new View(mContext);
        line.setBackgroundColor(mContext.getColor(R.color.separator));
        group.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                hairline()));
    }

    private TextView text(CharSequence text, int sp, int color, boolean bold) {
        TextView view = new TextView(mContext);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(mContext.getColor(color));
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int hairline() {
        return Math.max(1, dp(1) / 3);
    }

    private int dp(int value) {
        return Math.round(value * mContext.getResources().getDisplayMetrics().density);
    }
}
