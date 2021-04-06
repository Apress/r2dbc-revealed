package com.example;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mariadb.r2dbc.MariadbConnectionConfiguration;
import org.mariadb.r2dbc.MariadbConnectionFactory;
import org.mariadb.r2dbc.api.MariadbBatch;
import org.mariadb.r2dbc.api.MariadbConnection;
import org.mariadb.r2dbc.api.MariadbStatement;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import reactor.core.publisher.Mono;

public class App 
{
    public static void main( String[] args ) throws InterruptedException {
        /* Initialize connection to be used in all subsequent methods */
        /* Includes `allowMultiQueries` from Listing 13-11 */
        MariadbConnection connection = obtainConnection();

        /*
        Initial examples for Listing 13-1, Listing 13-2, and Listing 13-3
        Blocking example from Listing 13-16
        */
        //createSchema(connection);

        /* Listing 13-8, Listing 13-9 */
        //insertData(connection);

        /* Listing 13-5 */
        //updateData(connection);

        /* Listing 13-6 */
        //selectData(connection);

        /* Listing 13-7 */
        //selectMetadata(connection);

        /* Listing 13-10 */
        //batchData(connection);

        /* Listing 13-12 */
        //multipleStatements(connection);

        /* Listing 13-13 */
        //selectIndexedParameterizedData(connection);

        /* Listing 13-14 */
        //selectNamedParameterizedData(connection);

        /* Listing 13-15 */
        //batchedParameterizedData(connection);

        /* Listing 13-18 */
        //customSubscriberSelect(connection);

        /* Removes MariaDB database schema */
        //dropDatabase(connection);

        // Close Connection 
        closeConnection(connection);

        // Keep the current thread running to allow time for publishers and subscribers to complete processing
        Thread.currentThread().join();
    }

    static void createSchema(MariadbConnection connection) {
        try{
            // Create a new database
            MariadbStatement createDatabaseStatement = connection.createStatement("CREATE DATABASE todo");
            createDatabaseStatement.execute().blockFirst();

            // Create a new table 
            MariadbStatement createTableStatement = connection.createStatement("CREATE TABLE todo.tasks (" +
                "id INT(11) unsigned NOT NULL AUTO_INCREMENT, " +
                "description VARCHAR(500) NOT NULL, " + 
                "completed BOOLEAN NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (id))"
            );
            createTableStatement.execute().blockLast();
        }
        catch (Exception e) {
            printException("Issue encountered in createSchema method", e);
            throw e;
        }
    }

    static void insertData(MariadbConnection connection) {
        try{
            MariadbStatement insertStatement = connection.createStatement("INSERT INTO todo.tasks (description,completed) VALUES ('Task 1',0)");
            insertStatement.execute().subscribe(result -> System.out.print(result.getRowsUpdated().subscribe(count -> print(count.toString()))));
        }
        catch (Exception e) {
            printException("Issue encountered in insertData method", e);
            throw e;
        } 
    }

    static void updateData(MariadbConnection connection) {
        try{
            MariadbStatement updateStatement = connection.createStatement("UPDATE todo.tasks SET completed = 1 where id < 5");    
            updateStatement.execute().subscribe(result -> result.getRowsUpdated().subscribe(count -> print(count.toString())));
        }
        catch (Exception e) {
            printException("Issue encountered in updateData method", e);
            throw e;
        } 
    }

    static void selectData(MariadbConnection connection) {
        try{
            MariadbStatement selectStatement = connection.createStatement("SELECT id, description as task, completed FROM todo.tasks WHERE completed = 1");
            selectStatement.execute()
                          .flatMap(result -> result.map((row,metadata) -> {
                            Integer id = row.get(0, Integer.class);
                            String descriptionFromAlias = row.get("task", String.class);
                            String isCompleted = (row.get(2, Boolean.class) == true) ? "Yes" : "No";
                            return String.format("ID: %s - Description: %s - Completed: %s",
                                id,
                                descriptionFromAlias,
                                isCompleted
                            );
                          }))
                          .subscribe(result -> print(result));
        }
        catch (Exception e) {
            printException("Issue encountered in selectData method", e);
            throw e;
        } 
    }

    static void selectMetadata(MariadbConnection connection) {
        try{
            MariadbStatement selectStatement = connection.createStatement("SELECT id, description as task, completed FROM todo.tasks WHERE completed = 1");
            selectStatement.execute()
                          .flatMap(result -> result.map((row,metadata) -> {
                            List<String> columnMetadata = new ArrayList<String>();
                            Iterator<String> iterator = metadata.getColumnNames().iterator();
                            while (iterator.hasNext()) {
                                String columnName = iterator.next();
                                columnMetadata.add(String.format("%s (type=%s)", columnName, metadata.getColumnMetadata(columnName).getJavaType().getName()));
                            }
                            return columnMetadata.toString();
                          }))
                          .subscribe(result -> print("Row Columns = "  + result));
        }
        catch (Exception e) {
            printException("Issue encountered in selectMetadata method", e);
            throw e;
        } 
    }

    static void batchData(MariadbConnection connection) {
        try{
            MariadbBatch batch = connection.createBatch();

            batch.add("INSERT INTO todo.tasks (description,completed) VALUES ('A New Task', 0)")
                 .add("SELECT * FROM todo.tasks WHERE id = last_insert_id()")
                 .execute()
                 .flatMap(result -> result.map((row,metadata) -> {
                    return row.get(0, Integer.class)  + " - " + row.get(1, String.class);
                  }))
                 .subscribe(result -> print(result));

        }
        catch (Exception e) {
            printException("Issue encountered in batchData method", e);
            throw e;
        } 
    }

    static void multipleStatements(MariadbConnection connection) {
        try {
            MariadbStatement multiStatement = connection.createStatement("INSERT INTO todo.tasks (description,completed) VALUES ('A New Task', 0); SELECT * FROM todo.tasks WHERE id = last_insert_id();");

            multiStatement.execute()
                          .flatMap(result -> result.map((row,metadata) -> {
                            return row.get(0, Integer.class)  + " - " + row.get(1, String.class);
                          }))
                          .subscribe(result -> print(result));
        }
        catch (Exception e) {
            printException("Issue encountered in multipleStatements method", e);
            throw e;
        } 
    }

    static void selectIndexedParameterizedData(MariadbConnection connection) {
        try{
            MariadbStatement selectStatement = connection.createStatement("SELECT id, description as task, completed FROM todo.tasks WHERE completed = ? AND id >= ?");
            selectStatement.bind(0, true);
            selectStatement.bind(1, 4);
            selectStatement.execute()
                          .flatMap(result -> result.map((row,metadata) -> {
                            Integer id = row.get(0, Integer.class);
                            String descriptionFromAlias = row.get("task", String.class);
                            String isCompleted = (row.get(2, Boolean.class) == true) ? "Yes" : "No";
                            return String.format("ID: %s - Description: %s - Completed: %s",
                                id,
                                descriptionFromAlias,
                                isCompleted
                            );
                          }))
                          .subscribe(result -> print(result));
        }
        catch (Exception e) {
            printException("Issue encountered in selectIndexedParameterizedData method", e);
            throw e;
        } 
    }

    static void selectNamedParameterizedData(MariadbConnection connection) {
        try{
            MariadbStatement selectStatement = connection.createStatement("SELECT id, description as task, completed FROM todo.tasks WHERE completed = :completed AND id >= :id");
            selectStatement.bind("completed", true);
            selectStatement.bind("id", 4);
            selectStatement.execute()
                          .flatMap(result -> result.map((row,metadata) -> {
                            Integer id = row.get(0, Integer.class);
                            String descriptionFromAlias = row.get("task", String.class);
                            String isCompleted = (row.get(2, Boolean.class) == true) ? "Yes" : "No";
                            return String.format("ID: %s - Description: %s - Completed: %s",
                                id,
                                descriptionFromAlias,
                                isCompleted
                            );
                          }))
                          .subscribe(result -> print(result));
        }
        catch (Exception e) {
            printException("Issue encountered in selectNamedParameterizedData method", e);
            throw e;
        } 
    }

    static void batchedParameterizedData(MariadbConnection connection) {
        try{
            MariadbStatement batchedInsertStatement = connection.createStatement("INSERT INTO todo.tasks (description,completed) VALUES (?,?)");
            batchedInsertStatement.bind(0, "New Task X").bind(1, false).add()
                                  .bind(0, "New Task Y").bind(1, false).add()
                                  .bind(0, "New Task Z").bind(1, true);
            batchedInsertStatement.execute().blockLast();
        }
        catch (Exception e) {
            printException("Issue encountered in batchedParameterizedData method", e);
            throw e;
        } 
    }

    static void customSubscriberSelect(MariadbConnection connection) {
        try{
            // Create a new database
            MariadbStatement selectStatement = connection.createStatement("SELECT * FROM todo.tasks");
            selectStatement.execute()
                           .flatMap(result -> result.map((row,metadata) -> {
                                return row.get("description", String.class);
                           }))
                           .subscribe(new Subscriber<String>() {
                                private Subscription s;
                                int onNextAmount;
                                int requestAmount = 2;
                            
                                @Override
                                public void onSubscribe(Subscription s) { 
                                    System.out.println("onSubscribe");
                                    this.s = s;
                                    System.out.println("Request (" + requestAmount + ")");
                                    s.request(requestAmount);
                                }
                            
                                @Override
                                public void onNext(String itemString) {
                                    onNextAmount++;
                                    System.out.println("onNext item received: " + itemString);
                                    if (onNextAmount % 2 == 0) {
                                        System.out.println("Request (" + requestAmount + ")");
                                        s.request(2);
                                    }
                                }
                            
                                @Override
                                public void onError(Throwable t) {
                                    System.out.println("onError");
                                }
                            
                                @Override
                                public void onComplete() {
                                    System.out.println("onComplete");
                                }
                            });	
       
        }
        catch (Exception e) {
            printException("Issue encountered in customSubscriberSelect method", e);
            throw e;
        } 
    }

    static void dropDatabase(MariadbConnection connection) {
        try{
            MariadbStatement dropDatabaseStatement = connection.createStatement("DROP DATABASE todo");
            dropDatabaseStatement.execute().blockFirst();
        }
        catch (Exception e) {
            printException("Issue encountered in dropDatabase method", e);
            throw e;
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
