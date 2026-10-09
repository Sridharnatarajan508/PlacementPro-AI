package com.careerpilot.app.service;

import com.careerpilot.app.exception.AIServiceException;
import com.careerpilot.app.exception.GeminiRateLimitException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GeminiServiceRateLimitTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GeminiService geminiService;

    private final String dummyApiKey = "AIzaSyTestApiKey12345";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        geminiService.setRestTemplate(restTemplate);
        ReflectionTestUtils.setField(geminiService, "apiKey", dummyApiKey);
        ReflectionTestUtils.setField(geminiService, "model", "gemini-3.6-flash");
        ReflectionTestUtils.setField(geminiService, "objectMapper", objectMapper);
    }

    @Test
    public void testSuccessfulGeminiRequest() {
        Map<String, Object> candidate = Map.of(
                "content", Map.of(
                        "parts", Collections.singletonList(Map.of("text", "{\"overallScore\":85,\"atsScore\":80,\"strengths\":[\"Java\"],\"weaknesses\":[],\"detectedSkills\":[\"Java\"],\"missingSkills\":[],\"suggestions\":[],\"sectionAnalysis\":{}}"))
                )
        );
        Map<String, Object> responseBody = Map.of("candidates", Collections.singletonList(candidate));
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(responseEntity);

        String result = ReflectionTestUtils.invokeMethod(geminiService, "callGeminiApi", "Test prompt");
        assertNotNull(result);
        assertTrue(result.contains("overallScore"));
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class));
    }

    @Test
    public void testHttp429ThrowsGeminiRateLimitExceptionWithRetryAfter() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "45");
        String errorJson = "{\"error\":{\"code\":429,\"message\":\"Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests\",\"status\":\"RESOURCE_EXHAUSTED\"}}";

        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                headers,
                errorJson.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(ex);

        GeminiRateLimitException thrown = assertThrows(GeminiRateLimitException.class, () -> {
            ReflectionTestUtils.invokeMethod(geminiService, "callGeminiApi", "Test prompt");
        });

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, thrown.getStatus());
        assertEquals(45, thrown.getRetryAfterSeconds());
        assertTrue(thrown.getMessage().contains("Gemini usage limit reached"));
        // Ensure no retries occurred on 429
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class));
    }

    @Test
    public void testResourceExhaustedInResponseBodyThrowsGeminiRateLimitException() {
        String errorJson = "{\"error\":{\"code\":403,\"message\":\"Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, please retry after 30s.\",\"status\":\"RESOURCE_EXHAUSTED\",\"details\":[{\"@type\":\"type.googleapis.com/google.rpc.RetryInfo\",\"retryDelay\":\"30s\"}]}}";

        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                new HttpHeaders(),
                errorJson.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(ex);

        GeminiRateLimitException thrown = assertThrows(GeminiRateLimitException.class, () -> {
            ReflectionTestUtils.invokeMethod(geminiService, "callGeminiApi", "Test prompt");
        });

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, thrown.getStatus());
        assertEquals(30, thrown.getRetryAfterSeconds());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class));
    }

    @Test
    public void testHttp400BadRequestThrowsAIServiceException() {
        String errorJson = "{\"error\":{\"code\":400,\"message\":\"Invalid argument in prompt\"}}";
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                new HttpHeaders(),
                errorJson.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(ex);

        AIServiceException thrown = assertThrows(AIServiceException.class, () -> {
            ReflectionTestUtils.invokeMethod(geminiService, "callGeminiApi", "Test prompt");
        });

        assertEquals(HttpStatus.BAD_GATEWAY, thrown.getHttpStatus());
        assertFalse(thrown.getMessage().contains(dummyApiKey));
    }

    @Test
    public void testHttp401UnauthorizedThrowsAIServiceException() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                new HttpHeaders(),
                "{\"error\":{\"code\":401,\"message\":\"API_KEY_INVALID\"}}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(ex);

        AIServiceException thrown = assertThrows(AIServiceException.class, () -> {
            ReflectionTestUtils.invokeMethod(geminiService, "callGeminiApi", "Test prompt");
        });

        assertEquals(HttpStatus.BAD_GATEWAY, thrown.getHttpStatus());
        assertTrue(thrown.getMessage().contains("authentication failed"));
        assertFalse(thrown.getMessage().contains(dummyApiKey));
    }

    @Test
    public void testHttp403ForbiddenThrowsAIServiceException() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                new HttpHeaders(),
                "{\"error\":{\"code\":403,\"message\":\"PERMISSION_DENIED\"}}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(ex);

        AIServiceException thrown = assertThrows(AIServiceException.class, () -> {
            ReflectionTestUtils.invokeMethod(geminiService, "callGeminiApi", "Test prompt");
        });

        assertEquals(HttpStatus.BAD_GATEWAY, thrown.getHttpStatus());
        assertTrue(thrown.getMessage().contains("restricted"));
        assertFalse(thrown.getMessage().contains(dummyApiKey));
    }

    @Test
    public void testHttp500InternalServerErrorThrowsAIServiceException() {
        HttpServerErrorException ex = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                new HttpHeaders(),
                "{\"error\":{\"code\":500,\"message\":\"Internal error occurred\"}}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(ex);

        AIServiceException thrown = assertThrows(AIServiceException.class, () -> {
            ReflectionTestUtils.invokeMethod(geminiService, "callGeminiApi", "Test prompt");
        });

        assertEquals(HttpStatus.BAD_GATEWAY, thrown.getHttpStatus());
        assertFalse(thrown.getMessage().contains(dummyApiKey));
    }

    @Test
    public void testHttp503ServiceUnavailableRetriesOnce() {
        HttpServerErrorException ex = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                new HttpHeaders(),
                "{\"error\":{\"code\":503,\"message\":\"The model is overloaded.\"}}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(ex);

        AIServiceException thrown = assertThrows(AIServiceException.class, () -> {
            ReflectionTestUtils.invokeMethod(geminiService, "callGeminiApi", "Test prompt");
        });

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, thrown.getHttpStatus());
        // Verify exactly 2 attempts were made for 503
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class));
    }

    @Test
    public void testTimeoutThrowsAIServiceExceptionGatewayTimeout() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        AIServiceException thrown = assertThrows(AIServiceException.class, () -> {
            ReflectionTestUtils.invokeMethod(geminiService, "callGeminiApi", "Test prompt");
        });

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, thrown.getHttpStatus());
        assertTrue(thrown.getMessage().toLowerCase().contains("timeout"));
    }

    @Test
    public void testMalformedJsonExtractionThrowsAIServiceException() {
        Map<String, Object> emptyCandidates = Map.of("candidates", Collections.emptyList());
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(emptyCandidates, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(responseEntity);

        assertThrows(AIServiceException.class, () -> {
            ReflectionTestUtils.invokeMethod(geminiService, "callGeminiApi", "Test prompt");
        });
    }

    @Test
    public void testApiKeyNeverPresentInExceptionMessages() {
        String leakedKeyJson = "{\"error\":{\"code\":400,\"message\":\"Invalid key=" + dummyApiKey + "\"}}";
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                new HttpHeaders(),
                leakedKeyJson.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(ex);

        AIServiceException thrown = assertThrows(AIServiceException.class, () -> {
            ReflectionTestUtils.invokeMethod(geminiService, "callGeminiApi", "Test prompt");
        });

        assertFalse(thrown.getMessage().contains(dummyApiKey));
    }

    @Test
    public void testModelConfigurationRemainsGemini36Flash() {
        assertEquals("gemini-3.6-flash", geminiService.getSanitizedModelName());
    }
}
