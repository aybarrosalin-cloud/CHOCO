package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.clienteModelo;
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

public class popupBuscarClienteController {

    @FXML private TextField txtBuscar;
    @FXML private TableView<clienteModelo>           tablaClientes;
    @FXML private TableColumn<clienteModelo, Number> colId;
    @FXML private TableColumn<clienteModelo, String> colCedula;
    @FXML private TableColumn<clienteModelo, String> colNombre;

    private final ObservableList<clienteModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();
    private Consumer<clienteModelo> callback;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d     -> d.getValue().idClienteProperty());
        colCedula.setCellValueFactory(d -> d.getValue().cedulaProperty());
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getNombre() + " " + d.getValue().getApellido()));

        FilteredList<clienteModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscar.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(c -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return c.getNombre().toLowerCase().contains(f)
                    || c.getApellido().toLowerCase().contains(f)
                    || c.getCedula().toLowerCase().contains(f)
                    || String.valueOf(c.getIdCliente()).contains(f);
            }));
        tablaClientes.setItems(listaFiltrada);

        tablaClientes.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) seleccionar();
        });

        cargarClientes();
    }

    private void cargarClientes() {
        String sql = "SELECT id_cliente, nombre, apellido, cedula FROM tbl_cliente WHERE estado = 'Activo' ORDER BY nombre";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new clienteModelo(
                    rs.getInt("id_cliente"),
                    rs.getString("nombre"),
                    rs.getString("apellido"),
                    rs.getString("cedula"),
                    "", "", "", "Activo"));
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Ocurrio un error inesperado. Intente nuevamente.").showAndWait();
        }
    }

    @FXML
    private void seleccionar() {
        clienteModelo sel = tablaClientes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, "Selecciona un cliente de la lista.").showAndWait();
            return;
        }
        if (callback != null) callback.accept(sel);
        cerrar();
    }

    @FXML
    private void cerrar() {
        ((Stage) txtBuscar.getScene().getWindow()).close();
    }

    public void setCallback(Consumer<clienteModelo> callback) {
        this.callback = callback;
    }

    public static void mostrar(Consumer<clienteModelo> callback) {
        try {
            FXMLLoader loader = new FXMLLoader(
                popupBuscarClienteController.class.getResource("/vistasFinales/popupBuscarCliente.fxml"));
            Parent root = loader.load();
            popupBuscarClienteController ctrl = loader.getController();
            ctrl.setCallback(callback);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Buscar Cliente");
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Ocurrio un error inesperado. Intente nuevamente.").showAndWait();
        }
    }
}
