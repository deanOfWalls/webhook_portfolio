package com.deanofwalls.webhook_portfolio.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

@RestController
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Value("${portfolio.repo.path}")
    private String repoPath;

    @PostMapping("/webhook")
    public void handleGithubWebhook(@RequestBody String payload) {
        logger.info("Received payload: {}", payload);
        try {
            markRepoAsSafe();
            cleanLocalRepo();
            pullLatestChanges();
        } catch (IOException | InterruptedException e) {
            logger.error("Error processing webhook", e);
        }
    }

    private void markRepoAsSafe() throws IOException, InterruptedException {
        logger.info("Marking repo as safe at {}", repoPath);

        ProcessBuilder safeDirBuilder = new ProcessBuilder("git", "config", "--global", "--add", "safe.directory", repoPath);
        Process safeDirProcess = safeDirBuilder.start();
        int safeDirExitCode = safeDirProcess.waitFor();
        if (safeDirExitCode != 0 && safeDirExitCode != 5) { // exit code 5 means it is already configured
            logger.error("Git config safe.directory command failed with exit code: {}", safeDirExitCode);
            throw new IOException("Git config safe.directory command failed");
        }
    }

    private void cleanLocalRepo() throws IOException, InterruptedException {
        logger.info("Cleaning local repo at {}", repoPath);

        ProcessBuilder cleanBuilder = new ProcessBuilder("git", "reset", "--hard");
        cleanBuilder.directory(new File(repoPath));
        Process cleanProcess = cleanBuilder.start();
        int cleanExitCode = cleanProcess.waitFor();
        if (cleanExitCode != 0) {
            logger.error("Git reset command failed with exit code: {}", cleanExitCode);
            throw new IOException("Git reset command failed");
        }

        ProcessBuilder cleanBuilder2 = new ProcessBuilder("git", "clean", "-fd", "-e", "webhook_portfolio-0.0.1-SNAPSHOT.jar");
        cleanBuilder2.directory(new File(repoPath));
        Process cleanProcess2 = cleanBuilder2.start();
        int cleanExitCode2 = cleanProcess2.waitFor();
        if (cleanExitCode2 != 0) {
            logger.error("Git clean command failed with exit code: {}", cleanExitCode2);
            throw new IOException("Git clean command failed");
        }
    }

    private void pullLatestChanges() throws IOException, InterruptedException {
        logger.info("Pulling latest changes at {}", repoPath);

        ProcessBuilder builder = new ProcessBuilder("git", "pull", "origin", "main");
        builder.directory(new File(repoPath));
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("Git pull command failed with exit code: {}", exitCode);
            throw new IOException("Git pull command failed");
        }
    }
}
