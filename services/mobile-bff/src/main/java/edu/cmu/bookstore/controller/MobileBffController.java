package edu.cmu.bookstore.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.cmu.bookstore.exception.BadRequestException;
import edu.cmu.bookstore.util.ForwardingClient;
import edu.cmu.bookstore.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * REST controller for the mobile Backend For Frontend (BFF).
 *
 * This controller validates the Authorization header and the presence
 * of the X-Client-Type header, forwards requests, and applies mobile-specific
 * transformations to selected responses.
 */
@RestController
public class MobileBffController {

    private final JwtUtil jwtUtil;
    private final ForwardingClient forwardingClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MobileBffController(JwtUtil jwtUtil, ForwardingClient forwardingClient) {
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

        ResponseEntity upstream = forwardingClient.get("/books");
        return transformBookListResponse(upstream);
    }

    @GetMapping("/books/{isbn}")
    public ResponseEntity getBook(
            @PathVariable String isbn,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        jwtUtil.validateAuthorizationHeader(authorization);
        requireClientTypeHeader(clientType);

        ResponseEntity upstream = forwardingClient.get("/books/" + encodePathSegment(isbn));
        return transformBookResponse(upstream);
    }

    @GetMapping("/books/isbn/{isbn}")
    public ResponseEntity getBookAlt(
            @PathVariable String isbn,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        jwtUtil.validateAuthorizationHeader(authorization);
        requireClientTypeHeader(clientType);

        ResponseEntity upstream = forwardingClient.get("/books/isbn/" + encodePathSegment(isbn));
        return transformBookResponse(upstream);
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

        ResponseEntity upstream = forwardingClient.get("/customers/" + encodePathSegment(id));
        return transformCustomerResponse(upstream);
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

        ResponseEntity upstream = forwardingClient.get("/customers?" + rawQuery);
        return transformCustomerResponse(upstream);
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

    private ResponseEntity transformBookResponse(ResponseEntity upstream) {
        if (!upstream.getStatusCode().is2xxSuccessful() || upstream.getBody() == null) {
            return upstream;
        }

        try {
            JsonNode root = objectMapper.readTree(upstream.getBody().toString());

            if (root instanceof ObjectNode objectNode) {
                JsonNode genreNode = objectNode.get("genre");
                if (genreNode != null && "non-fiction".equalsIgnoreCase(genreNode.asText())) {
                    objectNode.put("genre", 3);
                }

                String updatedBody = objectMapper.writeValueAsString(objectNode);
                return ResponseEntity
                        .status(upstream.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(updatedBody);
            }

            return upstream;
        } catch (Exception e) {
            return upstream;
        }
    }

    private ResponseEntity transformBookListResponse(ResponseEntity upstream) {
        if (!upstream.getStatusCode().is2xxSuccessful() || upstream.getBody() == null) {
            return upstream;
        }

        try {
            JsonNode root = objectMapper.readTree(upstream.getBody().toString());

            if (root.isArray()) {
                for (JsonNode node : root) {
                    if (node instanceof ObjectNode objectNode) {
                        JsonNode genreNode = objectNode.get("genre");
                        if (genreNode != null && "non-fiction".equalsIgnoreCase(genreNode.asText())) {
                            objectNode.put("genre", 3);
                        }
                    }
                }

                String updatedBody = objectMapper.writeValueAsString(root);
                return ResponseEntity
                        .status(upstream.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(updatedBody);
            }

            return upstream;
        } catch (Exception e) {
            return upstream;
        }
    }

    private ResponseEntity transformCustomerResponse(ResponseEntity upstream) {
        if (!upstream.getStatusCode().is2xxSuccessful() || upstream.getBody() == null) {
            return upstream;
        }

        try {
            JsonNode root = objectMapper.readTree(upstream.getBody().toString());

            if (root instanceof ObjectNode objectNode) {
                objectNode.remove("address");
                objectNode.remove("address2");
                objectNode.remove("city");
                objectNode.remove("state");
                objectNode.remove("zipcode");

                String updatedBody = objectMapper.writeValueAsString(objectNode);
                return ResponseEntity
                        .status(upstream.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(updatedBody);
            }

            return upstream;
        } catch (Exception e) {
            return upstream;
        }
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}