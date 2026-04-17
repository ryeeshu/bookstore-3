package edu.cmu.bookstore.repository;

import edu.cmu.bookstore.model.Book;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BookRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Book> bookRowMapper = (rs, rowNum) -> {
        Book book = new Book();
        book.setIsbn(rs.getString("isbn"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setDescription(rs.getString("description"));
        book.setGenre(rs.getString("genre"));
        book.setPrice(rs.getBigDecimal("price"));
        book.setQuantity(rs.getInt("quantity"));
        book.setSummary(rs.getString("summary"));
        return book;
    };

    public BookRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByIsbn(String isbn) {
        String sql = "SELECT COUNT(*) FROM books WHERE isbn = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, isbn);
        return count != null && count > 0;
    }

    public void insertBook(Book book) {
        String sql = """
                INSERT INTO books (isbn, title, author, description, genre, price, quantity, summary)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getGenre(),
                book.getPrice(),
                book.getQuantity(),
                book.getSummary()
        );
    }

    public int updateBook(Book book) {
        String sql = """
                UPDATE books
                SET title = ?, author = ?, description = ?, genre = ?, price = ?, quantity = ?
                WHERE isbn = ?
                """;

        return jdbcTemplate.update(
                sql,
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getGenre(),
                book.getPrice(),
                book.getQuantity(),
                book.getIsbn()
        );
    }

    public int updateSummary(String isbn, String summary) {
        String sql = "UPDATE books SET summary = ? WHERE isbn = ?";
        return jdbcTemplate.update(sql, summary, isbn);
    }

    public Optional<Book> findByIsbn(String isbn) {
        String sql = """
                SELECT isbn, title, author, description, genre, price, quantity, summary
                FROM books
                WHERE isbn = ?
                """;

        List<Book> books = jdbcTemplate.query(sql, bookRowMapper, isbn);

        if (books.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(books.get(0));
    }

    public List<Book> findAll() {
        String sql = """
                SELECT isbn, title, author, description, genre, price, quantity, summary
                FROM books
                ORDER BY title
                """;

        return jdbcTemplate.query(sql, bookRowMapper);
    }
}