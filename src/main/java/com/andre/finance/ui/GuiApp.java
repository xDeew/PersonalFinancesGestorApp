
package com.andre.finance.ui;

import com.andre.finance.model.Transaction;
import com.andre.finance.service.FinanceService;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class GuiApp extends Application {
    private final FinanceService service = new FinanceService();
    private final TabPane tabs = new TabPane();
    private final Label lblTotal = new Label();
    private final List<String> categories = List.of("Comida", "Transporte", "Ocio", "Suscripciones", "Vivienda", "Salud", "Otros");
    private final HBox summaryBox = new HBox((double) 20.0F);
    private final Label lblGastos = new Label();
    private final Label lblIngresos = new Label();
    private final Label lblBalance = new Label();
    private final PieChart pie = new PieChart();
    private final NumberFormat fmt = NumberFormat.getNumberInstance(Locale.getDefault());
    private final Label tipLabel = new Label();

    public GuiApp() {
        this.fmt.setMinimumFractionDigits(2);
        this.fmt.setMaximumFractionDigits(2);
    }

    @Override
    public void start(Stage stage) {

        pie.setPrefSize(300, 300);
        pie.setMinSize(300, 300);
        pie.setMaxSize(300, 300);

        // 1) Creamos toolbar y formulario
        ToolBar toolbar = createToolbar();
        HBox form    = createForm();

        // 2) Metemos toolbar + form en un VBox “top-box”
        VBox topBox = new VBox(toolbar, form);
        topBox.getStyleClass().add("top-box");

        // 3) Inicializamos el TabPane
        tabs.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        rebuildTabs();
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            updateTotal();
            updateSummaryAndChart();
        });

        // 4) Preparamos el summaryBox
        summaryBox.getStyleClass().add("summary-box");
        lblGastos.getStyleClass().add("summary-label");
        lblIngresos.getStyleClass().add("summary-label");
        lblBalance.getStyleClass().add("summary-label");
        summaryBox.getChildren().setAll(lblGastos, lblIngresos, lblBalance, pie);
        summaryBox.setAlignment(Pos.CENTER);

        // 5) Configuramos el tipLabel
        tipLabel.getStyleClass().add("tip");
        tipLabel.setWrapText(true);
        tipLabel.setMaxWidth(400);
        tipLabel.setVisible(false);

        // 6) Bottom box (resumen + mensaje + total)
        VBox bottomBox = new VBox(10, summaryBox, tipLabel, lblTotal);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(10));
        BorderPane root = new BorderPane();

        // asegurarse de que el contenedor principal da prioridad de crecimiento al tabs
        VBox rootContainer = new VBox(topBox, tabs, bottomBox);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        root.setCenter(rootContainer);

        // 7) Montamos el BorderPane
        root.setTop(topBox);
        root.setCenter(tabs);
        root.setBottom(bottomBox);

        // 8) Lanzamos escena
        Scene scene = new Scene(root, 1200, 700);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Personal Finance Manager");
        lblTotal.getStyleClass().add("total");
        stage.show();

        // 9) Primera actualización
        updateTotal();
        updateSummaryAndChart();
    }


    private ToolBar createToolbar() {
        Button btnDeleteMonth = new Button("Borrar mes");
        btnDeleteMonth.setOnAction((e) -> {
            Tab sel = (Tab) this.tabs.getSelectionModel().getSelectedItem();
            if (sel != null) {
                YearMonth ym = (YearMonth) sel.getUserData();
                Alert confirm = new Alert(AlertType.CONFIRMATION, "¿Seguro que quieres eliminar todas las transacciones de\n" + ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.getYear() + "?", new ButtonType[]{ButtonType.YES, ButtonType.NO});
                confirm.setHeaderText((String) null);
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
        // 1) recordar mes seleccionado
        YearMonth previouslySelected = null;
        Tab sel = tabs.getSelectionModel().getSelectedItem();
        if (sel != null) {
            previouslySelected = (YearMonth) sel.getUserData();
        }

        tabs.getTabs().clear();
        var all = service.findAll();

        // 2) construye el conjunto de meses
        TreeSet<YearMonth> months = all.stream()
                .map(tx -> YearMonth.from(tx.getDate()))
                .collect(Collectors.toCollection(TreeSet::new));
        months.add(YearMonth.now());

        // 3) crea cada pestaña
        for (YearMonth ym : months) {
            List<Transaction> lista = all.stream()
                    .filter(tx -> YearMonth.from(tx.getDate()).equals(ym))
                    .sorted(Comparator.comparing(Transaction::getDate))
                    .toList();

            TableView<Transaction> table = createTable(ym);
            table.setItems(FXCollections.observableArrayList(lista));

            String ph = lista.isEmpty()
                    ? "No hay transacciones en " +
                    ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                    " " + ym.getYear()
                    : "";
            table.setPlaceholder(new Label(ph));

            String title = ym.getMonth()
                    .getDisplayName(TextStyle.FULL, Locale.getDefault())
                    + " " + ym.getYear();
            Tab tab = new Tab(title, table);
            tab.setUserData(ym);
            tabs.getTabs().add(tab);
        }

        // 4) restaurar selección si existía, si no, mes actual
        int indexToSelect = 0;
        if (previouslySelected != null) {
            for (int i = 0; i < tabs.getTabs().size(); i++) {
                if (previouslySelected.equals(tabs.getTabs().get(i).getUserData())) {
                    indexToSelect = i;
                    break;
                }
            }
        } else {
            // selecciona mes actual
            YearMonth now = YearMonth.now();
            for (int i = 0; i < tabs.getTabs().size(); i++) {
                if (now.equals(tabs.getTabs().get(i).getUserData())) {
                    indexToSelect = i;
                    break;
                }
            }
        }
        tabs.getSelectionModel().select(indexToSelect);
    }


    private TableView<Transaction> createTable(YearMonth allowedMonth) {
        TableView<Transaction> table = new TableView();
        table.setEditable(true);
        TableColumn<Transaction, LocalDate> cDate = new TableColumn("Fecha");
        cDate.setCellValueFactory(new PropertyValueFactory("date"));
        cDate.setCellFactory((col) -> new MonthRestrictedDatePickerCell(allowedMonth));
        cDate.setOnEditCommit((e) -> {
            Transaction t = (Transaction) e.getRowValue();
            t.setDate((LocalDate) e.getNewValue());
            this.service.updateTransaction(t);
            this.updateTotal();
            this.updateSummaryAndChart();
        });
        TableColumn<Transaction, String> cDesc = new TableColumn("Descripción");
        cDesc.setCellValueFactory(new PropertyValueFactory("description"));
        cDesc.setCellFactory(TextFieldTableCell.forTableColumn());
        cDesc.setOnEditCommit((e) -> {
            Transaction t = (Transaction) e.getRowValue();
            t.setDescription((String) e.getNewValue());
            this.service.updateTransaction(t);
            this.updateSummaryAndChart();
        });
        TableColumn<Transaction, Double> cAmt = new TableColumn<>("Importe");
        cAmt.setCellValueFactory(new PropertyValueFactory<>("amount"));
        cAmt.setCellFactory((col) -> new TextFieldTableCell<>(new StringConverter<Double>() {
            public String toString(Double d) {
                return GuiApp.this.fmt.format(d);
            }

            public Double fromString(String s) {
                return Double.parseDouble(s.replace(",", "."));
            }
        }));
        cAmt.setOnEditCommit((e) -> {
            Transaction t = (Transaction) e.getRowValue();
            t.setAmount((Double) e.getNewValue());
            this.service.updateTransaction(t);
            this.updateTotal();
            this.updateSummaryAndChart();
        });
        TableColumn<Transaction, String> cCat = new TableColumn("Categoría");
        cCat.setCellValueFactory(new PropertyValueFactory("category"));
        cCat.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(this.categories)));
        cCat.setOnEditCommit((e) -> {
            Transaction t = (Transaction) e.getRowValue();
            t.setCategory((String) e.getNewValue());
            this.service.updateTransaction(t);
            this.updateSummaryAndChart();
        });
        table.getColumns().addAll(new TableColumn[]{cDate, cDesc, cAmt, cCat});
        table.setRowFactory((tv) -> {
            TableRow<Transaction> row = new TableRow();
            MenuItem del = new MenuItem("Borrar");
            del.setOnAction((ev) -> {
                Transaction t = (Transaction) row.getItem();
                this.service.deleteTransaction(t.getId());
                this.rebuildTabs();
                this.updateTotal();
                this.updateSummaryAndChart();
            });
            ContextMenu menu = new ContextMenu(new MenuItem[]{del});
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(menu));
            return row;
        });
        return table;
    }

    private final Map<String, String> CATEGORY_COLORS = Map.of(
            "Comida", "#4CAF50",  // verde
            "Transporte", "#FF9800",  // naranja
            "Ocio", "#2196F3",  // azul
            "Suscripciones", "#9C27B0",  // morado
            "Vivienda", "#795548",  // marrón
            "Salud", "#F44336",  // rojo
            "Inversión", "#3F51B5",  // índigo
            "Ingreso", "#009688",  // teal
            "Otros", "#607D8B"   // gris azulado
    );

    private void updateSummaryAndChart() {
        Tab sel = tabs.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        YearMonth ym = (YearMonth) sel.getUserData();

        // Filtrar las transacciones de este mes
        List<Transaction> mes = service.findAll().stream()
                .filter(tx -> YearMonth.from(tx.getDate()).equals(ym))
                .toList();

        // Calcular gastos, ingresos y balance
        double gastos = mes.stream().filter(t -> t.getAmount() < 0)
                .mapToDouble(Transaction::getAmount).sum();
        double ingresos = mes.stream().filter(t -> t.getAmount() >= 0)
                .mapToDouble(Transaction::getAmount).sum();
        double balance = ingresos + gastos;

        // Actualizar etiquetas
        lblGastos.setText("Gastos   : " + fmt.format(Math.abs(gastos)));
        lblIngresos.setText("Ingresos : " + fmt.format(ingresos));
        lblBalance.setText("Balance  : " + fmt.format(balance));

        // Agrupar por categoría y sumar
        Map<String, Double> byCat = mes.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        // Asegurarnos de que "Otros" siempre esté presente (aunque a 0)
        byCat.putIfAbsent("Otros", 0.0);

        // Construir los slices
        double totalAbs = byCat.values().stream()
                .mapToDouble(Math::abs).sum();

        ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();
        byCat.forEach((cat, suma) -> {
            double abs = Math.abs(suma);
            PieChart.Data slice = new PieChart.Data(cat, abs);
            slices.add(slice);

            // Colocar tooltip
            slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    double pct = totalAbs == 0 ? 0 : abs / totalAbs * 100;
                    Tooltip tp = new Tooltip(
                            String.format(Locale.getDefault(), "%s: %.2f%%", cat, pct)
                    );
                    newNode.setOnMouseEntered(evt -> {
                        Bounds b = newNode.localToScreen(newNode.getBoundsInLocal());
                        tp.show(newNode, b.getMinX(), b.getMinY());
                    });
                    newNode.setOnMouseExited(evt -> tp.hide());

                    // **Aquí asignamos el color fijo**:
                    String color = CATEGORY_COLORS.getOrDefault(cat, "#CCCCCC");
                    newNode.setStyle("-fx-pie-color: " + color + ";");
                }
            });
        });

        pie.setData(slices);
        pie.setLabelsVisible(false);
        pie.setLegendVisible(true);

        pie.applyCss(); // fuerza que se apliquen estilos & se genere la leyenda
        for (Node legendItem : pie.lookupAll(".chart-legend-item")) {
            // cada legendItem es un Label: "■ Comida", etc.
            if (!(legendItem instanceof Label)) continue;
            Label lbl = (Label) legendItem;
            String cat = lbl.getText();
            String color = CATEGORY_COLORS.getOrDefault(cat, "#CCCCCC");

            // dentro del Label, el símbolo es una Region con clase .chart-legend-item-symbol
            Node symbol = lbl.lookup(".chart-legend-item-symbol");
            if (symbol != null) {
                symbol.setStyle("-fx-background-color: " + color + ";");
            }
        }
        showSmartTips(3);
    }

    private void showSmartTips(int monthsBack) {
        Tab sel = tabs.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        YearMonth current = (YearMonth) sel.getUserData();

        // 1) Total absoluto por categoría este mes
        Map<String, Double> thisMonthByCat = service.findAll().stream()
                .filter(tx -> YearMonth.from(tx.getDate()).equals(current))
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(tx -> Math.abs(tx.getAmount()))
                ));

        // 2) Media absoluta por categoría de los N meses anteriores
        Map<String, Double> avgByCat = service.averageSpendingPerCategory(current, monthsBack);
        if (avgByCat.isEmpty()) {
            tipLabel.setVisible(false);
            return;
        }

        // 3) Comparamos y acumulamos todos los mensajes
        List<String> tips = new ArrayList<>();
        for (String cat : thisMonthByCat.keySet()) {
            if ("Ingreso".equals(cat)) continue;
            double actual = thisMonthByCat.get(cat);
            double media = avgByCat.getOrDefault(cat, 0.0);
            if (media > 0 && actual > media * 1.10) {
                double pct = (actual - media) / media * 100;
                if ("Inversión".equals(cat)) {
                    tips.add(String.format(
                            Locale.getDefault(),
                            "Has invertido un %.0f%% más que la media de los últimos %d meses.",
                            pct, monthsBack
                    ));
                } else {
                    tips.add(String.format(
                            Locale.getDefault(),
                            "Has gastado un %.0f%% más en %s que la media de los últimos %d meses.",
                            pct, cat, monthsBack
                    ));
                }
            }
        }

        // 4) Mostramos todos los tips (cada uno en su línea)
        if (!tips.isEmpty()) {
            tipLabel.setText(String.join("\n", tips));
            tipLabel.setVisible(true);
        } else {
            tipLabel.setVisible(false);
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
            LocalDate fecha = (LocalDate) dp.getValue();
            String desc = descField.getText().trim();
            String type = (String) cbType.getValue();
            String category = (String) cbCat.getValue();
            String sAmt = amtField.getText().trim();

            double rawAmt;
            try {
                rawAmt = Double.parseDouble(sAmt);
                if (rawAmt < (double) 0.0F) {
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
        HBox form = new HBox((double) 8.0F, new Node[]{new Label("Fecha:"), dp, new Label("Desc.:"), descField, new Label("Tipo:"), cbType, new Label("Cat.:"), cbCat, new Label("Imp.:"), amtField, btn});
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets((double) 10.0F));
        return form;
    }

    private void updateTotal() {
        Tab sel = (Tab) this.tabs.getSelectionModel().getSelectedItem();
        if (sel == null) {
            this.lblTotal.setText("");
        } else {
            YearMonth ym = (YearMonth) sel.getUserData();
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
            this.datePicker.setOnAction((e) -> this.commitEdit((LocalDate) this.datePicker.getValue()));
            this.setContentDisplay(ContentDisplay.TEXT_ONLY);
        }

        public void startEdit() {
            if (!this.isEmpty()) {
                super.startEdit();
                this.datePicker.setValue((LocalDate) this.getItem());
                this.setGraphic(this.datePicker);
                this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

        }

        public void cancelEdit() {
            super.cancelEdit();
            this.updateItem((LocalDate) this.getItem(), false);
        }

        public void updateItem(LocalDate item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                this.setText((String) null);
                this.setGraphic((Node) null);
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
