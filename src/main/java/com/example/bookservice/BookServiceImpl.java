package com.example.bookservice;

import com.example.BookServiceGrpc;
import com.example.BookServiceProto;
import io.grpc.stub.StreamObserver;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BookServiceImpl extends BookServiceGrpc.BookServiceImplBase {

    private final Map<String, BookServiceProto.Book> books = new ConcurrentHashMap<>();
    private final AtomicInteger bookIdCounter = new AtomicInteger(0);

    @Override
    public void addBook(BookServiceProto.AddBookRequest request,
                        StreamObserver<BookServiceProto.BookResponse> responseObserver) {
        try {
            String bookId = "B" + (bookIdCounter.getAndIncrement());

            BookServiceProto.Book book = BookServiceProto.Book.newBuilder()
                    .setId(bookId)
                    .setTitle(request.getTitle())
                    .setAuthor(request.getAuthor())
                    .setIsbn(request.getIsbn())
                    .setPublicationYear(request.getPublicationYear())
                    .build();

            books.put(bookId, book);

            BookServiceProto.BookResponse response = BookServiceProto.BookResponse.newBuilder()
                    .setMessage("Book added successfully")
                    .setSuccess(true)
                    .setBook(book)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteBook(BookServiceProto.DeleteBookRequest request,
                           StreamObserver<BookServiceProto.DeleteBookResponse> responseObserver) {
        try {
            String bookId = request.getId();

            if (books.containsKey(bookId)) {
                books.remove(bookId);

                BookServiceProto.DeleteBookResponse response = BookServiceProto.DeleteBookResponse.newBuilder()
                        .setMessage("Book deleted successfully")
                        .setSuccess(true)
                        .build();

                responseObserver.onNext(response);
            } else {
                BookServiceProto.DeleteBookResponse response = BookServiceProto.DeleteBookResponse.newBuilder()
                        .setMessage("Book not found")
                        .setSuccess(false)
                        .build();

                responseObserver.onNext(response);
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getBook(BookServiceProto.GetBookRequest request,
                        StreamObserver<BookServiceProto.BookResponse> responseObserver) {
        try {
            String bookId = request.getId();
            BookServiceProto.Book book = books.get(bookId);

            if (book != null) {
                BookServiceProto.BookResponse response = BookServiceProto.BookResponse.newBuilder()
                        .setMessage("Book found")
                        .setSuccess(true)
                        .setBook(book)
                        .build();

                responseObserver.onNext(response);
            } else {
                BookServiceProto.BookResponse response = BookServiceProto.BookResponse.newBuilder()
                        .setMessage("Book not found")
                        .setSuccess(false)
                        .build();

                responseObserver.onNext(response);
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void listBooks(BookServiceProto.ListBooksRequest request,
                          StreamObserver<BookServiceProto.ListBooksResponse> responseObserver) {
        try {
            BookServiceProto.ListBooksResponse.Builder responseBuilder =
                    BookServiceProto.ListBooksResponse.newBuilder();

            for (BookServiceProto.Book book : books.values()) {
                responseBuilder.addBooks(book);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateBook(BookServiceProto.UpdateBookRequest request,
                           StreamObserver<BookServiceProto.BookResponse> responseObserver) {
        try {
            String bookId = request.getId();

            if (books.containsKey(bookId)) {
                BookServiceProto.Book updatedBook = BookServiceProto.Book.newBuilder()
                        .setId(bookId)
                        .setTitle(request.getTitle())
                        .setAuthor(request.getAuthor())
                        .setIsbn(request.getIsbn())
                        .setPublicationYear(request.getPublicationYear())
                        .build();

                books.put(bookId, updatedBook);

                BookServiceProto.BookResponse response = BookServiceProto.BookResponse.newBuilder()
                        .setMessage("Book updated successfully")
                        .setSuccess(true)
                        .setBook(updatedBook)
                        .build();

                responseObserver.onNext(response);
            } else {
                BookServiceProto.BookResponse response = BookServiceProto.BookResponse.newBuilder()
                        .setMessage("Book not found")
                        .setSuccess(false)
                        .build();

                responseObserver.onNext(response);
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}