package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.clienteModelo;
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

public class consultaClientesController {

    @FXML private TableView<clienteModelo>           tablaClientes;
    @FXML private TableColumn<clienteModelo, Number> colId;
    @FXML private TableColumn<clienteModelo, String> colNombre;
    @FXML private TableColumn<clienteModelo, String> colApellido;
    @FXML private TableColumn<clienteModelo, String> colCedula;
    @FXML private TableColumn<clienteModelo, String> colEmail;
    @FXML private TableColumn<clienteModelo, String> colTelefono;
    @FXML private TableColumn<clienteModelo, String> colDireccion;
    @FXML private TableColumn<clienteModelo, String> colEstado;
    @FXML private TextField txtBuscarTabla;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<clienteModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d        -> d.getValue().idClienteProperty());
        colNombre.setCellValueFactory(d    -> d.getValue().nombreProperty());
        colApellido.setCellValueFactory(d  -> d.getValue().apellidoProperty());
        colCedula.setCellValueFactory(d    -> d.getValue().cedulaProperty());
        colEmail.setCellValueFactory(d     -> d.getValue().emailProperty());
        colTelefono.setCellValueFactory(d  -> d.getValue().telefonoProperty());
        colDireccion.setCellValueFactory(d -> d.getValue().direccionProperty());
        colEstado.setCellValueFactory(d    -> d.getValue().estadoProperty());

        tablaClientes.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(clienteModelo c, boolean empty) {
                super.updateItem(c, empty);
                if (c == null || empty) setStyle("");
                else if ("Inactivo".equalsIgnoreCase(c.getEstado()))
                    setStyle("-fx-background-color:#e0e0e0; -fx-text-fill:#888;");
                else setStyle("");
            }
        });

        FilteredList<clienteModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(c -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return c.getNombre().toLowerCase().contains(f)
                    || c.getApellido().toLowerCase().contains(f)
                    || c.getCedula().toLowerCase().contains(f)
                    || c.getEmail().toLowerCase().contains(f);
            }));
        tablaClientes.setItems(listaFiltrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarClientes();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarClientes() {
        List<clienteModelo> tmp = new ArrayList<>();
        String sql = "SELECT id_cliente, nombre, apellido, cedula, email, telefono, direccion, estado FROM tbl_cliente ORDER BY id_cliente";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) tmp.add(new clienteModelo(
                rs.getInt("id_cliente"), rs.getString("nombre"),
                rs.getString("apellido"), rs.getString("cedula"),
                rs.getString("email"), rs.getString("telefono"),
                rs.getString("direccion"), rs.getString("estado")));
        Platform.runLater(() -> lista.setAll(tmp));
    } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar clientes: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    // navegacion
    @FXML private void volverAlRegistro(javafx.event.ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaRegistroDeCliente.fxml", e); }
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
