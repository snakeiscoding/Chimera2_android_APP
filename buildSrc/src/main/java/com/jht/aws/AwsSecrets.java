package com.jht.aws;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class AwsSecrets {
    private AwsSecrets() {
    }

    public static String getSecret(String key) throws IOException, InterruptedException {
        String base = System.getenv("CODEBUILD_BUILD_ID") != null ? "/CodeBuild/" : "/local-dev/";
        return getSecret(base, key);
    }

    public static String getSecret(String base, String key) throws IOException, InterruptedException {
        String[] command = {
                "aws",
                "secretsmanager",
                "get-secret-value",
                "--region",
                "us-east-2",
                "--secret-id",
                base + key,
                "--query",
                "SecretString",
                "--output",
                "text"
        };
        return runCommand(command);
    }

    private static String runCommand(String[] command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();
        process.waitFor(60, TimeUnit.SECONDS);

        String errorText = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.exitValue() != 0) {
            throw new IOException(errorText);
        }
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    }
}
