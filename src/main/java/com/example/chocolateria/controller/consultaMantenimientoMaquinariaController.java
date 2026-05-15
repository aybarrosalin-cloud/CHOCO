package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.mantenimientoMaquinariaModelo;
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

public class consultaMantenimientoMaquinariaController {

    @FXML private TableView<mantenimientoMaquinariaModelo>                    tablaMantenimientos;
    @FXML private TableColumn<mantenimientoMaquinariaModelo, Number>          colId;
    @FXML private TableColumn<mantenimientoMaquinariaModelo, LocalDate>       colFecha;
    @FXML private TableColumn<mantenimientoMaquinariaModelo, LocalDate>       colProximo;
    @FXML private TableColumn<mantenimientoMaquinariaModelo, String>          colMaquina;
    @FXML private TableColumn<mantenimientoMaquinariaModelo, String>          colTecnico;
    @FXML private TableColumn<mantenimientoMaquinariaModelo, Number>          colCosto;
    @FXML private TableColumn<mantenimientoMaquinariaModelo, String>          colEstado;
    @FXML private TableColumn<mantenimientoMaquinariaModelo, String>          colTipo;
    @FXML private TableColumn<mantenimientoMaquinariaModelo, String>          colObservaciones;
    @FXML private TextField txtBuscarTabla;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<mantenimientoMaquinariaModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d           -> d.getValue().idProperty());
        colFecha.setCellValueFactory(d        -> d.getValue().fechaMantenimientoProperty());
        colProximo.setCellValueFactory(d      -> d.getValue().fechaProximoMantenimientoProperty());
        colMaquina.setCellValueFactory(d      -> d.getValue().maquinaProperty());
        colTecnico.setCellValueFactory(d      -> d.getValue().tecnicoProperty());
        colCosto.setCellValueFactory(d        -> d.getValue().costoProperty());
        colEstado.setCellValueFactory(d       -> d.getValue().estadoMaquinaProperty());
        colTipo.setCellValueFactory(d         -> d.getValue().tipoMantenimientoProperty());
        colObservaciones.setCellValueFactory(d-> d.getValue().observacionesProperty());

        FilteredList<mantenimientoMaquinariaModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(m -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return m.getMaquina().toLowerCase().contains(f)
                    || m.getTecnico().toLowerCase().contains(f)
                    || m.getTipoMantenimiento().toLowerCase().contains(f)
                    || m.getEstadoMaquina().toLowerCase().contains(f);
            }));
        tablaMantenimientos.setItems(listaFiltrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarMantenimientos();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarMantenimientos() {
        List<mantenimientoMaquinariaModelo> tmp = new ArrayList<>();
        String sql = "SELECT id, fecha_mantenimiento, fecha_proximo_mantenimiento, maquina, tecnico, costo, " +
                     "estado_maquina, tipo_mantenimiento, observaciones " +
                     "FROM tbl_mantenimiento_maquinaria ORDER BY id DESC";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date dF = rs.getDate("fecha_mantenimiento");
                Date dP = rs.getDate("fecha_proximo_mantenimiento");
                tmp.add(new mantenimientoMaquinariaModelo(
                    rs.getInt("id"),
                    dF != null ? dF.toLocalDate() : null,
                    dP != null ? dP.toLocalDate() : null,
                    rs.getString("maquina"),
                    rs.getString("tecnico"),
                    rs.getDouble("costo"),
                    rs.getString("estado_maquina"),
                    rs.getString("tipo_mantenimiento"),
                    rs.getString("observaciones") != null ? rs.getString("observaciones") : ""));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar mantenimientos: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    // navegacion
    @FXML private void volverAlRegistro(javafx.event.ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaMantenimientoMaquinaria.fxml", e); }
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
