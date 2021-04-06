package com.example;

import java.sql.SQLException;
import org.mariadb.r2dbc.MariadbConnectionConfiguration;
import org.mariadb.r2dbc.MariadbConnectionFactory;
import org.mariadb.r2dbc.api.MariadbConnection;
import org.mariadb.r2dbc.api.MariadbStatement;
import org.reactivestreams.Publisher;

import io.r2dbc.spi.IsolationLevel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/*
    This class contains samples that demonstrate the examples from Chapter 14. 
    To use, simply uncomment the target block of code, complile and run the application. 
*/
public class App 
{
    public static void main( String[] args ) throws InterruptedException {
        /* Initialize connection to be used in all subsequent methods */
        MariadbConnection connection = obtainConnection();

        /* 
            Based on the snippet in Listing 14-1
            Optional: Execute the following to wipe and reset your underlying MariaDB 'todo' database.
        */
        resetTaskTable(connection);

        /* 
            Getting and setting aonnection auto-commit - Listing 14-3 
        */
        //boolean isAutoCommit = connection.isAutoCommit();
        //connection.setAutoCommit(false).block();

        /* 
            Imperative (blocking/step-by-step) transaction commit example based on Listings 14-4 and 14-5    
        */
        //commitTransactionSample(connection);

        /* 
            Imperative (blocking/step-by-step) commit and rollback transaction example based on Listings 14-7   
        */
        //rollbackTransactionSample(connection);

        /* 
            Imperative (blocking/step-by-step) commit and rollback transaction example based on Listings 14-9   
        */
        //fullTransactionImperativeSample(connection, true);

        /* 
            Declarative commit and rollback transaction example based on Listings 14-9   
        */
        //fullTransactionDeclarativeSample(connection);

        /* 
            Managing Savepoints - Listings 14-12 and 14-13
        */
        //savePointSample(connection);

        /*
            Getting and setting transaction isolation leve - Listings 14-18 and 14-19
        */
        //IsolationLevel isolationLevel = connection.getTransactionIsolationLevel();
        //connection.setTransactionIsolationLevel(IsolationLevel.READ_COMMITTED)
   
        closeConnection(connection);

        // Keep the current thread running to allow time for publishers and subscribers to complete processing
        Thread.currentThread().join();
    }

    static void resetTaskTable(MariadbConnection connection) {
        try {
            MariadbStatement multiStatement = connection.createStatement("TRUNCATE TABLE tasks; INSERT INTO tasks (description) VALUES ('Task A'), ('Task B'), ('Task C');");
            multiStatement.execute().subscribe(result -> print("Task table reset"));
        }
        catch(Exception e) {
            printException("Issue encountered in resetTaskTable method", e);
            throw e;
        }
    }

    static void commitTransactionSample(MariadbConnection connection) {
        try {
            connection.beginTransaction().block();

            MariadbStatement multiStatement = connection.createStatement("INSERT INTO tasks (description) VALUES ('New Task X');SELECT * FROM tasks");

            multiStatement.execute()
                        .flatMap(result -> result.map((row,metadata) -> String.format("%s - %s", row.get("id"), row.get("description"))))
                        .subscribe(result -> print(result));

            connection.commitTransaction().block();

            MariadbStatement selectStatement = connection.createStatement("SELECT * FROM tasks");

            selectStatement.execute()
                        .flatMap(result -> result.map((row,metadata) -> String.format("%s - %s", row.get("id"), row.get("description"))))
                        .subscribe(result -> System.out.println(result));
        }
        catch(Exception e) {
            printException("Issue encountered in commitTransaction method", e);
            throw e;
        }
    }

    static void rollbackTransactionSample(MariadbConnection connection) {
        try {

            connection.beginTransaction().block();

            MariadbStatement multiStatement = connection.createStatement("DELETE FROM tasks WHERE description = 'New Task X';SELECT * FROM tasks");

            multiStatement.execute()
                        .flatMap(result -> result.map((row,metadata) -> String.format("%s - %s", row.get("id"), row.get("description"))))
                        .subscribe(result -> print(result));

            connection.rollbackTransaction().block();

            MariadbStatement selectStatement = connection.createStatement("SELECT * FROM tasks");

            selectStatement.execute()
                        .flatMap(result -> result.map((row,metadata) -> String.format("%s - %s", row.get("id"), row.get("description"))))
                        .subscribe(result -> System.out.println(result));
        }
        catch(Exception e) {
            printException("Issue encountered in commitTransaction", e);
            throw e;
        }
    }

    static void fullTransactionImperativeSample(MariadbConnection connection, Boolean simulateException) {
        try {
            connection.beginTransaction().block();

            MariadbStatement multiStatement = connection.createStatement("DELETE FROM tasks;INSERT INTO tasks (description) VALUES ('New Task D');SELECT * FROM tasks");

            multiStatement.execute()
                        .flatMap(result -> result.map((row,metadata) -> String.format("%s - %s", row.get("id"), row.get("description"))))
                        .subscribe(result -> print(result));

            if (simulateException) {
                throw new SQLException("Simulated SQL Exception");
            }

            connection.commitTransaction().block();
        }
        catch(SQLException e) {
            connection.rollbackTransaction().block();
            printException("Issue encountered in fullTransactionSample", e);
        }
    }

    static void fullTransactionDeclarativeSample(MariadbConnection connection) {
        try {
            MariadbStatement multiStatement = connection.createStatement("DELETE FROM tasks;INSERT INTO tasks (description) VALUES ('New Task D');SELECT * FROM tasks");

            connection.beginTransaction()
                      .then(multiStatement.execute()
                                          .flatMap(result -> result.map((row,metadata) -> String.format("%s - %s", row.get("id"), row.get("description"))))
                                          .then(connection.commitTransaction()))
                      .subscribe();
        }
        catch(Exception e) {
            connection.rollbackTransaction().subscribe();
            printException("Issue encountered in fullTransactionSample", e);
        }
    }

    static void savePointSample(MariadbConnection connection) {
        try {
            connection.beginTransaction().block();

            Boolean rollbackToSavepoint = false;

            MariadbStatement insertStatement = connection.createStatement("INSERT INTO tasks (description) VALUES ('TASK X')");
            MariadbStatement deleteStatement = connection.createStatement("DELETE FROM tasks WHERE id = 2");

            insertStatement.execute().then(
                connection.createSavepoint("savepoint_1").then(
                    deleteStatement.execute()
                                   .then(rollBackOrCommit(connection,rollbackToSavepoint))
                )
            ).subscribe();
        }
        catch (Exception e) {
            printException("Issue encountered in savePointSample method", e);
            throw e;
        }
    }

    static Mono<Void> rollBackOrCommit(MariadbConnection connection, Boolean rollback) {
        if (rollback) {
            return connection.rollbackTransactionToSavepoint("savepoint_1").then(connection.commitTransaction());
        }
        else {
            return connection.commitTransaction();
        }
    }

    static MariadbConnection obtainConnection() {
        try {
            MariadbConnectionFactory connectionFactory;

            // Discover Connection Factory using ConnectionFactoryOptions
            connectionFactory = createConnectionFactory();

            // Create a MariadbConnection
            return connectionFactory.create().block();
        }
        catch (java.lang.IllegalArgumentException e) {
           printException("Issue encountered while attempting to obtain a connection", e);
           throw e;
        }
    }

    static MariadbConnectionFactory createConnectionFactory() {
        try{
            // Configure the Connection
            MariadbConnectionConfiguration connectionConfiguration = MariadbConnectionConfiguration.builder()
                                                                        .host("127.0.0.1")
                                                                        .port(3306)
                                                                        .username("app_user")
                                                                        .password("Password123!")
                                                                        .database("todo")
                                                                        .allowMultiQueries(true)
                                                                        .build();

            // Instantiate a Connection Factory
            MariadbConnectionFactory connectionFactory = new MariadbConnectionFactory(connectionConfiguration);
            return connectionFactory;
        }
        catch(Exception e) {
            printException("Unable to create a new MariadbConnectionFactory", e);
            throw e;
        }
    }

    static void closeConnection(MariadbConnection connection) {
        try {
            Publisher<Void> closePublisher = connection.close();
            Mono.from(closePublisher).block();
        }
        catch (java.lang.IllegalArgumentException e) {
           printException("Issue encountered while attempting to verify a connection", e);
        }
    }

    static void printException(String description, Exception e) {
        print(description);
        e.printStackTrace();
    }

    static void print(String val) {
        System.out.println(val);
    }
}
