package edu.cmu.bookstore.controller;

import edu.cmu.bookstore.exception.BadRequestException;
import edu.cmu.bookstore.util.ForwardingClient;
import edu.cmu.bookstore.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * REST controller for the web Backend For Frontend (BFF).
 *
 * This controller validates the Authorization header and the presence
 * of the X-Client-Type header, then forwards requests to downstream services.
 */
@RestController
public class WebBffController {

    private final JwtUtil jwtUtil;
    private final ForwardingClient forwardingClient;

    public WebBffController(JwtUtil jwtUtil, ForwardingClient forwardingClient) {
        this.jwtUtil = jwtUtil;
        this.forwardingClient = forwardingClient;
    }

    @PostMapping("/books")
    public ResponseEntity createBook(
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String body) {

        jwtUtil.validateAuthorizationHeader(authorization);
        requireClientTypeHeader(clientType);
        return forwardingClient.post("/books", body);
    }

    @PutMapping("/books/{isbn}")
    public ResponseEntity updateBook(
            @PathVariable String isbn,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String body) {

        jwtUtil.validateAuthorizationHeader(authorization);
        requireClientTypeHeader(clientType);
        return forwardingClient.put("/books/" + encodePathSegment(isbn), body);
    }

    @GetMapping("/books")
    public ResponseEntity getAllBooks(
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        jwtUtil.validateAuthorizationHeader(authorization);
        requireClientTypeHeader(clientType);
        return forwardingClient.get("/books");
    }

    @GetMapping("/books/{isbn}")
    public ResponseEntity getBook(
            @PathVariable String isbn,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        jwtUtil.validateAuthorizationHeader(authorization);
        requireClientTypeHeader(clientType);
        return forwardingClient.get("/books/" + encodePathSegment(isbn));
    }

    @GetMapping("/books/isbn/{isbn}")
    public ResponseEntity getBookAlt(
            @PathVariable String isbn,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        jwtUtil.validateAuthorizationHeader(authorization);
        requireClientTypeHeader(clientType);
        return forwardingClient.get("/books/isbn/" + encodePathSegment(isbn));
    }

    @PostMapping("/customers")
    public ResponseEntity createCustomer(
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String body) {

        jwtUtil.validateAuthorizationHeader(authorization);
        requireClientTypeHeader(clientType);
        return forwardingClient.post("/customers", body);
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity getCustomerById(
            @PathVariable String id,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        jwtUtil.validateAuthorizationHeader(authorization);
        requireClientTypeHeader(clientType);
        return forwardingClient.get("/customers/" + encodePathSegment(id));
    }

    @GetMapping("/customers")
    public ResponseEntity getCustomerByUserId(
            HttpServletRequest request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        jwtUtil.validateAuthorizationHeader(authorization);
        requireClientTypeHeader(clientType);

        String rawQuery = request.getQueryString();
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new BadRequestException("Missing required query parameter: userId");
        }

        return forwardingClient.get("/customers?" + rawQuery);
    }

    @GetMapping("/books/{isbn}/related-books")
    public ResponseEntity getRelatedBooks(
            @PathVariable String isbn,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        jwtUtil.validateAuthorizationHeader(authorization);
        requireClientTypeHeader(clientType);
        return forwardingClient.get("/books/" + encodePathSegment(isbn) + "/related-books");
    }

    private void requireClientTypeHeader(String clientType) {
        if (clientType == null || clientType.trim().isEmpty()) {
            throw new BadRequestException("Missing X-Client-Type header.");
        }
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}