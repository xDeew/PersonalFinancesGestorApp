package com.andre.finance.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String URL = "jdbc:h2:./data/finance;AUTO_SERVER=TRUE";
    private static Connection conn;

    public static Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection("jdbc:h2:./data/finance;AUTO_SERVER=TRUE", "sa", "");
        }

        return conn;
    }
}
