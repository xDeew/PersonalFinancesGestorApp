package com.andre.finance.ui;

import com.andre.finance.model.Transaction;

import java.time.LocalDate;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class EditDialog extends Dialog<Transaction> {
    public EditDialog(Transaction original) {
        this.setTitle("Editar transacción");
        DatePicker dp = new DatePicker(original.getDate());
        TextField tfDesc = new TextField(original.getDescription());
        TextField tfAmt = new TextField(String.valueOf(original.getAmount()));
        GridPane grid = new GridPane();
        grid.setHgap((double) 10.0F);
        grid.setVgap((double) 10.0F);
        grid.addRow(0, new Node[]{new Label("Fecha:"), dp});
        grid.addRow(1, new Node[]{new Label("Desc.:"), tfDesc});
        grid.addRow(2, new Node[]{new Label("Imp.:"), tfAmt});
        this.getDialogPane().setContent(grid);
        this.getDialogPane().getButtonTypes().addAll(new ButtonType[]{ButtonType.OK, ButtonType.CANCEL});
        this.setResultConverter((btn) -> {
            if (btn == ButtonType.OK) {
                try {
                    original.setDate((LocalDate) dp.getValue());
                    original.setDescription(tfDesc.getText().trim());
                    original.setAmount(Double.parseDouble(tfAmt.getText().trim()));
                    return original;
                } catch (NumberFormatException var6) {
                }
            }

            return null;
        });
    }
}
