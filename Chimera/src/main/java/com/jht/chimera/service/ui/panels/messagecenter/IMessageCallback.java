package com.jht.chimera.service.ui.panels.messagecenter;

import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;

/**
 * Callback for a message dialog. The message dialog
 */
public interface IMessageCallback
{
    void closeMessage();
    void openModal(MessageCenterMessage message);
}
