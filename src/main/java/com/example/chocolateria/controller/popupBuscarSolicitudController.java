package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;
import java.util.function.Consumer;

public class popupBuscarSolicitudController {

    @FXML private TextField txtBuscar;
    @FXML private TableView<String[]> tablaSolicitudes;
    @FXML private TableColumn<String[], String> colId;
    @FXML private TableColumn<String[], String> colFecha;
    @FXML private TableColumn<String[], String> colObs;

    private Consumer<String> callback;
    private final ObservableList<String[]> datos = FXCollections.observableArrayList();
    private FilteredList<String[]> filtrados;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        colObs.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));

        filtrados = new FilteredList<>(datos, p -> true);
        tablaSolicitudes.setItems(filtrados);

        txtBuscar.textProperty().addListener((obs, old, nv) -> {
            String q = nv == null ? "" : nv.toLowerCase();
            filtrados.setPredicate(row ->
                    q.isEmpty() || row[0].contains(q) || row[2].toLowerCase().contains(q));
        });

        cargarDatos();
    }

    private void cargarDatos() {
        conexion con = new conexion();
        String sql = "SELECT ISNULL(CAST(id_solicitud AS VARCHAR),''), " +
                     "ISNULL(CONVERT(VARCHAR,fecha_solicitud,105),''), " +
                     "ISNULL(observaciones,'') " +
                     "FROM tbl_solicitud_produccion ORDER BY id_solicitud DESC";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                datos.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
            }
        } catch (Exception e) {
            // silently ignored — table shows empty on error
        }
    }

    @FXML
    private void seleccionar() {
        String[] fila = tablaSolicitudes.getSelectionModel().getSelectedItem();
        if (fila != null) {
            callback.accept(fila[0]);
            ((Stage) tablaSolicitudes.getScene().getWindow()).close();
        }
    }

    @FXML
    private void cerrar() {
        ((Stage) tablaSolicitudes.getScene().getWindow()).close();
    }

    public static void mostrar(Consumer<String> callback) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    popupBuscarSolicitudController.class.getResource(
                            "/vistasFinales/popupBuscarSolicitud.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Buscar Solicitud de Producción");
            stage.setScene(new Scene(loader.load()));
            popupBuscarSolicitudController ctrl = loader.getController();
            ctrl.callback = callback;
            stage.showAndWait();
        } catch (Exception e) {
            // popup failed to open
        }
    }
}
