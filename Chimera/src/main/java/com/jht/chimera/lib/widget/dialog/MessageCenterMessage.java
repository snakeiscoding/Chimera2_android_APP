package com.jht.chimera.lib.widget.dialog;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;

/**
 * The contents of a message and the formatting for the message. Priority will affect when the
 * message is displayed and whether it is displayed before other messages.
 */
public class MessageCenterMessage implements Parcelable {
    private Instant timestamp, expiration;
    private boolean hide, hideOnSplash, hideCloseBtn, showPostWorkout;
    private String messageHeader, messageText;
    private int backgroundColor, textColor, headerColor, closeButtonColor;
    private int priority, hideTime, id, workoutOffset;
    @NonNull private String actionUrl = "";
    @NonNull private String actionText = "";
    @Nullable private String tag = null; // a more generic form of id, since id has reserved values

    public MessageCenterMessage() {
        timestamp = null;
        expiration = null;
        hide = false;
        hideOnSplash = true;
        hideCloseBtn = false;
        showPostWorkout = false;
        messageHeader = "";
        messageText = "";
        backgroundColor = Color.BLACK;
        textColor = Color.WHITE;
        headerColor = Color.BLACK;
        closeButtonColor = Color.WHITE;
        hideTime = 10;
        id = 0;
        tag = null;
    }

    public MessageCenterMessage(final String messageHeader, final String messageText, final int priority) {
        this();
        this.messageHeader = messageHeader;
        this.messageText = messageText;
        this.priority = priority;
    }

    public MessageCenterMessage(final String messageHeader, final String messageText, final int priority, final int hideTime) {
        this();
        this.messageHeader = messageHeader;
        this.messageText = messageText;
        this.priority = priority;
        this.hideTime = hideTime;
    }

    protected MessageCenterMessage(@NonNull final Parcel in) {
        readFromParcel(in);
    }

    public MessageCenterMessage setTag(@Nullable final String tag) {
        this.tag = tag;
        return this;
    }
    public @Nullable String getTag() {
        return tag;
    }

    public MessageCenterMessage setTimestamp(final Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Nullable public Instant getTimestamp() {
        return timestamp;
    }

    public MessageCenterMessage setExpiration(final Instant expiration) {
        this.expiration = expiration;
        return this;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public MessageCenterMessage setHidden(final boolean hide) {
        this.hide = hide;
        return this;
    }

    public boolean isHidden() {
        return hide;
    }

    public MessageCenterMessage setHideOnSplash(final boolean hideOnSplash) {
        this.hideOnSplash = hideOnSplash;
        return this;
    }

    public boolean isHiddenOnSplash() {
        return hideOnSplash;
    }

    public MessageCenterMessage setHideCloseBtn(final boolean hideCloseBtn) {
        this.hideCloseBtn = hideCloseBtn;
        return this;
    }

    public boolean isCloseButtonHidden() {
        return hideCloseBtn;
    }

    /**
     * Set whether this message should be shown with the workout summary or not. The default is
     * false.
     *
     * @param showPostWorkout whether to show this message during workout summary or not.
     */
    public MessageCenterMessage setShowPostWorkout(final boolean showPostWorkout) {
        this.showPostWorkout = showPostWorkout;
        return this;
    }

    /**
     * @return true if this should be shown after the workout, else false.
     */
    public boolean showsPostWorkout() {
        return showPostWorkout;
    }

    public MessageCenterMessage setMessageHeader(final String messageHeader) {
        this.messageHeader = messageHeader;
        return this;
    }

    public String getMessageHeader() {
        return messageHeader;
    }

    public MessageCenterMessage setMessageText(final String messageText) {
        this.messageText = messageText;
        return this;
    }

    @NonNull
    public String getActionUrl() {
        return actionUrl;
    }

    public MessageCenterMessage setActionUrl(@NonNull final String actionUrl) {
        this.actionUrl = actionUrl;
        return this;
    }

    @NonNull
    public String getActionText() {
        return actionText;
    }

    public MessageCenterMessage setActionText(@NonNull final String actionText) {
        this.actionText = actionText;
        return this;
    }

    public String getMessageText() {
        return messageText;
    }

    public MessageCenterMessage setBackgroundColor(final int backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public MessageCenterMessage setTextColor(final int textColor) {
        this.textColor = textColor;
        return this;
    }

    public int getTextColor() {
        return textColor;
    }

    public MessageCenterMessage setHeaderColor(final int headerColor) {
        this.headerColor = headerColor;
        return this;
    }

    public int getHeaderColor() {
        return headerColor;
    }

    public MessageCenterMessage setCloseButtonColor(final int closeButtonColor) {
        this.closeButtonColor = closeButtonColor;
        return this;
    }

    public int getCloseButtonColor() {
        return closeButtonColor;
    }

    public MessageCenterMessage setPriority(final int priority) {
        this.priority = priority;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Sets the time the message will be visible before hiding without user interaction.
     *
     * @param hideTime The time the message will be visible without user interaction
     */
    public MessageCenterMessage setHideTime(final int hideTime) {
        this.hideTime = hideTime;
        return this;
    }

    public int getHideTime() {
        return hideTime;
    }

    public MessageCenterMessage setId(final int id) {
        this.id = id;
        return this;
    }

    public int getId() {
        return id;
    }

    public MessageCenterMessage setWorkoutOffset(final int workoutOffset) {
        this.workoutOffset = workoutOffset;
        return this;
    }

    public int getWorkoutOffset() {
        return workoutOffset;
    }

    /**
     * @return true if this message expired, else false.
     */
    public boolean isExpired() {
        return expiration != null && Instant.now().isAfter(expiration);
    }

    public boolean isRelative() {
        return timestamp == null;
    }

    @Override public int describeContents() {
        return 0;
    }

    private void readFromParcel(@NonNull final Parcel in) {
        backgroundColor = in.readInt();
        textColor = in.readInt();
        headerColor = in.readInt();
        messageHeader = in.readString();
        messageText = in.readString();
        priority = in.readInt();
        hideTime = in.readInt();
        id = in.readInt();
        tag = in.readString();
        hide = in.readInt() == 1;
        hideOnSplash = in.readInt() == 1;
        hideCloseBtn = in.readInt() == 1;

        String timeStampString = in.readString();
        if (timeStampString != null && !timeStampString.isEmpty()) {
            timestamp = Instant.ofEpochMilli(Long.parseLong(timeStampString));
        }

        String expirationString = in.readString();
        if (expirationString != null && !expirationString.isEmpty()) {
            expiration = Instant.ofEpochMilli(Long.parseLong(expirationString));
        }

        showPostWorkout = in.readInt() == 1;
        closeButtonColor = in.readInt();
        workoutOffset = in.readInt();
        String url = in.readString();
        if (url != null) actionUrl = url;
        String urlText = in.readString();
        if (urlText != null) actionText = urlText;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(backgroundColor);
        dest.writeInt(textColor);
        dest.writeInt(headerColor);
        dest.writeString(messageHeader);
        dest.writeString(messageText);
        dest.writeInt(priority);
        dest.writeInt(hideTime);
        dest.writeInt(id);
        dest.writeString(tag);
        dest.writeInt(hide ? 1 : 0);
        dest.writeInt(hideOnSplash ? 1 : 0);
        dest.writeInt(hideCloseBtn ? 1 : 0);
        dest.writeString(timestamp == null ? "" : "" + timestamp.toEpochMilli());
        dest.writeString(expiration == null ? "" : "" + expiration.toEpochMilli());
        dest.writeInt(showPostWorkout ? 1 : 0);
        dest.writeInt(closeButtonColor);
        dest.writeInt(workoutOffset);
        dest.writeString(actionUrl);
        dest.writeString(actionText);
    }

    public void setTitle(String title) {
        messageHeader = title;
    }

    public void setMessage(String text) {
        messageText = text;
    }

    public static final Creator<MessageCenterMessage> CREATOR = new Creator<MessageCenterMessage>() {
        @Override public MessageCenterMessage createFromParcel(Parcel in) {
            return new MessageCenterMessage(in);
        }

        @Override public MessageCenterMessage[] newArray(int size) {
            return new MessageCenterMessage[size];
        }
    };

    @Override
    public boolean equals(Object other) {
        return other instanceof MessageCenterMessage && ((MessageCenterMessage)other).getId() == getId();
    }
}
