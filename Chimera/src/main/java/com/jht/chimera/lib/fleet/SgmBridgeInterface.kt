package com.jht.chimera.lib.fleet


interface SgmBridgeInterface {
    fun getCertPem() : String
    fun getKeyPem() : String
    fun getPublicKey() : ByteArray
    fun getKeySignature() : ByteArray
    fun deliverKeyEnvelope(envelope: EciesEnvelope, thingName: String, slotId: WalletSlot)
    fun eraseKey(slotId: WalletSlot)
}