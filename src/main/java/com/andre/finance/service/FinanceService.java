//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.andre.finance.service;

import com.andre.finance.dao.TransactionDao;
import com.andre.finance.model.Transaction;

import java.io.PrintStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FinanceService {
    private final TransactionDao dao = new TransactionDao();

    public boolean addTransaction(LocalDate date, String desc, double rawAmt, String category, String type) {
        YearMonth ym = YearMonth.from(LocalDate.now());
        // if (!YearMonth.from(date).equals(ym)) {
        if (date.isAfter(LocalDate.now())) {
            return false;
        } else {
            double var10000;
            switch (type) {
                case "Gasto" -> var10000 = -Math.abs(rawAmt);
                case "Ingreso" -> var10000 = Math.abs(rawAmt);
                case "Inversión" -> var10000 = Math.abs(rawAmt);
                default -> var10000 = rawAmt;
            }

            double amt = var10000;
            Transaction tx = new Transaction(date, desc, amt, category);

            try {
                this.dao.save(tx);
                return true;
            } catch (SQLException e) {
                System.err.println("❌ Error al guardar: " + e.getMessage());
                return false;
            }
        }
    }

    public void listTransactions() {
        try {
            List<Transaction> all = this.dao.findAll();
            if (all.isEmpty()) {
                System.out.println("— No hay transacciones registradas.");
            } else {
                System.out.println("\nFecha     | Descripción           |   Importe");
                System.out.println("------------------------------------------------");
                PrintStream var10001 = System.out;
                Objects.requireNonNull(var10001);
                all.forEach(var10001::println);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al leer transacciones: " + e.getMessage());
        }

    }

    public List<Transaction> findAll() {
        try {
            return this.dao.findAll();
        } catch (SQLException e) {
            System.err.println("❌ Error al leer transacciones: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public void deleteTransaction(long id) {
        try {
            this.dao.delete(id);
        } catch (SQLException e) {
            System.err.println("Error borrando: " + e.getMessage());
        }

    }

    public void deleteAllTransactions() {
        try {
            this.dao.deleteAll();
            System.out.println("✔ Se han borrado todas las transacciones.");
        } catch (SQLException e) {
            System.err.println("❌ Error borrando todas las transacciones: " + e.getMessage());
        }

    }

    public void deleteTransactionsInMonth(YearMonth ym) {
        try {
            this.dao.deleteByMonth(ym);
            System.out.println("✔ Se han borrado todas las transacciones de " + String.valueOf(ym));
        } catch (SQLException e) {
            PrintStream var10000 = System.err;
            String var10001 = String.valueOf(ym);
            var10000.println("❌ Error al borrar mes " + var10001 + ": " + e.getMessage());
        }

    }

    public void updateTransaction(Transaction tx) {
        try {
            this.dao.update(tx);
        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar: " + e.getMessage());
        }

    }


    /**
     * Calcula el gasto promedio absoluto por categoría en los N meses anteriores al mes dado.
     */
    public Map<String, Double> averageSpendingPerCategory(YearMonth current, int monthsBack) {
        // 1) construye la lista de YearMonth previos
        List<YearMonth> prevMonths = IntStream.rangeClosed(1, monthsBack)
                .mapToObj(i -> current.minusMonths(i))
                .collect(Collectors.toList());

        // 2) filtra transacciones que caigan en prevMonths
        List<Transaction> prevTx = findAll().stream()
                .filter(tx -> prevMonths.contains(YearMonth.from(tx.getDate())))
                .collect(Collectors.toList());

        // 3) agrupa y suma gastos absolutos
        Map<String, Double> sumByCat = prevTx.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(tx -> Math.abs(tx.getAmount()))
                ));

        // 4) divide cada suma entre monthsBack para obtener promedio
        return sumByCat.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue() / monthsBack
        ));
    }


}
