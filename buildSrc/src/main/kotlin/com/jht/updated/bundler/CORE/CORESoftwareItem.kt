package com.jht.updated.bundler.CORE

import org.json.JSONArray
import org.json.JSONObject

class CORESoftwareItem(val url: String, var md5: String, val softwareType: String, val version: String, val machineId: String, var softwareId: String) {

    class UploadFailedException(message: String) : Exception(message) {};

    companion object {
        /** Core has a character limit of 20 on the name of a software version. Needed in case version string is longer than the limit it will always believe the item does not
         *  exist already */
        private val MAX_VERSION_LENGTH = 20
        /**
         * Static function to Upload provided software item if it is not already uploaded
         * @param core CORE API Wrapper
         * @param softwareItem SoftwareItem to be uploaded
         */
        fun upload(core: CORE, softwareItem: CORESoftwareItem) : Boolean {
            if(!checkExists(core, softwareItem)) {
                try {
                    val softwareItemResponse = JSONObject(core.postCOREAPI(CORE_SOFTWARE_URL, softwareItem.getPostData().toString(), false))
                    softwareItem.softwareId = softwareItemResponse.getString("id")
                    return true
                }
                catch (ex: CORE.COREException) {
                    throw UploadFailedException("Error uploading $this: " + ex.message)
                }
            }
            else{
                println("Software Item already Exists")
            }
            return false
        }

        /**
         * Calls CORE API to verify if the provided software item exists on CORE already or not.
         * https://rscu-api.jfit.co/software/by-key? requires software Type, Version and MD5.
         * this causes issue as if existing version has a different MD5 that the computed builds MD5 it will not recognize a version exists.
         * Loop implementation isn't the best, but is only way right now to find a software item just by Type and Version
         * @param softwareItem item to check for
         * @param core CORE API Wrapper
         */
        fun checkExists(core: CORE, softwareItem: CORESoftwareItem) : Boolean {
            val getCall = CORE_SOFTWARE_URL

            val response = core.getCOREAPI(getCall)
            val jsonResponse = JSONArray(response)
            val cutVersion = softwareItem.version.take(MAX_VERSION_LENGTH)

            for(i in 0 until jsonResponse.length()) {
                val item = jsonResponse.getJSONObject(i)
                if(item.get("software_type") == softwareItem.softwareType && item.get("version") == cutVersion){
                    return true
                }
            }
            return false
        }
    }

    /**
     * Generate String of postData for Uploading software Item to CORE
     */
    private fun getPostData() : JSONObject {
        val softwareItem = JSONObject()
        softwareItem.put("software_type", softwareType)
        softwareItem.put("version", version)
        softwareItem.put("url", url)
        softwareItem.put("md5", md5)
        if(machineId != null && machineId.isNotEmpty()) {
            softwareItem.put("machine_id", machineId)
        }
        return softwareItem
    }

    fun equals(version: String, url: String): Boolean {
        return this.version == version && this.url == url
    }


    override fun equals(other: Any?): Boolean {
        if (other is CORESoftwareItem) {
            val otherSoftwareItem = other as CORESoftwareItem
            return otherSoftwareItem.url == url && otherSoftwareItem.version == version
        }
        return false
    }

    override fun toString(): String {
        return "Type: $softwareType, Version: $version, url: $url MD5: $md5, id: $softwareId, machineId: $machineId"
    }
}