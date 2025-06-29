package com.andre.finance.ui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;

public class MonthRestrictedDatePickerCell<S> extends TableCell<S, LocalDate> {
    private final DatePicker picker = new DatePicker();
    private final YearMonth allowed;

    public MonthRestrictedDatePickerCell(YearMonth allowed) {
        this.allowed = allowed;
      /*   this.picker.setDayCellFactory((p) -> new DateCell() {
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (!empty && !YearMonth.from(date).equals(allowed)) {
                    this.setDisable(true);
                    this.setStyle("-fx-background-color: lightgray;");
                }

            }
        }); */
        picker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (!empty && date.isAfter(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                }
            }
        });
        this.picker.setOnAction((e) -> this.commitEdit((LocalDate) this.picker.getValue()));
        this.setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

    public void startEdit() {
        if (!this.isEmpty()) {
            super.startEdit();
            this.picker.setValue((LocalDate) this.getItem());
            this.setGraphic(this.picker);
            this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

    }

    public void cancelEdit() {
        super.cancelEdit();
        this.updateItem((LocalDate) this.getItem(), false);
    }

    protected void updateItem(LocalDate item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
            if (this.isEditing()) {
                this.picker.setValue(item);
                this.setGraphic(this.picker);
                this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                this.setText(item.format(DateTimeFormatter.ISO_LOCAL_DATE));
                this.setContentDisplay(ContentDisplay.TEXT_ONLY);
            }
        } else {
            this.setText((String) null);
            this.setGraphic((Node) null);
        }

    }
}
