package com.automation.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON-RPC 2.0 message envelope for the MCP protocol.
 *
 * Rules:
 * - @Builder.Default is intentionally removed to avoid the Lombok/Jackson conflict
 *   where @AllArgsConstructor receives a synthetic $default$ parameter that Jackson
 *   cannot match.
 * - jsonrpc is always set explicitly to "2.0" in the factory methods below.
 * - @JsonIgnoreProperties(ignoreUnknown=true) lets us safely ignore extra fields
 *   the MCP server may send (e.g. serverInfo, capabilities in the initialize response).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MCPMessage {

    private String  jsonrpc;   // always "2.0"  — set by factory methods
    private String  method;
    private Object  params;
    private Integer id;        // null for notifications

    // ── Response fields (only one of result / error is present) ──────────────
    private Object   result;
    private MCPError error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MCPError {
        private int    code;
        private String message;
        private Object data;
    }

    // ── Factory methods — always set jsonrpc explicitly ───────────────────────

    /** Build a JSON-RPC request (expects a matching response by id). */
    public static MCPMessage request(int id, String method, Object params) {
        return MCPMessage.builder()
            .jsonrpc("2.0")
            .id(id)
            .method(method)
            .params(params)
            .build();
    }

    /** Build a JSON-RPC notification (no response expected, no id field). */
    public static MCPMessage notification(String method, Object params) {
        return MCPMessage.builder()
            .jsonrpc("2.0")
            .method(method)
            .params(params)
            .build();
    }

    public boolean isError() {
        return error != null;
    }
}
