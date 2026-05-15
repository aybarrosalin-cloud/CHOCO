package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.recepcionModelo;
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

public class consultaRecepcionController {

    @FXML private TableView<recepcionModelo>                    tablaRecepciones;
    @FXML private TableColumn<recepcionModelo, Number>          colId;
    @FXML private TableColumn<recepcionModelo, String>          colNumeroOrden;
    @FXML private TableColumn<recepcionModelo, String>          colRnc;
    @FXML private TableColumn<recepcionModelo, LocalDate>       colFecha;
    @FXML private TableColumn<recepcionModelo, Number>          colMonto;
    @FXML private TableColumn<recepcionModelo, String>          colObservaciones;
    @FXML private TextField txtBuscarTabla;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<recepcionModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d           -> d.getValue().idRecepcionProperty());
        colNumeroOrden.setCellValueFactory(d  -> d.getValue().numeroOrdenProperty());
        colRnc.setCellValueFactory(d          -> d.getValue().rncProveedorProperty());
        colFecha.setCellValueFactory(d        -> d.getValue().fechaRecepcionProperty());
        colMonto.setCellValueFactory(d        -> d.getValue().montoTotalProperty());
        colObservaciones.setCellValueFactory(d-> d.getValue().observacionesProperty());

        FilteredList<recepcionModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(r -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return r.getRncProveedor().toLowerCase().contains(f)
                    || r.getNumeroOrden().toLowerCase().contains(f);
            }));
        tablaRecepciones.setItems(listaFiltrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarRecepciones();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarRecepciones() {
        List<recepcionModelo> tmp = new ArrayList<>();
        String sql = "SELECT id_recepcion, rnc_proveedor, numero_orden, fecha_recepcion, monto_total, observaciones, codigo_orden " +
                     "FROM tbl_recepcion ORDER BY id_recepcion DESC";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date d = rs.getDate("fecha_recepcion");
                tmp.add(new recepcionModelo(
                    rs.getInt("id_recepcion"),
                    rs.getString("rnc_proveedor") != null ? rs.getString("rnc_proveedor") : "",
                    rs.getString("numero_orden")  != null ? rs.getString("numero_orden")  : "",
                    rs.getInt("codigo_orden"),
                    d != null ? d.toLocalDate() : null,
                    rs.getDouble("monto_total"),
                    rs.getString("observaciones") != null ? rs.getString("observaciones") : ""));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar recepciones: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    // navegacion
    @FXML private void volverAlRegistro(javafx.event.ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaRecepcion.fxml", e); }
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
