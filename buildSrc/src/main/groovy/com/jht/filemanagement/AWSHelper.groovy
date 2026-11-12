package com.jht.filemanagement
/**
 * This helps us upload packages to AWS.
 */
class AWSHelper {
    private AWSHelper() { throw AssertionError("Stop trying to instantiate helper classes") }

    /**
     * Return true if the object exists on AWS.
     *
     * @param bucket     the AWS bucket
     * @param key        the AWS key
     * @param disableAWS
     * @return true if the object exists on AWS, else false.
     */
    static boolean objectExists(String bucket, String key, boolean disableAWS) {
        String command = "aws s3 ls s3://$bucket/$key"
        println(command)

        if (disableAWS) {
            return false
        }

        // FIXME(ARR) - Convert to something that is easier to translate to a better language.
        Process p = command.execute()
        p.waitFor()
        println("object exists exit value ${p.exitValue()}")
        return p.exitValue() == 0
    }

    /**
     * Uploads the file to AWS if it does not exist.
     *
     * @param bucket     The AWS bucket.
     * @param key        The AWS key.
     * @param file       The file to upload.
     * @param disableAWS
     */
    static void putObjectIfNotExists(String bucket, String key, File file, String sourceMD5, long installSize, boolean disableAWS) {
        if (!objectExists(bucket, key, disableAWS)) {
            println("put object")
            putObject(bucket, key, file, sourceMD5, installSize, disableAWS)
        }
    }

    /**
     * Uploads the file to AWS. This will override any existing file if called.
     *
     * @param bucket     The AWS bucket
     * @param key        The AWS key
     * @param file       The file to upload.
     * @param disableAWS
     */
    static void putObject(String bucket, String key, File file, String sourceMD5, long installSize, boolean disableAWS) {
        String md5 = FileManagement.getMD5(file)

        String[] command = (String[]) [
                "aws",
                "s3api",
                "put-object",
                "--bucket",
                bucket,
                "--key",
                key,
                "--metadata",
                "Content-MD5=${md5},Install-Size=${installSize},SourceMD5=${sourceMD5}",
                "--acl",
                "public-read",
                "--body",
                file.absolutePath
        ]

        println(command)

        if (!disableAWS) {
            Process process = command.execute()
            process.waitForProcessOutput(System.out, System.err)
            println("Exit value ${process.exitValue()}")
        }
    }

    /**
     * Downloads new files from the specified S3 bucket recursively to the directory given
     *
     * @param key       The AWS folder or file to download
     * @param outfile   The folder/file location on the local device where the download is placed
     */
    static void s3Sync(String key, String outfile) {

        String[] command = (String[]) [
                "aws",
                "s3",
                "sync",
                key,
                outfile,
                "--no-progress"
        ]

        println(command)

        Process process = command.execute()
        process.waitForProcessOutput(System.out, System.err)
//        println("Exit value ${process.exitValue()}")
    }
}
// aws s3api head-object --bucket my-bucket --key index.html
