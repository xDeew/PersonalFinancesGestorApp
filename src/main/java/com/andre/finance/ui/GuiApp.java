
package com.andre.finance.ui;

import com.andre.finance.model.Transaction;
import com.andre.finance.service.FinanceService;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class GuiApp extends Application {
    private final FinanceService service = new FinanceService();
    private final TabPane tabs = new TabPane();
    private final Label lblTotal = new Label();
    private final List<String> categories = List.of("Comida", "Transporte", "Ocio", "Suscripciones", "Vivienda", "Salud", "Otros");
    private final HBox summaryBox = new HBox((double)20.0F);
    private final Label lblGastos = new Label();
    private final Label lblIngresos = new Label();
    private final Label lblBalance = new Label();
    private final PieChart pie = new PieChart();
    private final NumberFormat fmt = NumberFormat.getNumberInstance(Locale.getDefault());

    public GuiApp() {
        this.fmt.setMinimumFractionDigits(2);
        this.fmt.setMaximumFractionDigits(2);
    }

    public void start(Stage stage) {
        ToolBar toolbar = this.createToolbar();
        this.tabs.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        this.rebuildTabs();
        this.tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            this.updateTotal();
            this.updateSummaryAndChart();
        });
        HBox form = this.createForm();
        this.summaryBox.getChildren().setAll(new Node[]{this.lblGastos, this.lblIngresos, this.lblBalance, this.pie});
        this.summaryBox.setAlignment(Pos.CENTER);
        VBox bottomBox = new VBox((double)10.0F, new Node[]{form, this.summaryBox, this.lblTotal});
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets((double)10.0F));
        BorderPane root = new BorderPane(this.tabs);
        root.setTop(toolbar);
        root.setBottom(bottomBox);
        Scene scene = new Scene(root, (double)900.0F, (double)700.0F);
        stage.setScene(scene);
        stage.setTitle("Personal Finance Manager");
        scene.getStylesheets().add(this.getClass().getResource("/app.css").toExternalForm());
        this.lblTotal.getStyleClass().add("total");
        stage.show();
        this.updateTotal();
        this.updateSummaryAndChart();
    }

    private ToolBar createToolbar() {
        Button btnDeleteMonth = new Button("Borrar mes");
        btnDeleteMonth.setOnAction((e) -> {
            Tab sel = (Tab)this.tabs.getSelectionModel().getSelectedItem();
            if (sel != null) {
                YearMonth ym = (YearMonth)sel.getUserData();
                Alert confirm = new Alert(AlertType.CONFIRMATION, "¿Seguro que quieres eliminar todas las transacciones de\n" + ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.getYear() + "?", new ButtonType[]{ButtonType.YES, ButtonType.NO});
                confirm.setHeaderText((String)null);
                confirm.showAndWait().filter((bt) -> bt == ButtonType.YES).ifPresent((bt) -> {
                    this.service.deleteTransactionsInMonth(ym);
                    this.rebuildTabs();
                    this.updateTotal();
                    this.updateSummaryAndChart();
                });
            }
        });
        return new ToolBar(new Node[]{btnDeleteMonth});
    }

    private void rebuildTabs() {
        this.tabs.getTabs().clear();
        List<Transaction> all = this.service.findAll();
        TreeSet<YearMonth> months = (TreeSet)all.stream().map((tx) -> YearMonth.from(tx.getDate())).collect(Collectors.toCollection(TreeSet::new));
        months.add(YearMonth.now());

        for(YearMonth ym : months) {
            List<Transaction> lista = all.stream().filter((tx) -> YearMonth.from(tx.getDate()).equals(ym)).sorted(Comparator.comparing(Transaction::getDate)).toList();
            TableView<Transaction> table = this.createTable(ym);
            table.setItems(FXCollections.observableArrayList(lista));
            String ph = lista.isEmpty() ? "No hay transacciones en " + ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.getYear() : "";
            table.setPlaceholder(new Label(ph));
            String var10000 = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
            String title = var10000 + " " + ym.getYear();
            Tab tab = new Tab(title, table);
            tab.setUserData(ym);
            this.tabs.getTabs().add(tab);
        }

        YearMonth now = YearMonth.now();

        for(int i = 0; i < this.tabs.getTabs().size(); ++i) {
            if (now.equals(((Tab)this.tabs.getTabs().get(i)).getUserData())) {
                this.tabs.getSelectionModel().select(i);
                break;
            }
        }

    }

    private TableView<Transaction> createTable(YearMonth allowedMonth) {
        TableView<Transaction> table = new TableView();
        table.setEditable(true);
        TableColumn<Transaction, LocalDate> cDate = new TableColumn("Fecha");
        cDate.setCellValueFactory(new PropertyValueFactory("date"));
        cDate.setCellFactory((col) -> new MonthRestrictedDatePickerCell(allowedMonth));
        cDate.setOnEditCommit((e) -> {
            Transaction t = (Transaction)e.getRowValue();
            t.setDate((LocalDate)e.getNewValue());
            this.service.updateTransaction(t);
            this.updateTotal();
            this.updateSummaryAndChart();
        });
        TableColumn<Transaction, String> cDesc = new TableColumn("Descripción");
        cDesc.setCellValueFactory(new PropertyValueFactory("description"));
        cDesc.setCellFactory(TextFieldTableCell.forTableColumn());
        cDesc.setOnEditCommit((e) -> {
            Transaction t = (Transaction)e.getRowValue();
            t.setDescription((String)e.getNewValue());
            this.service.updateTransaction(t);
            this.updateSummaryAndChart();
        });
        TableColumn<Transaction, Double> cAmt = new TableColumn("Importe");
        cAmt.setCellValueFactory(new PropertyValueFactory("amount"));
        cAmt.setCellFactory((col) -> new TextFieldTableCell(new StringConverter<Double>() {
            public String toString(Double d) {
                return GuiApp.this.fmt.format(d);
            }

            public Double fromString(String s) {
                return Double.parseDouble(s.replace(",", "."));
            }
        }));
        cAmt.setOnEditCommit((e) -> {
            Transaction t = (Transaction)e.getRowValue();
            t.setAmount((Double)e.getNewValue());
            this.service.updateTransaction(t);
            this.updateTotal();
            this.updateSummaryAndChart();
        });
        TableColumn<Transaction, String> cCat = new TableColumn("Categoría");
        cCat.setCellValueFactory(new PropertyValueFactory("category"));
        cCat.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(this.categories)));
        cCat.setOnEditCommit((e) -> {
            Transaction t = (Transaction)e.getRowValue();
            t.setCategory((String)e.getNewValue());
            this.service.updateTransaction(t);
            this.updateSummaryAndChart();
        });
        table.getColumns().addAll(new TableColumn[]{cDate, cDesc, cAmt, cCat});
        table.setRowFactory((tv) -> {
            TableRow<Transaction> row = new TableRow();
            MenuItem del = new MenuItem("Borrar");
            del.setOnAction((ev) -> {
                Transaction t = (Transaction)row.getItem();
                this.service.deleteTransaction(t.getId());
                this.rebuildTabs();
                this.updateTotal();
                this.updateSummaryAndChart();
            });
            ContextMenu menu = new ContextMenu(new MenuItem[]{del});
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu)null).otherwise(menu));
            return row;
        });
        return table;
    }

    private void updateSummaryAndChart() {
        Tab sel = (Tab)this.tabs.getSelectionModel().getSelectedItem();
        if (sel != null) {
            YearMonth ym = (YearMonth)sel.getUserData();
            List<Transaction> mes = this.service.findAll().stream().filter((tx) -> YearMonth.from(tx.getDate()).equals(ym)).toList();
            double gastos = mes.stream().filter((t) -> t.getAmount() < (double)0.0F).mapToDouble(Transaction::getAmount).sum();
            double ingresos = mes.stream().filter((t) -> t.getAmount() >= (double)0.0F).mapToDouble(Transaction::getAmount).sum();
            double balance = ingresos + gastos;
            NumberFormat var10001 = this.fmt;
            this.lblGastos.setText("Gastos   : " + var10001.format(Math.abs(gastos)));
            Label var10000 = this.lblIngresos;
            String var15 = this.fmt.format(ingresos);
            var10000.setText("Ingresos : " + var15);
            var10000 = this.lblBalance;
            var15 = this.fmt.format(balance);
            var10000.setText("Balance  : " + var15);
            Map<String, Double> byCat = (Map)mes.stream().collect(Collectors.groupingBy(Transaction::getCategory, Collectors.summingDouble(Transaction::getAmount)));
            byCat.putIfAbsent("Otros", (double)0.0F);
            double totalAbs = byCat.values().stream().mapToDouble(Math::abs).sum();
            ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();
            byCat.forEach((cat, suma) -> {
                double abs = Math.abs(suma);
                PieChart.Data slice = new PieChart.Data(cat, abs);
                slices.add(slice);
                slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        double pct = totalAbs == (double)0.0F ? (double)0.0F : abs / totalAbs * (double)100.0F;
                        Tooltip tp = new Tooltip(String.format(Locale.getDefault(), "%s: %.2f%%", cat, pct));
                        newNode.setOnMouseEntered((evt) -> {
                            Bounds b = newNode.localToScreen(newNode.getBoundsInLocal());
                            tp.show(newNode, b.getMinX(), b.getMinY());
                        });
                        newNode.setOnMouseExited((evt) -> tp.hide());
                    }

                });
            });
            this.pie.setData(slices);
            this.pie.setLabelsVisible(false);
            this.pie.setLegendVisible(true);
        }
    }

    private HBox createForm() {
        DatePicker dp = new DatePicker(LocalDate.now());
        dp.setDayCellFactory((picker) -> new DateCell() {
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (!empty && date.isAfter(LocalDate.now())) {
                    this.setDisable(true);
                    this.setStyle("-fx-background-color: #ffc0cb;");
                }

            }
        });
        TextField descField = new TextField();
        descField.setPromptText("Descripción");
        ComboBox<String> cbType = new ComboBox(FXCollections.observableArrayList(new String[]{"Gasto", "Ingreso", "Inversión"}));
        cbType.getSelectionModel().selectFirst();
        ComboBox<String> cbCat = new ComboBox();
        cbCat.setItems(FXCollections.observableArrayList(this.categories));
        cbCat.getSelectionModel().selectFirst();
        cbType.getSelectionModel().selectedItemProperty().addListener((obs, oldType, newType) -> {
            if ("Gasto".equals(newType)) {
                cbCat.setItems(FXCollections.observableArrayList(this.categories));
                cbCat.getSelectionModel().selectFirst();
                cbCat.setDisable(false);
            } else {
                cbCat.setItems(FXCollections.observableArrayList(new String[]{newType}));
                cbCat.getSelectionModel().select(0);
                cbCat.setDisable(true);
            }

        });
        cbType.getSelectionModel().getSelectedItem();
        TextField amtField = new TextField();
        amtField.setPromptText("Importe");
        Button btn = new Button("Añadir");
        btn.setOnAction((e) -> {
            LocalDate fecha = (LocalDate)dp.getValue();
            String desc = descField.getText().trim();
            String type = (String)cbType.getValue();
            String category = (String)cbCat.getValue();
            String sAmt = amtField.getText().trim();

            double rawAmt;
            try {
                rawAmt = Double.parseDouble(sAmt);
                if (rawAmt < (double)0.0F) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException var15) {
                (new Alert(AlertType.ERROR, "Importe inválido (debe ser >0)", new ButtonType[0])).showAndWait();
                return;
            }

            boolean ok = this.service.addTransaction(fecha, desc, rawAmt, category, type);
            if (!ok) {
                (new Alert(AlertType.WARNING, "Solo se permiten fechas del mes actual", new ButtonType[0])).showAndWait();
            } else {
                descField.clear();
                amtField.clear();
                this.rebuildTabs();
                this.updateTotal();
                this.updateSummaryAndChart();
            }
        });
        HBox form = new HBox((double)8.0F, new Node[]{new Label("Fecha:"), dp, new Label("Desc.:"), descField, new Label("Tipo:"), cbType, new Label("Cat.:"), cbCat, new Label("Imp.:"), amtField, btn});
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets((double)10.0F));
        return form;
    }

    private void updateTotal() {
        Tab sel = (Tab)this.tabs.getSelectionModel().getSelectedItem();
        if (sel == null) {
            this.lblTotal.setText("");
        } else {
            YearMonth ym = (YearMonth)sel.getUserData();
            double total = this.service.findAll().stream().filter((tx) -> YearMonth.from(tx.getDate()).equals(ym)).mapToDouble(Transaction::getAmount).sum();
            String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
            this.lblTotal.setText(String.format("%s %d: %s", monthName, ym.getYear(), this.fmt.format(total)));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static class DatePickerTableCell<S> extends TableCell<S, LocalDate> {
        private final DatePicker datePicker = new DatePicker();

        public DatePickerTableCell() {
            this.datePicker.setOnAction((e) -> this.commitEdit((LocalDate)this.datePicker.getValue()));
            this.setContentDisplay(ContentDisplay.TEXT_ONLY);
        }

        public void startEdit() {
            if (!this.isEmpty()) {
                super.startEdit();
                this.datePicker.setValue((LocalDate)this.getItem());
                this.setGraphic(this.datePicker);
                this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

        }

        public void cancelEdit() {
            super.cancelEdit();
            this.updateItem((LocalDate)this.getItem(), false);
        }

        public void updateItem(LocalDate item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                this.setText((String)null);
                this.setGraphic((Node)null);
            } else if (this.isEditing()) {
                this.datePicker.setValue(item);
                this.setGraphic(this.datePicker);
                this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                this.setText(item.format(DateTimeFormatter.ISO_LOCAL_DATE));
                this.setContentDisplay(ContentDisplay.TEXT_ONLY);
            }

        }
    }
}
