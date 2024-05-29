package org.example.createdoc.controller;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping(value = "/api")
@Slf4j
public class CrptApi {
    private final static int CAPACITY = 20;
    private final static int TOKENS = 20;
    private final static int AMOUNT = 1;
    private final Bucket bucket;

    public CrptApi() {

        Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.greedy(TOKENS, Duration.of(AMOUNT, TimeUnit.MINUTES.toChronoUnit())));
        log.info("Creating bucket");
        this.bucket = Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    @PostMapping(value = "/v3/lk/documents/create")
    public ResponseEntity<?> createDoc(@RequestBody Document document, @RequestParam(name = "signature") String signature) {
        if (bucket.tryConsume(1)) {
            log.info("Document created");
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Authorization", "Bearer " + signature)
                    .body(document);
        } else {
            log.info("Create document failed");
            throw new SomeRateLimitingException();
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Document {
        Description description;
        String doc_id;
        String doc_status;
        String doc_type;
        boolean importRequest;
        String owner_inn;
        String participant_inn;
        String producer_inn;
        String production_date;
        String production_type;
        List<Product> products;
        String reg_date;
        String reg_number;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Description {
        String participantInn;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Product {
        String certificate_document;
        String certificate_document_date;
        String certificate_document_number;
        String owner_inn;
        String producer_inn;
        String production_date;
        String tnved_code;
        String uit_code;
        String uitu_code;
    }

    public static class SomeRateLimitingException extends RuntimeException {
        public SomeRateLimitingException() {
            super();
        }

        public SomeRateLimitingException(String message) {
            super(message);
        }
    }

    @RestControllerAdvice
    public static class CustomExceptionHandler {
        @ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS)
        @ExceptionHandler(SomeRateLimitingException.class)
        public String HandlerUnauthorized(SomeRateLimitingException ex) {
            return ex.getMessage();
        }
    }
}
