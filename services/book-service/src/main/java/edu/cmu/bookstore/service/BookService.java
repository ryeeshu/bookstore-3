package edu.cmu.bookstore.service;

import edu.cmu.bookstore.exception.BadRequestException;
import edu.cmu.bookstore.exception.ConflictException;
import edu.cmu.bookstore.exception.NotFoundException;
import edu.cmu.bookstore.model.Book;
import edu.cmu.bookstore.model.RelatedBook;
import edu.cmu.bookstore.model.request.CreateBookRequest;
import edu.cmu.bookstore.model.request.UpdateBookRequest;
import edu.cmu.bookstore.repository.BookRepository;
import edu.cmu.bookstore.util.CircuitBreaker;
import edu.cmu.bookstore.validation.BookValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookValidator bookValidator;
    private final LlmService llmService;
    private final CircuitBreaker circuitBreaker;
    private final RestTemplate recommendationRestTemplate;

    @Value("${app.recommendation.url:http://52.73.13.84}")
    private String recommendationUrl;

    public BookService(BookRepository bookRepository,
                       BookValidator bookValidator,
                       LlmService llmService,
                       CircuitBreaker circuitBreaker,
                       RestTemplateBuilder restTemplateBuilder) {
        this.bookRepository = bookRepository;
        this.bookValidator = bookValidator;
        this.llmService = llmService;
        this.circuitBreaker = circuitBreaker;
        this.recommendationRestTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }

    public Book createBook(CreateBookRequest request) {
        bookValidator.validateCreateRequest(request);

        String isbn = request.getIsbn().trim();

        if (bookRepository.existsByIsbn(isbn)) {
            throw new ConflictException("This ISBN already exists in the system.");
        }

        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setDescription(request.getDescription().trim());
        book.setGenre(request.getGenre().trim());
        book.setPrice(request.getPrice());
        book.setQuantity(request.getQuantity());
        book.setSummary(null);

        bookRepository.insertBook(book);

        try {
            String summary = llmService.generateSummary(book);
            if (summary != null && !summary.trim().isEmpty()) {
                bookRepository.updateSummary(book.getIsbn(), summary);
                book.setSummary(summary);
            }
        } catch (Exception ignored) {
        }

        return book;
    }

    public Book updateBook(String pathIsbn, UpdateBookRequest request) {
        bookValidator.validatePathIsbn(pathIsbn);
        bookValidator.validateUpdateRequest(request);

        String trimmedPathIsbn = pathIsbn.trim();
        String bodyIsbn = request.getIsbn().trim();

        if (!trimmedPathIsbn.equals(bodyIsbn)) {
            throw new BadRequestException("ISBN in path and body must match.");
        }

        Book existingBook = bookRepository.findByIsbn(trimmedPathIsbn)
                .orElseThrow(() -> new NotFoundException("Book not found."));

        existingBook.setTitle(request.getTitle().trim());
        existingBook.setAuthor(request.getAuthor().trim());
        existingBook.setDescription(request.getDescription().trim());
        existingBook.setGenre(request.getGenre().trim());
        existingBook.setPrice(request.getPrice());
        existingBook.setQuantity(request.getQuantity());

        bookRepository.updateBook(existingBook);

        return bookRepository.findByIsbn(trimmedPathIsbn)
                .orElseThrow(() -> new NotFoundException("Book not found."));
    }

    public Book getBookByIsbn(String isbn) {
        bookValidator.validatePathIsbn(isbn);

        Book book = bookRepository.findByIsbn(isbn.trim())
                .orElseThrow(() -> new NotFoundException("Book not found."));

        if (book.getSummary() == null || book.getSummary().trim().isEmpty()) {
            try {
                String summary = llmService.generateSummary(book);
                if (summary != null && !summary.trim().isEmpty()) {
                    bookRepository.updateSummary(book.getIsbn(), summary);
                    book.setSummary(summary);
                }
            } catch (Exception ignored) {
            }
        }

        return book;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public List<RelatedBook> getRelatedBooks(String isbn) {
        bookValidator.validatePathIsbn(isbn);

        if (!circuitBreaker.isCallAllowed()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Circuit is open");
        }

        String url = String.format("%s/recommended-titles/isbn/%s", recommendationUrl, isbn.trim());

        try {
            RelatedBook[] results = recommendationRestTemplate.getForObject(url, RelatedBook[].class);

            circuitBreaker.recordSuccess();

            if (results == null || results.length == 0) {
                return Collections.emptyList();
            }

            return Arrays.asList(results);
        } catch (ResourceAccessException e) {
            circuitBreaker.recordFailure();
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Call to recommendation service timed out.");
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error calling recommendation service.");
        }
    }
}