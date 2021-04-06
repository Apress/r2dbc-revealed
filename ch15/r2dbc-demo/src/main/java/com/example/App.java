package com.example;

import java.time.Duration;

import org.mariadb.r2dbc.api.MariadbConnection;
import org.mariadb.r2dbc.api.MariadbStatement;
import org.reactivestreams.Publisher;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import reactor.core.publisher.Mono;

/*
    This class contains samples that demonstrate the examples from Chapter 13. 
    To use, simply uncomment the target block of code, complile and run the application. 
*/
public class App 
{
    public static void main( String[] args ) throws InterruptedException {
        
        /* Initialize connection to be used in all subsequent methods */
        Connection connection = obtainConnection();

        selectData(connection);
   
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

    static Connection obtainConnection() {
        ConnectionFactory connectionFactory;
        ConnectionPool connectionPool = null;

        try {
            // Discover Connection Factory using ConnectionFactoryOptions
            connectionFactory = getConnectionFactory();

            // Configure connection pool
            ConnectionPoolConfiguration configuration = ConnectionPoolConfiguration.builder(connectionFactory)
            .maxIdleTime(Duration.ofMillis(1000))
            .maxSize(5)
            .build();
            
            // Create a new connection pool
            connectionPool = new ConnectionPool(configuration);

            return connectionPool.create().block();
        }
        catch (java.lang.IllegalArgumentException e) {
           printException("Issue encountered while attempting to obtain a connection", e);
           throw e;
        }
        finally {
            if (connectionPool != null) {
                connectionPool.close();
            }
        }
    }

    //public static MariadbConnectionFactory getConnectionFactory() {
    public static ConnectionFactory getConnectionFactory() {
        try{
            ConnectionFactoryOptions connectionFactoryOptions = ConnectionFactoryOptions.builder()
                                                                    .option(ConnectionFactoryOptions.DRIVER, "pool")
                                                                    .option(ConnectionFactoryOptions.PROTOCOL, "mariadb")
                                                                    .option(ConnectionFactoryOptions.HOST, "127.0.0.1")
                                                                    .option(ConnectionFactoryOptions.PORT, 3306)
                                                                    .option(ConnectionFactoryOptions.USER, "app_user")
                                                                    .option(ConnectionFactoryOptions.PASSWORD, "Password123!")
                                                                    .option(ConnectionFactoryOptions.DATABASE, "todo")
                                                                    .build();

            ConnectionFactory connectionFactory = ConnectionFactories.get(connectionFactoryOptions);

            return connectionFactory;
        }
        catch(Exception e) {
            printException("Unable to discover MariadbConnectionFactory using ConnectionFactoryOptions", e);
            throw e;
        }
    }

    static void selectData(Connection connection) {
        try{
            MariadbStatement selectStatement = (MariadbStatement)connection.createStatement("SELECT * FROM tasks");
            selectStatement.execute()
                          .flatMap(result -> result.map((row,metadata) -> {
                            return String.format("ID: %s - Description: %s - Completed: %s",
                                row.get("id", Integer.class),
                                row.get("description", String.class),
                                (row.get(2, Boolean.class) == true) ? "Yes" : "No"
                            );
                          }))
                          .subscribe(result -> print(result));
        }
        catch (Exception e) {
            printException("Issue encountered in selectData method", e);
            throw e;
        } 
    }

    static void closeConnection(Connection connection) {
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
