package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.reclamoModelo;
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

public class consultaReclamosController {

    @FXML private TableView<reclamoModelo>              tablaReclamos;
    @FXML private TableColumn<reclamoModelo, Number>   colId;
    @FXML private TableColumn<reclamoModelo, String>   colCliente;
    @FXML private TableColumn<reclamoModelo, Number>   colOrden;
    @FXML private TableColumn<reclamoModelo, String>   colTipo;
    @FXML private TableColumn<reclamoModelo, String>   colEstado;
    @FXML private TableColumn<reclamoModelo, String>   colPrioridad;
    @FXML private TableColumn<reclamoModelo, String>   colFecha;
    @FXML private TextField txtBuscarTabla;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<reclamoModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d       -> d.getValue().idReclamoProperty());
        colCliente.setCellValueFactory(d  -> d.getValue().clienteProperty());
        colOrden.setCellValueFactory(d    -> d.getValue().idOrdenProperty());
        colTipo.setCellValueFactory(d     -> d.getValue().tipoReclamoProperty());
        colEstado.setCellValueFactory(d   -> d.getValue().estadoProperty());
        colPrioridad.setCellValueFactory(d-> d.getValue().prioridadProperty());
        colFecha.setCellValueFactory(d    -> d.getValue().fechaReclamoProperty().asString());

        tablaReclamos.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(reclamoModelo r, boolean empty) {
                super.updateItem(r, empty);
                if (r == null || empty) { setStyle(""); return; }
                switch (r.getPrioridad()) {
                    case "Alta"   -> setStyle("-fx-background-color:#fde8e8;");
                    case "Media"  -> setStyle("-fx-background-color:#fff8e1;");
                    case "Baja"   -> setStyle("-fx-background-color:#e8f5e9;");
                    default       -> setStyle("");
                }
            }
        });

        FilteredList<reclamoModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(r -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return r.getCliente().toLowerCase().contains(f)
                    || r.getTipoReclamo().toLowerCase().contains(f)
                    || r.getEstado().toLowerCase().contains(f)
                    || r.getPrioridad().toLowerCase().contains(f);
            }));
        tablaReclamos.setItems(listaFiltrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarReclamos();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarReclamos() {
        List<reclamoModelo> tmp = new ArrayList<>();
        String sql = "SELECT r.id_reclamo, r.id_cliente, c.nombre + ' ' + c.apellido AS cliente, " +
                     "r.id_orden, r.tipo_reclamo, r.estado, r.prioridad, r.descripcion, r.fecha_reclamo " +
                     "FROM tbl_reclamo r " +
                     "LEFT JOIN tbl_cliente c ON r.id_cliente = c.id_cliente " +
                     "ORDER BY r.id_reclamo DESC";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date d = rs.getDate("fecha_reclamo");
                tmp.add(new reclamoModelo(
                    rs.getInt("id_reclamo"),
                    rs.getInt("id_cliente"),
                    rs.getString("cliente") != null ? rs.getString("cliente") : "",
                    rs.getInt("id_orden"),
                    rs.getString("tipo_reclamo") != null ? rs.getString("tipo_reclamo") : "",
                    rs.getString("estado") != null ? rs.getString("estado") : "",
                    rs.getString("prioridad") != null ? rs.getString("prioridad") : "",
                    rs.getString("descripcion") != null ? rs.getString("descripcion") : "",
                    d != null ? d.toLocalDate() : null,
                    0));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar reclamos: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    // navegacion
    @FXML private void volverAlRegistro(javafx.event.ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaGestionReclamos.fxml", e); }
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
