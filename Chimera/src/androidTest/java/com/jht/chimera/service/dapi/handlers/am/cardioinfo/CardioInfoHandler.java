package com.jht.chimera.service.dapi.handlers.am.cardioinfo;

import static com.jhtp.dapiservice.wt.cardio.DapiCardioComplete.MACHINE_TYPE_CARDIO;
import static com.jhtp.dapiservice.wt.cardio.DapiCardioComplete.MACHINE_TYPE_INDOOR_CYCLE;
import static com.jhtp.dapiservice.wt.cardio.DapiCardioComplete.MACHINE_TYPE_ROWER;
import static com.jhtp.dapiservice.wt.cardio.DapiCardioComplete.MACHINE_TYPE_SDRIVE;

import android.util.Log;

import com.jacob.CborDecoder;
import com.jht.chimera.service.dapi.handlers.RequestHandler;
import com.jht.chimera.service.dapi.testfixture.DAPIServiceTestFixture;
import com.jhtp.dapiservice.am.CardioInfo;
import com.jhtp.dapiservice.comm.CborCommands;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;

public class CardioInfoHandler extends RequestHandler {
    private static final String TAG = "CardioInfoHandler";
    private final DAPIServiceTestFixture testFixture;
    public CardioInfoHandler(DAPIServiceTestFixture testFixture) {
        this.testFixture = testFixture;
    }
    public @NotNull MockResponse parse(CborDecoder decoder) throws IOException {
        testFixture.receivedRequest(new CardioInfoDAPIRequest(getVersion(), new ParseCardioInfo(decoder).parse()));
        return createGenericResponse(0, "");

    }

    @Override
    public int getCommand() {
        return CborCommands.CardioInfo.Value;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    private static class ParseCardioInfo {
        private final CborDecoder decoder;
        ParseCardioInfo(CborDecoder decoder) {
            this.decoder = decoder;
        }
        private CardioInfo parse() throws IOException {
            CardioInfo cardioInfo = new CardioInfo();
            cardioInfo.machineId = decoder.readTextString();
            cardioInfo.brand = (int) decoder.readInt();
            cardioInfo.machineType = (int) decoder.readInt();
            cardioInfo.modelType = (int) decoder.readInt();
            cardioInfo.consoleType = (int) decoder.readInt();
            cardioInfo.csafeManufacture = (int) decoder.readInt();
            cardioInfo.csafeId = (int) decoder.readInt();
            cardioInfo.csafeModelId = (int) decoder.readInt();
            cardioInfo.consoleSerialNumber = decoder.readTextString();
            cardioInfo.frameSerialNumber = decoder.readTextString();
            cardioInfo.clubId = (int) decoder.readInt();
            cardioInfo.softwareVersion = decoder.readTextString();
            cardioInfo.osVersion = decoder.readTextString();
            cardioInfo.maxUsers = (int) decoder.readInt();
            cardioInfo.ipAddress = decoder.readTextString();
            cardioInfo.totalTime = (int) decoder.readInt();
            cardioInfo.averageTimePerWorkout = (int) decoder.readInt();
            cardioInfo.totalDistance = decoder.readFloat();
            cardioInfo.averageDistancePerWorkout = decoder.readFloat();
            cardioInfo.averagePace = (int) decoder.readInt();
            cardioInfo.peakResistance = (int) decoder.readInt();
            cardioInfo.averageResistance = decoder.readFloat();
            cardioInfo.peakWatts = (int) decoder.readInt();
            cardioInfo.averageWatts = decoder.readFloat();
            cardioInfo.batteryPercentage = decoder.readFloat();
            cardioInfo.peakKph = decoder.readFloat();
            cardioInfo.averageKph = decoder.readFloat();
            cardioInfo.headphoneInsertions = (int) decoder.readInt();
            cardioInfo.headphoneRemovals = (int) decoder.readInt();
            int arrayLength = (int)decoder.readArrayLength();
            Log.d(TAG, "reading data length " + arrayLength);

            cardioInfo.machineSubtype = decoder.readTextString();
            Log.d(TAG, "reading machine subtype " + cardioInfo.machineSubtype);

            switch (cardioInfo.machineSubtype) {
                case MACHINE_TYPE_CARDIO:
                    getCardioSubtypeInfo(cardioInfo);
                    break;
                case MACHINE_TYPE_INDOOR_CYCLE:
                    getIndoorCycleSubtypeInfo(cardioInfo);
                    break;
                case MACHINE_TYPE_ROWER:
                    getRowerSubtypeInfo(cardioInfo);
                    break;
                case MACHINE_TYPE_SDRIVE:
                default:
                    break;
            }
            return cardioInfo;
        }

        private void getRowerSubtypeInfo(CardioInfo cardioInfo) throws IOException {
            cardioInfo.peakSpm = (int) decoder.readInt();
            cardioInfo.averageSpm = decoder.readFloat();
        }

        private void getIndoorCycleSubtypeInfo(CardioInfo cardioInfo) throws IOException {
            cardioInfo.peakRpm = (int) decoder.readInt();
            cardioInfo.averageRpm = decoder.readFloat();
        }

        private void getCardioSubtypeInfo(CardioInfo cardioInfo) throws IOException {
            cardioInfo.driveType = (int) decoder.readInt();
            cardioInfo.minKph = (int) decoder.readInt();
            cardioInfo.maxKph = (int) decoder.readInt();
            cardioInfo.minResistance = (int) decoder.readInt();
            cardioInfo.maxResistance = (int) decoder.readInt();
            cardioInfo.minIncline = decoder.readFloat();
            cardioInfo.maxIncline = decoder.readFloat();
            cardioInfo.lcbVendor = (int) decoder.readInt();
            cardioInfo.lcbVersion = decoder.readTextString();
        }
    }
}
