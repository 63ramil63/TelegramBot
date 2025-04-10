package org.example.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    public static DataBaseConnection dataBaseConnection;

    //инициализация подключения к бд
    static {
        try {
            dataBaseConnection = new DataBaseConnection();
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public static boolean getUser(long chatId) {
        System.out.println("Get User");
        String sql = "select Name from users where Id=?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            //устанавливаем значения для знаков ? в запросе sql
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            System.out.println(resultSet);
            //проверяем есть ли запись
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        System.out.println("return false");
        return false;
    }

    public static String getObj(long chatId) {
        String sql = "select Obj from users where Id=?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            //устанавливаем значения для знаков ? в запросе sql
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String result = resultSet.getNString("Obj");
                if (result != null) {
                    System.out.println("User with chatId:" + chatId + " got from database: obj=" + result);
                    return result;
                }
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return "Not found";
    }

    public static String getFilePath(long chatId) {
        String sql = "select FilePath from users where Id=?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            //устанавливаем значения для знаков ? в запросе sql
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String result = resultSet.getNString("FilePath");
                if (result != null) {
                    return result;
                }
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return "Not found";
    }

    public static String getUserFullName(long chatId) {
        String sql = "select FullName from users where Id=?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getNString("FullName");
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return "Not found";
    }

    public static boolean getCanAddFolder(long chatId) {
        String sql = "select CanAddFolder from users where Id=?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            //устанавливаем значения для знаков ? в запросе sql
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int result = resultSet.getInt("CanAddFolder");
                //если получаем 1, то возвращаем true, если же 0, то код вернет false в конце
                if (result == 1) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return false;
    }

    public static void addUser(long chatId) {
        System.out.println("add user");
        String sql = "insert into users (Id) values (?)";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            //устанавливаем значения для знаков ? в запросе sql
            preparedStatement.setLong(1, chatId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                System.out.println("Не найдено пользователя с Id: " + chatId);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setObj(long chatId, String obj) {
        String sql = "update users set Obj=? where Id=?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, obj);
            preparedStatement.setLong(2, chatId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                System.out.println("Не найдено пользователя с Id: " + chatId);
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public static void setFilePath(long chatId, String filePath) {
        String sql = "update users set FilePath=? where Id=?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, filePath);
            preparedStatement.setLong(1, chatId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                System.out.println("Не найдено пользователя с Id: " + chatId);
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public static void setCanAddFolder(long chatId, byte bool) {
        String sql = "update users set CanAddFolder = ? where Id =?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            //bool имеет значение 0 или 1
            preparedStatement.setByte(1, bool);
            preparedStatement.setString(2, String.valueOf(chatId));
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                System.out.println("Не найдено пользователей с Id: " + chatId);
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public static void setUserFullName(long chatId, String fullName) {
        String sql = "update users set FullName=? where Id=?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, fullName);
            preparedStatement.setLong(2, chatId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                System.out.println("Не найдено пользователя с Id: " + chatId);
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public static void setUserName(long chatId, String userName) {
        String sql = "update users set name=? where Id=?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, userName);
            preparedStatement.setString(2, String.valueOf(chatId));
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                System.out.println("Не найдено пользователей с Id: " + chatId);
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }
}
