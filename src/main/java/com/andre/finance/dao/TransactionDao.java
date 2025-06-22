package com.andre.finance.dao;

import com.andre.finance.model.Transaction;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class TransactionDao {
    public TransactionDao() {
        try (Connection conn = DatabaseManager.getConnection(); Statement st = conn.createStatement();) {
            st.executeUpdate("    CREATE TABLE IF NOT EXISTS transactions (\n      id IDENTITY PRIMARY KEY,\n      date DATE,\n      description VARCHAR(255),\n      amount DOUBLE\n    )\n");
            ResultSet rs = conn.getMetaData().getColumns((String) null, (String) null, "TRANSACTIONS", "CATEGORY");
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE transactions ADD category VARCHAR(100) DEFAULT 'Otros'");
            }

            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void save(Transaction tx) throws SQLException {
        String sql = "INSERT INTO transactions (date, description, amount, category) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql, 1)) {
            ps.setDate(1, Date.valueOf(tx.getDate()));
            ps.setString(2, tx.getDescription());
            ps.setDouble(3, tx.getAmount());
            ps.setString(4, tx.getCategory());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    tx.setId(rs.getLong(1));
                }
            }
        }

    }

    public List<Transaction> findAll() throws SQLException {
        String sql = "SELECT id, date, description, amount, category FROM transactions ORDER BY date";
        List<Transaction> list = new ArrayList();

        try (ResultSet rs = DatabaseManager.getConnection().createStatement().executeQuery(sql)) {
            while (rs.next()) {
                Transaction tx = new Transaction(rs.getDate("date").toLocalDate(), rs.getString("description"), rs.getDouble("amount"), rs.getString("category"));
                tx.setId(rs.getLong("id"));
                list.add(tx);
            }
        }

        return list;
    }

    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }

    }

    public void update(Transaction tx) throws SQLException {
        String sql = "UPDATE transactions SET date=?, description=?, amount=?, category=? WHERE id=?";

        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(tx.getDate()));
            ps.setString(2, tx.getDescription());
            ps.setDouble(3, tx.getAmount());
            ps.setString(4, tx.getCategory());
            ps.setLong(5, tx.getId());
            ps.executeUpdate();
        }

    }

    public void deleteAll() throws SQLException {
        try (Statement st = DatabaseManager.getConnection().createStatement()) {
            st.executeUpdate("DELETE FROM transactions");
        }

    }

    public void deleteByMonth(YearMonth ym) throws SQLException {
        String sql = "    DELETE FROM transactions\n     WHERE date >= ?\n       AND date < ?\n";
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.plusMonths(1L).atDay(1);

        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            ps.executeUpdate();
        }

    }
}
