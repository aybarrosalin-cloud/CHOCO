package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.ordenProveedorModelo;
import javafx.beans.property.SimpleStringProperty;
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
import java.util.HashMap;
import java.util.Map;

public class consultaOrdenProveedorController {

    @FXML private TableView<ordenProveedorModelo>               tablaOrdenes;
    @FXML private TableColumn<ordenProveedorModelo, Number>     colId;
    @FXML private TableColumn<ordenProveedorModelo, String>     colRnc;
    @FXML private TableColumn<ordenProveedorModelo, String>     colProveedor;
    @FXML private TableColumn<ordenProveedorModelo, LocalDate>  colFecha;
    @FXML private TableColumn<ordenProveedorModelo, String>     colPrioridad;
    @FXML private TableColumn<ordenProveedorModelo, String>     colCategoria;
    @FXML private TableColumn<ordenProveedorModelo, Number>     colMonto;
    @FXML private TableColumn<ordenProveedorModelo, String>     colEstadoPago;
    @FXML private TableColumn<ordenProveedorModelo, String>     colProductos;
    @FXML private TextField txtBuscarTabla;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<ordenProveedorModelo> lista = FXCollections.observableArrayList();
    private final Map<Integer, String> productosMap = new HashMap<>();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d          -> d.getValue().codigoProperty());
        colRnc.setCellValueFactory(d         -> d.getValue().rncProveedorProperty());
        colProveedor.setCellValueFactory(d   -> d.getValue().proveedorProperty());
        colFecha.setCellValueFactory(d       -> d.getValue().fechaRequeridaProperty());
        colPrioridad.setCellValueFactory(d   -> d.getValue().prioridadProperty());
        colCategoria.setCellValueFactory(d   -> d.getValue().categoriaProperty());
        colMonto.setCellValueFactory(d       -> d.getValue().montoTotalProperty());
        colEstadoPago.setCellValueFactory(d  -> d.getValue().estadoPagoProperty());
        colProductos.setCellValueFactory(d   -> new SimpleStringProperty(
            productosMap.getOrDefault(d.getValue().getCodigo(), "")));

        tablaOrdenes.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(ordenProveedorModelo o, boolean empty) {
                super.updateItem(o, empty);
                if (o == null || empty) { setStyle(""); return; }
                switch (o.getPrioridad()) {
                    case "Alta"  -> setStyle("-fx-background-color:#fde8e8;");
                    case "Media" -> setStyle("-fx-background-color:#fff8e1;");
                    case "Baja"  -> setStyle("-fx-background-color:#e8f5e9;");
                    default      -> setStyle("");
                }
            }
        });

        FilteredList<ordenProveedorModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(ord -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return ord.getRncProveedor().toLowerCase().contains(f)
                    || ord.getProveedor().toLowerCase().contains(f)
                    || ord.getEstadoPago().toLowerCase().contains(f)
                    || ord.getPrioridad().toLowerCase().contains(f)
                    || ord.getCategoria().toLowerCase().contains(f);
            }));
        tablaOrdenes.setItems(listaFiltrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarProductosMap();
                cargarOrdenes();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarProductosMap() {
        String sql = "SELECT id_orden, COUNT(*) AS total FROM tbl_orden_proveedor_detalle GROUP BY id_orden";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                productosMap.put(rs.getInt("id_orden"), rs.getInt("total") + " producto(s)");
            }
        } catch (Exception ignored) {}
    }

    private void cargarOrdenes() {
        List<ordenProveedorModelo> tmp = new ArrayList<>();
        String sql = "SELECT codigo, rnc_proveedor, ISNULL(proveedor,'') AS proveedor, " +
                     "fecha_requerida, prioridad, categoria, estado_pago, monto_total, " +
                     "ISNULL(descripcion,'') AS descripcion " +
                     "FROM tbl_orden_proveedor ORDER BY codigo DESC";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date d = rs.getDate("fecha_requerida");
                tmp.add(new ordenProveedorModelo(
                    rs.getInt("codigo"),
                    rs.getString("rnc_proveedor") != null ? rs.getString("rnc_proveedor") : "",
                    rs.getString("proveedor"),
                    d != null ? d.toLocalDate() : null,
                    rs.getString("prioridad")  != null ? rs.getString("prioridad")  : "",
                    rs.getString("categoria")  != null ? rs.getString("categoria")  : "",
                    rs.getString("estado_pago")!= null ? rs.getString("estado_pago"): "",
                    rs.getDouble("monto_total"),
                    rs.getString("descripcion")));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar órdenes: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    // navegacion
    @FXML private void volverAlRegistro(javafx.event.ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaOrdenProveedor.fxml", e); }
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
