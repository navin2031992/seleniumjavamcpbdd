package com.automation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a Jira ticket/story fetched by JiraAgent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraTicket {
    private String key;           // e.g. PROJ-123
    private String summary;
    private String description;
    private String issueType;     // Story, Task, Bug, Epic
    private String status;
    private String priority;
    private String assignee;
    private String reporter;
    private List<String> labels;
    private List<String> components;
    private List<String> acceptanceCriteria;
    private String storyPoints;
    private String epicLink;
    private String sprintName;
    private List<String> attachments;

    public String toPromptContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("JIRA TICKET: ").append(key).append("\n");
        sb.append("Type: ").append(issueType).append("\n");
        sb.append("Summary: ").append(summary).append("\n");
        if (description != null && !description.isEmpty()) {
            sb.append("Description:\n").append(description).append("\n");
        }
        if (acceptanceCriteria != null && !acceptanceCriteria.isEmpty()) {
            sb.append("Acceptance Criteria:\n");
            acceptanceCriteria.forEach(ac -> sb.append("  - ").append(ac).append("\n"));
        }
        if (labels != null && !labels.isEmpty()) {
            sb.append("Labels: ").append(String.join(", ", labels)).append("\n");
        }
        return sb.toString();
    }
}
