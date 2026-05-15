package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.suplidorModelo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class consultaSuplidoresController {

    @FXML private TextField txtBuscarTabla;
    @FXML private TableView<suplidorModelo>           tablaSuplidores;
    @FXML private TableColumn<suplidorModelo, Number> colId;
    @FXML private TableColumn<suplidorModelo, String> colNombre;
    @FXML private TableColumn<suplidorModelo, String> colApellido;
    @FXML private TableColumn<suplidorModelo, String> colRnc;
    @FXML private TableColumn<suplidorModelo, String> colTelefono;
    @FXML private TableColumn<suplidorModelo, String> colCorreo;
    @FXML private TableColumn<suplidorModelo, String> colCiudad;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<suplidorModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d       -> d.getValue().idSuplidorProperty());
        colNombre.setCellValueFactory(d   -> d.getValue().nombreProperty());
        colApellido.setCellValueFactory(d -> d.getValue().apellidoProperty());
        colRnc.setCellValueFactory(d      -> d.getValue().rncProperty());
        colTelefono.setCellValueFactory(d -> d.getValue().telefonoProperty());
        colCorreo.setCellValueFactory(d   -> d.getValue().correoProperty());
        colCiudad.setCellValueFactory(d   -> d.getValue().ciudadProperty());

        FilteredList<suplidorModelo> filtrada = new FilteredList<>(lista, p -> true);
        if (txtBuscarTabla != null) {
            txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
                filtrada.setPredicate(s -> {
                    if (nv == null || nv.isBlank()) return true;
                    String f = nv.toLowerCase();
                    return s.getNombre().toLowerCase().contains(f)
                        || s.getApellido().toLowerCase().contains(f)
                        || s.getRnc().toLowerCase().contains(f)
                        || s.getCiudad().toLowerCase().contains(f);
                })
            );
        }
        tablaSuplidores.setItems(filtrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarSuplidores();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarSuplidores() {
        List<suplidorModelo> tmp = new ArrayList<>();
        String sql = "SELECT id_suplidor, nombre, apellido, rnc, telefono, correo, ciudad " +
                     "FROM tbl_suplidor ORDER BY nombre";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                tmp.add(new suplidorModelo(
                    rs.getInt("id_suplidor"),
                    rs.getString("nombre"),
                    rs.getString("apellido"),
                    rs.getString("rnc"),
                    rs.getString("telefono") != null ? rs.getString("telefono") : "",
                    rs.getString("correo")   != null ? rs.getString("correo")   : "",
                    rs.getString("ciudad")   != null ? rs.getString("ciudad")   : ""
                ));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar suplidores: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    @FXML private void volverAlRegistro(javafx.event.ActionEvent e) { Navegacion.irA("/vistasFinales/vistaRegistroSuplidor.fxml", e); }
    @FXML private void irAInicio(javafx.event.ActionEvent e)              { Navegacion.irA("/vistasFinales/vistaInicio.fxml", e); }
    @FXML private void irAOrdenCliente(javafx.event.ActionEvent e)        { Navegacion.irA("/vistasFinales/vistaOrdenCliente.fxml", e); }
    @FXML private void irAPagoVenta(javafx.event.ActionEvent e)           { Navegacion.irA("/vistasFinales/vistaPagoVenta.fxml", e); }
    @FXML private void irAGestionEnvios(javafx.event.ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaGestionEnvios.fxml", e); }
    @FXML private void irAGestionReclamos(javafx.event.ActionEvent e)     { Navegacion.irA("/vistasFinales/vistaGestionReclamos.fxml", e); }
    @FXML private void irASolicitudProduccion(javafx.event.ActionEvent e) { Navegacion.irA("/vistasFinales/vistaSolicitudDeProduccion.fxml", e); }
    @FXML private void irAOrdenProduccion(javafx.event.ActionEvent e)     { Navegacion.irA("/vistasFinales/vistaOrdenProduccion.fxml", e); }
    @FXML private void irASalidaMateriales(javafx.event.ActionEvent e)    { Navegacion.irA("/vistasFinales/vistaSalidaMateriales.fxml", e); }
    @FXML private void irASalidaProductos(javafx.event.ActionEvent e)     { Navegacion.irA("/vistasFinales/vistaSalidaProductos.fxml", e); }
    @FXML private void irAOrdenProveedor(javafx.event.ActionEvent e)      { Navegacion.irA("/vistasFinales/vistaOrdenProveedor.fxml", e); }
    @FXML private void irAPagoCompra(javafx.event.ActionEvent e)          { Navegacion.irA("/vistasFinales/vistaPagoCompra.fxml", e); }
    @FXML private void irARegistroProducto(javafx.event.ActionEvent e)    { Navegacion.irA("/vistasFinales/vistaRegistroProducto.fxml", e); }
    @FXML private void irARegistroEmpleado(javafx.event.ActionEvent e)    { Navegacion.irA("/vistasFinales/vistaRegistroDeEmpleado.fxml", e); }
    @FXML private void irARegistroCliente(javafx.event.ActionEvent e)     { Navegacion.irA("/vistasFinales/vistaRegistroDeCliente.fxml", e); }
    @FXML private void irARegistroSuplidor(javafx.event.ActionEvent e)    { Navegacion.irA("/vistasFinales/vistaRegistroSuplidor.fxml", e); }
    @FXML private void irARegistroMaquinaria(javafx.event.ActionEvent e)  { Navegacion.irA("/vistasFinales/vistaRegistroMaquinaria.fxml", e); }
    @FXML private void irAReportesVentas(javafx.event.ActionEvent e)      { Navegacion.irA("/vistasFinales/vistaReportes.fxml", e); }
    @FXML private void irAReportesCompras(javafx.event.ActionEvent e)     { Navegacion.irA("/vistasFinales/vistaReportes.fxml", e); }
    @FXML private void irAReportesInventario(javafx.event.ActionEvent e)  { Navegacion.irA("/vistasFinales/vistaReportes.fxml", e); }
    @FXML private void irAReportesProduccion(javafx.event.ActionEvent e)  { Navegacion.irA("/vistasFinales/vistaReportes.fxml", e); }
    @FXML private void irAMantenimientoMaquinaria(javafx.event.ActionEvent e) { Navegacion.irA("/vistasFinales/vistaMantenimientoMaquinaria.fxml", e); }
    @FXML private void irAConsultas(javafx.event.ActionEvent e)           { Navegacion.irA("/vistasFinales/vistaReportes.fxml", e); }
    @FXML private void salir(javafx.event.ActionEvent e)                  { Navegacion.salir(e); }
}
