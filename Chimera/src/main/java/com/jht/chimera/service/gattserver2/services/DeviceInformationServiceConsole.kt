package com.jht.chimera.service.gattserver2.services

import android.util.Log
import com.jht.chimera.BuildConfig
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.gattserver2.IGattServer
import com.jht.gattserver2.services.DeviceInformationService

class DeviceInformationServiceConsole(server: IGattServer) : DeviceInformationService(server) {
    companion object {
        private const val ProductIdRfid1 = 0xCE60
        private const val ProductIdRfid2 = 0xCE61
        private const val ProductIdSgm = 0xD00A
        private const val ProductVersionRfid = 0x83
        private const val ProductVersionSgm = 0x0100
    }

    init {
        addManufacturerName("Matrix")
        addModelNumber("GUI")
        addSerialNumber(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, ""))
        addHardwareRevision(ProductConfig.detectUCBVendor())
        addSoftwareRevision(BuildConfig.SoftwareVersion)
        // only used when there's no standalone BT-Sink module (it reports its own PnP)
        val rfidModuleName = DataMatrix.getString(HALKeys.DATA_RFID_MODULE_NAME_STRING, "")
        val (productId, productVersion) = when (rfidModuleName) {
            "rfid1" -> ProductIdRfid1 to ProductVersionRfid
            "rfid4" -> ProductIdSgm to ProductVersionSgm
            else -> ProductIdRfid2 to ProductVersionRfid // rfid2, or nothing detected yet
        }
        Log.d("GattServer", "Detected rfid: $rfidModuleName, id: $productId, version: $productVersion")
        addPnpId(VendorIdSource.BluetoothSig, VendorIdBtMatrix, productId, productVersion)
    }
}
