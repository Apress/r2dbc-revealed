package com.example;

import org.mariadb.r2dbc.MariadbConnectionConfiguration;
import org.mariadb.r2dbc.MariadbConnectionFactory;
import org.mariadb.r2dbc.api.MariadbConnection;
import org.reactivestreams.Publisher;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ValidationDepth;
import reactor.core.publisher.Mono;

public class App 
{
    public static void main( String[] args )
    {
        // Initialize Connection
        MariadbConnection connection = obtainConnection();

        // Verify Connection 
        verifyConnection(connection);

        // Close Connection 
        closeConnection(connection);

        // Re-verify Connection (after closing the connection)
        verifyConnection(connection);
    }

    public static MariadbConnection obtainConnection() {
        try {
            MariadbConnectionFactory connectionFactory;
            // ******************************************************************************
            // ConnectionFactory options
            // ******************************************************************************

            // Option 1: Create a new Connection Factory using MariadbConnectionConfiguration
            connectionFactory = createConnectionFactory();

            // Option 2: Discover Connection Factory using ConnectionFactoryOptions
            //connectionFactory = discoverConnectionFactoryWithConfiguration();

            // Option 3: Discover Connection Factory using Url
            //connectionFactory = discoverConnectionFactoryWithUrl();
    
            // Use connectionFactory to create a new MariaDBConnection
            return connectionFactory.create().block();
        }
        catch (java.lang.IllegalArgumentException e) {
           printException("Issue encountered while attempting to obtain a connection", e);
           throw e;
        }
    }

    public static MariadbConnectionFactory createConnectionFactory() {
        try{
            // Create a new ConnectionConfiguration object
            MariadbConnectionConfiguration connectionConfiguration = MariadbConnectionConfiguration.builder()
                                                                        .host("127.0.0.1")
                                                                        .port(3306)
                                                                        .username("app_user")
                                                                        .password("Password123!")
                                                                        .build();

            // Use the ConnectionConfiguration object to create a new ConnectionFactory object
            MariadbConnectionFactory connectionFactory = new MariadbConnectionFactory(connectionConfiguration);

            return connectionFactory;
        }
        catch(Exception e) {
            printException("Unable to create a new MariadbConnectionFactory", e);
            throw e;
        }
    }

    public static MariadbConnectionFactory discoverConnectionFactoryWithConfiguration() {
        try{
            ConnectionFactoryOptions connectionFactoryOptions = ConnectionFactoryOptions.builder()
                                                                    .option(ConnectionFactoryOptions.DRIVER, "mariadb")
                                                                    .option(ConnectionFactoryOptions.PROTOCOL, "pipes")
                                                                    .option(ConnectionFactoryOptions.HOST, "127.0.0.1")
                                                                    .option(ConnectionFactoryOptions.PORT, 3306)
                                                                    .option(ConnectionFactoryOptions.USER, "app_user")
                                                                    .option(ConnectionFactoryOptions.PASSWORD, "Password123!")
                                                                    //.option(ConnectionFactoryOptions.DATABASE, "todo")
                                                                    .build();

            // Use ConnectionFactoryOptions to discover a ConnectionFactory object                                                 
            MariadbConnectionFactory connectionFactory = (MariadbConnectionFactory)ConnectionFactories.get(connectionFactoryOptions);

            return connectionFactory;
        }
        catch(Exception e) {
            printException("Unable to discover MariadbConnectionFactory using ConnectionFactoryOptions", e);
            throw e;
        }
    }

    public static MariadbConnectionFactory discoverConnectionFactoryWithUrl() {
        try {
            MariadbConnectionFactory connectionFactory = (MariadbConnectionFactory)ConnectionFactories.get("r2dbc:mariadb:pipes://app_user:Password123!@127.0.0.1:3306");
            return connectionFactory;
        }
        catch (Exception e) {
            printException("Unable to discover MariadbConnectionFactory using Url", e);
            throw e;
        }
    }

    public static void verifyConnection(MariadbConnection connection) {
        try {
            Publisher<Boolean> validatePublisher = connection.validate(ValidationDepth.LOCAL);
            Mono<Boolean> monoValidated = Mono.from(validatePublisher);
            monoValidated.subscribe(validated -> {
                if (validated) {
                    print("Connection is valid");
                }
                else {
                    print("Connection is not valid");
                }
            });
        }
        catch (Exception e) {
           printException("Issue encountered while attempting to verify a connection", e);
        }
    }

    public static void closeConnection(MariadbConnection connection) {
        try {
            Publisher<Void> closePublisher = connection.close();
            Mono<Void> monoClose = Mono.from(closePublisher);
            monoClose.subscribe();
        }
        catch (java.lang.IllegalArgumentException e) {
           printException("Issue encountered while attempting to verify a connection", e);
        }
    }

    public static void printException(String description, Exception e) {
        print(description);
        e.printStackTrace();
    }

    public static void print(String val) {
        System.out.println(val);
    }
}

