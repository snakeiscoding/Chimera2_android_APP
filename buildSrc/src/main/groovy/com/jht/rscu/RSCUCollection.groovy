package com.jht.rscu

import org.json.JSONArray
import org.json.JSONObject

class RSCUCollection {
    class CollectionCreateException extends Exception {
        CollectionCreateException(String msg) {
            super(msg)
        }
    }
    private final String softwareConfiguration
    private final String version
    private String collectionId

    RSCUCollection(String softwareConfiguration, String version) {
        this.softwareConfiguration = softwareConfiguration
        this.version = version
    }

    private void createCollection(RSCU rscu, List<RSCUSoftwarePackage> softwarePackages) {
        JSONObject softwareCollection = new JSONObject()
        softwareCollection.put("software_configuration_class", softwareConfiguration)
        softwareCollection.put("version", version)
        JSONArray softwarePackagesJSON = new JSONArray()
        for(RSCUSoftwarePackage p : softwarePackages) {
            softwarePackagesJSON.put(p.getSoftwareId())
        }
        softwareCollection.put("software_ids", softwarePackagesJSON)

        try {
            def softwareCollectionResponse = rscu.postRSCUAPI("https://rscu-api.jfit.co/collection/", softwareCollection.toString(), false)
            if(softwareCollectionResponse == null || softwareCollectionResponse.id == null || softwareCollectionResponse.id.isEmpty()) {
                throw new CollectionCreateException("Did not get a collection id when creating a software collection.")
            }
            collectionId = softwareCollectionResponse.id
        }
        catch(RSCU.RSCUException ex) {
            throw new CollectionCreateException("Software id creation failed: " + ex.getMessage())
        }

    }

    void deploy(RSCU rscu, List<RSCUSoftwarePackage> softwarePackages, List<RSCUAssociation> associations) {
        createCollection(rscu, softwarePackages)
        for(RSCUAssociation association : associations) {
            deploy(rscu, association)
        }
    }

    void deploy(RSCU rscu, RSCUAssociation association) {
        JSONObject softwareDeploy = new JSONObject()
        softwareDeploy.put("collection_id", collectionId)
        JSONObject env = new JSONObject()
        env.put("dev", association.dev() ? 1 : 0)
        env.put("qa", association.qa() ? 1 : 0)
        env.put("staging", association.staging() ? 1 : 0)
        env.put("prod",0)
        softwareDeploy.put("env", env)
        softwareDeploy.put("club_id", association.clubId())

        try {
            rscu.postRSCUAPI("https://rscu-api.jfit.co/deploy/", softwareDeploy.toString(), false)
        }
        catch(RSCU.RSCUException ex) {
            throw new CollectionCreateException("Software deploy failed: " + ex.getMessage())
        }
    }

}