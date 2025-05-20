package com.yourname.booktracker;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RateLimiter rateLimiter;
    private final String token;
    private final String apiUrl;

    public CrptApi(TimeUnit timeUnit, int requestLimit, String token) {
        this(
                timeUnit,
                requestLimit,
                token,
                HttpClient.newHttpClient(),
                "https://ismp.crpt.ru/api/v3/lk/documents/create"
        );
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit, String token, String apiUrl) {
        this(
                timeUnit,
                requestLimit,
                token,
                HttpClient.newHttpClient(),
                apiUrl
        );
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit, String token, HttpClient httpClient, String apiUrl) {
        this.rateLimiter = new RateLimiter(timeUnit, requestLimit);
        this.token = token;
        this.httpClient = httpClient;
        this.apiUrl = apiUrl;
    }

    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        rateLimiter.acquire();
        String requestBody = serializeRequest(document, signature);
        HttpRequest request = buildRequest(requestBody);
        sendRequest(request);
    }

    private String serializeRequest(Document document, String signature) throws IOException {
        return objectMapper.writeValueAsString(new CreateDocumentRequest(document, signature));
    }

    private HttpRequest buildRequest(String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private void sendRequest(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException(
                    "Failed to create document. Status: " + response.statusCode() + ", Body: " + response.body());
        }
    }

    private static class CreateDocumentRequest {
        public final Document product_document;
        public final String signature;

        public CreateDocumentRequest(Document document, String signature) {
            this.product_document = document;
            this.signature = signature;
        }
    }

    public static class Document {
        public String description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public String importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_type;
        public String production_date;
        public String reg_date;
        public String reg_number;

        private Document() {
        }

        public static class Builder {
            private final Document d = new Document();

            public Builder withDescription(String val) {
                d.description = val;
                return this;
            }

            public Builder withDocId(String val) {
                d.doc_id = val;
                return this;
            }

            public Builder withDocStatus(String val) {
                d.doc_status = val;
                return this;
            }

            public Builder withDocType(String val) {
                d.doc_type = val;
                return this;
            }

            public Builder withImportRequest(String val) {
                d.importRequest = val;
                return this;
            }

            public Builder withOwnerInn(String val) {
                d.owner_inn = val;
                return this;
            }

            public Builder withParticipantInn(String val) {
                d.participant_inn = val;
                return this;
            }

            public Builder withProducerInn(String val) {
                d.producer_inn = val;
                return this;
            }

            public Builder withProductionType(String val) {
                d.production_type = val;
                return this;
            }

            public Builder withProductionDate(String val) {
                d.production_date = val;
                return this;
            }

            public Builder withRegDate(String val) {
                d.reg_date = val;
                return this;
            }

            public Builder withRegNumber(String val) {
                d.reg_number = val;
                return this;
            }

            public Document build() {
                return d;
            }
        }
    }

    private static class RateLimiter {
        private final long intervalMillis;
        private final int maxRequests;
        private final Queue<Long> requestTimestamps = new LinkedList<>();

        public RateLimiter(TimeUnit timeUnit, int maxRequests) {
            this.intervalMillis = timeUnit.toMillis(1);
            this.maxRequests = maxRequests;
        }

        public synchronized void acquire() {
            long now = Instant.now().toEpochMilli();

            while (requestTimestamps.size() >= maxRequests) {
                long earliest = requestTimestamps.peek();
                long elapsed = now - earliest;
                if (elapsed < intervalMillis) {
                    try {
                        wait(intervalMillis - elapsed);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread interrupted while rate limiting", e);
                    }
                    now = Instant.now().toEpochMilli();
                } else {
                    requestTimestamps.poll();
                }
            }

            requestTimestamps.add(now);
            notifyAll();
        }
    }
}