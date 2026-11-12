package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.service.PersistedDataConverter;
import com.jht.datamatrixlib.service.PersistedDataConverterUtils;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class DataMatrixUnitTest {
    @NonNls final private String TAG = "DataMatrixUnitTest";

    @NonNls private final String KEY = "key";

    private int errorCount = 0;
    private String testName = "";
    private final Map<String, Vector<String>> keyNameData = new HashMap<>();
    private final Map<String, JSONObject> persistedData = new HashMap<>();
    private final Vector<String> keyNameInvalidVec = new Vector<>();


    @Test
    public void BTSinkKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(BTSinkKeys key : BTSinkKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void ClubComKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(ClubComKeys key : ClubComKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void ConsoleConfigKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(ConsoleConfigKeys key : ConsoleConfigKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void DAPIKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(DAPIKeys key : DAPIKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void FeaturedWorkoutKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(FeaturedWorkoutKeys key : FeaturedWorkoutKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void HALKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(HALKeys key : HALKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void ImportExportKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(ImportExportKeys key : ImportExportKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void LoggerKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(LoggerKeys key : LoggerKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void MainAppKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(MainAppKeys key : MainAppKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void PTPKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(PTPKeys key : PTPKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void SettingsKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(SettingKeys key : SettingKeys.values()) {
            processKeyName(key.name());
        }

        for (SettingKeys.General key : SettingKeys.General.values()) {
            processKeyName(key.name());
        }

        for (SettingKeys.Workout key : SettingKeys.Workout.values()) {
            processKeyName(key.name());
        }

        for (SettingKeys.Update key : SettingKeys.Update.values()) {
            processKeyName(key.name());
        }

        for (SettingKeys.AssetManagement key : SettingKeys.AssetManagement.values()) {
            processKeyName(key.name());
        }

        for (SettingKeys.Weather key : SettingKeys.Weather.values()) {
            processKeyName(key.name());
        }

        for (SettingKeys.SetupDefault key : SettingKeys.SetupDefault.values()) {
            processKeyName(key.name());
        }

        for (SettingKeys.Application key : SettingKeys.Application.values()) {
            processKeyName(key.name());
        }

        for (SettingKeys.Calibration key : SettingKeys.Calibration.values()) {
            processKeyName(key.name());
        }

        for (SettingKeys.Hardware key : SettingKeys.Hardware.values()) {
            processKeyName(key.name());
        }

        for (SettingKeys.Debug key : SettingKeys.Debug.values()) {
            processKeyName(key.name());
        }

        for (SettingKeys.TV key : SettingKeys.TV.values()) {
            processKeyName(key.name());
        }

        for (SettingKeys.Experimental key : SettingKeys.Experimental.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void Sprint8KeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(Sprint8Keys key : Sprint8Keys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void SSOKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(SSOKeys key : SSOKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void TiersKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(TiersKeys key : TiersKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void TopBarKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(TopBarKeys key : TopBarKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void TVKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(TVKeys key : TVKeys.values()) {
            processKeyName(key.name());
        }
        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void UIServiceKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(UiServiceKeys key : UiServiceKeys.values()) {
            processKeyName(key.name());
        }
        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void USBMediaKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(UsbMediaDeviceKeys key : UsbMediaDeviceKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void VAKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(VaKeys key : VaKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void WorkoutKeyTypeTest() {
        testName = new Object() {} .getClass().getEnclosingMethod().getName();

        for(WorkoutKeys key : WorkoutKeys.values()) {
            processKeyName(key.name());
        }

        validateKeyNameTest();
        clearTestResults();
    }

    @Test
    public void PersistedDataConversionTest() {
        testName = new Object() {}.getClass().getEnclosingMethod().getName();

        convertPersistedJSONData();
        clearTestResults();
    }

    private void processKeyName(String keyName) {
        keyNameData.put(keyName, new Vector<>(Arrays.asList(keyName.split("_"))));
    }

    /**
     * TODO: This test will need to be moved to an instrumentation test to pull the json_settings
     *       file from a console rather than a local PC folder.
     */
    public void convertPersistedJSONData() {
        try {
            Map<String, PersistedDataConverterUtils.PersistedKeyData> persistedSettingKeyThesaurus = PersistedDataConverterUtils.getInstance().getPersistedSettingKeyThesaurus();

            JSONParser jsonParser = new JSONParser();
            File originalPersistedSettingFile = new File("D:\\UnitTesting\\TestData\\settings\\json_settings");
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
                if (persistedSettingKeyThesaurus.get(currentKeyName).getDataDefault().exportType().isEmpty()) {
                    continue;
                }

                exportedKeyCount++;
                List<String> obsoleteKeyNames = persistedSettingKeyThesaurus.get(currentKeyName).getObsoleteKeyNames();

                for (String currentObsoleteKeyName : obsoleteKeyNames) {
                    if (convertedJSONData.containsKey(currentObsoleteKeyName)) {
                        foundObsoletePersistedSettingKeys.add(currentObsoleteKeyName);
                    }
                }
            }

            // The number of exported keys must be greater than or equal to the number of keys found in the imported file
            // There may be more keys marked for export than the current file that has been converted
            Assert.assertTrue(exportedKeyCount >= convertedJSONData.size());
            Assert.assertEquals(0, foundObsoletePersistedSettingKeys.size());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void validateKeyNameTest() {
        for(Map.Entry<String, Vector<String>> keyNameDataEntry : keyNameData.entrySet()) {
            String keyName = keyNameDataEntry.getKey();
            Vector<String> tokenVec = keyNameDataEntry.getValue();

            // Enforce that the key has at least 3 tokens
            if(tokenVec.size() < 3) {
                errorCount++;
                keyNameInvalidVec.add(keyName);
            }
            
            // The key name starts with one of valid key types
            String firstElement = tokenVec.get(0);
            switch (firstElement) {
                case "DATA":
                    // All data keys must specify the data type that will be returned as the last element of the name
                    processIsKeyNameSuffixValid(tokenVec.get(tokenVec.size() - 1), keyName);
                    break;
                case "ACTION":
                case "EVENT":
                    // Fall through, No additional checks are needed at this time
                    break;
                default:
                    errorCount++;
                    keyNameInvalidVec.add(keyName);
            }
        }

        System.out.format("Test Running: %s%n%n", testName);

        if(keyNameInvalidVec.size() > 0) {
            System.out.format("The following keys do not meet naming conventions:%n");
            for(int i = 0; i < keyNameInvalidVec.size(); i++) {
                System.out.println(keyNameInvalidVec.get(i));
            }

            System.out.format("%nError Count: %d%n%n", errorCount);

        } else {
            System.out.format("All keys in this test meet naming conventions!%n");
        }

        Assert.assertEquals(0, errorCount);
    }

    private void processIsKeyNameSuffixValid(@NotNull String lastElement, String keyName) {
        switch(lastElement) {
            case "INT":
            case "LONG":
            case "BYTES":
            case "FLOAT":
            case "DOUBLE":
            case "BOOL":
            case "STRING":
            case "LIST":
            case "PARCELABLE":
                // Fall through, These are the only valid data types
                break;
            default:
                errorCount++;
                keyNameInvalidVec.add(keyName);
        }
    }

    private void clearTestResults() {
        keyNameData.clear();
        keyNameInvalidVec.clear();
        persistedData.clear();
        errorCount = 0;
    }
}
