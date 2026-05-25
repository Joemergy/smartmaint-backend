package com.smartmaint.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    Map<String, ?> details
) {
}