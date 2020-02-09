package services;

import resources.LoginRegError;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

public class AuthService {
    private static Statement statement = DatabaseSQL.getInstance().getStatement();

    public static Integer checkLogin(String login, String pass) {
        String query = String.format("select id from users where login = lower('%s') and password='%s'", login, pass);
        ResultSet result;
        try {
            result = statement.executeQuery(query);
            if (result.next()) {
                return result.getInt(1);
            }
        } catch (SQLException e) {
            LogService.SERVER.error("AuthService", login, pass, Arrays.toString(e.getStackTrace()));
        }
        return null;
    }

    public static LoginRegError registerAndEchoMsg(String login, String pass) {
        try {
            if (isLoginExists(login)) return LoginRegError.LOGIN_EXISTS;
            String query = String.format("insert into users(login,password) values (lower('%s'),'%s')", login, pass);
            if (statement.executeUpdate(query) > 0) return null;
            else return LoginRegError.REG_ERROR;
        } catch (SQLException e) {
            LogService.SERVER.error("AuthService", login, pass, Arrays.toString(e.getStackTrace()));
            return LoginRegError.DB_ERROR;
        }
    }

    private static boolean isLoginExists(String login) throws SQLException {
        String query = String.format("select * from users where login = lower('%s')", login);
        ResultSet result = statement.executeQuery(query);
        return result.next();
    }
}
