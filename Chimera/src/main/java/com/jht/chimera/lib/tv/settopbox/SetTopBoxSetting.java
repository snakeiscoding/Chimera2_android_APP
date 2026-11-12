package com.jht.chimera.lib.tv.settopbox;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class SetTopBoxSetting {
    private static final String TAG = "SetTopBoxSetting";
    public enum REMOTE_BUTTON_PULSE_SEQUENCE_TYPES {
        HEADER,
        DATA,
        END,
    }

    public static class RemoteButtonPulseSequence {
        public int[] sequence;

        RemoteButtonPulseSequence(@Nullable JSONArray json) {
            if(json == null) {
                sequence = new int[0];
            }
            else {
                sequence = new int[json.length()];
                for(int i = 0; i < json.length(); i++) {
                    sequence[i] = json.optInt(i, 0);

                }
            }

        }
    }

    public static class RemoteButton {
        public String buttonName;
        public int buttonId;
        public RemoteButtonPulseSequence[] pulseSequences = new RemoteButtonPulseSequence[REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.values().length];
        public String command;


        RemoteButton(JSONObject json) {
            buttonName = json.optString("buttonName", "");
            buttonId = json.optInt("buttonId", -1);

            for(int i = 0; i < pulseSequences.length; i++) {
                pulseSequences[i] = new RemoteButtonPulseSequence(json.optJSONArray(REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.values()[i].name().toLowerCase()));

            }
            command = json.optString("command", "");
        }

        JSONObject toJSON() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("buttonName", buttonName);
                obj.put("buttonId", buttonId);
                obj.put("command", command);
                for(int i = 0; i < pulseSequences.length; i++) {
                    if(pulseSequences[i].sequence.length == 0) {
                        obj.put(REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.values()[i].name().toLowerCase(), null);
                    }
                    else {
                        JSONArray array = new JSONArray();
                        for(int sequence : pulseSequences[i].sequence) {
                            array.put(sequence);
                        }
                        obj.put(REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.values()[i].name().toLowerCase(), array);
                    }
                }
            } catch (JSONException ignored) {}
            return obj;

        }

        private int irCommandSize() {
            int size = 1;
            for(RemoteButtonPulseSequence sequence : pulseSequences) {
                size += 1 + 1 + sequence.sequence.length * 2;
            }
            return size;
        }

        public byte[] getIRCommand(SetTopBoxSetting settings) {
            int frequency = settings.frequencyKhz;
            int repeats = settings.repeats;
            ByteBuffer byteBuffer = ByteBuffer.allocate(irCommandSize());
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.put((byte)frequency);
            for (RemoteButtonPulseSequence pulseSequence : pulseSequences) {
                byteBuffer.put((byte) pulseSequence.sequence.length);
                byteBuffer.put((byte) repeats);
                for (int j = 0; j < pulseSequence.sequence.length; j++) {
                    byteBuffer.putChar((char)(frequency == 0 ? (int) Math.round(pulseSequence.sequence[j] / 8.0) : (int) Math.round(frequency * pulseSequence.sequence[j] / 500.0)));
                }
            }
            return byteBuffer.array();
        }
    }

    private final String folderName;
    public String setTopBoxName = "None"; //NON-NLS
    public boolean useIR;
    public int frequencyKhz;
    public int baudRate;
    public String serialPortDevice;
    public int repeats;
    public Map<String, RemoteButton> remoteButtonMap = new HashMap<>();

    public SetTopBoxSetting(String folderName, JSONObject json) {
        this.folderName = folderName;
        load(json);
    }

    public String getFolderName() {
        return folderName;
    }

    void load(JSONObject json) {
        if (json == null) return;
        try {
            setTopBoxName = json.getString("setTopBoxName");
            useIR = json.getBoolean("useIR"); //NON-NLS
            serialPortDevice = json.getString("serialPortDevice");
            baudRate = json.getJSONObject("serialPortConfiguration").getInt("baudRate");
            frequencyKhz = json.optInt("irFrequencyKhz", 0);
            repeats = json.optInt("repeats", 0);
            JSONArray jsonArray = json.getJSONArray("remoteButtons");
            for (int i = 0; i < jsonArray.length(); i++) {
                RemoteButton remoteButton = new RemoteButton(jsonArray.getJSONObject(i));
                remoteButtonMap.put(remoteButton.buttonName, remoteButton);
            }

        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("serialPortDevice", serialPortDevice);
            json.put("setTopBoxName", setTopBoxName);
            json.put("irFrequencyKhz", frequencyKhz);
            json.put("useIR", useIR);
            JSONObject serialPortConfiguration = new JSONObject();
            serialPortConfiguration.put("baudRate", baudRate);
            json.put("serialPortConfiguration", serialPortConfiguration);
            json.put("repeats", repeats);

            JSONArray jsonArray = new JSONArray();
            for(RemoteButton button : remoteButtonMap.values()) {
                jsonArray.put(button.toJSON());
            }
            json.put("remoteButtons", jsonArray);
        } catch(JSONException ignored) {
        }
        return json;
    }

}
