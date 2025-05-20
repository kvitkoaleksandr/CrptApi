package com.yourname.booktracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CrptApiTest {

    @Mock
    HttpClient mockHttpClient;

    @Mock
    HttpResponse<String> mockResponse;

    CrptApi api;

    @BeforeEach
    void setUp() {
        api = new CrptApi(
                TimeUnit.MINUTES,
                5,
                "test-token",
                mockHttpClient,
                "https://test-api");
    }

    @Test
    void createDocument_shouldCallHttpClientSend() throws Exception {
        CrptApi.Document document = new CrptApi.Document.Builder()
                .withDocId("123")
                .withDocStatus("draft")
                .withDocType("LP_INTRODUCE_GOODS")
                .withProductionType("local")
                .withOwnerInn("1234567890")
                .withParticipantInn("1234567890")
                .withProducerInn("1234567890")
                .build();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);

        api.createDocument(document, "test-signature");

        verify(mockHttpClient, times(1)).send(any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class));
    }

    @Test
    void createDocument_shouldThrowIOException_onServerError() throws Exception {
        CrptApi.Document doc = new CrptApi.Document.Builder()
                .withDocId("bad")
                .build();

        when(mockHttpClient.<String>send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(500);

        IOException ex = assertThrows(IOException.class, () -> {
            api.createDocument(doc, "sig");
        });

        assertTrue(ex.getMessage().contains("Failed to create document"));
    }
}