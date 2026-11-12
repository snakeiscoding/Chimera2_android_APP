package com.jht.chimera.service.usbmedia.ios.lib.iap2;

import android.content.Context;
import android.util.Log;

import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.service.usbmedia.ios.lib.auth.AuthCp3;
import com.jht.chimera.service.usbmedia.ios.lib.iap2.Packet.ControlMask;
import com.jht.chimera.service.usbmedia.ios.lib.iap2.Sessions.ControlMessageType;
import com.jht.chimera.service.usbmedia.ios.lib.iap2.Sessions.SessionID;
import com.jht.datamatrixlib.DataMatrix;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ControlSession {
    private static final String TAG = "ControlSession";
    private Context mContext;
    private Link mLink;
    private WriteThread mWriteThread;
    private String mRevision;
    private String mRadioLibraryRevision;
//    private String mMediaLibraryId;
    private List<String> mMediaLibraryIds = new ArrayList<>();

    public enum MediaType {
        MUSIC, PODCAST, AUDIO_BOOK;

        public static MediaType getMediaType(int ordinal) {
            return values()[ordinal];
        }
    }

    public ControlSession(Context context) {
        mContext = context;
    }
    public ControlSession(Context context, Link link, WriteThread writeThread) {
        this(context);
        mLink = link;
        mWriteThread = writeThread;
    }

    public void setLink(Link link) {
        mLink = link;
    }

//    public void setWriteThread(WriteThread writeThread) {
//        mWriteThread = writeThread;
//    }

    public void sendAuthCertificate(Packet packet) {
        try (AuthCp3 authCp3 = new AuthCp3(ProductConfig.portMappings.authCp3Path())) {
//        acknowledge(packet);
            byte[] accCertificateData = authCp3.getAccessoryCertificate();
//        for(byte b : accCertificateData) {
//            Log.d(TAG, "sendAuthCertificate: accessory certificate " + String.format("%#04x", b));
//        }
            sendACKPacket(ControlMessageType.AUTHENTICATION_CERTIFICATE, (char) 0, mLink.getLastReceivedSequence(), accCertificateData);
        } catch (Exception e) {
            Log.e(TAG, "sendAuthCertificate failed", e);
        }
    }

    public void sendAuthResponse(Packet packet) {
        try (AuthCp3 authCp3 = new AuthCp3(ProductConfig.portMappings.authCp3Path())) {
//        acknowledge(packet);
            Parameter p = new Parameter();
            p.parse(packet.getMessage().getData());
            byte[] challenge = p.getData();
            byte[] challengeResponse = authCp3.getChallengeResponse(challenge);
            sendACKPacket(ControlMessageType.AUTHENTICATION_RESPONSE, (char) 0, mLink.getLastReceivedSequence(), challengeResponse);
        } catch (Exception e) {
            Log.e(TAG, "sendAuthResponse failed", e);
        }
    }

    public void sendIdentificationInfo() {
        byte[] data;
        Parameter parameter;
        List<Parameter> parameters = new ArrayList<>();
        /*int lvdsValue = -1;
        new ExecuteAsRoot()
                .addCommand("cat /sys/class/extcon/extcon0/state > /data/tmp")
                .execute();
        FileReader fr;
        try {
            fr = new FileReader("/data/tmp");
            BufferedReader br = new BufferedReader(fr);
            while (br.ready()) {
//                Log.d(TAG, "sendIdentificationInfo: " + br.readLine());
                String s = br.readLine();
                lvdsValue = Integer.valueOf(s.substring(s.indexOf("=") + 1));
                Log.d(TAG, "sendIdentificationInfo: LVDS: " + lvdsValue);
            }
            fr.close();
        } catch (FileNotFoundException e) {
            Log.w(TAG, e);
        } catch (IOException e) {
            e.printStackTrace();
        }*/


        String name;
//        String modelId = "T-PS-Touch" + extra;
        String modelId;
        String manufacturer = "Johnson Health Tech"; // NON-NLS
        String serialNumber = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
        String firmwareVersion = BuildConfig.SoftwareVersion;
        String hardwareVersion = DataMatrix.getString(HALKeys.DATA_VERSION_IO_STRING);
        String uid;

        switch(ProductConfig.getConsoleType()) {
            case athena:
            case athena2:
                name = "Matrix Virtual Training Cycle"; // NON-NLS
                modelId = "CXV"; // NON-NLS
                uid = "5e9d55cc6cc01c3b"; // NON-NLS
                break;
            case chimera_22:
            case chimera2_22:
                name = "Matrix Touch XL Console"; // NON-NLS
                modelId = "TOUCHXL-C"; // NON-NLS
                uid = "80ec0c278b68ec4b"; // NON-NLS
                break;

            case chimera_16:
            case chimera2_16:
            default:
                name = "Matrix Touch Console"; // NON-NLS
                modelId = "TOUCH-C"; // NON-NLS
                uid = "40c281b747c60943"; // NON-NLS
                break;
        }

        // Name parameter
        data = (name + "\0").getBytes(StandardCharsets.UTF_8);
        parameter = new Parameter((char) 0);
        parameter.setData(data);
        parameters.add(parameter);

        // ModelIdentifier parameter
        data = (modelId + "\0").getBytes(StandardCharsets.UTF_8);
        parameter = new Parameter((char) 1);
        parameter.setData(data);
        parameters.add(parameter);

        // Manufacturer parameter
        data = (manufacturer + "\0").getBytes(StandardCharsets.UTF_8);
        parameter = new Parameter((char) 2);
        parameter.setData(data);
        parameters.add(parameter);

        // SerialNumber parameter
        data = (serialNumber + "\0").getBytes(StandardCharsets.UTF_8);
        parameter = new Parameter((char) 3);
        parameter.setData(data);
        parameters.add(parameter);

        // FirmwareVersion parameter
        data = (firmwareVersion + "\0").getBytes(StandardCharsets.UTF_8);
        parameter = new Parameter((char) 4);
        parameter.setData(data);
        parameters.add(parameter);

        // HardwareVersion parameter
        data = (hardwareVersion + "\0").getBytes(StandardCharsets.UTF_8);
        parameter = new Parameter((char) 5);
        parameter.setData(data);
        parameters.add(parameter);

        // MessageSentByAccessory
        parameter = new Parameter((char) 6);
        List<Byte> tempDataList = new ArrayList<>();
//        data = new byte[30];
        byte[] msgByteArray = char2ByteArray(ControlMessageType.START_MEDIA_LIBRARY_INFORMATION.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.STOP_MEDIA_LIBRARY_INFORMATION.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.START_MEDIA_LIBRARY_UPDATES.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.STOP_MEDIA_LIBRARY_UPDATES.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

//        msgByteArray = char2ByteArray(ControlMessageType.PLAY_MEDIA_LIBRARY_CURRENT_SELECTION.getId());
//        for(byte b : msgByteArray) {
//            tempDataList.add(b);
//        }

        msgByteArray = char2ByteArray(ControlMessageType.PLAY_MEDIA_LIBRARY_ITEMS.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        /*msgByteArray = char2ByteArray(ControlMessageType.PLAY_MEDIA_LIBRARY_COLLECTION.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }*/

        msgByteArray = char2ByteArray(ControlMessageType.START_NOW_PLAYING_UPDATES.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.STOP_NOW_PLAYING_UPDATES.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.START_POWER_UPDATES.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.POWER_SOURCE_UPDATE.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.STOP_POWER_UPDATES.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.START_HID.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }


        msgByteArray = char2ByteArray(ControlMessageType.ACCESSORY_HID_REPORT.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.STOP_HID.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.START_USB_DEVICE_MODE_AUDIO.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.STOP_USB_DEVICE_MODE_AUDIO.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        data = new byte[tempDataList.size()];
        for(int i = 0; i < tempDataList.size(); i++) {
            data[i] = tempDataList.get(i);
        }
        parameter.setData(data);
        parameters.add(parameter);

        // MessageReceivedFromDevice
        parameter = new Parameter((char) 7);
        tempDataList.clear();
        msgByteArray = char2ByteArray(ControlMessageType.MEDIA_LIBRARY_INFORMATION.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.MEDIA_LIBRARY_UPDATE.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.NOW_PLAYING_UPDATE.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.POWER_UPDATE.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.DEVICE_INFORMATION_UPDATE.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.DEVICE_HID_REPORT.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        msgByteArray = char2ByteArray(ControlMessageType.USB_DEVICE_MODE_AUDIO_INFORMATION.getId());
        for(byte b : msgByteArray) {
            tempDataList.add(b);
        }

        data = new byte[tempDataList.size()];
        for(int i = 0; i < tempDataList.size(); i++) {
            data[i] = tempDataList.get(i);
        }
        parameter.setData(data);
        parameters.add(parameter);

        // PowerProvidingCapability
        data = new byte[]{2};
        parameter = new Parameter((char) 8);
        parameter.setData(data);
        parameters.add(parameter);

        // MaxCurrentDrawn
        data = char2ByteArray((char) 0);
        parameter = new Parameter((char) 9);
        parameter.setData(data);
        parameters.add(parameter);

        // CurrentLanguage
        data = "en\0".getBytes(StandardCharsets.UTF_8); //NON-NLS
        parameter = new Parameter((char) 12);
        parameter.setData(data);
        parameters.add(parameter);

        // SupportedLanguage
        data = "en\0".getBytes(StandardCharsets.UTF_8); //NON-NLS
        parameter = new Parameter((char) 13);
        parameter.setData(data);
        parameters.add(parameter);

        // USBDeviceTransportComponent
        parameter = new Parameter((char) 15);
        // USBDeviceTransportComponent - TransportComponentIdentifier
        Parameter subParameter = new Parameter((char) 0);
        data = char2ByteArray((char) 1);
        subParameter.setData(data);
        parameter.setData(subParameter.getAll());
        // USBDeviceTransportComponent - TransportComponentName
        subParameter = new Parameter((char) 1);
        data = "Lightning Connector\0".getBytes(StandardCharsets.UTF_8); //NON-NLS
        subParameter.setData(data);
        parameter.setData(subParameter.getAll());
        // USBDeviceTransportComponent - TransportSupportsiAP2Connection
        subParameter = new Parameter((char) 2);
        parameter.setData(subParameter.getAll());
        // USBDeviceTransportComponent - USBDeviceSupportedAudioSampleRate(32000 Hz)
        subParameter = new Parameter((char) 3);
        data = new byte[]{6};
        subParameter.setData(data);
        parameter.setData(subParameter.getAll());
        // USBDeviceTransportComponent - USBDeviceSupportedAudioSampleRate(44100 Hz)
        subParameter = new Parameter((char) 3);
        data = new byte[]{7};
        subParameter.setData(data);
        parameter.setData(subParameter.getAll());
        // USBDeviceTransportComponent - USBDeviceSupportedAudioSampleRate(48000 Hz)
        subParameter = new Parameter((char) 3);
        data = new byte[]{8};
        subParameter.setData(data);
        parameter.setData(subParameter.getAll());
        parameters.add(parameter);

        // iAP2HIDComponent
        parameter = new Parameter((char) 18);
        // iAP2HIDComponent - HIDComponentIdentifier
        subParameter = new Parameter((char) 0);
        data = char2ByteArray((char) 0);
        subParameter.setData(data);
        parameter.setData(subParameter.getAll());
        // iAP2HIDComponent - HIDComponentName
        subParameter = new Parameter((char) 1);
        data = "HIDMediaPlaybackRemote\0".getBytes(StandardCharsets.UTF_8); //NON-NLS
        subParameter.setData(data);
        parameter.setData(subParameter.getAll());
        // iAP2HIDComponent - HIDComponentFunction
        subParameter = new Parameter((char) 2);
        data = new byte[]{1};
        subParameter.setData(data);
        parameter.setData(subParameter.getAll());
        parameters.add(parameter);

        // ProductPlanUID
        data = (uid + "\0").getBytes(StandardCharsets.UTF_8);
        parameter = new Parameter((char) 34);
        parameter.setData(data);
        parameters.add(parameter);

        Message m = new Message(ControlMessageType.IDENTIFICATION_INFORMATION.getId());
        m.setParameters(parameters);
//        Packet packet = new Packet(mLink, ControlMask.ACK, (byte) mLink.getLastReceivedInSequencePsn(), SessionID.CONTROL.getId());
        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);


    }

    public void setAccessoryPowerSource() {
        List<Parameter> parameters = new ArrayList<>();

        // StartPowerUpdates Message
        Message m = new Message(ControlMessageType.START_POWER_UPDATES.getId());

        Parameter p = new Parameter((char) 0);
        parameters.add(p);
        p = new Parameter((char) 1);
        parameters.add(p);

        m.setParameters(parameters);
        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);

        parameters.clear();

        // PowerSourceUpdate Message
        m = new Message(ControlMessageType.POWER_SOURCE_UPDATE.getId());

        p = new Parameter((char) 0);
        p.setData(char2ByteArray((char) 2100));
        parameters.add(p);
        p = new Parameter((char) 1);
        p.setData(new byte[]{1});
        parameters.add(p);
        m.setParameters(parameters);

        packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);

    }

    public void sendStartHID() {
        byte[] data;
        Message m = new Message(ControlMessageType.START_HID.getId());
        List<Parameter> parameters = new ArrayList<>();

        Parameter p = new Parameter((char) 0);
        data = char2ByteArray((char) 0);
        p.setData(data);
        parameters.add(p);

        // VendorIdentifier
        p = new Parameter((char) 1);
        data = new byte[]{0x31, 0x17};
        p.setData(data);
        parameters.add(p);

        // ProductIdentifier
        p = new Parameter((char) 2);
        data = new byte[]{0x23, 0x03};
        p.setData(data);
        parameters.add(p);

        // HIDReportDescriptor
        p = new Parameter((char) 4);
        byte[] reportDescriptor =
                {0x05, 0x0c,				// USAGE_PAGE (Consumer Devices)
                        0x09, 0x01,				    // USAGE (Consumer Control)
                        (byte) 0xa1, 0x01,		    // COLLECTION (Application)
                        0x15, 0x00,				    // LOGICAL_MINIMUM (0)
                        0x25, 0x01,				    // LOGICAL_MAXIMUM (1)
                        0x75, 0x01,				    // REPORT_SIZE (1)
                        (byte) 0x95, 0x07,		    // REPORT_COUNT (7)
                        0x09, (byte) 0xcd,		    // USAGE (Play/Pause)
                        0x09, (byte) 0xb5,		    // USAGE (Scan Next Track)
                        0x09, (byte) 0xb6,		    // USAGE (Scan Previous Track)
                        0x09, (byte) 0xb9,		    // USAGE (Random Play)
                        0x09, (byte) 0xbc,		    // USAGE (Repeat)
                        0x09, (byte) 0xb0,          // USAGE (Play)
                        0x09, (byte) 0xb1,          // USAGE (Pause)
                        (byte) 0x81, 0x02,		    // INPUT (Data,Var,Abs)
                        0x75, 0x01,				    // REPORT_SIZE (1)
                        (byte) 0x95, 0x01,		    // REPORT_COUNT (1)
                        (byte) 0x81, 0x03,		    // INPUT (Cnst,Var,Abs)
                        (byte) 0xc0};			    // END_COLLECTION
        p.setData(reportDescriptor);
        parameters.add(p);

        m.setParameters(parameters);

        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);
    }

    public void sendStartUSBDeviceModeAudio() {
        Message m = new Message(ControlMessageType.START_USB_DEVICE_MODE_AUDIO.getId());
        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);
    }

    public void sendStopUsbDeviceModeAudio() {
        Message m = new Message(ControlMessageType.STOP_USB_DEVICE_MODE_AUDIO.getId());
        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);
    }

    public void sendStartNowPlayingUpdates() {
        Message m = new Message(ControlMessageType.START_NOW_PLAYING_UPDATES.getId());
        List<Parameter> parameters = new ArrayList<>();

        // MediaItemAttributes
        Parameter p = new Parameter((char) 0);
        // MediaItemAttributes - MediaItemPersistentIdentifier
        Parameter subParameter = new Parameter((char) 0);
        p.setData(subParameter.getAll());
        // MediaItemAttributes - MediaItemTitle
        subParameter = new Parameter((char) 1);
        p.setData(subParameter.getAll());
        // MediaItemAttributes - MediaItemPlaybackDurationInMilliseconds
        subParameter = new Parameter((char) 4);
        p.setData(subParameter.getAll());
        // MediaItemAttributes - MediaItemAlbumTitle
        subParameter = new Parameter((char) 6);
        p.setData(subParameter.getAll());
        // MediaItemAttributes - MediaItemArtist
        subParameter = new Parameter((char) 12);
        p.setData(subParameter.getAll());
        // MediaItemAttributes - MediaItemGenre
        subParameter = new Parameter((char) 16);
        p.setData(subParameter.getAll());
        // MediaItemAttributes - MediaItemComposer
        subParameter = new Parameter((char) 18);
        p.setData(subParameter.getAll());
        // MediaItemAttributes - MediaItemArtworkFileTransferIdentifier
        subParameter = new Parameter((char) 26);
        p.setData(subParameter.getAll());

        parameters.add(p);

        // PlaybackAttributes
        p = new Parameter((char) 1);
        // PlaybackAttributes - PlaybackStatus
        subParameter = new Parameter((char) 0);
        p.setData(subParameter.getAll());
        // PlaybackAttributes - PlaybackElapsedTimeInMilliseconds
        subParameter = new Parameter((char) 1);
        p.setData(subParameter.getAll());
        // PlaybackAttributes - PlaybackShuffleMode
        subParameter = new Parameter((char) 5);
        p.setData(subParameter.getAll());
        // PlaybackAttributes - PlaybackRepeatMode
        subParameter = new Parameter((char) 6);
        p.setData(subParameter.getAll());
        // PlaybackAttributes - PlaybackAppName
        subParameter = new Parameter((char) 7);
        p.setData(subParameter.getAll());
        // PlaybackAttributes - PlaybackMediaLibraryUniqueIdentifier
        subParameter = new Parameter((char) 8);
        p.setData(subParameter.getAll());
        /*// PlaybackAttributes - PBAppleMusicRadioStationName
        subParameter = new Parameter((char) 10);
        p.setData(subParameter.getAll());
        // PlaybackAttributes - PBAppleMusicRadioMediaPlaylistID
        subParameter = new Parameter((char) 11);
        p.setData(subParameter.getAll());*/
        // PlaybackAttributes - PlaybackAppBundleID
        subParameter = new Parameter((char) 16);
        p.setData(subParameter.getAll());
        /*// PlaybackAttributes - PlaybackQueueListContentTransferSize
        subParameter = new Parameter((char) 17);
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(0);
        buffer.position(0);
        subParameter.setData(buffer.array());
        p.setData(subParameter.getAll());*/
        parameters.add(p);

        m.setParameters(parameters);
        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);


    }

    public void sendStartMediaLibraryInfo() {
        Message m = new Message(ControlMessageType.START_MEDIA_LIBRARY_INFORMATION.getId());
        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);
    }

    public void sendStartMediaLibraryUpdates(String mediaLibraryId, String revision) {
        Log.d(TAG, "[MediaLibraryUpdate]sendStartMediaLibraryUpdates: media library id: " + mediaLibraryId + ", revision: " + revision);
//        mMediaLibraryId = mediaLibraryId;
        mMediaLibraryIds.add(mediaLibraryId);
        Message m = new Message(ControlMessageType.START_MEDIA_LIBRARY_UPDATES.getId());
        List<Parameter> parameters = new ArrayList<>();
        byte[] data;
        Parameter para;

        // MediaLibraryUniqueIdentifier
        data = (mediaLibraryId + "\0").getBytes(StandardCharsets.UTF_8);
        para = new Parameter((char) 0);
        para.setData(data);
        parameters.add(para);

        // LastKnownMediaLibraryRevision
        if(revision != null) {
            data = (mRevision + "\0").getBytes(StandardCharsets.UTF_8);
            para = new Parameter((char) 1);
            para.setData(data);
            parameters.add(para);
        }

        // MediaItemProperties
        para = new Parameter((char) 2);
        // MediaItemProperties - MediaItemPropertyPersistentIdentifier
        Parameter subPara = new Parameter((char) 0);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyTitle
        subPara = new Parameter((char) 1);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyMediaType
        subPara = new Parameter((char) 2);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyPlaybackDurationInMilliseconds
        subPara = new Parameter((char) 4);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyAlbumPersistentIdentifier
        subPara = new Parameter((char) 5);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyAlbumTitle
        subPara = new Parameter((char) 6);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyArtistPersistentIdentifier
        subPara = new Parameter((char) 11);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyArtist
        subPara = new Parameter((char) 12);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemAlbumArtistPersistentIdentifier
        subPara = new Parameter((char) 13);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemAlbumArtist
        subPara = new Parameter((char) 14);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyGenrePersistentIdentifier
        subPara = new Parameter((char) 15);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyGenre
        subPara = new Parameter((char) 16);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyComposerPersistentIdentifier
        subPara = new Parameter((char) 17);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyComposer
        subPara = new Parameter((char) 18);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyIsPartOfCompilation
        subPara = new Parameter((char) 19);
        para.setData(subPara.getAll());
        // MediaItemProperties - MediaItemPropertyIsResidentOnDevice
        subPara = new Parameter((char) 25);
        para.setData(subPara.getAll());

        parameters.add(para);

        // MediaPlaylistProperties
        para = new Parameter((char) 3);
        //
        subPara = new Parameter((char) 0);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 1);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 2);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 3);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 4);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 5);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 6);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 7);
        para.setData(subPara.getAll());

        parameters.add(para);

        // MediaLibraryUpdateProgress
        para = new Parameter((char) 4);
        parameters.add(para);

        // MediaLibraryIsHidingRemoteItems
        para = new Parameter((char) 5);
        parameters.add(para);

        // PlayAllSongsCapable
        /*para = new Parameter((char) 6);
        parameters.add(para);*/

        // MediaPlaylistContentTransferInfoRequest
        para = new Parameter((char) 7);
        //
        subPara = new Parameter((char) 0);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 1);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 6);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 12);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 14);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 16);
        para.setData(subPara.getAll());
        //
        subPara = new Parameter((char) 18);
        para.setData(subPara.getAll());
        parameters.add(para);

        m.setParameters(parameters);
        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);

    }

    public void sendStopMediaLibraryInfo() {
        Message m = new Message(ControlMessageType.STOP_MEDIA_LIBRARY_INFORMATION.getId());
        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);
    }

    public void sendStopMediaLibraryUpdates() {
        Message m = new Message(ControlMessageType.STOP_MEDIA_LIBRARY_UPDATES.getId());
        byte[] data;
        Parameter para;
        List<Parameter> parameters = new ArrayList<>();

        // MediaLibraryUniqueIdentifier
        for(String mediaLibraryId : mMediaLibraryIds) {
            data = (mediaLibraryId + "\0").getBytes(StandardCharsets.UTF_8);
            para = new Parameter((char) 0);
            para.setData(data);
            parameters.add(para);
        }

        m.setParameters(parameters);
        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);

    }



    public void sendAccessoryHID(byte[] HIDReport) {
        Message m = new Message(ControlMessageType.ACCESSORY_HID_REPORT.getId());
        List<Parameter> paras = new ArrayList<>();
        Parameter para;

        // HIDComponentIdentifier
        para = new Parameter((char) 0);
        para.setData(char2ByteArray((char) 0));
        paras.add(para);

        // HID Report
        para = new Parameter((char) 1);
        para.setData(HIDReport);
        paras.add(para);

        m.setParameters(paras);

        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);


    }

    public void sendPlayMediaLibraryItems(List<String> mediaItemIds, int startingIndex, String mediaLibraryId) {
        Log.d(TAG, "sendPlayMediaLibraryItems: ");
        Message message = new Message(ControlMessageType.PLAY_MEDIA_LIBRARY_ITEMS.getId());
        List<Parameter> paras = new ArrayList<>();
        Parameter para;
        byte[] data;

        para = new Parameter((char) 0);
        for(String s : mediaItemIds) {
            BigInteger bInt = new BigInteger(s);
            byte[] idArray = bInt.toByteArray();
            para.setData(idArray);
        }
        paras.add(para);

        para = new Parameter((char) 1);
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(startingIndex);
        buffer.position(0);
        data = buffer.array();
        para.setData(data);
        paras.add(para);

        data = (mediaLibraryId + "\0").getBytes(StandardCharsets.UTF_8);
        para = new Parameter((char) 2);
        para.setData(data);
        paras.add(para);

        message.setParameters(paras);
        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(message.getAll());
        mWriteThread.addPacket(packet);

    }

    /*
    public void sendPlayMediaLibraryCurrentSelection(String mediaLibraryId) {
        Message message = new Message(ControlMessageType.PLAY_MEDIA_LIBRARY_CURRENT_SELECTION.getId());
        List<Parameter> paras = new ArrayList<>();
        Parameter para;
        byte[] data;

        data = (mediaLibraryId + "\0").getBytes(StandardCharsets.UTF_8);
        para = new Parameter((char) 0);
        para.setData(data);
        paras.add(para);

        message.setParameters(paras);
        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(message.getAll());
        mWriteThread.addPacket(packet);
    }*/

    public void playRadio(String id, int startingIndex, String mediaLibraryId) {
        Message message = new Message(ControlMessageType.PLAY_MEDIA_LIBRARY_COLLECTION.getId());
        List<Parameter> paras = new ArrayList<>();
        Parameter para;
        byte[] data;

        // 0
        para = new Parameter((char) 0);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(Long.valueOf(id));
        buffer.position(0);
        data = buffer.array();
//        BigInteger bInt = new BigInteger(id);
//        byte[] idArray = bInt.toByteArray();
        para.setData(data);
        paras.add(para);

        // 1
        para = new Parameter((char) 1);
        data = new byte[]{0};
        para.setData(data);
        paras.add(para);

        // 2
        para = new Parameter((char) 2);
        buffer = ByteBuffer.allocate(4);
        buffer.putInt(startingIndex);
        buffer.position(0);
        data = buffer.array();
        para.setData(data);
        paras.add(para);

        // 3
        try {
            para = new Parameter((char) 3);
            data = (mediaLibraryId + "\0").getBytes("UTF-8");
            para.setData(data);
            paras.add(para);
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, e);
        }

        message.setParameters(paras);
        Packet packet = new Packet(mLink, ControlMask.ACK, mLink.getLastReceivedSequence(), SessionID.CONTROL.getId());
        packet.setPayloadData(message.getAll());
        mWriteThread.addPacket(packet);

    }

    private byte[] char2ByteArray(char value) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putChar(value);
        buffer.position(0);
        return buffer.array();
    }

    private void sendACKPacket(ControlMessageType type, char parameterId, byte ack, byte[] data) {
        Packet packet = new Packet(mLink, ControlMask.ACK, ack, SessionID.CONTROL.getId());
        Message m = new Message(type.getId());
        Parameter p = new Parameter(parameterId);
        p.setData(data);
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(p);
        m.setParameters(parameters);
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);
    }

    private void sendACKPacket(ControlMessageType type, char parameterId, int ack, byte[] data) {
        Packet packet = new Packet(mLink, ControlMask.ACK, ack, SessionID.CONTROL.getId());
        Message m = new Message(type.getId());
        Parameter p = new Parameter(parameterId);
        p.setData(data);
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(p);
        m.setParameters(parameters);
        packet.setPayloadData(m.getAll());
        mWriteThread.addPacket(packet);
    }

    public void acknowledge(Packet packet) {
        mLink.setLastReceivedSequence(packet.getSequence());
        Packet packetToWrite = new Packet(mLink, Packet.ControlMask.ACK, mLink.getLastReceivedSequence(), (byte) 0x00);
        mWriteThread.addPacket(packetToWrite);
    }
}
