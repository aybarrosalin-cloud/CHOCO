package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.ordenProduccionModelo;
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

public class consultaOrdenProduccionController {

    @FXML private TableView<ordenProduccionModelo>               tablaOrdenes;
    @FXML private TableColumn<ordenProduccionModelo, Number>     colId;
    @FXML private TableColumn<ordenProduccionModelo, String>     colTipo;
    @FXML private TableColumn<ordenProduccionModelo, String>     colCliente;
    @FXML private TableColumn<ordenProduccionModelo, LocalDate>  colFechaOrden;
    @FXML private TableColumn<ordenProduccionModelo, LocalDate>  colFechaInicio;
    @FXML private TableColumn<ordenProduccionModelo, LocalDate>  colFechaEntrega;
    @FXML private TableColumn<ordenProduccionModelo, String>     colResponsable;
    @FXML private TableColumn<ordenProduccionModelo, String>     colEstado;
    @FXML private TableColumn<ordenProduccionModelo, String>     colPrioridad;
    @FXML private TableColumn<ordenProduccionModelo, String>     colCategoria;
    @FXML private TextField txtBuscarTabla;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<ordenProduccionModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d           -> d.getValue().idOrdenProperty());
        colTipo.setCellValueFactory(d         -> d.getValue().tipoOrdenProperty());
        colCliente.setCellValueFactory(d      -> d.getValue().clienteProperty());
        colFechaOrden.setCellValueFactory(d   -> d.getValue().fechaOrdenProperty());
        colFechaInicio.setCellValueFactory(d  -> d.getValue().fechaInicioProperty());
        colFechaEntrega.setCellValueFactory(d -> d.getValue().fechaEntregaProperty());
        colResponsable.setCellValueFactory(d  -> d.getValue().responsableProperty());
        colEstado.setCellValueFactory(d       -> d.getValue().estadoProperty());
        colPrioridad.setCellValueFactory(d    -> d.getValue().prioridadProperty());
        colCategoria.setCellValueFactory(d    -> d.getValue().categoriaProperty());

        tablaOrdenes.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(ordenProduccionModelo o, boolean empty) {
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

        FilteredList<ordenProduccionModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(ord -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return ord.getResponsable().toLowerCase().contains(f)
                    || ord.getEstado().toLowerCase().contains(f)
                    || ord.getCliente().toLowerCase().contains(f)
                    || ord.getPrioridad().toLowerCase().contains(f);
            }));
        tablaOrdenes.setItems(listaFiltrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarOrdenes();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarOrdenes() {
        List<ordenProduccionModelo> tmp = new ArrayList<>();
        String sql = "SELECT o.id_orden, o.tipo_orden, o.fecha_inicio, o.fecha_orden, o.fecha_entrega, " +
                     "o.id_responsable, o.estado, o.prioridad, o.categoria, " +
                     "ISNULL(o.materiales,'') AS materiales, ISNULL(o.observaciones,'') AS observaciones, " +
                     "ISNULL(e.nombre+' '+e.apellido,'') AS responsable, " +
                     "ISNULL(c.nombre+' '+c.apellido,'') AS cliente " +
                     "FROM tbl_orden_produccion o " +
                     "LEFT JOIN tbl_empleado e ON o.id_responsable = e.id_empleado " +
                     "LEFT JOIN tbl_cliente  c ON o.id_cliente = c.id_cliente " +
                     "ORDER BY o.id_orden DESC";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date dI = rs.getDate("fecha_inicio");
                Date dO = rs.getDate("fecha_orden");
                Date dE = rs.getDate("fecha_entrega");
                tmp.add(new ordenProduccionModelo(
                    rs.getInt("id_orden"),
                    rs.getString("tipo_orden"),
                    dI != null ? dI.toLocalDate() : null,
                    dO != null ? dO.toLocalDate() : null,
                    dE != null ? dE.toLocalDate() : null,
                    rs.getInt("id_responsable"),
                    rs.getString("responsable"),
                    rs.getString("estado"),
                    rs.getString("prioridad"),
                    rs.getString("categoria"),
                    rs.getString("materiales"),
                    rs.getString("observaciones"),
                    rs.getString("cliente")));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar ordenes: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    // navegacion
    @FXML private void volverAlRegistro(javafx.event.ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaOrdenProduccion.fxml", e); }
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
