package com.auction.shared.network;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Message - Dùng để giao tiếp giữa Client và Server.
 * Serializable để có thể gửi qua Socket.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private String action;
    private Object data;
    private String status; // SUCCESS, ERROR
    private String message;
    private Map<String, Object> dataMap;

    // ── Constructors ──────────────────────────────────────────

    public Message() {
        this.dataMap = new HashMap<>();
    }

    public Message(String action, Object data) {
        this.action = action;
        this.data = data;
        this.dataMap = new HashMap<>();
    }

    public Message(String action, String status, String message) {
        this.action = action;
        this.status = status;
        this.message = message;
        this.dataMap = new HashMap<>();
    }

    // ── Data Map Methods ───────────────────────────────────────

    public void setData(String key, Object value) {
        if (dataMap == null) {
            dataMap = new HashMap<>();
        }
        dataMap.put(key, value);
    }

    public Object getData(String key) {
        return dataMap != null ? dataMap.get(key) : null;
    }

    // ── Getters / Setters ──────────────────────────────────────

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getDataMap() { return dataMap; }
    public void setDataMap(Map<String, Object> dataMap) { this.dataMap = dataMap; }

    @Override
    public String toString() {
        return "Message{action='" + action + "', status='" + status + "', message='" + message + "'}";
    }
}
