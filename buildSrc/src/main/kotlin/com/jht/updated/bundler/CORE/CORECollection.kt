package com.jht.updated.bundler.CORE

import org.json.JSONArray
import org.json.JSONObject

/**
 * Class used to hold logic for creating Software Collections on CORE and deploying them to Clubs provided in COREAssociations
 */
class CORECollection(val softwareConfiguration: String, val version: String) {

    private var collectionId: String = "";

    class CollectionCreateException(message: String) : Exception(message) {};

    companion object {
        /**
         * Calls CORE API to verify if the provided software item exists on CORE already or not.
         * https://rscu-api.jfit.co/software/by-key? requires software Type, Version and MD5.
         * this causes issue as if existing version has a different MD5 that the computed builds MD5 it will not recognize a version exists.
         * Loop implementation isn't the best, but is only way right now to find a software item just by Type and Version
         * @param softwareCollection collection to check for
         * @param core CORE API Wrapper
         */
        fun checkExists(core: CORE, softwareCollection: CORECollection) : Boolean {
            val getCall = CORE_COLLECTION_URL

            val response = core.getCOREAPI(getCall)
            val jsonResponse = JSONArray(response)

            for(i in 0 until jsonResponse.length()) {
                val item = jsonResponse.getJSONObject(i)
                if(item.get("software_configuration_class") == softwareCollection.softwareConfiguration &&
                    item.get("version") == softwareCollection.version) {
                    println("Software Collection found")
                    return true
                }
            }
            println("Software Collection was not found")
            return false
        }
    }

    /**
     * Creates a Software Collection on CORE and deploys it to all provided COREAssociations
     * @param core
     * @param softwareItem software item deployed that will be linked to software collection
     * @param associations list of COREAssociations that the new collection will be deployed to
     */
    fun deploy(core: CORE, softwareItem: CORESoftwareItem, associations: List<COREAssociation>) {
        createCollection(core, softwareItem)
        for(association in associations) {
            deployToClub(core, association)
        }
    }

    /**
     * Method to deploy this Software Collection to a given club on the expected non production DAPI environments
     * @param core
     * @param association club association to deploy collection to
     */
    private fun deployToClub(core: CORE, association: COREAssociation) {
        val softwareDeploy = JSONObject()
        softwareDeploy.put("collection_id", collectionId)
        val env = JSONObject()
        env.put("dev", if (association.dev) 1 else 0)
        env.put("qa", if (association.qa) 1 else 0)
        env.put("staging", if (association.staging) 1 else 0)
        env.put("prod", 0)
        softwareDeploy.put("env", env)
        softwareDeploy.put("club_id", association.clubId)

        try {
            core.postCOREAPI(CORE_DEPLOYS_URL, softwareDeploy.toString(), false)
        }
        catch(ex: CORE.COREException) {
            throw CollectionCreateException("Software deploy failed: " + ex.message)
        }
    }

    /**
     * Method to construct message to create a software collection in CORE with the given softwareItem.
     * Method calls API with info needed to create collection and stores the newly created collections ID future club deployments
     */
    private fun createCollection(core: CORE, softwareItem: CORESoftwareItem) {
        val softwareCollection = JSONObject()
        softwareCollection.put("software_configuration_class", softwareConfiguration)
        softwareCollection.put("version", version)
        val softwarePackagesJSON = JSONArray()
        softwarePackagesJSON.put(softwareItem.softwareId)
        softwareCollection.put("software_ids", softwarePackagesJSON)

        try {
            val softwareCollectionResponse = JSONObject(core.postCOREAPI(CORE_COLLECTION_URL, softwareCollection.toString(), false))
            if(softwareCollectionResponse.getString("id") == null || softwareCollectionResponse.getString("id").isEmpty()) {
                throw CollectionCreateException("Did not get a collection id when creating a software collection.")
            }
            collectionId = softwareCollectionResponse.getString("id")
        }
        catch(ex: CORE.COREException) {
            throw  CollectionCreateException("Software id creation failed: " + ex.message)
        }
    }
}