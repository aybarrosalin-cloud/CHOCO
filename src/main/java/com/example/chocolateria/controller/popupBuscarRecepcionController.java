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

public class popupBuscarRecepcionController {

    @FXML private TextField txtBuscar;
    @FXML private TableView<String[]> tablaRecepciones;
    @FXML private TableColumn<String[], String> colId;
    @FXML private TableColumn<String[], String> colOrden;
    @FXML private TableColumn<String[], String> colRnc;
    @FXML private TableColumn<String[], String> colFecha;

    private final ObservableList<String[]> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();
    private Consumer<String> callback;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue()[0]));
        colOrden.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colRnc.setCellValueFactory(d   -> new SimpleStringProperty(d.getValue()[2]));
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[3]));

        FilteredList<String[]> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscar.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(row -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return row[0].contains(f)
                    || row[1].toLowerCase().contains(f)
                    || row[2].toLowerCase().contains(f);
            }));
        tablaRecepciones.setItems(listaFiltrada);

        tablaRecepciones.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) seleccionar();
        });

        cargarRecepciones();
    }

    private void cargarRecepciones() {
        String sql = "SELECT id_recepcion, ISNULL(numero_orden, ''), ISNULL(rnc_proveedor, ''), " +
                     "ISNULL(CONVERT(VARCHAR, fecha_recepcion, 105), '') " +
                     "FROM tbl_recepcion ORDER BY id_recepcion DESC";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new String[]{
                    String.valueOf(rs.getInt(1)),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4)
                });
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Ocurrio un error inesperado. Intente nuevamente.").showAndWait();
        }
    }

    @FXML
    private void seleccionar() {
        String[] sel = tablaRecepciones.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, "Selecciona una recepción de la lista.").showAndWait();
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
                popupBuscarRecepcionController.class.getResource("/vistasFinales/popupBuscarRecepcion.fxml"));
            Parent root = loader.load();
            popupBuscarRecepcionController ctrl = loader.getController();
            ctrl.setCallback(callback);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Buscar Recepción");
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Ocurrio un error inesperado. Intente nuevamente.").showAndWait();
        }
    }
}
