package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.maquinariaModelo;
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

public class consultaMaquinariasController {

    @FXML private TextField txtBuscarTabla;
    @FXML private TableView<maquinariaModelo>                    tablaMaquinarias;
    @FXML private TableColumn<maquinariaModelo, Number>          colId;
    @FXML private TableColumn<maquinariaModelo, String>          colNombre;
    @FXML private TableColumn<maquinariaModelo, String>          colTipo;
    @FXML private TableColumn<maquinariaModelo, String>          colMarca;
    @FXML private TableColumn<maquinariaModelo, String>          colSerie;
    @FXML private TableColumn<maquinariaModelo, LocalDate>       colFecha;
    @FXML private TableColumn<maquinariaModelo, String>          colEstado;
    @FXML private TableColumn<maquinariaModelo, String>          colResponsable;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<maquinariaModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d          -> d.getValue().idMaquinariaProperty());
        colNombre.setCellValueFactory(d      -> d.getValue().nombreProperty());
        colTipo.setCellValueFactory(d        -> d.getValue().tipoProperty());
        colMarca.setCellValueFactory(d       -> d.getValue().marcaModeloProperty());
        colSerie.setCellValueFactory(d       -> d.getValue().numeroSerieProperty());
        colFecha.setCellValueFactory(d       -> d.getValue().fechaAdquisicionProperty());
        colEstado.setCellValueFactory(d      -> d.getValue().estadoProperty());
        colResponsable.setCellValueFactory(d -> d.getValue().responsableProperty());

        tablaMaquinarias.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(maquinariaModelo m, boolean empty) {
                super.updateItem(m, empty);
                if (m == null || empty) { setStyle(""); return; }
                switch (m.getEstado()) {
                    case "Activo"            -> setStyle("-fx-background-color:#e8f5e9;");
                    case "En mantenimiento"  -> setStyle("-fx-background-color:#fff8e1;");
                    case "Fuera de servicio" -> setStyle("-fx-background-color:#fde8e8;");
                    case "Retirado"          -> setStyle("-fx-background-color:#eeeeee;");
                    default                  -> setStyle("");
                }
            }
        });

        FilteredList<maquinariaModelo> filtrada = new FilteredList<>(lista, p -> true);
        if (txtBuscarTabla != null) {
            txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
                filtrada.setPredicate(m -> {
                    if (nv == null || nv.isBlank()) return true;
                    String f = nv.toLowerCase();
                    return m.getNombre().toLowerCase().contains(f)
                        || m.getTipo().toLowerCase().contains(f)
                        || m.getEstado().toLowerCase().contains(f)
                        || m.getResponsable().toLowerCase().contains(f);
                })
            );
        }
        tablaMaquinarias.setItems(filtrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarMaquinarias();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarMaquinarias() {
        List<maquinariaModelo> tmp = new ArrayList<>();
        String sql = "SELECT id_maquinaria, nombre, tipo, marca_modelo, numero_serie, " +
                     "fecha_adquisicion, estado, id_responsable, responsable " +
                     "FROM tbl_maquinaria ORDER BY nombre";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date d = rs.getDate("fecha_adquisicion");
                tmp.add(new maquinariaModelo(
                    rs.getInt("id_maquinaria"),
                    rs.getString("nombre"),
                    rs.getString("tipo"),
                    rs.getString("marca_modelo")  != null ? rs.getString("marca_modelo")  : "",
                    rs.getString("numero_serie")  != null ? rs.getString("numero_serie")  : "",
                    d != null ? d.toLocalDate() : null,
                    rs.getString("estado"),
                    rs.getInt("id_responsable"),
                    rs.getString("responsable")   != null ? rs.getString("responsable")   : ""
                ));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar maquinarias: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    @FXML private void volverAlRegistro(javafx.event.ActionEvent e) { Navegacion.irA("/vistasFinales/vistaRegistroMaquinaria.fxml", e); }
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
