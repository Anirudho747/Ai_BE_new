package com.testleaf.controller;

import lombok.RequiredArgsConstructor;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class GitHubController {

    @Value("${github.token}")
    private String githubToken;

    @Value("${github.repo}")
    private String githubRepo; // Format: "owner/repo"

    // Endpoint to fetch branches
    @GetMapping("/branches")
    public ResponseEntity<List<String>> getBranches() {
        try {
            GitHub github = new GitHubBuilder().withOAuthToken(githubToken).build();
            GHRepository repo = github.getRepository(githubRepo);
            List<String> branches = repo.getBranches().values()
                    .stream()
                    .map(GHBranch::getName)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(branches);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // DTO for push code request
    public static class GitPushRequest {
        private String branch;
        private String commitMessage;
        private String javaCode;

        public String getBranch() {
            return branch;
        }
        public void setBranch(String branch) {
            this.branch = branch;
        }
        public String getCommitMessage() {
            return commitMessage;
        }
        public void setCommitMessage(String commitMessage) {
            this.commitMessage = commitMessage;
        }
        public String getJavaCode() {
            return javaCode;
        }
        public void setJavaCode(String javaCode) {
            this.javaCode = javaCode;
        }
    }

    // Endpoint to push code
    @PostMapping("/pushCode")
    public ResponseEntity<String> pushCode(@RequestBody GitPushRequest request) {
        try {
            GitHub github = new GitHubBuilder().withOAuthToken(githubToken).build();
            GHRepository repo = github.getRepository(githubRepo);
            String filePath = "generated-tests/GeneratedTest.java"; // Example file path

            GHContent content;
            try {
                content = repo.getFileContent(filePath, request.getBranch());
                // If the file exists, update it using three parameters.
                content.update(request.getJavaCode(), request.getCommitMessage(), content.getSha());
            } catch (GHFileNotFoundException ex) {
                // File doesn't exist; create it.
                repo.createContent()
                    .path(filePath)
                    .content(request.getJavaCode())
                    .message(request.getCommitMessage())
                    .branch(request.getBranch())
                    .commit();
            }
            return ResponseEntity.ok("Code pushed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error pushing code: " + e.getMessage());
        }
    }

}
