package com.android.inputmethod.latin.glass;

public interface PanelHost {
    void commitText(String text);

    void commitImage(java.io.File file, String mimeType);

    void sendKey(int keyCode, boolean shift);

    void performContextMenuAction(int id);

    void performFeedback(android.view.View view);

    void closePanel();

    void enableClipboardHistory();
}
