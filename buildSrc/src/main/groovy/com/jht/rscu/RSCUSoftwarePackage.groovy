package com.jht.rscu

import org.json.JSONObject

class RSCUSoftwarePackage {
    class UploadFailedException extends Exception {
        UploadFailedException(String msg) {
            super(msg)
        }
    }
    private final String url
    private String md5
    private String softwareId
    private final String softwareType
    private final String version
    private final String machineId

    RSCUSoftwarePackage(String url, String md5, String softwareType, String version, String machineId, String softwareId) {
        this.url = url
        this.md5 = md5 // FileManagement.getMD5(file)
        this.softwareType = softwareType
        this.version = version
        this.machineId = machineId
        this.softwareId = softwareId
    }

    static List<RSCUSoftwarePackage> getPackages(RSCU rscu, String softwareType) {
        List<RSCUSoftwarePackage> packages = new ArrayList<>()
        try {
            def softwareItemResponse = rscu.getRSCUAPI("https://rscu-api.jfit.co/software/", "", true)
            if(softwareItemResponse != null) {
                softwareItemResponse.each {
                    if(softwareType == it.software_type) {
                        packages.add(new RSCUSoftwarePackage(it.url, it.md5, it.software_type, it.version, "", it.id))
                    }
                }
            }
        }
        catch(RSCU.RSCUException ignored) {
        }
        return packages
    }

    private boolean checkExists(List<RSCUSoftwarePackage> packages) {
        for(RSCUSoftwarePackage pkg : packages) {
            if(pkg.softwareType == softwareType && pkg.version == version) {
                return true
            }
        }
        return false
    }

    void upload(RSCU rscu, List<RSCUSoftwarePackage> packages) {
        if (!checkExists(packages)) {
            try {
                def softwareItemResponse = rscu.postRSCUAPI("https://rscu-api.jfit.co/software/", getPostData().toString(), true)
                if (softwareItemResponse == null || softwareItemResponse.software == null || softwareItemResponse.software.id == null || softwareItemResponse.software.id.isEmpty()) {
                    throw new UploadFailedException("Error uploading $this.")
                }
                softwareId = softwareItemResponse.software.id
            }
            catch (RSCU.RSCUException ex) {
                throw new UploadFailedException("Error uploading $this: " + ex.getMessage())
            }
        }
    }

    void update(RSCU rscu, String md5) {
        if(this.md5 != md5) {
            this.md5 = md5
            try {
                def softwareItemResponse = rscu.putRSCUAPI("https://rscu-api.jfit.co/software/$softwareId", getPostData().toString(), true)
                if (softwareItemResponse == null) {
                    throw new UploadFailedException("Error updating software $this.")
                }
            }
            catch (RSCU.RSCUException ex) {
                throw new UploadFailedException("Error updating software $this: " + ex.getMessage())
            }
            println("Updated $this on RSCU")
        }
    }

    private JSONObject getPostData() {
        JSONObject softwareItem = new JSONObject()
        softwareItem.put("software_type", softwareType)
        softwareItem.put("version", version)
        softwareItem.put("url", url)
        softwareItem.put("md5", md5)
        if(machineId != null && !machineId.isEmpty()) {
            softwareItem.put("machine_id", machineId)
        }
        return softwareItem
    }

    String getSoftwareId() {
        return softwareId
    }

    boolean equals(String version, String url) {
        return this.version == version && this.url == url
    }

    @Override
    boolean equals(Object other) {
        if(other instanceof RSCUSoftwarePackage) {
            RSCUSoftwarePackage otherObj = (RSCUSoftwarePackage)other
            return otherObj.url == url && otherObj.version == version
        }
        return false
    }

    @Override
    String toString() {
        return "Type: $softwareType, Version: $version, url: $url MD5: $md5, id: $softwareId, machineId: $machineId"
    }

}