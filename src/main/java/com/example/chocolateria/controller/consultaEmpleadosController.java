package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.empleadoModelo;
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

public class consultaEmpleadosController {

    @FXML private TextField txtBuscarTabla;
    @FXML private TableView<empleadoModelo>           tablaEmpleados;
    @FXML private TableColumn<empleadoModelo, Number> colId;
    @FXML private TableColumn<empleadoModelo, String> colNombre;
    @FXML private TableColumn<empleadoModelo, String> colApellido;
    @FXML private TableColumn<empleadoModelo, String> colCedula;
    @FXML private TableColumn<empleadoModelo, String> colTelefono;
    @FXML private TableColumn<empleadoModelo, String> colTipoEmpleado;
    @FXML private TableColumn<empleadoModelo, String> colRol;
    @FXML private TableColumn<empleadoModelo, String> colEstado;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<empleadoModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d           -> d.getValue().idEmpleadoProperty());
        colNombre.setCellValueFactory(d       -> d.getValue().nombreProperty());
        colApellido.setCellValueFactory(d     -> d.getValue().apellidoProperty());
        colCedula.setCellValueFactory(d       -> d.getValue().cedulaProperty());
        colTelefono.setCellValueFactory(d     -> d.getValue().telefonoProperty());
        colTipoEmpleado.setCellValueFactory(d -> d.getValue().tipoEmpleadoProperty());
        colRol.setCellValueFactory(d          -> d.getValue().rolProperty());
        colEstado.setCellValueFactory(d       -> d.getValue().estadoProperty());

        tablaEmpleados.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(empleadoModelo e, boolean empty) {
                super.updateItem(e, empty);
                if (e == null || empty) setStyle("");
                else if ("Inactivo".equalsIgnoreCase(e.getEstado()))
                    setStyle("-fx-background-color:#e0e0e0; -fx-text-fill:#888;");
                else setStyle("");
            }
        });

        FilteredList<empleadoModelo> filtrada = new FilteredList<>(lista, p -> true);
        if (txtBuscarTabla != null) {
            txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
                filtrada.setPredicate(e -> {
                    if (nv == null || nv.isBlank()) return true;
                    String f = nv.toLowerCase();
                    return e.getNombre().toLowerCase().contains(f)
                        || e.getApellido().toLowerCase().contains(f)
                        || e.getCedula().toLowerCase().contains(f)
                        || e.getRol().toLowerCase().contains(f);
                })
            );
        }
        tablaEmpleados.setItems(filtrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarEmpleados();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarEmpleados() {
        List<empleadoModelo> tmp = new ArrayList<>();
        String sql = "SELECT id_empleado, nombre, apellido, cedula, telefono, tipo_empleado, rol, estado " +
                     "FROM tbl_empleado ORDER BY nombre";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                tmp.add(new empleadoModelo(
                    rs.getInt("id_empleado"),
                    rs.getString("nombre"),
                    rs.getString("apellido"),
                    rs.getString("cedula"),
                    rs.getString("telefono"),
                    rs.getString("tipo_empleado"),
                    rs.getString("rol"),
                    rs.getString("estado")
                ));
        Platform.runLater(() -> lista.setAll(tmp));
    }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar empleados: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    @FXML private void volverAlRegistro(javafx.event.ActionEvent e) { Navegacion.irA("/vistasFinales/vistaRegistroDeEmpleado.fxml", e); }
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
