package com.materialstudio.dto;

import java.util.Map;

public record ErrorResponse(String code, String message, Map<String, String> fieldErrors) {
    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
