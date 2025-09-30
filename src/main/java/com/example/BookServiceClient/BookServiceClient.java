package com.example.BookServiceClient;

import com.example.BookServiceGrpc;
import com.example.BookServiceProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class BookServiceClient {
    private final ManagedChannel channel;
    private final BookServiceGrpc.BookServiceBlockingStub blockingStub;

    public BookServiceClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = BookServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void addBook(String title, String author, String isbn, int publicationYear) {
        try {
            BookServiceProto.AddBookRequest request = BookServiceProto.AddBookRequest.newBuilder()
                    .setTitle(title)
                    .setAuthor(author)
                    .setIsbn(isbn)
                    .setPublicationYear(publicationYear)
                    .build();

            BookServiceProto.BookResponse response = blockingStub.addBook(request);

            if (response.getSuccess()) {
                System.out.println(" " + response.getMessage());
                System.out.println(" Book ID: " + response.getBook().getId());
            } else {
                System.out.println(" " + response.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error adding book: " + e.getMessage());
        }
    }

    public void deleteBook(String bookId) {
        try {
            BookServiceProto.DeleteBookRequest request = BookServiceProto.DeleteBookRequest.newBuilder()
                    .setId(bookId)
                    .build();

            BookServiceProto.DeleteBookResponse response = blockingStub.deleteBook(request);

            if (response.getSuccess()) {
                System.out.println(" " + response.getMessage());
            } else {
                System.out.println(" " + response.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error deleting book: " + e.getMessage());
        }
    }

    public void getBook(String bookId) {
        try {
            BookServiceProto.GetBookRequest request = BookServiceProto.GetBookRequest.newBuilder()
                    .setId(bookId)
                    .build();

            BookServiceProto.BookResponse response = blockingStub.getBook(request);

            if (response.getSuccess()) {
                System.out.println(" " + response.getMessage());
                BookServiceProto.Book book = response.getBook();
                System.out.print("Book Details:");
                System.out.println("   ID: " + book.getId());
                System.out.println("   Title: " + book.getTitle());
                System.out.println("   Author: " + book.getAuthor());
                System.out.println("   ISBN: " + book.getIsbn());
                System.out.println("   Year: " + book.getPublicationYear());
            } else {
                System.out.println(" " + response.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error getting book: " + e.getMessage());
        }
    }

    public void listBooks() {
        try {
            BookServiceProto.ListBooksRequest request = BookServiceProto.ListBooksRequest.newBuilder().build();
            BookServiceProto.ListBooksResponse response = blockingStub.listBooks(request);

            System.out.println(" Book List:");
            if (response.getBooksList().isEmpty()) {
                System.out.println("   No books available");
            } else {
                for (BookServiceProto.Book book : response.getBooksList()) {
                    System.out.println("    " + book.getTitle() + " by " + book.getAuthor() + " (ID: " + book.getId() + ")");
                }
            }

        } catch (Exception e) {
            System.err.println("Error listing books: " + e.getMessage());
        }
    }

    public void updateBook(String bookId, String title, String author, String isbn, int publicationYear) {
        try {
            BookServiceProto.UpdateBookRequest request = BookServiceProto.UpdateBookRequest.newBuilder()
                    .setId(bookId)
                    .setTitle(title)
                    .setAuthor(author)
                    .setIsbn(isbn)
                    .setPublicationYear(publicationYear)
                    .build();

            BookServiceProto.BookResponse response = blockingStub.updateBook(request);

            if (response.getSuccess()) {
                System.out.println(" " + response.getMessage());
            } else {
                System.out.println(" " + response.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error updating book: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        BookServiceClient client = new BookServiceClient("localhost", 8980);
        Scanner scanner = new Scanner(System.in);

        try {
            while (true) {
                System.out.println("\n=== Book Management System ===");
                System.out.println("1. Add Book");
                System.out.println("2. Delete Book");
                System.out.println("3. Get Book");
                System.out.println("4. List Books");
                System.out.println("5. Update Book");
                System.out.println("6. Exit");
                System.out.print("Choose an option: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                switch (choice) {
                    case 1:
                        System.out.print("Enter title: ");
                        String title = scanner.nextLine();
                        System.out.print("Enter author: ");
                        String author = scanner.nextLine();
                        System.out.print("Enter ISBN: ");
                        String isbn = scanner.nextLine();
                        System.out.print("Enter publication year: ");
                        int year = scanner.nextInt();
                        client.addBook(title, author, isbn, year);
                        break;

                    case 2:
                        System.out.print("Enter book ID to delete: ");
                        String deleteId = scanner.nextLine();
                        client.deleteBook(deleteId);
                        break;

                    case 3:
                        System.out.print("Enter book ID to get: ");
                        String getBookId = scanner.nextLine();
                        client.getBook(getBookId);
                        break;

                    case 4:
                        client.listBooks();
                        break;

                    case 5:
                        System.out.print("Enter book ID to update: ");
                        String updateId = scanner.nextLine();
                        System.out.print("Enter new title: ");
                        String newTitle = scanner.nextLine();
                        System.out.print("Enter new author: ");
                        String newAuthor = scanner.nextLine();
                        System.out.print("Enter new ISBN: ");
                        String newIsbn = scanner.nextLine();
                        System.out.print("Enter new publication year: ");
                        int newYear = scanner.nextInt();
                        client.updateBook(updateId, newTitle, newAuthor, newIsbn, newYear);
                        break;

                    case 6:
                        System.out.println("Exiting...");
                        return;

                    default:
                        System.out.println("Invalid option!");
                }
            }
        } finally {
            client.shutdown();
            scanner.close();
        }
    }
}