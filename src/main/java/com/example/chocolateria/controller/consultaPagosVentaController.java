package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.ventaModelo;
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

public class consultaPagosVentaController {

    @FXML private TextField txtBuscarTabla;
    @FXML private TableView<ventaModelo>                    tablaVentas;
    @FXML private TableColumn<ventaModelo, Number>          colId;
    @FXML private TableColumn<ventaModelo, String>          colCliente;
    @FXML private TableColumn<ventaModelo, LocalDate>       colFecha;
    @FXML private TableColumn<ventaModelo, Number>          colSubtotal;
    @FXML private TableColumn<ventaModelo, Number>          colItbis;
    @FXML private TableColumn<ventaModelo, Number>          colDescuento;
    @FXML private TableColumn<ventaModelo, Number>          colTotal;
    @FXML private TableColumn<ventaModelo, Number>          colPagado;
    @FXML private TableColumn<ventaModelo, Number>          colBalance;
    @FXML private TableColumn<ventaModelo, String>          colEstado;
    @FXML private TableColumn<ventaModelo, String>          colNcf;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<ventaModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d        -> d.getValue().idVentaProperty());
        colCliente.setCellValueFactory(d   -> d.getValue().clienteProperty());
        colFecha.setCellValueFactory(d     -> d.getValue().fechaVentaProperty());
        colSubtotal.setCellValueFactory(d  -> d.getValue().subtotalProperty());
        colItbis.setCellValueFactory(d     -> d.getValue().itbisProperty());
        colDescuento.setCellValueFactory(d -> d.getValue().descuentoProperty());
        colTotal.setCellValueFactory(d     -> d.getValue().montoTotalProperty());
        colPagado.setCellValueFactory(d    -> d.getValue().montoPagadoProperty());
        colBalance.setCellValueFactory(d   -> d.getValue().balancePendienteProperty());
        colEstado.setCellValueFactory(d    -> d.getValue().estadoPagoProperty());
        colNcf.setCellValueFactory(d       -> d.getValue().ncfProperty());

        tablaVentas.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ventaModelo v, boolean empty) {
                super.updateItem(v, empty);
                if (v == null || empty) { setStyle(""); return; }
                switch (v.getEstadoPago()) {
                    case "Pagado"   -> setStyle("-fx-background-color:#e8f5e9;");
                    case "Parcial"  -> setStyle("-fx-background-color:#fff8e1;");
                    case "Pendiente"-> setStyle("-fx-background-color:#fde8e8;");
                    default         -> setStyle("");
                }
            }
        });

        FilteredList<ventaModelo> filtrada = new FilteredList<>(lista, p -> true);
        if (txtBuscarTabla != null) {
            txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
                filtrada.setPredicate(v -> {
                    if (nv == null || nv.isBlank()) return true;
                    String f = nv.toLowerCase();
                    return v.getCliente().toLowerCase().contains(f)
                        || v.getEstadoPago().toLowerCase().contains(f)
                        || v.getNcf().toLowerCase().contains(f);
                })
            );
        }
        tablaVentas.setItems(filtrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarVentas();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarVentas() {
        List<ventaModelo> tmp = new ArrayList<>();
        String sql = "SELECT v.id_venta, v.id_orden, v.fecha_venta, " +
                     "ISNULL(o.cliente,'') AS cliente, " +
                     "v.subtotal, v.descuento, v.itbis, v.monto_total, " +
                     "v.monto_pagado, v.balance_pendiente, v.tipo_pago, " +
                     "v.estado_pago, " +
                     "ISNULL(CAST(v.id_comprobante AS VARCHAR),'') AS ncf, " +
                     "ISNULL(v.id_empleado,0) AS id_empleado, " +
                     "ISNULL(e.nombre + ' ' + e.apellido,'') AS empleado " +
                     "FROM tbl_venta v " +
                     "LEFT JOIN tbl_orden_cliente o ON v.id_orden = o.id_orden " +
                     "LEFT JOIN tbl_empleado e ON v.id_empleado = e.id_empleado " +
                     "ORDER BY v.fecha_venta DESC";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date d = rs.getDate("fecha_venta");
                tmp.add(new ventaModelo(
                    rs.getInt("id_venta"),
                    rs.getInt("id_orden"),
                    d != null ? d.toLocalDate() : null,
                    rs.getString("cliente"),
                    rs.getDouble("subtotal"),
                    rs.getDouble("descuento"),
                    rs.getDouble("itbis"),
                    rs.getDouble("monto_total"),
                    rs.getDouble("monto_pagado"),
                    rs.getDouble("balance_pendiente"),
                    rs.getString("tipo_pago")    != null ? rs.getString("tipo_pago")    : "",
                    rs.getString("estado_pago")  != null ? rs.getString("estado_pago")  : "",
                    "",
                    rs.getString("ncf"),
                    rs.getInt("id_empleado"),
                    rs.getString("empleado")
                ));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar ventas: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    @FXML private void volverAlRegistro(javafx.event.ActionEvent e) { Navegacion.irA("/vistasFinales/vistaPagoVenta.fxml", e); }
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
