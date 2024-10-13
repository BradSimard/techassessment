package tech.simard.thinkon.db;

import java.sql.*;
import java.util.Optional;
import java.util.Properties;

public class DBConnection {
    Connection conn;

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

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        // Create a new prepared statement
        return this.conn.prepareStatement(sql);
    }

    public ResultSet query(PreparedStatement query) throws SQLException {
        // Execute the query and retrieve the result set
        return query.executeQuery();
    }
}
