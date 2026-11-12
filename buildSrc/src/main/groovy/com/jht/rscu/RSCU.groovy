package com.jht.rscu

import groovy.json.JsonSlurper

import javax.net.ssl.HttpsURLConnection
import java.util.zip.GZIPInputStream

class RSCU {

    private String rscuUser
    private String rscuPassword

    class RSCUException extends Exception {
        RSCUException(String msg) {
            super(msg)
        }
    }

    private List<RSCUSoftwarePackage> softwarePackages = new ArrayList<>()
    private final RSCULogin rscuLogin = new RSCULogin()

    RSCU(String user, String password) {
        rscuUser = user
        rscuPassword = password
    }

    void addPackage(RSCUSoftwarePackage softwarePackage) {
        softwarePackages.add(softwarePackage)
    }

    void login() {
        rscuLogin.login(this, rscuUser, rscuPassword)
    }

    void deploy(String softwareConfigurationClass, String version, List<RSCUAssociation> associations, String softwareType) {
        try {
            rscuLogin.login(this, rscuUser, rscuPassword)
            List<RSCUSoftwarePackage> deployedPackages = RSCUSoftwarePackage.getPackages(this, softwareType)
            for(RSCUSoftwarePackage softwarePackage : softwarePackages) {
                softwarePackage.upload(this, deployedPackages)
            }
            new RSCUCollection(softwareConfigurationClass, version).deploy(this, softwarePackages, associations)
        }
        catch(Exception ex) {
            println(ex.getMessage())
        }
    }

    Object postRSCUAPI(String api, String postData, boolean zip) {
        callRSCUAPI(api, "POST", postData, zip)
    }

    Object putRSCUAPI(String api, String postData, boolean zip) {
        callRSCUAPI(api, "PUT", postData, zip)
    }

    Object getRSCUAPI(String api, String postData, boolean zip) {
        callRSCUAPI(api, "GET", postData, zip)
    }

    Object callRSCUAPI(String api, String method, String postData, boolean zip) {
        URL rscuUrl = new URL(api)
        HttpsURLConnection urlConnection = rscuUrl.openConnection()
        urlConnection.setRequestMethod(method)
        // println("Posting data " + postData + " to " + api)

        urlConnection.setRequestProperty("Accept", "application/json, text/plain, */*")
        if (zip) {
            urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate, br")
            urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        }
        urlConnection.setRequestProperty("Connection", "keep-alive")
        if(!postData.isEmpty()) {
            urlConnection.setRequestProperty("Content-Length", postData.length() + "")
            urlConnection.setRequestProperty("Content-Type", "application/json")
        }
        if(rscuLogin.loggedIn()) {
            urlConnection.setRequestProperty("Authorization", "Bearer " + rscuLogin.getAuthToken())
        }

        urlConnection.setDoInput(true)

        if(!postData.isEmpty()) {
            urlConnection.setDoOutput(true)
            urlConnection.outputStream.write(postData.bytes)
            urlConnection.outputStream.close()
        }
        else {
            urlConnection.setDoOutput(false)
        }

        int responseCode = urlConnection.getResponseCode()
        if(responseCode != 200 && responseCode != 201) {
            // println("Got response code " + responseCode)
            throw new RSCUException("Unexpected response " + responseCode)
        }
        String response
        if(zip) {
            try {
                InputStream inflaterStream = new GZIPInputStream(urlConnection.inputStream)
                response = inflaterStream.getText('UTF-8')
            }
            catch(Exception ignored) {
                response = urlConnection.inputStream.getText()
            }
        }
        else {
            response = urlConnection.inputStream.getText()
        }
        // println("Response from RSCU " + response)

        def retVal = new JsonSlurper().parseText(response)
        return retVal
    }


}
