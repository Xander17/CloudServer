package services;

import java.sql.*;

public class DatabaseSQL {

    private static DatabaseSQL instance = null;

    private static Connection connection;
    private static Statement statement;

    private final String CONNECTION_STRING = "jdbc:mysql://localhost:3306/cloud_server?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private final String USERNAME = "user";
    private final String PASSWORD = "pass";

    private DatabaseSQL() {

    }

    public static DatabaseSQL getInstance() {
        if (instance == null) instance = new DatabaseSQL();
        return instance;
    }

    public void connect() {
        try {
            connection = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSWORD);
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

    public PreparedStatement getPreparedStatement(String query) throws SQLException {
        return connection.prepareStatement(query);
    }

    public void closePreparedStatement(PreparedStatement preparedStatement) {
        try {
            if (preparedStatement != null) preparedStatement.close();
        } catch (SQLException e) {
            LogService.SERVER.error("DB", e.toString());
        }
    }
}
