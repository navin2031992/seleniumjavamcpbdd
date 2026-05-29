package com.automation.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON-RPC 2.0 message envelope used by the MCP protocol.
 * Covers both requests (with id) and notifications (no id).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MCPMessage {

    @Builder.Default
    private String jsonrpc = "2.0";

    private String method;
    private Object params;
    private Integer id;

    // Response fields
    private Object result;
    private MCPError error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MCPError {
        private int code;
        private String message;
        private Object data;
    }

    // ── Factory helpers ────────────────────────────────────────────────────────

    public static MCPMessage request(int id, String method, Object params) {
        return MCPMessage.builder().id(id).method(method).params(params).build();
    }

    public static MCPMessage notification(String method, Object params) {
        return MCPMessage.builder().method(method).params(params).build();
    }

    public boolean isError() {
        return error != null;
    }
}
