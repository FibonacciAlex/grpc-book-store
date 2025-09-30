# Book Store gRPC Server and Client

## Introduction
This project is a Java-based gRPC service that exposes CRUD operations for managing books. The server keeps data in-memory, making it ideal for experimenting with gRPC patterns, request/response handling, and client-server interaction without external infrastructure.

## Features
- Add new books with title, author, ISBN, and publication year
- Fetch a single book by its identifier
- List all stored books
- Update existing book details
- Delete books from the catalog
- Interactive CLI client for exercising the RPC methods

## Project Structure
```
micro-test/
|-- pom.xml
|-- README.md
`-- src/
    `-- main/
        |-- java/
        |   `-- com/example/
        |       |-- bookservice/
        |       |   |-- Book.java
        |       |   |-- BookServiceImpl.java
        |       |   `-- BookServiceServer.java
        |       `-- BookServiceClient/
        |           `-- BookServiceClient.java
        |-- proto/
        |   `-- book_service.proto
        `-- resources/
```

## Prerequisites
- Java 11 or newer on your PATH
- Maven 3.6 or newer
- (Optional) Two terminals if you want to run server and client simultaneously

## Compile and Run
1. Generate sources and compile everything:
   ```bash
   mvn clean compile
   ```
2. Start the gRPC server (port 8980) from the project root:
   ```bash
   mvn -Dexec.mainClass=com.example.bookservice.BookServiceServer -Dexec.classpathScope=runtime org.codehaus.mojo:exec-maven-plugin:3.1.0:java
   ```
3. In a second terminal, launch the interactive client:
   ```bash
   mvn -Dexec.mainClass=com.example.BookServiceClient.BookServiceClient -Dexec.classpathScope=runtime org.codehaus.mojo:exec-maven-plugin:3.1.0:java
   ```
4. Stop either process with Ctrl+C when finished.


