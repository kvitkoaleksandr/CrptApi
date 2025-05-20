package com.yourname.booktracker;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        String token = "test-token";
        String testUrl = "https://postman-echo.com/post";

        CrptApi api = new CrptApi(TimeUnit.MINUTES, 5, token, testUrl);

        CrptApi.Document document = new CrptApi.Document.Builder()
                .withDescription("Пример")
                .withDocId("123456")
                .withDocStatus("draft")
                .withDocType("LP_INTRODUCE_GOODS")
                .withImportRequest("false")
                .withOwnerInn("1234567890")
                .withParticipantInn("1234567890")
                .withProducerInn("1234567890")
                .withProductionType("local")
                .withProductionDate("2024-01-01")
                .withRegDate("2024-01-02")
                .withRegNumber("ABC123456")
                .build();

        try {
            api.createDocument(document, "test-signature");
            System.out.println("Документ успешно отправлен.");
        } catch (Exception e) {
            System.out.println("Ошибка при отправке: " + e.getMessage());
        }
    }
}