package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.envioModelo;
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

public class consultaEnviosController {

    @FXML private TableView<envioModelo>           tablaEnvios;
    @FXML private TableColumn<envioModelo, Number>    colId;
    @FXML private TableColumn<envioModelo, String>    colCliente;
    @FXML private TableColumn<envioModelo, LocalDate> colFechaEnvio;
    @FXML private TableColumn<envioModelo, LocalDate> colFechaEntrega;
    @FXML private TableColumn<envioModelo, String>    colTransportista;
    @FXML private TableColumn<envioModelo, String>    colEstado;
    @FXML private TableColumn<envioModelo, String>    colGuia;
    @FXML private TableColumn<envioModelo, String>    colProvincia;
    @FXML private TableColumn<envioModelo, String>    colCiudad;
    @FXML private TextField txtBuscarTabla;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<envioModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d           -> d.getValue().idEnvioProperty());
        colCliente.setCellValueFactory(d      -> d.getValue().clienteProperty());
        colFechaEnvio.setCellValueFactory(d   -> d.getValue().fechaEnvioProperty());
        colFechaEntrega.setCellValueFactory(d -> d.getValue().fechaEntregaProperty());
        colTransportista.setCellValueFactory(d-> d.getValue().transportistaProperty());
        colEstado.setCellValueFactory(d       -> d.getValue().estadoProperty());
        colGuia.setCellValueFactory(d         -> d.getValue().numeroGuiaProperty());
        colProvincia.setCellValueFactory(d    -> d.getValue().provinciaProperty());
        colCiudad.setCellValueFactory(d       -> d.getValue().ciudadProperty());

        tablaEnvios.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(envioModelo e, boolean empty) {
                super.updateItem(e, empty);
                if (e == null || empty) { setStyle(""); return; }
                switch (e.getEstado()) {
                    case "Entregado"              -> setStyle("-fx-background-color:#e8f5e9;");
                    case "Cancelado","Devuelto"   -> setStyle("-fx-background-color:#fde8e8;");
                    case "En tránsito"            -> setStyle("-fx-background-color:#fff8e1;");
                    default                       -> setStyle("");
                }
            }
        });

        FilteredList<envioModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(e -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return e.getCliente().toLowerCase().contains(f)
                    || e.getEstado().toLowerCase().contains(f)
                    || e.getTransportista().toLowerCase().contains(f)
                    || e.getProvincia().toLowerCase().contains(f);
            }));
        tablaEnvios.setItems(listaFiltrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarEnvios();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarEnvios() {
        List<envioModelo> tmp = new ArrayList<>();
        String sql = "SELECT id_envio,id_cliente,cliente,fecha_envio,fecha_entrega,transportista,temperatura_transporte,estado,numero_guia,provincia,ciudad,direccion FROM tbl_envio ORDER BY id_envio DESC";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date dEnv = rs.getDate("fecha_envio");
                Date dEnt = rs.getDate("fecha_entrega");
                tmp.add(new envioModelo(
                    rs.getInt("id_envio"), rs.getInt("id_cliente"),
                    rs.getString("cliente"),
                    dEnv != null ? dEnv.toLocalDate() : null,
                    dEnt != null ? dEnt.toLocalDate() : null,
                    rs.getString("transportista"),
                    rs.getString("temperatura_transporte"),
                    rs.getString("estado"),
                    rs.getString("numero_guia") != null ? rs.getString("numero_guia") : "",
                    rs.getString("provincia"),
                    rs.getString("ciudad") != null ? rs.getString("ciudad") : "",
                    rs.getString("direccion")));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar envios: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    // navegacion
    @FXML private void volverAlRegistro(javafx.event.ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaGestionEnvios.fxml", e); }
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
