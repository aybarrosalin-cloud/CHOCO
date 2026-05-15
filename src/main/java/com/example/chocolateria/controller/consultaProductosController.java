package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.productoModelo;
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

public class consultaProductosController {

    @FXML private TableView<productoModelo>           tablaProductos;
    @FXML private TableColumn<productoModelo, String> colCodigo;
    @FXML private TableColumn<productoModelo, String> colNombre;
    @FXML private TableColumn<productoModelo, Number> colPrecioU;
    @FXML private TableColumn<productoModelo, Number> colPrecioM;
    @FXML private TableColumn<productoModelo, String> colUnidad;
    @FXML private TableColumn<productoModelo, String> colCategoria;
    @FXML private TableColumn<productoModelo, String> colTipo;
    @FXML private TableColumn<productoModelo, Number> colStock;
    @FXML private TextField txtBuscarTabla;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final ObservableList<productoModelo> lista = FXCollections.observableArrayList();
    private final conexion con = new conexion();

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colCodigo.setCellValueFactory(d    -> d.getValue().codigoProperty());
        colNombre.setCellValueFactory(d    -> d.getValue().nombreProperty());
        colPrecioU.setCellValueFactory(d   -> d.getValue().precioUnitarioProperty());
        colPrecioM.setCellValueFactory(d   -> d.getValue().precioMayorProperty());
        colUnidad.setCellValueFactory(d    -> d.getValue().unidadMedidaProperty());
        colCategoria.setCellValueFactory(d -> d.getValue().categoriaProperty());
        colTipo.setCellValueFactory(d      -> d.getValue().tipoProperty());
        colStock.setCellValueFactory(d     -> d.getValue().stockProperty());

        FilteredList<productoModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
        txtBuscarTabla.textProperty().addListener((obs, o, nv) ->
            listaFiltrada.setPredicate(p -> {
                if (nv == null || nv.isBlank()) return true;
                String f = nv.toLowerCase();
                return p.getCodigo().toLowerCase().contains(f)
                    || p.getNombre().toLowerCase().contains(f)
                    || p.getCategoria().toLowerCase().contains(f);
            }));
        tablaProductos.setItems(listaFiltrada);

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() throws Exception {
                cargarProductos();
                return null;
            }
        };
        new Thread(cargar).start();
    
    }

    private void cargarProductos() {
        List<productoModelo> tmp = new ArrayList<>();
        String sql = "SELECT codigo,nombre,precio_unitario,precio_mayor,descripcion,unidad_medida,categoria,tipo,stock FROM tbl_producto ORDER BY codigo";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) tmp.add(new productoModelo(
                rs.getString("codigo"), rs.getString("nombre"),
                rs.getDouble("precio_unitario"), rs.getDouble("precio_mayor"),
                rs.getString("descripcion") != null ? rs.getString("descripcion") : "",
                rs.getString("unidad_medida") != null ? rs.getString("unidad_medida") : "",
                rs.getString("categoria"), rs.getString("tipo"), rs.getInt("stock")));
        Platform.runLater(() -> lista.setAll(tmp));
    } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al cargar productos: " + "Ocurrió un error. Intente de nuevo.").showAndWait();
        }
    }

    // navegacion
    @FXML private void volverAlRegistro(javafx.event.ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaRegistroProducto.fxml", e); }
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
