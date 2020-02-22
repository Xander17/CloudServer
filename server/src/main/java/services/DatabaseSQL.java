package services;

import resources.ServerSettings;
import services.settings.Settings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSQL {

    private static DatabaseSQL instance = null;

    private static Connection connection;
    private static Statement statement;

    private String connectionString;
    private String username;
    private String pass;

    private DatabaseSQL() {
        String host = Settings.get(ServerSettings.DATABASE_HOST);
        String port = Settings.get(ServerSettings.DATABASE_PORT);
        String name = Settings.get(ServerSettings.DATABASE_NAME);
        String settings = Settings.get(ServerSettings.DATABASE_SETTINGS_STRING);
        username = Settings.get(ServerSettings.DATABASE_USERNAME);
        pass = Settings.get(ServerSettings.DATABASE_PASS);
        connectionString = String.format("jdbc:mysql://%s:%s/%s?%s", host, port, name, settings);
    }

    public static DatabaseSQL getInstance() {
        if (instance == null) instance = new DatabaseSQL();
        return instance;
    }

    public void connect() {
        try {
            connection = DriverManager.getConnection(connectionString, username, pass);
            statement = connection.createStatement();
            checkTablesExist();
            LogService.SERVER.info("Clients DB connected.");
        } catch (SQLException e) {
            LogService.SERVER.error("Clients DB connection failed", e.toString());
        }
    }

    private void checkTablesExist() throws SQLException {
        statement.execute("CREATE TABLE IF NOT EXISTS `cloud_server`.`users` (" +
                "`id` INT NOT NULL AUTO_INCREMENT," +
                "`login` VARCHAR(45) NOT NULL," +
                "`password` VARCHAR(45) NOT NULL," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE INDEX `login_UNIQUE` (`login` ASC) VISIBLE)" +
                "DEFAULT CHARACTER SET = utf8mb4;");
    }

    public void shutdown() {
        try {
            connection.close();
            LogService.SERVER.info("Clients DB stopped.");
        } catch (SQLException e) {
            LogService.SERVER.error("DB", e.toString());
        }
    }

    public Statement getStatement() {
        return statement;
    }
}
