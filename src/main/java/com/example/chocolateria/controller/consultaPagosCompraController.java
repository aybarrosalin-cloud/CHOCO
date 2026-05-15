package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.deudaCompraModelo;
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
import java.time.LocalDate;

public class consultaPagosCompraController {

    @FXML private TextField txtBuscarTabla;
    @FXML private TableView<deudaCompraModelo>                    tablaDeudas;
    @FXML private TableColumn<deudaCompraModelo, Number>          colId;
    @FXML private TableColumn<deudaCompraModelo, String>          colOrden;
    @FXML private TableColumn<deudaCompraModelo, String>          colRnc;
    @FXML private TableColumn<deudaCompraModelo, LocalDate>       colFecha;
    @FXML private TableColumn<deudaCompraModelo, Number>          colTotal;
    @FXML private TableColumn<deudaCompraModelo, Number>          colPagado;
    @FXML private TableColumn<deudaCompraModelo, Number>          colPendiente;
    @FXML private TableColumn<deudaCompraModelo, String>          colEstado;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<deudaCompraModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d        -> d.getValue().idDeudaProperty());
        colOrden.setCellValueFactory(d     -> d.getValue().numeroOrdenProperty());
        colRnc.setCellValueFactory(d       -> d.getValue().rncSuplidorProperty());
        colFecha.setCellValueFactory(d     -> d.getValue().fechaDeudaProperty());
        colTotal.setCellValueFactory(d     -> d.getValue().montoTotalProperty());
        colPagado.setCellValueFactory(d    -> d.getValue().montoPagadoProperty());
        colPendiente.setCellValueFactory(d -> d.getValue().montoPendienteProperty());
        colEstado.setCellValueFactory(d    -> d.getValue().estadoProperty());

        tablaDeudas.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(deudaCompraModelo d, boolean empty) {
                super.updateItem(d, empty);
                if (d == null || empty) { setStyle(""); return; }
                switch (d.getEstado()) {
                    case "Saldado"   -> setStyle("-fx-background-color:#e8f5e9;");
                    case "Parcial"   -> setStyle("-fx-background-color:#fff8e1;");
                    case "Pendiente" -> setStyle("-fx-background-color:#fde8e8;");
                    default          -> setStyle("");
                }
            }
        });

        FilteredList<deudaCompraModelo> filtrada = new FilteredList<>(lista, p -> true);
        if (txtBuscarTabla != null) {
            txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
                filtrada.setPredicate(d -> {
                    if (nv == null || nv.isBlank()) return true;
                    String f = nv.toLowerCase();
                    return d.getRncSuplidor().toLowerCase().contains(f)
                        || d.getEstado().toLowerCase().contains(f)
                        || d.getNumeroOrden().toLowerCase().contains(f);
                })
            );
        }
        tablaDeudas.setItems(filtrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarDeudas();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarDeudas() {
        List<deudaCompraModelo> tmp = new ArrayList<>();
        String sql = "SELECT id_deuda, numero_orden, id_recepcion, rnc_suplidor, fecha_deuda, " +
                     "monto_total, monto_pagado, monto_pendiente, estado, " +
                     "ISNULL(observaciones,'') AS observaciones " +
                     "FROM tbl_deuda_compra ORDER BY fecha_deuda DESC";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date d = rs.getDate("fecha_deuda");
                tmp.add(new deudaCompraModelo(
                    rs.getInt("id_deuda"),
                    rs.getString("numero_orden")    != null ? rs.getString("numero_orden")  : "",
                    rs.getInt("id_recepcion"),
                    rs.getString("rnc_suplidor")    != null ? rs.getString("rnc_suplidor")  : "",
                    d != null ? d.toLocalDate() : null,
                    rs.getDouble("monto_total"),
                    rs.getDouble("monto_pagado"),
                    rs.getDouble("monto_pendiente"),
                    rs.getString("estado"),
                    rs.getString("observaciones")
                ));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar historial de pagos: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    @FXML private void volverAlRegistro(javafx.event.ActionEvent e) { Navegacion.irA("/vistasFinales/vistaPagoCompra.fxml", e); }
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
