package com.example.bookservice;

import com.example.BookServiceGrpc;
import com.example.BookServiceProto;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BookServiceImpl extends BookServiceGrpc.BookServiceImplBase {

    private static final long LOCK_WAIT_TIMEOUT_MS = 100;
    private static final int LOCK_MAX_ATTEMPTS = 50;
    

    private final ConcurrentHashMap<String, BookEntry> books = new ConcurrentHashMap<>();
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

            books.put(bookId, new BookEntry(book));

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
            BookEntry entry = books.get(bookId);

            if (entry == null) {
                BookServiceProto.DeleteBookResponse response = BookServiceProto.DeleteBookResponse.newBuilder()
                        .setMessage("Book not found")
                        .setSuccess(false)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Lock writeLock = entry.writeLock();
            boolean acquired = false;
            try {
                acquired = acquireWithRetry(writeLock);
                if (!acquired) {
                    BookServiceProto.DeleteBookResponse response = BookServiceProto.DeleteBookResponse.newBuilder()
                            .setMessage("Book is currently being modified, please retry later")
                            .setSuccess(false)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }

                entry.clear();
                books.remove(bookId, entry);

                BookServiceProto.DeleteBookResponse response = BookServiceProto.DeleteBookResponse.newBuilder()
                        .setMessage("Book deleted successfully")
                        .setSuccess(true)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } finally {
                if (acquired) {
                    writeLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseObserver.onError(Status.CANCELLED
                    .withDescription("Interrupted while waiting to delete book")
                    .withCause(e)
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getBook(BookServiceProto.GetBookRequest request,
                        StreamObserver<BookServiceProto.BookResponse> responseObserver) {
        try {
            String bookId = request.getId();
            BookEntry entry = books.get(bookId);

            if (entry == null) {
                BookServiceProto.BookResponse response = BookServiceProto.BookResponse.newBuilder()
                        .setMessage("Book not found")
                        .setSuccess(false)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Lock readLock = entry.readLock();
            boolean acquired = false;
            try {
                acquired = acquireWithRetry(readLock);
                if (!acquired) {
                    BookServiceProto.BookResponse response = BookServiceProto.BookResponse.newBuilder()
                            .setMessage("Book is currently being modified, please retry later")
                            .setSuccess(false)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }

                BookServiceProto.Book book = entry.snapshot();
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
            } finally {
                if (acquired) {
                    readLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseObserver.onError(Status.CANCELLED
                    .withDescription("Interrupted while waiting to read book")
                    .withCause(e)
                    .asRuntimeException());
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

            for (Map.Entry<String, BookEntry> entry : books.entrySet()) {
                BookEntry bookEntry = entry.getValue();
                Lock readLock = bookEntry.readLock();
                boolean acquired = false;
                try {
                    acquired = readLock.tryLock(LOCK_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    if (!acquired) {
                        continue;
                    }

                    BookServiceProto.Book book = bookEntry.snapshot();
                    if (book != null) {
                        responseBuilder.addBooks(book);
                    }
                } finally {
                    if (acquired) {
                        readLock.unlock();
                    }
                }
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseObserver.onError(Status.CANCELLED
                    .withDescription("Interrupted while listing books")
                    .withCause(e)
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }


    @Override
    public void updateBook(BookServiceProto.UpdateBookRequest request,
                           StreamObserver<BookServiceProto.BookResponse> responseObserver) {
        try {
            String bookId = request.getId();
            BookEntry entry = books.get(bookId);

            if (entry == null) {
                BookServiceProto.BookResponse response = BookServiceProto.BookResponse.newBuilder()
                        .setMessage("Book not found")
                        .setSuccess(false)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Lock writeLock = entry.writeLock();
            boolean acquired = false;
            try {
                acquired = acquireWithRetry(writeLock);
                if (!acquired) {
                    BookServiceProto.BookResponse response = BookServiceProto.BookResponse.newBuilder()
                            .setMessage("Book is currently being modified, please retry later")
                            .setSuccess(false)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }

                BookServiceProto.Book updatedBook = BookServiceProto.Book.newBuilder()
                        .setId(bookId)
                        .setTitle(request.getTitle())
                        .setAuthor(request.getAuthor())
                        .setIsbn(request.getIsbn())
                        .setPublicationYear(request.getPublicationYear())
                        .build();

                entry.replace(updatedBook);

                BookServiceProto.BookResponse response = BookServiceProto.BookResponse.newBuilder()
                        .setMessage("Book updated successfully")
                        .setSuccess(true)
                        .setBook(updatedBook)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } finally {
                if (acquired) {
                    writeLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseObserver.onError(Status.CANCELLED
                    .withDescription("Interrupted while waiting to update book")
                    .withCause(e)
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }


    private boolean acquireWithRetry(Lock lock) throws InterruptedException {
        for (int attempt = 0; attempt < LOCK_MAX_ATTEMPTS; attempt++) {
            if (lock.tryLock(LOCK_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Combine book and lock
     */
    private static final class BookEntry {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private BookServiceProto.Book book;

        BookEntry(BookServiceProto.Book book) {
            this.book = book;
        }

        Lock readLock() {
            return lock.readLock();
        }

        Lock writeLock() {
            return lock.writeLock();
        }

        BookServiceProto.Book snapshot() {
            return book;
        }

        void replace(BookServiceProto.Book updated) {
            this.book = updated;
        }

        void clear() {
            this.book = null;
        }
    }
}
