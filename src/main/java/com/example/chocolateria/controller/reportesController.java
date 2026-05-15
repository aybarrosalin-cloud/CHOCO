package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.sql.*;

public class reportesController {

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    @FXML private Label     lblReporteActivo;
    @FXML private Label     lblTotal;
    @FXML private TableView<ObservableList<String>> tablaResultados;
    @FXML private TextField txtBuscar;

    @FXML private Button btnFacturacion;
    @FXML private Button btnDelivery;
    @FXML private Button btnReclamaciones;
    @FXML private Button btnPedido;

    private final conexion con = new conexion();
    private String sqlActual    = "";
    private String tituloActual = "";

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);
    }

    // reportes

    @FXML
    private void reporteFacturacion() {
        sqlActual = "SELECT v.id_venta AS [ID Factura], " +
                    "ISNULL(o.cliente,'') AS Cliente, " +
                    "v.fecha_venta AS [Fecha], " +
                    "v.subtotal AS Subtotal, " +
                    "v.descuento AS Descuento, " +
                    "v.itbis AS ITBIS, " +
                    "v.monto_total AS [Total], " +
                    "v.monto_pagado AS Pagado, " +
                    "v.balance_pendiente AS [Balance Pendiente], " +
                    "v.estado_pago AS [Estado Pago], " +
                    "v.tipo_pago AS [Tipo Pago], " +
                    "ISNULL(CAST(v.id_comprobante AS VARCHAR),'') AS NCF " +
                    "FROM tbl_venta v " +
                    "LEFT JOIN tbl_orden_cliente o ON v.id_orden = o.id_orden " +
                    "ORDER BY v.fecha_venta DESC";
        tituloActual = "Reporte de Facturación";
        resaltarBoton(btnFacturacion);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML
    private void reporteDelivery() {
        sqlActual = "SELECT e.id_envio AS ID, " +
                    "ISNULL(o.cliente,'') AS Cliente, " +
                    "e.direccion_entrega AS [Dirección Entrega], " +
                    "e.fecha_envio AS [Fecha Envio], " +
                    "e.fecha_entrega_estimada AS [Fecha Estimada], " +
                    "e.fecha_entrega_real AS [Fecha Real], " +
                    "e.metodo_envio AS [Método Envio], " +
                    "ISNULL(e.transportista,'') AS Transportista, " +
                    "e.costo_envio AS [Costo Envio], " +
                    "e.estado AS Estado, " +
                    "ISNULL(e.numero_tracking,'') AS Tracking " +
                    "FROM tbl_envio e " +
                    "LEFT JOIN tbl_orden_cliente o ON e.id_orden = o.id_orden " +
                    "ORDER BY e.fecha_envio DESC";
        tituloActual = "Reporte de Delivery y Entrega";
        resaltarBoton(btnDelivery);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML
    private void reporteReclamaciones() {
        sqlActual = "SELECT r.id_reclamo AS ID, " +
                    "ISNULL(c.nombre + ' ' + c.apellido,'') AS Cliente, " +
                    "r.tipo_reclamo AS [Tipo Reclamo], " +
                    "r.descripcion AS Descripción, " +
                    "r.fecha_reclamo AS [Fecha Reclamo], " +
                    "r.estado AS Estado, " +
                    "ISNULL(r.resolucion,'') AS Resolución, " +
                    "ISNULL(CONVERT(VARCHAR,r.fecha_resolucion,23),'Pendiente') AS [Fecha Resolución] " +
                    "FROM tbl_reclamo r " +
                    "LEFT JOIN tbl_cliente c ON r.id_cliente = c.id_cliente " +
                    "ORDER BY r.fecha_reclamo DESC";
        tituloActual = "Reporte de Reclamaciones";
        resaltarBoton(btnReclamaciones);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML
    private void reportePedido() {
        sqlActual = "SELECT o.id_orden AS ID, " +
                    "o.cliente AS Cliente, " +
                    "o.fecha_registro AS [Fecha Registro], " +
                    "o.fecha_entrega AS [Fecha Entrega], " +
                    "o.metodo_pago AS [Método Pago], " +
                    "o.estado AS Estado, " +
                    "o.observaciones AS Observaciones " +
                    "FROM tbl_orden_cliente o " +
                    "ORDER BY o.fecha_registro DESC";
        tituloActual = "Reporte de Pedidos";
        resaltarBoton(btnPedido);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    // busqueda

    @FXML
    private void ejecutarBusqueda() {
        String filtro = txtBuscar.getText().trim();
        if (sqlActual.isEmpty()) { mostrarAlerta("Selecciona un reporte primero."); return; }
        if (filtro.isEmpty())    { ejecutarConsulta(sqlActual, tituloActual); return; }

        ObservableList<ObservableList<String>> todos = FXCollections.observableArrayList();
        try (Connection conn = con.establecerConexion();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sqlActual)) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                ObservableList<String> fila = FXCollections.observableArrayList();
                boolean coincide = false;
                for (int i = 1; i <= cols; i++) {
                    String val = rs.getString(i) != null ? rs.getString(i) : "";
                    fila.add(val);
                    if (val.toLowerCase().contains(filtro.toLowerCase())) coincide = true;
                }
                if (coincide) todos.add(fila);
            }
        } catch (Exception e) { mostrarAlerta("Error al filtrar: " + "Ocurrió un error. Intente de nuevo."); return; }

        tablaResultados.setItems(todos);
        lblTotal.setText("Resultados filtrados: " + todos.size());
    }

    @FXML
    private void limpiarBusqueda() {
        txtBuscar.clear();
        if (!sqlActual.isEmpty()) ejecutarConsulta(sqlActual, tituloActual);
    }

    // motor generico

    private void ejecutarConsulta(String sql, String titulo) {
        tablaResultados.getColumns().clear();
        tablaResultados.getItems().clear();
        lblReporteActivo.setText(titulo);
        lblTotal.setText("");

        try (Connection conn = con.establecerConexion();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            for (int i = 1; i <= cols; i++) {
                final int idx = i - 1;
                TableColumn<ObservableList<String>, String> col =
                        new TableColumn<>(meta.getColumnLabel(i));
                col.setCellValueFactory(data -> new SimpleStringProperty(
                        data.getValue().size() > idx ? data.getValue().get(idx) : ""));
                col.setStyle("-fx-background-color:#48295a; -fx-text-fill:white; " +
                             "-fx-font-weight:bold; -fx-alignment:CENTER;");
                col.setMinWidth(90);
                col.setPrefWidth(140);
                tablaResultados.getColumns().add(col);
            }

            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
            while (rs.next()) {
                ObservableList<String> fila = FXCollections.observableArrayList();
                for (int i = 1; i <= cols; i++)
                    fila.add(rs.getString(i) != null ? rs.getString(i) : "");
                data.add(fila);
            }
            tablaResultados.setItems(data);
            lblTotal.setText("Total de registros: " + data.size());

        } catch (Exception e) {
            mostrarAlerta("Error al ejecutar el reporte: " + "Ocurrió un error. Intente de nuevo.");
        }
    }

    // resaltar boton activo

    private void resaltarBoton(Button activo) {
        String normal  = "-fx-background-color:#6d3c87; -fx-text-fill:white; " +
                         "-fx-font-weight:bold; -fx-background-radius:10;";
        String activos = "-fx-background-color:#f0e6ff; -fx-text-fill:#3B1A5C; " +
                         "-fx-font-weight:bold; -fx-background-radius:10; " +
                         "-fx-border-color:#6d3c87; -fx-border-width:2;";
        if (btnFacturacion   != null) btnFacturacion.setStyle(normal);
        if (btnDelivery      != null) btnDelivery.setStyle(normal);
        if (btnReclamaciones != null) btnReclamaciones.setStyle(normal);
        if (btnPedido        != null) btnPedido.setStyle(normal);
        if (activo           != null) activo.setStyle(activos);
    }

    private void mostrarAlerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Aviso"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // navegacion

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
    @FXML private void irAConsultas(javafx.event.ActionEvent e)           { Navegacion.irA("/vistasFinales/vistaConsultasGenerales.fxml", e); }
    @FXML private void salir(javafx.event.ActionEvent e)                  { Navegacion.salir(e); }
}
