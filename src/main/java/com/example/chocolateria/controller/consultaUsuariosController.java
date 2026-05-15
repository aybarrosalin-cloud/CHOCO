package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.usuarioModelo;
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

public class consultaUsuariosController {

    @FXML private TableView<usuarioModelo>              tablaUsuarios;
    @FXML private TableColumn<usuarioModelo, Number>   colId;
    @FXML private TableColumn<usuarioModelo, String>   colUsuario;
    @FXML private TableColumn<usuarioModelo, String>   colRol;
    @FXML private TableColumn<usuarioModelo, String>   colDepartamento;
    @FXML private TableColumn<usuarioModelo, String>   colEstado;
    @FXML private TextField txtBuscarTabla;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<usuarioModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colId.setCellValueFactory(d           -> d.getValue().idUsuarioProperty());
        colUsuario.setCellValueFactory(d      -> d.getValue().usuarioProperty());
        colRol.setCellValueFactory(d          -> d.getValue().rolProperty());
        colDepartamento.setCellValueFactory(d -> d.getValue().departamentoProperty());
        colEstado.setCellValueFactory(d       -> d.getValue().estadoProperty());

        tablaUsuarios.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(usuarioModelo u, boolean empty) {
                super.updateItem(u, empty);
                if (u == null || empty) setStyle("");
                else if ("Inactivo".equalsIgnoreCase(u.getEstado()))
                    setStyle("-fx-background-color:#e0e0e0; -fx-text-fill:#888;");
                else setStyle("");
            }
        });

        FilteredList<usuarioModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(u -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return u.getUsuario().toLowerCase().contains(f)
                    || u.getRol().toLowerCase().contains(f)
                    || u.getDepartamento().toLowerCase().contains(f);
            }));
        tablaUsuarios.setItems(listaFiltrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarUsuarios();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarUsuarios() {
        List<usuarioModelo> tmp = new ArrayList<>();
        String sql = "SELECT u.id_usuario, u.usuario, u.rol, u.estado, " +
                     "ISNULL(u.id_departamento, 0) AS id_departamento, " +
                     "ISNULL(d.nombre, '') AS departamento " +
                     "FROM tbl_usuario u " +
                     "LEFT JOIN tbl_departamento d ON u.id_departamento = d.id_departamento " +
                     "ORDER BY u.id_usuario";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) tmp.add(new usuarioModelo(
                rs.getInt("id_usuario"),
                rs.getString("usuario"),
                rs.getString("rol")         != null ? rs.getString("rol")         : "",
                rs.getString("estado")      != null ? rs.getString("estado")      : "",
                rs.getInt("id_departamento"),
                rs.getString("departamento")));
        Platform.runLater(() -> lista.setAll(tmp));
    } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar usuarios: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    // navegacion
    @FXML private void volverAlRegistro(javafx.event.ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaGestionUsuarios.fxml", e); }
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
