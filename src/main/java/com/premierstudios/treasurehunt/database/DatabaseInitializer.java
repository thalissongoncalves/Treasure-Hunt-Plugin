package com.premierstudios.treasurehunt.database;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Initializes the database tables on first run.
 * Creates the treasures and treasure_completed tables if they don't exist.
 */
public class DatabaseInitializer {

    private static final String CREATE_TREASURES_TABLE = """
            CREATE TABLE IF NOT EXISTS treasures (
              id VARCHAR(50) PRIMARY KEY,
              command VARCHAR(255) NOT NULL,
              world VARCHAR(50) NOT NULL,
              x INT NOT NULL,
              y INT NOT NULL,
              z INT NOT NULL,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;

    private static final String CREATE_COMPLETED_TABLE = """
            CREATE TABLE IF NOT EXISTS treasure_completed (
              treasure_id VARCHAR(50) NOT NULL,
              player_uuid CHAR(36) NOT NULL,
              completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (treasure_id, player_uuid),
              FOREIGN KEY (treasure_id) REFERENCES treasures(id) ON DELETE CASCADE
            );
            """;

    /**
     * Initializes the database by creating necessary tables.
     *
     * @param dataSource the HikariCP data source
     * @return true if initialization was successful, false otherwise
     */
    public static boolean initialize(HikariDataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(CREATE_TREASURES_TABLE);
            statement.execute(CREATE_COMPLETED_TABLE);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
