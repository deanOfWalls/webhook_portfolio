package com.deanofwalls.webhook_portfolio.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.apache.commons.codec.binary.Hex;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @Value("${webhook.secret}")
    private String secretToken;

    @PostMapping
    public ResponseEntity<String> handleGithubWebhook(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            String signature = request.getHeader("X-Hub-Signature-256");
            if (verifySignature(payload, signature)) {
                if (payload.containsKey("ref") && "refs/heads/main".equals(payload.get("ref"))) {
                    pullLatestChanges();
                    return ResponseEntity.ok("Pulled latest changes");
                }
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
            }
        } catch (IOException | InterruptedException | NoSuchAlgorithmException | InvalidKeyException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error pulling changes");
        }
        return ResponseEntity.ok("Ignored non-main branch push");
    }

    private boolean verifySignature(Map<String, Object> payload, String signature) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secretToken.getBytes(), "HmacSHA256");
        sha256Hmac.init(secretKey);
        String hash = "sha256=" + Hex.encodeHexString(sha256Hmac.doFinal(payload.toString().getBytes()));
        return hash.equals(signature);
    }

    private void pullLatestChanges() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "pull");
        processBuilder.directory(new File("/var/www/portfolio"));
        Process process = processBuilder.start();
        process.waitFor();
    }
}
