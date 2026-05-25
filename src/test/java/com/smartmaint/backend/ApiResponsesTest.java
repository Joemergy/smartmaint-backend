package com.smartmaint.backend;

import com.smartmaint.api.ApiResponses;
import com.smartmaint.exception.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiResponsesTest {

    @Test
    void badRequest_debeMantenerErrorConReasonPhrase() {
        ResponseEntity<ApiErrorResponse> response = ApiResponses.badRequest("Dato invalido");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("BAD_REQUEST", response.getBody().code());
        assertEquals("Dato invalido", response.getBody().message());
    }
}
