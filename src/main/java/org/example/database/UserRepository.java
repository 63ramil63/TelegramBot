package org.example.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    private static final DataBaseConnection dataBaseConnection;
    private static final String tableName;
    //инициализация подключения к бд
    static {
        dataBaseConnection = DataBaseConnection.getInstance();
        tableName = dataBaseConnection.tableName;
    }

    //создаем таблицу, если таковой нет
    public static void createTable() {
        final String sql = "create table if not exists " + dataBaseConnection.tableName + " (" +
                "Id int not null," +
                "Name varchar(64) null," +
                "FilePath varchar(100) null," +
                "CanAddFolder tinyint default 0," +
                "Obj varchar(4) null," +
                "FullName varchar(64) null," +
                "primary key (Id))";
        try (Connection connection = dataBaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.executeUpdate();
            System.out.println("Created/found table " + tableName);
        } catch (SQLException e) {
            System.out.println("Failed to create table " + tableName + "\n" + e);
            throw new RuntimeException(e);
        }
    }

    public static boolean getUser(long chatId) {
        String sql = "select Name from " +  tableName + " where Id=?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            //устанавливаем значения для знаков ? в запросе sql
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            //проверяем есть ли запись
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return false;
    }

    public static String getObj(long chatId) {
        String sql = "select Obj from " + tableName + " where Id=?";
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
        String sql = "select FilePath from " + tableName + " where Id=?";
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

//    public static String getUserFullName(long chatId) {
//        String sql = "select FullName from " + tableName + " where Id=?";
//        try (Connection connection = dataBaseConnection.getConnection();
//             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
//            preparedStatement.setLong(1, chatId);
//            ResultSet resultSet = preparedStatement.executeQuery();
//            if (resultSet.next()) {
//                return resultSet.getNString("FullName");
//            }
//        } catch (SQLException e) {
//            System.out.println(e);
//            throw new RuntimeException(e);
//        }
//        return "Not found";
//    }

    public static boolean getCanAddFolder(long chatId) {
        String sql = "select CanAddFolder from " + tableName + " where Id=?";
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
        System.out.println("Add user with Id: " + chatId);
        String sql = "insert into " + tableName + " (Id) values (?)";
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
        String sql = "update " + tableName + " set Obj=? where Id=?";
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
        String sql = "update " + tableName + " set FilePath=? where Id=?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, filePath);
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

    public static void setCanAddFolder(long chatId, byte bool) {
        String sql = "update " + tableName + " set CanAddFolder = ? where Id =?";
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
        String sql = "update " + tableName + " set FullName=? where Id=?";
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
        String sql = "update " + tableName + " set name=? where Id=?";
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
