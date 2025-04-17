package org.example.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.Main;

public class DataBaseConnection {
    private static HikariDataSource dataSource;
    public String tableName;

    //сам экземпляр объекты
    private static final DataBaseConnection dataBaseConnection;

    //получаем экземпляр объекта
    static {
        try {
            dataBaseConnection = new DataBaseConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //используем singleton, чтобы быть уверенным что только один экземпляр класса будет создан
    private DataBaseConnection() throws SQLException {
        Properties properties = new Properties();
        String USER;
        String PASS;
        String URL;
        //получаем переменные из файла конфигурации
        try (FileInputStream fis = new FileInputStream(Main.propertyPath)) {
            properties.load(fis);
            URL = properties.getProperty("databaseURL");
            USER = properties.getProperty("user");
            PASS = properties.getProperty("pass");
            tableName = properties.getProperty("tableName");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASS);
        config.setMaximumPoolSize(20);
        dataSource = new HikariDataSource(config);
        //закрытие базы данных при выключении проекта
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (dataSource != null) {
                dataSource.close();
            }
        }));
    }

    public static DataBaseConnection getInstance() {
        return dataBaseConnection;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
