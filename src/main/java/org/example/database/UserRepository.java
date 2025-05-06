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
                "Id bigint not null," +
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

    private String executeSQLQuery(String sql, long chatId) {
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            //устанавливаем значения для знаков ? в запросе sql
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String result = resultSet.getNString(1);
                if (result != null) {
                    resultSet.close();
                    return result;
                }
            }
            resultSet.close();
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return "Not found";
    }

    public String getFilePath(long chatId) {
        String sql = "select FilePath from " + tableName + " where Id=?";
        return executeSQLQuery(sql, chatId);
    }

    public boolean getUser(long chatId) {
        String sql = "select Name from " +  tableName + " where Id=?";
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            //устанавливаем значения для знаков ? в запросе sql
            preparedStatement.setLong(1, chatId);
            ResultSet resultSet = preparedStatement.executeQuery();
            //проверяем есть ли запись
            if (resultSet.next()) {
                resultSet.close();
                return true;
            }
            resultSet.close();
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return false;
    }

    public String getObj(long chatId) {
        String sql = "select Obj from " + tableName + " where Id=?";
        return executeSQLQuery(sql, chatId);
    }

    public boolean getCanAddFolder(long chatId) {
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
                    resultSet.close();
                    return true;
                }
            }
            resultSet.close();
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        return false;
    }

    /**
     *
     * @param sql sql query
     * @param params takes params for sql query. Give params in right order in your sql query
     */
    private void executeSQLUpdate(String sql, Object ... params) {
        try (Connection connection = dataBaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Long) {
                    preparedStatement.setLong(i + 1, (Long) params[i]);
                } else if (params[i] instanceof String) {
                    preparedStatement.setString(i + 1, (String) params[i]);
                } else if (params[i] instanceof Byte) {
                    preparedStatement.setByte(i + 1,(byte) params[i]);
                }
            }
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                System.out.println("Ошибка на строке 147 UserRepository");
            }
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public void addUser(long chatId) {
        System.out.println("Add user with Id: " + chatId);
        String sql = "insert into " + tableName + " (Id) values (?)";
        executeSQLUpdate(sql, chatId);
    }

    public void setObj(long chatId, String obj) {
        String sql = "update " + tableName + " set Obj=? where Id=?";
        executeSQLUpdate(sql, obj, chatId);
    }

    public void setFilePath(long chatId, String filePath) {
        String sql = "update " + tableName + " set FilePath=? where Id=?";
        executeSQLUpdate(sql, filePath, chatId);
    }

    public void setCanAddFolder(long chatId, byte bool) {
        String sql = "update " + tableName + " set CanAddFolder = ? where Id =?";
        executeSQLUpdate(sql, bool, chatId);
    }

    public void setUserFullName(long chatId, String fullName) {
        String sql = "update " + tableName + " set FullName=? where Id=?";
        executeSQLUpdate(sql, fullName, chatId);
    }

    public void setUserName(long chatId, String userName) {
        String sql = "update " + tableName + " set name=? where Id=?";
        executeSQLUpdate(sql, userName, chatId);
    }
}
