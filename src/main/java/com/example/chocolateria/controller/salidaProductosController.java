package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.salidaProductosDetalleModelo;
import com.example.chocolateria.modelo.salidaProductosModelo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.sql.*;
import java.time.LocalDate;

public class salidaProductosController {

    @FXML private TextField  txtIdSalida;
    @FXML private TextField  txtIdOrdenCliente;
    @FXML private DatePicker dpFechaSalida;
    @FXML private TextField  txtCliente;
    @FXML private TextField  txtResponsable;
    @FXML private TextArea   txtObservaciones;

    @FXML private TextField txtCodigoDetalle;
    @FXML private Label     lblNombreProducto;
    @FXML private TextField txtCantidadDetalle;
    @FXML private TextField txtPrecioDetalle;

    @FXML private TableView<salidaProductosDetalleModelo>           tablaDetalle;
    @FXML private TableColumn<salidaProductosDetalleModelo, String> colDetCodigo;
    @FXML private TableColumn<salidaProductosDetalleModelo, String> colDetProducto;
    @FXML private TableColumn<salidaProductosDetalleModelo, Number> colDetCantidad;
    @FXML private TableColumn<salidaProductosDetalleModelo, Number> colDetPrecio;
    @FXML private TableColumn<salidaProductosDetalleModelo, Number> colDetTotal;

    private final ObservableList<salidaProductosDetalleModelo> listaDetalle = FXCollections.observableArrayList();
    private final conexion con = new conexion();
    private String productoSeleccionado = "";
    private salidaProductosModelo salidaCargada = null;

    @FXML private Label lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    @FXML private Button btnBuscar, btnLimpiar;
    @FXML private Button btnGuardar;
    @FXML private Button btnEliminar;

    @FXML
    public void initialize() {
        actualizarBotones(0);
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        colDetCodigo.setCellValueFactory(d   -> d.getValue().codigoProductoProperty());
        colDetProducto.setCellValueFactory(d -> d.getValue().productoProperty());
        colDetCantidad.setCellValueFactory(d -> d.getValue().cantidadProperty());
        colDetPrecio.setCellValueFactory(d   -> d.getValue().precioUnitarioProperty());
        colDetTotal.setCellValueFactory(d    -> d.getValue().totalProperty());
        tablaDetalle.setItems(listaDetalle);

        generarSiguienteId();
    }

    @FXML
    private void abrirBuscadorOrdenCliente() {
        popupBuscarOrdenClienteController.mostrar(id -> {
            txtIdOrdenCliente.setText(id);
            cargarDesdeOrden();
        });
    }

    @FXML
    private void abrirBuscadorProducto() {
        popupBuscarProductoController.mostrar(codigo -> {
            txtCodigoDetalle.setText(codigo);
            buscarProductoDetalle();
        });
    }

    @FXML
    private void buscarProductoDetalle() {
        String codigo = txtCodigoDetalle.getText().trim();
        if (codigo.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "atencion", "escribe el codigo del producto.");
            return;
        }
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT nombre, precio_unitario FROM tbl_producto WHERE codigo = ?")) {
            ps.setString(1, codigo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                productoSeleccionado = rs.getString("nombre");
                lblNombreProducto.setText(productoSeleccionado);
                String precio = rs.getString("precio_unitario");
                if (precio != null && !precio.isBlank()) {
                    txtPrecioDetalle.setText(precio);
                }
            } else {
                productoSeleccionado = "";
                lblNombreProducto.setText("no encontrado");
                mostrarAlerta(Alert.AlertType.WARNING, "no encontrado",
                        "no existe un producto con el codigo " + codigo + ".");
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void cargarDesdeOrden() {
        String idTexto = txtIdOrdenCliente.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "atencion", "escribe el id de la orden de cliente.");
            return;
        }
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT oc.id_orden, oc.cliente FROM tbl_orden_cliente oc WHERE oc.id_orden = ?")) {
            ps.setString(1, idTexto);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                txtCliente.setText(rs.getString("cliente") != null ? rs.getString("cliente") : "");
                dpFechaSalida.setValue(LocalDate.now());
                mostrarAlerta(Alert.AlertType.INFORMATION, "orden cargada",
                        "orden #" + idTexto + " encontrada. agrega los productos en el detalle.");
            } else {
                mostrarAlerta(Alert.AlertType.WARNING, "no encontrado",
                        "no se encontro ninguna orden con ese id.");
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void agregarProducto() {
        if (productoSeleccionado.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "atencion", "busca un producto por codigo antes de agregar.");
            return;
        }
        if (txtCantidadDetalle.getText().trim().isEmpty() || txtPrecioDetalle.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "atencion", "ingresa cantidad y precio.");
            return;
        }
        try {
            int    cantidad = Integer.parseInt(txtCantidadDetalle.getText().trim());
            double precio   = Double.parseDouble(txtPrecioDetalle.getText().trim());

            if (cantidad <= 0 || precio <= 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "valores invalidos", "cantidad y precio deben ser mayores a 0.");
                return;
            }

            String codigo = txtCodigoDetalle.getText().trim();
            double total  = cantidad * precio;

            for (salidaProductosDetalleModelo d : listaDetalle) {
                if (d.getCodigoProducto().equals(codigo)) {
                    mostrarAlerta(Alert.AlertType.WARNING, "duplicado", "este producto ya esta en la lista.");
                    return;
                }
            }

            listaDetalle.add(new salidaProductosDetalleModelo(0, 0, codigo, productoSeleccionado, cantidad, precio, total));
            recalcularTotal();

            txtCodigoDetalle.clear();
            lblNombreProducto.setText("");
            txtCantidadDetalle.clear();
            txtPrecioDetalle.clear();
            productoSeleccionado = "";

        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "valores invalidos", "cantidad debe ser entero y precio numero valido.");
        }
    }

    @FXML
    private void quitarProducto() {
        salidaProductosDetalleModelo sel = tablaDetalle.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "atencion", "selecciona un producto para quitarlo.");
            return;
        }
        listaDetalle.remove(sel);
        recalcularTotal();
    }

    private Label lblTotal; // not used, but kept for pattern

    private void recalcularTotal() {
        // no total field in this screen, but kept for consistency
    }

    @FXML
    private void guardarSalida() {
        if (estadoActual == 1) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "accion no disponible",
                    "ya hay un registro cargado. usa 'editar' para modificarlo o 'limpiar' para crear uno nuevo.");
            return;
        }
        if (!validarCampos()) return;

        double totalGeneral = listaDetalle.stream().mapToDouble(salidaProductosDetalleModelo::getTotal).sum();

        String sqlMaestro = "INSERT INTO tbl_salida_productos (id_orden_cliente, cliente, fecha_salida, responsable, observaciones, total) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sqlMaestro, Statement.RETURN_GENERATED_KEYS)) {

            String idOrd = txtIdOrdenCliente.getText().trim();
            ps.setInt(1, idOrd.isEmpty() ? 0 : Integer.parseInt(idOrd));
            ps.setString(2, txtCliente.getText().trim());
            ps.setDate(3, Date.valueOf(dpFechaSalida.getValue()));
            ps.setString(4, txtResponsable.getText().trim());
            ps.setString(5, txtObservaciones.getText().trim());
            ps.setDouble(6, totalGeneral);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (!rs.next()) throw new SQLException("no se obtuvo el id de la salida.");
            int nuevoId = rs.getInt(1);

            String sqlDetalle = "INSERT INTO tbl_salida_productos_detalle (id_salida, codigo_producto, producto, cantidad, precio_unitario, total) VALUES (?, ?, ?, ?, ?, ?)";
            String sqlStock   = "UPDATE tbl_producto SET stock = stock - ? WHERE codigo = ?";

            try (PreparedStatement psDet   = conn.prepareStatement(sqlDetalle);
                 PreparedStatement psStock = conn.prepareStatement(sqlStock)) {

                for (salidaProductosDetalleModelo det : listaDetalle) {
                    psDet.setInt(1, nuevoId);
                    psDet.setString(2, det.getCodigoProducto());
                    psDet.setString(3, det.getProducto());
                    psDet.setInt(4, det.getCantidad());
                    psDet.setDouble(5, det.getPrecioUnitario());
                    psDet.setDouble(6, det.getTotal());
                    psDet.addBatch();

                    psStock.setInt(1, det.getCantidad());
                    psStock.setString(2, det.getCodigoProducto());
                    psStock.addBatch();
                }

                psDet.executeBatch();
                psStock.executeBatch();
            }

            mostrarAlerta(Alert.AlertType.INFORMATION, "exito",
                    "salida guardada. stock actualizado para " + listaDetalle.size() + " producto(s).");
            limpiarCampos();

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "error al guardar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEliminar() {
        if (salidaCargada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "atencion", "busca una salida por id antes de eliminar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("confirmar eliminacion");
        confirm.setHeaderText(null);
        confirm.setContentText("¿eliminar la salida #" + salidaCargada.getIdSalida() +
                "?\nnota: el stock no se revertira automaticamente.");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try (Connection conn = con.establecerConexion()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM tbl_salida_productos_detalle WHERE id_salida=?")) {
                        ps.setInt(1, salidaCargada.getIdSalida());
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM tbl_salida_productos WHERE id_salida=?")) {
                        ps.setInt(1, salidaCargada.getIdSalida());
                        ps.executeUpdate();
                    }
                    listaDetalle.clear();
                    salidaCargada = null;
                    mostrarAlerta(Alert.AlertType.INFORMATION, "exito", "salida eliminada correctamente.");
                    limpiarCampos();
                } catch (Exception e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "error al eliminar", "Ocurrió un error. Intente de nuevo.");
                }
            }
        });
    }

    @FXML
    private void fnBuscar() {
        actualizarBotones(0);
        String idTexto = txtIdSalida.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "atencion", "escribe un id para buscar.");
            return;
        }
        int idBuscar;
        try {
            idBuscar = Integer.parseInt(idTexto);
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "id invalido", "el id debe ser un numero entero.");
            return;
        }

        String sql = "SELECT id_salida, id_orden_cliente, cliente, fecha_salida, responsable, observaciones " +
                "FROM tbl_salida_productos WHERE id_salida=?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idBuscar);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Date d = rs.getDate("fecha_salida");
                salidaCargada = new salidaProductosModelo(
                        rs.getInt("id_salida"),
                        rs.getInt("id_orden_cliente"),
                        rs.getString("cliente") != null ? rs.getString("cliente") : "",
                        d != null ? d.toLocalDate() : null,
                        rs.getString("responsable") != null ? rs.getString("responsable") : "",
                        rs.getString("observaciones") != null ? rs.getString("observaciones") : "");
                cargarEnFormulario(salidaCargada);
                cargarDetalle(idBuscar);
            } else {
                actualizarBotones(1);
                mostrarAlerta(Alert.AlertType.WARNING, "no encontrado",
                        "no existe una salida con el id " + idBuscar + ".");
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "error de busqueda", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void limpiarCampos() {
        actualizarBotones(0);
        txtIdSalida.clear();
        txtIdOrdenCliente.clear();
        dpFechaSalida.setValue(null);
        txtCliente.clear();
        txtResponsable.clear();
        txtObservaciones.clear();
        txtCodigoDetalle.clear();
        lblNombreProducto.setText("");
        txtCantidadDetalle.clear();
        txtPrecioDetalle.clear();
        productoSeleccionado = "";
        listaDetalle.clear();
        salidaCargada = null;
        generarSiguienteId();
    }

    private void cargarDetalle(int idSalida) {
        listaDetalle.clear();
        String sql = "SELECT id_detalle, id_salida, codigo_producto, producto, cantidad, precio_unitario, total " +
                "FROM tbl_salida_productos_detalle WHERE id_salida = ?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSalida);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                listaDetalle.add(new salidaProductosDetalleModelo(
                        rs.getInt("id_detalle"),
                        rs.getInt("id_salida"),
                        rs.getString("codigo_producto") != null ? rs.getString("codigo_producto") : "",
                        rs.getString("producto"),
                        rs.getInt("cantidad"),
                        rs.getDouble("precio_unitario"),
                        rs.getDouble("total")
                ));
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "error al cargar detalle", "Ocurrió un error. Intente de nuevo.");
        }
    }

    private void cargarEnFormulario(salidaProductosModelo s) {
        txtIdSalida.setText(String.valueOf(s.getIdSalida()));
        txtIdOrdenCliente.setText(String.valueOf(s.getIdOrdenCliente()));
        dpFechaSalida.setValue(s.getFechaSalida());
        txtCliente.setText(s.getCliente());
        txtResponsable.setText(s.getResponsable());
        txtObservaciones.setText(s.getObservaciones());
    }

    private void generarSiguienteId() {
        String sql = "SELECT ISNULL(MAX(id_salida), 0) + 1 AS siguiente FROM tbl_salida_productos";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) txtIdSalida.setText(String.valueOf(rs.getInt("siguiente")));
        } catch (Exception e) {
            txtIdSalida.setText("1");
        }
    }

    private boolean validarCampos() {
        if (dpFechaSalida.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "fecha requerida", "selecciona la fecha de salida.");
            return false;
        }
        if (listaDetalle.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "productos requeridos", "agrega al menos un producto antes de guardar.");
            return false;
        }
        return true;
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // navegacion
    @FXML private void irAInicio(javafx.event.ActionEvent e)              { Navegacion.irA("/vistasFinales/vistaInicio.fxml", e); }
    @FXML private void irAConsultaSalidaProductos(javafx.event.ActionEvent e) { consultasController.setConsultaPendiente("salidaProductos"); Navegacion.irA("/vistasFinales/vistaConsultasGenerales.fxml", e); }
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
    @FXML private void irAConsultas(javafx.event.ActionEvent e)           { Navegacion.irA("/vistasFinales/vistaConsultasGenerales.fxml", e); }
    @FXML private void salir(javafx.event.ActionEvent e)                  { Navegacion.salir(e); }

    private int estadoActual = 0;

    // estado de botones
    private void actualizarBotones(int estado) {
        this.estadoActual = estado;
        btnBuscar.setDisable(false);
        btnBuscar.setStyle("-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");
        btnLimpiar.setDisable(false);
        btnLimpiar.setStyle("-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");
        boolean actGuardar = (estado != 1);
        btnGuardar.setDisable(false);
        btnGuardar.setStyle(actGuardar ? "-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#e8d5f0; -fx-text-fill:#9b6baf; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
        boolean actEliminar = (estado != 0);
        btnEliminar.setDisable(false);
        btnEliminar.setStyle(actEliminar ? "-fx-background-color:#a83c5b; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#f5d0da; -fx-text-fill:#c47a8a; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
    }
}
