package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.ordenClienteModelo;
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

public class consultaOrdenClienteController {

    @FXML private TableView<ordenClienteModelo>               tablaOrdenes;
    @FXML private TableColumn<ordenClienteModelo, Number>     colId;
    @FXML private TableColumn<ordenClienteModelo, String>     colCliente;
    @FXML private TableColumn<ordenClienteModelo, LocalDate>  colFechaReg;
    @FXML private TableColumn<ordenClienteModelo, LocalDate>  colFechaEnt;
    @FXML private TableColumn<ordenClienteModelo, String>     colEstado;
    @FXML private TableColumn<ordenClienteModelo, String>     colMetodoPago;
    @FXML private TableColumn<ordenClienteModelo, String>     colProductos;
    @FXML private TextField txtBuscarTabla;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<ordenClienteModelo> lista = FXCollections.observableArrayList();
    private final Map<Integer, String> productosMap = new HashMap<>();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d          -> d.getValue().idOrdenProperty());
        colCliente.setCellValueFactory(d     -> d.getValue().clienteProperty());
        colFechaReg.setCellValueFactory(d    -> d.getValue().fechaRegistroProperty());
        colFechaEnt.setCellValueFactory(d    -> d.getValue().fechaEntregaProperty());
        colEstado.setCellValueFactory(d      -> d.getValue().estadoProperty());
        colMetodoPago.setCellValueFactory(d  -> d.getValue().metodoPagoProperty());
        colProductos.setCellValueFactory(d   -> new SimpleStringProperty(
            productosMap.getOrDefault(d.getValue().getIdOrden(), "")));

        tablaOrdenes.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(ordenClienteModelo o, boolean empty) {
                super.updateItem(o, empty);
                if (o == null || empty) { setStyle(""); return; }
                switch (o.getEstado()) {
                    case "Entregado"  -> setStyle("-fx-background-color:#e8f5e9;");
                    case "Cancelado"  -> setStyle("-fx-background-color:#fde8e8;");
                    case "Pendiente"  -> setStyle("-fx-background-color:#fff8e1;");
                    default           -> setStyle("");
                }
            }
        });

        FilteredList<ordenClienteModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(ord -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return ord.getCliente().toLowerCase().contains(f)
                    || ord.getEstado().toLowerCase().contains(f)
                    || ord.getMetodoPago().toLowerCase().contains(f);
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
        String sql = "SELECT id_orden, COUNT(*) AS total FROM tbl_orden_detalle GROUP BY id_orden";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                productosMap.put(rs.getInt("id_orden"), rs.getInt("total") + " producto(s)");
            }
        } catch (Exception ignored) {}
    }

    private void cargarOrdenes() {
        List<ordenClienteModelo> tmp = new ArrayList<>();
        String sql = "SELECT id_orden, id_cliente, cliente, fecha_registro, fecha_entrega, metodo_pago, estado, " +
                     "ISNULL(observaciones,'') AS observaciones " +
                     "FROM tbl_orden_cliente ORDER BY id_orden DESC";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date dR = rs.getDate("fecha_registro");
                Date dE = rs.getDate("fecha_entrega");
                tmp.add(new ordenClienteModelo(
                    rs.getInt("id_orden"), rs.getInt("id_cliente"),
                    rs.getString("cliente"),
                    dR != null ? dR.toLocalDate() : null,
                    dE != null ? dE.toLocalDate() : null,
                    rs.getString("metodo_pago"),
                    rs.getString("estado"),
                    rs.getString("observaciones"),
                    ""));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar ordenes: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    // navegacion
    @FXML private void volverAlRegistro(javafx.event.ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaOrdenCliente.fxml", e); }
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
