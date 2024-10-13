package tech.simard.thinkon.db;

import java.sql.*;
import java.util.Optional;
import java.util.Properties;

public class DBConnection {
    Connection conn;

    /**
     * Create a connection to the database using the provided authentication and connection details
     */
    public DBConnection() {
        // Use environment variables if they're available
        String username = Optional.ofNullable(System.getenv("DB_USER")).orElse("*YOUR USERNAME*");
        String password = Optional.ofNullable(System.getenv("DB_PASSWORD")).orElse("*YOUR PASSWORD*");
        String connectionUrl = Optional.ofNullable(System.getenv("DB_CONNECTION_URL")).orElse("*YOUR CONNECTION URL*");

        // Setup db connection auth
        Properties connConfig = new Properties();
        connConfig.setProperty("user", username);
        connConfig.setProperty("password", password);

        try {
            // Establish a connection to the database
            this.conn = DriverManager.getConnection(connectionUrl, connConfig);

            // Turn off auto commit so that we can manage the commit/rollback manually
            this.conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Close the DB connection. Will perform a transaction commit or rollback depending on the value provided for isSuccess.
     * @param isSuccess true will commit the transaction, false will roll it back
     * @throws SQLException
     */
    public void finish(boolean isSuccess) throws SQLException {
        // Commit changes to DB if successful, otherwise rollback
        if (isSuccess) {
            this.conn.commit();
        } else {
            this.conn.rollback();
        }

        // Close the connection to the DB
        this.conn.close();
    }

    /**
     * Create a prepared statement for the db connection using a given raw sql string.
     * @param sql raw sql string used to build the foundation of the prepared statement
     * @return the newly created prepared statement
     * @throws SQLException
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        // Create a new prepared statement
        return this.conn.prepareStatement(sql);
    }

    /**
     * Run the provided prepared statement and return any results the database provided.
     * @param query the prepared statement that will be ran against the db
     * @return results provided by the database. These depend on what the query requested.
     * @throws SQLException
     */
    public ResultSet query(PreparedStatement query) throws SQLException {
        // Execute the query and retrieve the result set
        return query.executeQuery();
    }
}
