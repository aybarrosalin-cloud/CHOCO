package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;
import java.util.function.Consumer;

public class popupBuscarProductoController {

    @FXML private TextField txtBuscar;
    @FXML private TableView<String[]> tablaProductos;
    @FXML private TableColumn<String[], String> colCodigo;
    @FXML private TableColumn<String[], String> colNombre;
    @FXML private TableColumn<String[], String> colStock;

    private final ObservableList<String[]> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();
    private Consumer<String> callback;

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colStock.setCellValueFactory(d  -> new SimpleStringProperty(d.getValue()[2]));

        FilteredList<String[]> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscar.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(row -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return row[0].toLowerCase().contains(f) || row[1].toLowerCase().contains(f);
            }));
        tablaProductos.setItems(listaFiltrada);

        tablaProductos.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) seleccionar();
        });

        cargarProductos();
    }

    private void cargarProductos() {
        String sql = "SELECT codigo, nombre, ISNULL(CAST(stock AS VARCHAR), '0') " +
                     "FROM tbl_producto ORDER BY nombre";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new String[]{
                    rs.getString(1) != null ? rs.getString(1) : "",
                    rs.getString(2) != null ? rs.getString(2) : "",
                    rs.getString(3) != null ? rs.getString(3) : "0"
                });
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Ocurrio un error inesperado. Intente nuevamente.").showAndWait();
        }
    }

    @FXML
    private void seleccionar() {
        String[] sel = tablaProductos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, "Selecciona un producto de la lista.").showAndWait();
            return;
        }
        if (callback != null) callback.accept(sel[0]);
        cerrar();
    }

    @FXML
    private void cerrar() {
        ((Stage) txtBuscar.getScene().getWindow()).close();
    }

    public void setCallback(Consumer<String> callback) {
        this.callback = callback;
    }

    public static void mostrar(Consumer<String> callback) {
        try {
            FXMLLoader loader = new FXMLLoader(
                popupBuscarProductoController.class.getResource("/vistasFinales/popupBuscarProducto.fxml"));
            Parent root = loader.load();
            popupBuscarProductoController ctrl = loader.getController();
            ctrl.setCallback(callback);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Buscar Producto");
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Ocurrio un error inesperado. Intente nuevamente.").showAndWait();
        }
    }
}
