package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.solicitudProduccionModelo;
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

public class consultaSolicitudesProduccionController {

    @FXML private TableView<solicitudProduccionModelo>              tablaSolicitudes;
    @FXML private TableColumn<solicitudProduccionModelo, Number>   colId;
    @FXML private TableColumn<solicitudProduccionModelo, String>   colFechaSolicitud;
    @FXML private TableColumn<solicitudProduccionModelo, String>   colFechaProduccion;
    @FXML private TableColumn<solicitudProduccionModelo, String>   colResponsable;
    @FXML private TableColumn<solicitudProduccionModelo, String>   colPrioridad;
    @FXML private TableColumn<solicitudProduccionModelo, String>   colEstado;
    @FXML private TableColumn<solicitudProduccionModelo, String>   colProductos;
    @FXML private TextField txtBuscarTabla;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<solicitudProduccionModelo> lista = FXCollections.observableArrayList();
    private final Map<Integer, String> productosMap = new HashMap<>();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d              -> d.getValue().idProperty());
        colFechaSolicitud.setCellValueFactory(d  -> d.getValue().fechaSolicitudProperty().asString());
        colFechaProduccion.setCellValueFactory(d -> d.getValue().fechaProduccionProperty().asString());
        colResponsable.setCellValueFactory(d     -> d.getValue().responsableProperty());
        colPrioridad.setCellValueFactory(d       -> d.getValue().prioridadProperty());
        colEstado.setCellValueFactory(d          -> d.getValue().estadoProperty());
        colProductos.setCellValueFactory(d -> new SimpleStringProperty(
            productosMap.getOrDefault(d.getValue().getId(), "")));

        tablaSolicitudes.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(solicitudProduccionModelo s, boolean empty) {
                super.updateItem(s, empty);
                if (s == null || empty) { setStyle(""); return; }
                switch (s.getPrioridad()) {
                    case "Alta"  -> setStyle("-fx-background-color:#fde8e8;");
                    case "Media" -> setStyle("-fx-background-color:#fff8e1;");
                    case "Baja"  -> setStyle("-fx-background-color:#e8f5e9;");
                    default      -> setStyle("");
                }
            }
        });

        FilteredList<solicitudProduccionModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(s -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return s.getResponsable().toLowerCase().contains(f)
                    || s.getEstado().toLowerCase().contains(f)
                    || s.getPrioridad().toLowerCase().contains(f);
            }));
        tablaSolicitudes.setItems(listaFiltrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarProductosMap();
                cargarSolicitudes();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarProductosMap() {
        String sql = "SELECT id_solicitud, COUNT(*) AS total FROM tbl_solicitud_detalle GROUP BY id_solicitud";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                productosMap.put(rs.getInt("id_solicitud"),
                    rs.getInt("total") + " producto(s)");
            }
        } catch (Exception ignored) {}
    }

    private void cargarSolicitudes() {
        List<solicitudProduccionModelo> tmp = new ArrayList<>();
        String sql = "SELECT s.id_solicitud, s.fecha_solicitud, s.fecha_produccion, " +
                     "s.prioridad, s.estado, s.id_responsable, " +
                     "ISNULL(e.nombre + ' ' + e.apellido, '') AS responsable, " +
                     "ISNULL(s.observaciones, '') AS observaciones " +
                     "FROM tbl_solicitud_produccion s " +
                     "LEFT JOIN tbl_empleado e ON s.id_responsable = e.id_empleado " +
                     "ORDER BY s.id_solicitud DESC";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date dSol = rs.getDate("fecha_solicitud");
                Date dPro = rs.getDate("fecha_produccion");
                tmp.add(new solicitudProduccionModelo(
                    rs.getInt("id_solicitud"),
                    dSol != null ? dSol.toLocalDate() : null,
                    dPro != null ? dPro.toLocalDate() : null,
                    rs.getString("prioridad")    != null ? rs.getString("prioridad")    : "",
                    rs.getString("estado")       != null ? rs.getString("estado")       : "",
                    rs.getInt("id_responsable"),
                    rs.getString("responsable"),
                    rs.getString("observaciones")));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar solicitudes: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    // navegacion
    @FXML private void volverAlRegistro(javafx.event.ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaSolicitudDeProduccion.fxml", e); }
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
