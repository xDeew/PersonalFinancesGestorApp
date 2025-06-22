package com.andre.finance.ui;

import com.andre.finance.service.FinanceService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final FinanceService service = new FinanceService();
    private final Scanner scanner;
    private static final List<String> CATEGORIES = List.of("Comida", "Transporte", "Ocio", "Suscripciones", "Vivienda", "Salud", "Otros");

    public ConsoleUI() {
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        int opt = 0;

        do {
            System.out.println("\n\ud83c\udfe6 Personal Finance Manager");
            System.out.println("1. Añadir transacción");
            System.out.println("2. Listar transacciones");
            System.out.println("3. Salir");
            System.out.print("> ");
            String line = this.scanner.nextLine().trim();

            try {
                opt = Integer.parseInt(line);
            } catch (NumberFormatException var4) {
                System.out.println("Introduce un número válido.");
                continue;
            }

            switch (opt) {
                case 1:
                    this.addTransactionFlow();
                    break;
                case 2:
                    this.service.listTransactions();
                    break;
                case 3:
                    System.out.println("¡Adiós!");
                    break;
                default:
                    System.out.println("Opción inválida.");
            }
        } while (opt != 3);

    }

    private void addTransactionFlow() {
        try {
            System.out.print("Fecha (YYYY-MM-DD): ");
            LocalDate date = LocalDate.parse(this.scanner.nextLine().trim());
            System.out.print("Descripción: ");
            String desc = this.scanner.nextLine().trim();
            System.out.print("Importe (solo cifra, sin signo): ");
            double rawAmt = Double.parseDouble(this.scanner.nextLine().trim());
            if (rawAmt < (double) 0.0F) {
                throw new NumberFormatException();
            }

            List<String> TYPES = List.of("Gasto", "Ingreso", "Inversión");
            System.out.println("Tipos:");

            for (int i = 0; i < TYPES.size(); ++i) {
                System.out.printf("  %d) %s%n", i + 1, TYPES.get(i));
            }

            System.out.print("Elige tipo (número): ");

            int typeIdx;
            try {
                typeIdx = Integer.parseInt(this.scanner.nextLine().trim()) - 1;
                if (typeIdx < 0 || typeIdx >= TYPES.size()) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException var11) {
                System.out.println("Selección inválida, usando 'Gasto'.");
                typeIdx = 0;
            }

            String type = (String) TYPES.get(typeIdx);
            String category;
            if (!"Gasto".equals(type)) {
                category = type;
                System.out.println("Categoría fijada como: " + type);
            } else {
                System.out.println("Categorías disponibles:");

                for (int i = 0; i < CATEGORIES.size(); ++i) {
                    System.out.printf("  %d) %s%n", i + 1, CATEGORIES.get(i));
                }

                System.out.print("Elige categoría (número): ");

                int catIdx;
                try {
                    catIdx = Integer.parseInt(this.scanner.nextLine().trim()) - 1;
                    if (catIdx < 0 || catIdx >= CATEGORIES.size()) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException var12) {
                    System.out.println("Selección inválida, asignando 'Otros'.");
                    catIdx = CATEGORIES.indexOf("Otros");
                }

                category = (String) CATEGORIES.get(catIdx);
            }

            boolean ok = this.service.addTransaction(date, desc, rawAmt, category, type);
            if (!ok) {
                System.out.println("❌ Solo se permiten fechas del mes actual.");
            } else {
                System.out.printf("✔ %s de %.2f en categoría '%s' añadido.%n", type, rawAmt, category);
            }
        } catch (DateTimeParseException var13) {
            System.out.println("Formato de fecha inválido.");
        } catch (NumberFormatException var14) {
            System.out.println("Importe inválido (debe ser número positivo).");
        }

    }
}
