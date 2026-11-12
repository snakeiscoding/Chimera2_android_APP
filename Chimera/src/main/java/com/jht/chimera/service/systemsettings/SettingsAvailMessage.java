package com.jht.chimera.service.systemsettings;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NonNls;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Stack;

class SettingsAvailMessage {

    private enum ParserState
    {
        BASE,
        CONSOLE_SETTINGS
    }

    String console = "";
    int apiVersion = 0;
    int settingsVersion = 0;
    String host = "";
    int port = 0;
    int settingsPort = 0;
    int nonce = 0;
    String sha1 = "";
    String settingsSha1 = "";
    String consoleSerialNumber = "";
    String frameSerialNumber = "";

    @Override
    public String toString() {
        return
                "SettingsAvailMessage:\n" +
                        "\tconsole:" + console + "\n" +
                        "\tapiVersion:" + apiVersion + "\n" +
                        "\tsettingsVersion:" + settingsVersion + "\n" +
                        "\thost:" + host + "\n" +
                        "\tport:" + port + "\n" +
                        "\tsettingsPort:" + settingsPort + "\n" +
                        "\tnonce:" + nonce + "\n" +
                        "\tsha1:" + sha1 + "\n" +
                        "\tsettingsSha1:" + settingsSha1 + "\n" +
                        "\tconsoleSerialNumber:" + consoleSerialNumber + "\n" +
                        "\tframeSerialNumber:" + frameSerialNumber;
    }

    SettingsAvailMessage(@NonNull String xml) {
        Stack<String> tagName = new Stack<>();
        ParserState state = ParserState.BASE;
        XmlPullParserFactory factory;
        XmlPullParser parser;
        try {
            factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));

            int eventType = parser.getEventType();
            String currentTag = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if( currentTag != null)
                            tagName.push(currentTag);
                        currentTag = parser.getName();
                        if( state == ParserState.BASE && currentTag.equalsIgnoreCase("SettingsAvailable") )
                            state = ParserState.CONSOLE_SETTINGS;
                        break;

                    case XmlPullParser.TEXT:
                        if( state == ParserState.CONSOLE_SETTINGS && currentTag != null )
                        {
                            console = getSetting(currentTag, "Console", parser.getText(), console);
                            apiVersion = getSetting(currentTag, "APIVersion", parser.getText(), apiVersion);
                            settingsVersion = getSetting(currentTag, "SettingsVersion", parser.getText(), settingsVersion);
                            host = getSetting(currentTag, "Host", parser.getText(), host);
                            port = getSetting(currentTag, "Port", parser.getText(), port);
                            settingsPort = getSetting(currentTag, "SettingsPort", parser.getText(), settingsPort);
                            nonce = getSetting(currentTag, "Nonce", parser.getText(), nonce);
                            sha1 = getSetting(currentTag, "SHA1", parser.getText(), sha1);
                            settingsSha1 = getSetting(currentTag, "SettingsSHA1", parser.getText(), settingsSha1);
                            consoleSerialNumber = getSetting(currentTag, "ConsoleSerialNumber", parser.getText(), consoleSerialNumber);
                            frameSerialNumber = getSetting(currentTag, "FrameSerialNumber", parser.getText(), frameSerialNumber);
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if( currentTag != null && currentTag.equalsIgnoreCase( parser.getName() ) )
                        {
                            if (currentTag.equalsIgnoreCase("SettingsAvailable"))
                                state = ParserState.BASE;
                            if( tagName.size() > 0 )
                                currentTag = tagName.pop();
                            else
                                currentTag = null;
                        }
                        else
                        {
                            return; // XML is malformed.  End tag did not match the start tag.
                        }
                        break;
                    default:
                        break;
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException ignored) {
        } catch (IOException ignored) {
        }

    }

    private String getSetting(@NonNls String currentTag, @NonNls String expectedTag, @NonNls String tagValue, @NonNls String defaultValue) {
        return currentTag.equalsIgnoreCase(expectedTag) ? tagValue : defaultValue;
    }
    private int getSetting(@NonNls String currentTag, @NonNls String expectedTag, @NonNls String tagValue, int defaultValue) {
        return currentTag.equalsIgnoreCase(expectedTag) ? Integer.parseInt(tagValue) : defaultValue;
    }
}
