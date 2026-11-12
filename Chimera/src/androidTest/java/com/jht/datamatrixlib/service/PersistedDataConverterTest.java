package com.jht.datamatrixlib.service;

import android.content.SharedPreferences;

import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.fixtures.BaseTestFixture;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PersistedDataConverterTest {

    /**
     * Test to ensure the shared preferences meet the following requirements
     *  1. Contains the SettingKeys values that have been marked to be persisted
     *      a. It is possible that a key may not be in the shared preferences that is an indicator that
     *      the persisted key has not been overridden by the user and should use the default value.
     *  2. All obsolete SettingKeys have been removed from the shared preferences
     */
    @Test
    @Emulator
    public void convertPersistedDataInSharedPreferences() {
        try(BaseTestFixture baseTestFixture = new BaseTestFixture()) {
            PersistedDataConverter.convertPersistedDataInSharedPreferences(baseTestFixture.getTargetContext(), true);

            SharedPreferences sharedPreferences = baseTestFixture.getTargetContext().getSharedPreferences(PersistedDataConverterUtils.SHARED_PREF_FILE_NAME, 0);
            Set<String> sharedPreferenceKeys = sharedPreferences.getAll().keySet();
            Set<String> missingPersistedSettingKeys = new HashSet<>();
            Set<String> foundObsoletePersistedSettingKeys = new HashSet<>();

            Map<String, PersistedDataConverterUtils.PersistedKeyData> persistedSettingKeyThesaurus = PersistedDataConverterUtils.getInstance().getPersistedSettingKeyThesaurus();


            for (String currentKeyName : persistedSettingKeyThesaurus.keySet()) {
                PersistedDataConverterUtils.PersistedKeyData keyData = persistedSettingKeyThesaurus.get(currentKeyName);
                if(keyData != null) {
                    List<String> obsoleteKeyNames = keyData.getObsoleteKeyNames();

                    if (!sharedPreferenceKeys.contains(currentKeyName)) {
                        missingPersistedSettingKeys.add(currentKeyName);
                    }

                    for (String currentObsoleteKeyName : obsoleteKeyNames) {
                        if (sharedPreferenceKeys.contains(currentObsoleteKeyName)) {
                            foundObsoletePersistedSettingKeys.add(currentObsoleteKeyName);
                        }
                    }
                }
            }

            StringBuilder testResult = new StringBuilder();
            if (persistedSettingKeyThesaurus.keySet().size() != sharedPreferenceKeys.size()) {
                testResult.append(String.format("PersistedDataConverterTest - PersistedSettingKeyThesaurusData Entries =  %s%n", persistedSettingKeyThesaurus.keySet().size()));
                testResult.append(String.format("PersistedDataConverterTest - SharedPreferenceKey Entries = %s%n", sharedPreferenceKeys.size()));
                testResult.append("PersistedDataConverterTest - The following keys were not found:\n");

                for(String currentMissingPersistedSettingThesaurusKey : missingPersistedSettingKeys) {
                    testResult.append(String.format("\tPersistedDataConverterTest - %s%n", currentMissingPersistedSettingThesaurusKey));
                }

                testResult.append("PersistedDataConverterTest - The following obsolete keys were found:\n");
                for(String foundObsoleteSettingKey : foundObsoletePersistedSettingKeys) {
                    testResult.append(String.format("\tPersistedDataConverterTest - %s%n", foundObsoleteSettingKey));
                }
            }

            Assert.assertTrue(testResult.toString(), persistedSettingKeyThesaurus.keySet().size() >= sharedPreferenceKeys.size());
            Assert.assertEquals(testResult.toString(), 0, foundObsoletePersistedSettingKeys.size());
        }
    }

    /**
     * Runs the specified JSON file through the persisted data file converter and verifies the following
     *  1. The number of exported is less than or equal to the number of known exported keys
     *  2. None of keys flagged as obsolete are present in the JSON data
     */
    @Test
    public void convertPersistedDataInFile() {
        try(BaseTestFixture ignored = new BaseTestFixture()) {
            Map<String, PersistedDataConverterUtils.PersistedKeyData> persistedSettingKeyThesaurus = PersistedDataConverterUtils.getInstance().getPersistedSettingKeyThesaurus();

            JSONParser jsonParser = new JSONParser();
            File originalPersistedSettingFile = new File("D:\\UnitTesting\\TestData\\json_settings");
            FileReader settingFileReader = new FileReader(originalPersistedSettingFile);
            JSONArray settingData = (JSONArray) jsonParser.parse(settingFileReader);

            PersistedDataConverter.convertPersistedDataInFile(settingData);
            Map<String, JSONObject> convertedJSONData = new HashMap<>();

            for (Object currentSettingData: settingData) {
                JSONObject currentJSONObject = new JSONObject(currentSettingData.toString());
                convertedJSONData.put(currentJSONObject.getString("key"), currentJSONObject);
            }

            List<String> foundObsoletePersistedSettingKeys = new ArrayList<>();
            int exportedKeyCount = 0;
            for (String currentKeyName : persistedSettingKeyThesaurus.keySet()) {
                PersistedDataConverterUtils.PersistedKeyData keyData = persistedSettingKeyThesaurus.get(currentKeyName);
                if(keyData != null) {
                    if (keyData.getDataDefault().exportType().isEmpty()) {
                        continue;
                    }

                    exportedKeyCount++;
                    List<String> obsoleteKeyNames = keyData.getObsoleteKeyNames();

                    for (String currentObsoleteKeyName : obsoleteKeyNames) {
                        Set<String> keySet = convertedJSONData.keySet();
                        if (keySet.contains(currentObsoleteKeyName)) {
                            foundObsoletePersistedSettingKeys.add(currentObsoleteKeyName);
                        }
                    }
                }
            }

            // The number of exported keys must be at greater than or equal to the number of keys found in the imported file
            Assert.assertTrue(exportedKeyCount >= convertedJSONData.size());
            Assert.assertEquals(0, foundObsoletePersistedSettingKeys.size());
        } catch (ParseException | IOException | JSONException e) {
            e.printStackTrace();
        }
    }

}