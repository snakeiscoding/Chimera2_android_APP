package com.jht.filemanagement

import groovy.json.JsonSlurper
import org.jetbrains.annotations.Nullable

class AWSPackageObject {
    private String packageMD5 = ""
    private String sourceMD5 = null
    private long installSize = 0;

    String getMetaString() {
        return "packageMD5=" + packageMD5 + ", sourceMD5=" + sourceMD5 + ", installSize=" + installSize
    }

    private AWSPackageObject() {}

    static @Nullable AWSPackageObject getInfoIfExists(String bucket, String key) {
        String[] command = (String[]) [
                "aws",
                "s3api",
                "head-object",
                "--bucket",
                bucket,
                "--key",
                key
        ]

        println(command)

        ByteArrayOutputStream out = new ByteArrayOutputStream()
        ByteArrayOutputStream err = new ByteArrayOutputStream()
        Process process = command.execute()

        process.waitForProcessOutput(out, err)
        if(process.exitValue() == 0) {
            AWSPackageObject newObject = new AWSPackageObject()
            JsonSlurper jsonSlurper = new JsonSlurper()
            def headerInfo = jsonSlurper.parseText(out.toString())
            print(headerInfo)
            if(headerInfo != null && headerInfo.Metadata != null) {
                try { newObject.installSize = Long.parseLong(headerInfo.Metadata["install-size"]) }
                catch (Exception ignored) { newObject.installSize = 0 }
                newObject.sourceMD5 = headerInfo.Metadata.sourcemd5
                String eTag = headerInfo.ETag // THIS SHOULD REALLY TRY METADATA FIRST AND FALLBACK TO ETAG!
                newObject.packageMD5 = eTag.replace("\"", ""); // etag is usually in quotes
            }
            return newObject
        } else {
            return null
        }
    }


    boolean sourceMd5Match(String sourceMD5) {

        return sourceMD5 != null && sourceMD5 == this.sourceMD5
    }

    boolean md5Match(String md5) {
        return packageMD5 == md5
    }

    String packageMD5() {
        return this.packageMD5
    }
}