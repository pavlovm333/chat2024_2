package ru.otus.chat.server;

import java.sql.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryAuthenticatedProvider implements AuthenticatedProvider {

    private class User {
        private String login;
        private String password;
        private String username;

        public User(String login, String password, String username) {
            this.login = login;
            this.password = password;
            this.username = username;
        }

        @Override
        public String toString() {
            return "User{" +
                    "login=" + login +
                    ", password='" + password + '\'' +
                    ", username='" + username + '\'' +
                    '}';
        }
    }

    private static final String DATABASE_URL = "jdbc:sqlite:chat2024_2.db";
    private static final String USERS_QUERY = "select * from users;";
    private static final String USERS_INSERT = "insert into users (login, password, name) values (?, ?, ?);";
    private static final String USERS_TO_ROLES_INSERT = "insert into users_to_roles (user_id, role_id) values ((select seq from sqlite_sequence where name = 'users'), 3);";

    private final Connection connection;

    private List<User> users;
    private Server server;

    public InMemoryAuthenticatedProvider(Server server) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection(DATABASE_URL);
        this.server = server;
        this.users = new CopyOnWriteArrayList<>();
    }

    @Override
    public void initialize() {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(USERS_QUERY)) {
                while (resultSet.next()) {
                    String login = resultSet.getString("login");
                    String password = resultSet.getString("password");
                    String name = resultSet.getString("name");
                    User user = new User(login, password, name);
                    users.add(user);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        System.out.println("Зарегистрированные пользователи");
        System.out.println(users.toString());
    }

    private String getUsernameByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.username;
            }
        }
        return null;
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        String authUsername = getUsernameByLoginAndPassword(login, password);
        if (authUsername == null) {
            clientHandler.sendMsg("Неверный логин/пароль");
            return false;
        }
        if (server.isUsernameBusy(authUsername)) {
            clientHandler.sendMsg("Указанная учетная запись уже занята");
            return false;
        }
        clientHandler.setUsername(authUsername);
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/authok " + authUsername);

        return true;
    }

    private boolean isLoginAlreadyExists(String login) {
        for (User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsernameAlreadyExists(String username) {
        for (User u : users) {
            if (u.username.equals(username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username) {
        if (login.length() < 3 || password.length() < 3 || username.length() < 3) {
            clientHandler.sendMsg("Логин 3+ символа, пароль 3+ символа, имя пользователя 3+ символа");
            return false;
        }
        if (isLoginAlreadyExists(login)) {
            clientHandler.sendMsg("Указанный логин уже занят");
            return false;
        }
        if (isUsernameAlreadyExists(username)) {
            clientHandler.sendMsg("Указанное имя пользователя уже занято");
            return false;
        }
        users.add(new User(login, password, username));

        try (PreparedStatement prStatement = connection.prepareStatement(USERS_INSERT)) {
            prStatement.setString(1, login);
            prStatement.setString(2, password);
            prStatement.setString(3, username);
            prStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(USERS_TO_ROLES_INSERT);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        clientHandler.setUsername(username);
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/regok " + username);
        return true;
    }
}
