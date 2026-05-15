package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.ordenClienteModelo;
import com.example.chocolateria.modelo.ordenDetalleModelo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.sql.*;
import java.time.LocalDate;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
public class ordenClienteController {

    // formulario
    @FXML private TextField        txtCodigo;
    @FXML private TextField        txtIdCliente;
    @FXML private TextField        txtNombreCliente;
    @FXML private DatePicker       dpFecha;
    @FXML private DatePicker       dpFecha1;
    @FXML private ChoiceBox<String> cbMetodoPago;
    @FXML private ChoiceBox<String> cbEstado;
    @FXML private TextArea         txtObservaciones;
    @FXML private TextField        txtIdCajero;
    @FXML private TextField        txtCajero;

    // agregar producto al detalle
    @FXML private TextField txtIdProducto;
    @FXML private TextField txtNombreProducto;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtPrecio;

    // tabla detalle (productos de la orden actual)
    @FXML private TableView<ordenDetalleModelo>               tablaDetalle;
    @FXML private TableColumn<ordenDetalleModelo, String>     colDetCodigo;
    @FXML private TableColumn<ordenDetalleModelo, String>     colDetProducto;
    @FXML private TableColumn<ordenDetalleModelo, Number>     colDetCantidad;
    @FXML private TableColumn<ordenDetalleModelo, Number>     colDetPrecio;
    @FXML private TableColumn<ordenDetalleModelo, Number>     colDetTotal;

    @FXML private Label lblTotal;

    private final ObservableList<ordenDetalleModelo>  listaDetalle = FXCollections.observableArrayList();
    private final conexion con = new conexion();
    private ordenClienteModelo ordenCargada = null;

    @FXML private Label lblUsuario;
    @FXML private ImageView imgFotoPerfil;


    @FXML private Button btnBuscar, btnLimpiar;
    @FXML private Button btnGuardar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    @FXML
    public void initialize() {
        actualizarBotones(0);
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        cbMetodoPago.setItems(FXCollections.observableArrayList(
            "Efectivo", "Transferencia", "Tarjeta", "Cheque", "Crédito"));

        cbEstado.setItems(FXCollections.observableArrayList(
            "Pendiente", "En proceso", "Completada", "Cancelada", "Entregada"));

        txtIdCliente.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) buscarNombreCliente();
        });
        txtIdCliente.setOnAction(e -> buscarNombreCliente());

        txtIdCajero.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) buscarNombreEmpleado();
        });
        txtIdCajero.setOnAction(e -> buscarNombreEmpleado());

        colDetCodigo.setCellValueFactory(d   -> d.getValue().codigoProperty());
        colDetProducto.setCellValueFactory(d -> d.getValue().productoProperty());
        colDetCantidad.setCellValueFactory(d -> d.getValue().cantidadProperty());
        colDetPrecio.setCellValueFactory(d   -> d.getValue().precioProperty());
        colDetTotal.setCellValueFactory(d    -> new SimpleDoubleProperty(
                d.getValue().getCantidad() * d.getValue().getPrecio()));
        tablaDetalle.setItems(listaDetalle);
        listaDetalle.addListener((javafx.collections.ListChangeListener<ordenDetalleModelo>) c -> recalcularTotal());

        dpFecha1.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        dpFecha1.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isBefore(LocalDate.now())) {
                dpFecha1.setValue(null);
                mostrarAlerta(Alert.AlertType.WARNING, "Fecha inválida", "La fecha de entrega no puede ser anterior a hoy.");
            }
        });

        generarSiguienteId();
    }

    @FXML
    private void abrirBuscadorCliente() {
        popupBuscarClienteController.mostrar(c -> {
            txtIdCliente.setText(String.valueOf(c.getIdCliente()));
            buscarNombreCliente();
        });
    }

    @FXML
    private void buscarNombreCliente() {
        String idTexto = txtIdCliente.getText().trim();
        if (idTexto.isEmpty()) { txtNombreCliente.clear(); return; }
        try {
            int id = Integer.parseInt(idTexto);
            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT nombre + ' ' + apellido AS nombre_completo FROM tbl_cliente WHERE id_cliente = ?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    txtNombreCliente.setText(rs.getString("nombre_completo"));
                } else {
                    txtNombreCliente.clear();
                    mostrarAlerta(Alert.AlertType.WARNING, "Cliente no encontrado",
                        "No existe un cliente con ID " + id + ".");
                }
            }
        } catch (NumberFormatException e) {
            txtNombreCliente.clear();
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void buscarProducto() {
        String codigo = txtIdProducto.getText().trim();
        if (codigo.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Escribe el código del producto.");
            return;
        }
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT nombre, precio_unitario FROM tbl_producto WHERE codigo = ?")) {
            ps.setString(1, codigo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                txtNombreProducto.setText(rs.getString("nombre"));
                txtPrecio.setText(rs.getString("precio_unitario"));
            } else {
                txtNombreProducto.clear();
                txtPrecio.clear();
                mostrarAlerta(Alert.AlertType.WARNING, "No encontrado",
                    "No existe producto con código " + codigo + ".");
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void agregarProducto() {
        if (txtNombreProducto.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un producto antes de agregar.");
            return;
        }
        if (txtCantidad.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Ingresa la cantidad.");
            return;
        }
        try {
            int    cantidad = Integer.parseInt(txtCantidad.getText().trim());
            double precio   = txtPrecio.getText().trim().isEmpty()
                ? 0.0 : Double.parseDouble(txtPrecio.getText().trim());

            if (cantidad <= 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "Cantidad inválida", "La cantidad debe ser mayor a 0.");
                return;
            }

            String codigo = txtIdProducto.getText().trim();
            for (ordenDetalleModelo d : listaDetalle) {
                if (d.getCodigo().equals(codigo)) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Duplicado", "Este producto ya está en la lista.");
                    return;
                }
            }

            listaDetalle.add(new ordenDetalleModelo(0, 0, codigo,
                txtNombreProducto.getText().trim(), "", cantidad, precio));

            txtIdProducto.clear();
            txtNombreProducto.clear();
            txtCantidad.clear();
            txtPrecio.clear();

        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "Valor inválido", "Cantidad y precio deben ser números válidos.");
        }
    }

    @FXML
    private void quitarProducto() {
        ordenDetalleModelo sel = tablaDetalle.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Selecciona un producto para quitarlo.");
            return;
        }
        listaDetalle.remove(sel);
    }

    @FXML
    private void guardar() {
        if (estadoActual == 1) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Acción no disponible", "Ya hay un registro cargado. Usa 'Editar' para modificarlo o 'Limpiar' para crear uno nuevo.");
            return;
        }
        if (!txtIdCliente.getText().trim().isEmpty()) buscarNombreCliente();
        if (!txtIdCajero.getText().trim().isEmpty())  buscarNombreEmpleado();
        if (!validarCampos()) return;

        String sql = "INSERT INTO tbl_orden_cliente (id_cliente, cliente, fecha_registro, fecha_entrega, metodo_pago, estado, observaciones, cajero) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int idCliente = Integer.parseInt(txtIdCliente.getText().trim());
            String clienteNombre = txtNombreCliente.getText().trim();

            ps.setInt(1, idCliente);
            ps.setString(2, clienteNombre);
            ps.setDate(3, Date.valueOf(dpFecha.getValue()));
            ps.setObject(4, dpFecha1.getValue() != null ? Date.valueOf(dpFecha1.getValue()) : null);
            ps.setString(5, cbMetodoPago.getValue());
            ps.setString(6, cbEstado.getValue());
            ps.setString(7, txtObservaciones.getText().trim());
            ps.setString(8, txtCajero.getText().trim());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (!rs.next()) throw new SQLException("No se obtuvo ID de la orden.");
            int nuevoId = rs.getInt(1);

            String sqlDet = "INSERT INTO tbl_orden_detalle (id_orden, codigo, producto, categoria, cantidad, precio) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psDet = conn.prepareStatement(sqlDet)) {
                for (ordenDetalleModelo d : listaDetalle) {
                    psDet.setInt(1, nuevoId);
                    psDet.setString(2, d.getCodigo());
                    psDet.setString(3, d.getProducto());
                    psDet.setString(4, d.getCategoria());
                    psDet.setInt(5, d.getCantidad());
                    psDet.setDouble(6, d.getPrecio());
                    psDet.addBatch();
                }
                psDet.executeBatch();
            }

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito",
                "Orden #" + nuevoId + " guardada con " + listaDetalle.size() + " producto(s).");
            limpiarCampos();

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEditar() {
        actualizarBotones(2);
        if (ordenCargada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca una orden por ID antes de editar.");
            return;
        }
        if (!txtIdCajero.getText().trim().isEmpty()) buscarNombreEmpleado();
        if (!validarCampos()) return;

        String sql = "UPDATE tbl_orden_cliente SET cliente=?, fecha_registro=?, fecha_entrega=?, " +
                     "metodo_pago=?, estado=?, observaciones=?, cajero=? WHERE id_orden=?";

        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, txtNombreCliente.getText().trim());
            ps.setDate(2, Date.valueOf(dpFecha.getValue()));
            ps.setObject(3, dpFecha1.getValue() != null ? Date.valueOf(dpFecha1.getValue()) : null);
            ps.setString(4, cbMetodoPago.getValue());
            ps.setString(5, cbEstado.getValue());
            ps.setString(6, txtObservaciones.getText().trim());
            ps.setString(7, txtCajero.getText().trim());
            ps.setInt(8, ordenCargada.getIdOrden());
            ps.executeUpdate();

            try (PreparedStatement psDel = conn.prepareStatement(
                 "DELETE FROM tbl_orden_detalle WHERE id_orden=?")) {
                psDel.setInt(1, ordenCargada.getIdOrden());
                psDel.executeUpdate();
            }
            String sqlDet = "INSERT INTO tbl_orden_detalle (id_orden, codigo, producto, categoria, cantidad, precio) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psDet = conn.prepareStatement(sqlDet)) {
                for (ordenDetalleModelo d : listaDetalle) {
                    psDet.setInt(1, ordenCargada.getIdOrden());
                    psDet.setString(2, d.getCodigo());
                    psDet.setString(3, d.getProducto());
                    psDet.setString(4, d.getCategoria());
                    psDet.setInt(5, d.getCantidad());
                    psDet.setDouble(6, d.getPrecio());
                    psDet.addBatch();
                }
                psDet.executeBatch();
            }

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Orden actualizada correctamente.");
            limpiarCampos();

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al editar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEliminar() {
        if (ordenCargada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca una orden por ID antes de eliminar.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar la orden #" + ordenCargada.getIdOrden() + " de " + ordenCargada.getCliente() + "?");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try (Connection conn = con.establecerConexion()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM tbl_orden_detalle WHERE id_orden=?")) {
                        ps.setInt(1, ordenCargada.getIdOrden());
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM tbl_orden_cliente WHERE id_orden=?")) {
                        ps.setInt(1, ordenCargada.getIdOrden());
                        ps.executeUpdate();
                    }
                    listaDetalle.clear();
                    ordenCargada = null;
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Orden eliminada correctamente.");
                    limpiarCampos();
                } catch (Exception e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar", "Ocurrió un error. Intente de nuevo.");
                }
            }
        });
    }

    @FXML
    private void fnBuscar() {
        actualizarBotones(0);
        String idTexto = txtCodigo.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Escribe un ID para buscar.");
            return;
        }
        int idBuscar;
        try {
            idBuscar = Integer.parseInt(idTexto);
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID inválido", "El ID debe ser un número entero.");
            return;
        }

        String sql = "SELECT id_orden, id_cliente, cliente, fecha_registro, fecha_entrega, metodo_pago, estado, observaciones, cajero " +
                     "FROM tbl_orden_cliente WHERE id_orden=?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idBuscar);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Date dReg = rs.getDate("fecha_registro");
                Date dEnt = rs.getDate("fecha_entrega");
                ordenCargada = new ordenClienteModelo(
                    rs.getInt("id_orden"),
                    rs.getInt("id_cliente"),
                    rs.getString("cliente"),
                    dReg != null ? dReg.toLocalDate() : null,
                    dEnt != null ? dEnt.toLocalDate() : null,
                    rs.getString("metodo_pago"),
                    rs.getString("estado"),
                    rs.getString("observaciones") != null ? rs.getString("observaciones") : "",
                    rs.getString("cajero") != null ? rs.getString("cajero") : "");
                actualizarBotones(1);
                cargarEnFormulario(ordenCargada);
                cargarDetalle(idBuscar);
            } else {
                mostrarAlerta(Alert.AlertType.WARNING, "No encontrado",
                    "No existe una orden con el ID " + idBuscar + ".");
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de búsqueda", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void buscarNombreEmpleado() {
        String idTexto = txtIdCajero.getText().trim();
        if (idTexto.isEmpty()) { txtCajero.clear(); return; }
        try {
            int id = Integer.parseInt(idTexto);
            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT nombre + ' ' + apellido AS nombre_completo FROM tbl_empleado WHERE id_empleado = ?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    txtCajero.setText(rs.getString("nombre_completo"));
                } else {
                    txtCajero.clear();
                    mostrarAlerta(Alert.AlertType.WARNING, "Empleado no encontrado",
                        "No existe un empleado con ID " + id + ".");
                }
            }
        } catch (NumberFormatException e) {
            txtCajero.clear();
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void limpiarCampos() {
        actualizarBotones(0);
        txtCodigo.clear();
        txtIdCliente.clear();
        txtNombreCliente.clear();
        dpFecha.setValue(null);
        dpFecha1.setValue(null);
        cbMetodoPago.setValue(null);
        cbEstado.setValue(null);
        txtObservaciones.clear();
        txtIdCajero.clear();
        txtCajero.clear();
        txtIdProducto.clear();
        txtNombreProducto.clear();
        txtCantidad.clear();
        txtPrecio.clear();
        listaDetalle.clear();
        ordenCargada = null;
        generarSiguienteId();
    }

    private void cargarDetalle(int idOrden) {
        listaDetalle.clear();
        String sql = "SELECT id_detalle, id_orden, codigo, producto, categoria, cantidad, precio " +
                     "FROM tbl_orden_detalle WHERE id_orden = ?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idOrden);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                listaDetalle.add(new ordenDetalleModelo(
                    rs.getInt("id_detalle"),
                    rs.getInt("id_orden"),
                    rs.getString("codigo"),
                    rs.getString("producto"),
                    rs.getString("categoria") != null ? rs.getString("categoria") : "",
                    rs.getInt("cantidad"),
                    rs.getDouble("precio")
                ));
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar detalle", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void exportarPDF() {
        if (ordenCargada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca y carga una orden antes de exportar.");
            return;
        }
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Guardar Factura PDF");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));
        fc.setInitialFileName("Factura_" + ordenCargada.getIdOrden() + ".pdf");
        java.io.File archivo = fc.showSaveDialog(null);
        if (archivo == null) return;
        try (Connection conn = new conexion().establecerConexion()) {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("ID_VENTA", ordenCargada.getIdOrden());
            params.put("LOGO", getClass().getResourceAsStream("/com/example/chocolateria/logo.png"));
            JasperReportUtil.exportarPDF("/reportes/facturalachoco.jrxml", params, conn, archivo.getAbsolutePath());
            mostrarAlerta(Alert.AlertType.INFORMATION, "PDF exportado",
                "Guardado en:\n" + archivo.getAbsolutePath());
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al exportar PDF", "Ocurrió un error. Intente de nuevo.");
        }
    }

    private void cargarEnFormulario(ordenClienteModelo o) {
        txtCodigo.setText(String.valueOf(o.getIdOrden()));
        txtIdCliente.setText(String.valueOf(o.getIdCliente()));
        txtNombreCliente.setText(o.getCliente());
        dpFecha.setValue(o.getFechaRegistro());
        dpFecha1.setValue(o.getFechaEntrega());
        cbMetodoPago.setValue(o.getMetodoPago());
        cbEstado.setValue(o.getEstado());
        txtObservaciones.setText(o.getObservaciones());
        txtIdCajero.clear();
        txtCajero.setText(o.getCajero());
    }

    private void generarSiguienteId() {
        String sql = "SELECT ISNULL(MAX(id_orden), 0) + 1 AS siguiente FROM tbl_orden_cliente";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) txtCodigo.setText(String.valueOf(rs.getInt("siguiente")));
        } catch (Exception e) {
            txtCodigo.setText("1");
        }
    }

    private boolean validarCampos() {
        if (txtNombreCliente.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo requerido", "Ingresa el ID del cliente.");
            return false;
        }
        if (dpFecha.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo requerido", "Selecciona la fecha de registro.");
            return false;
        }
        if (dpFecha1.getValue() != null && dpFecha1.getValue().isBefore(LocalDate.now())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha inválida", "La fecha de entrega no puede ser anterior a hoy.");
            return false;
        }
        if (cbMetodoPago.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo requerido", "Selecciona el método de pago.");
            return false;
        }
        if (cbEstado.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo requerido", "Selecciona el estado.");
            return false;
        }
        if (listaDetalle.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Productos requeridos",
                "Agrega al menos un producto a la orden.");
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

    private void recalcularTotal() {
        double total = listaDetalle.stream()
                .mapToDouble(d -> d.getCantidad() * d.getPrecio())
                .sum();
        lblTotal.setText(String.format("RD$ %,.2f", total));
    }

    @FXML
    private void generarCotizacion() {
        if (listaDetalle.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin productos", "Agrega productos a la orden antes de generar la cotización.");
            return;
        }

        String cliente   = !txtNombreCliente.getText().trim().isEmpty() ? txtNombreCliente.getText().trim() : "—";
        String metodo    = cbMetodoPago.getValue() != null ? cbMetodoPago.getValue() : "—";
        String estado    = cbEstado.getValue() != null ? cbEstado.getValue() : "—";
        String fechaReg  = dpFecha.getValue() != null ? dpFecha.getValue().toString() : "—";
        String fechaEnt  = dpFecha1.getValue() != null ? dpFecha1.getValue().toString() : "—";
        String idOrden   = txtCodigo.getText().trim();
        double subtotal  = listaDetalle.stream().mapToDouble(d -> d.getCantidad() * d.getPrecio()).sum();
        double itbis     = subtotal * 0.18;
        double total     = subtotal + itbis;

        // encabezado
        VBox root = new VBox(14);
        root.setPadding(new Insets(24, 30, 24, 30));
        root.setStyle("-fx-background-color:#f9f5ff;");

        Label titulo = new Label("COTIZACIÓN DE VENTA");
        titulo.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#48295a;");

        Label empresa = new Label("Chocolatería — Gestión de Pedidos");
        empresa.setStyle("-fx-font-size:12px; -fx-text-fill:#6d3c87;");

        javafx.scene.control.Separator sep1 = new javafx.scene.control.Separator();

        // datos de la orden
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(18);
        grid.setVgap(6);
        String[][] campos = {
            {"N° Orden:", idOrden.isEmpty() ? "(nueva)" : "#" + idOrden},
            {"Cliente:", cliente},
            {"Fecha de registro:", fechaReg},
            {"Fecha de entrega:", fechaEnt},
            {"Método de pago:", metodo},
            {"Estado:", estado}
        };
        for (int i = 0; i < campos.length; i++) {
            Label lbl = new Label(campos[i][0]);
            lbl.setStyle("-fx-font-weight:bold; -fx-text-fill:#3B1A5C; -fx-font-size:12px;");
            Label val = new Label(campos[i][1]);
            val.setStyle("-fx-text-fill:#333; -fx-font-size:12px;");
            grid.add(lbl, 0, i);
            grid.add(val, 1, i);
        }

        // tabla de productos
        TableView<ordenDetalleModelo> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(200);

        TableColumn<ordenDetalleModelo, String> cCod  = new TableColumn<>("Código");
        cCod.setCellValueFactory(d -> d.getValue().codigoProperty());

        TableColumn<ordenDetalleModelo, String> cProd = new TableColumn<>("Producto");
        cProd.setCellValueFactory(d -> d.getValue().productoProperty());

        TableColumn<ordenDetalleModelo, Number> cCant = new TableColumn<>("Cantidad");
        cCant.setCellValueFactory(d -> d.getValue().cantidadProperty());

        TableColumn<ordenDetalleModelo, String> cPrec = new TableColumn<>("Precio unit. (RD$)");
        cPrec.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%,.2f", d.getValue().getPrecio())));

        TableColumn<ordenDetalleModelo, String> cSub  = new TableColumn<>("Subtotal (RD$)");
        cSub.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%,.2f", d.getValue().getCantidad() * d.getValue().getPrecio())));

        tabla.getColumns().addAll(cCod, cProd, cCant, cPrec, cSub);
        tabla.setItems(listaDetalle);

        // subtotal / ITBIS / total
        String styleEtiq = "-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#48295a;";
        String styleVal  = "-fx-font-size:12px; -fx-text-fill:#333;";
        String styleTot  = "-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#48295a; " +
                           "-fx-background-color:#e8d5f0; -fx-background-radius:8; " +
                           "-fx-padding:4 16 4 16; -fx-border-color:#C5A8E8; -fx-border-radius:8;";

        VBox vTotales = new VBox(6);
        vTotales.setAlignment(Pos.CENTER_RIGHT);

        HBox hSub = new HBox(12);
        hSub.setAlignment(Pos.CENTER_RIGHT);
        Label lSub = new Label("Subtotal:"); lSub.setStyle(styleEtiq);
        Label vSub = new Label(String.format("RD$ %,.2f", subtotal)); vSub.setStyle(styleVal);
        hSub.getChildren().addAll(lSub, vSub);

        HBox hItbis = new HBox(12);
        hItbis.setAlignment(Pos.CENTER_RIGHT);
        Label lItbis = new Label("ITBIS (18%):"); lItbis.setStyle(styleEtiq);
        Label vItbis = new Label(String.format("RD$ %,.2f", itbis)); vItbis.setStyle(styleVal);
        hItbis.getChildren().addAll(lItbis, vItbis);

        HBox hTotal = new HBox(10);
        hTotal.setAlignment(Pos.CENTER_RIGHT);
        Label lblTituloTotal = new Label("TOTAL:");
        lblTituloTotal.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#48295a;");
        Label lblValorTotal = new Label(String.format("RD$ %,.2f", total));
        lblValorTotal.setStyle(styleTot);
        hTotal.getChildren().addAll(lblTituloTotal, lblValorTotal);

        vTotales.getChildren().addAll(hSub, hItbis, new javafx.scene.control.Separator(), hTotal);

        // nota + botón exportar
        Label nota = new Label("Esta cotización es válida por 30 días desde la fecha de registro.");
        nota.setStyle("-fx-font-size:10px; -fx-text-fill:#888; -fx-font-style:italic;");

        javafx.scene.control.Button btnPDF = new javafx.scene.control.Button("Exportar PDF");
        btnPDF.setStyle("-fx-background-color:#8B0000; -fx-text-fill:white; -fx-font-weight:bold; " +
                        "-fx-background-radius:10; -fx-font-size:12px;");
        btnPDF.setOnAction(ev -> {
            if (ordenCargada == null) {
                mostrarAlerta(Alert.AlertType.WARNING, "Orden no guardada",
                    "Guarda la orden primero para poder exportar el PDF.");
                return;
            }
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Guardar Cotización PDF");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));
            fc.setInitialFileName("Cotizacion_" + ordenCargada.getIdOrden() + ".pdf");
            java.io.File archivo = fc.showSaveDialog(null);
            if (archivo == null) return;
            try (Connection connPDF = new conexion().establecerConexion()) {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("ID_VENTA", ordenCargada.getIdOrden());
                params.put("LOGO", getClass().getResourceAsStream("/com/example/chocolateria/logo.png"));
                JasperReportUtil.exportarPDF("/reportes/facturalachoco.jrxml", params, connPDF, archivo.getAbsolutePath());
                mostrarAlerta(Alert.AlertType.INFORMATION, "PDF exportado",
                    "Guardado en:\n" + archivo.getAbsolutePath());
            } catch (Exception ex) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al exportar PDF", ex.getMessage());
            }
        });

        HBox hBotones = new HBox(btnPDF);
        hBotones.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(titulo, empresa, sep1, grid,
                new javafx.scene.control.Separator(), tabla, vTotales, nota, hBotones);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Cotización — " + cliente);
        stage.setScene(new Scene(root, 740, 620));
        stage.show();
    }

    // navegacion
    @FXML private void irAConsultaOrdenCliente(javafx.event.ActionEvent e)  { Navegacion.irA("/vistasFinales/vistaConsultaOrdenCliente.fxml", e); }
    // generar reporte Jasper

    @FXML
    private void generarReporte() {
        if (ordenCargada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca y carga una orden antes de generar la factura.");
            return;
        }
        try (java.sql.Connection conn = new conexion().establecerConexion()) {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("ID_VENTA", ordenCargada.getIdOrden());
            params.put("LOGO", getClass().getResourceAsStream("/com/example/chocolateria/logo.png"));
            JasperReportUtil.mostrarReporte("/reportes/facturalachoco.jrxml", params, conn);
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al generar factura", "Ocurrió un error. Intente de nuevo.");
        }
    }

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
    @FXML private void irAConsultas(javafx.event.ActionEvent e)           { Navegacion.irA("/vistasFinales/vistaConsultasGenerales.fxml", e); }
    @FXML private void salir(javafx.event.ActionEvent e)                  { Navegacion.salir(e); }

    private int estadoActual = 0;


    private void actualizarBotones(int estado) {
        this.estadoActual = estado;

        btnBuscar.setDisable(false);
        btnBuscar.setStyle("-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");
        btnLimpiar.setDisable(false);
        btnLimpiar.setStyle("-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");
        boolean actGuardar = (estado != 1);
        btnGuardar.setDisable(false);
        btnGuardar.setStyle(actGuardar ? "-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#e8d5f0; -fx-text-fill:#9b6baf; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
        boolean actEditar = (estado == 1);
        btnEditar.setDisable(false);
        btnEditar.setStyle(actEditar ? "-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#e8d5f0; -fx-text-fill:#9b6baf; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
        boolean actEliminar = (estado != 0);
        btnEliminar.setDisable(false);
        btnEliminar.setStyle(actEliminar ? "-fx-background-color:#a83c5b; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#f5d0da; -fx-text-fill:#c47a8a; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
    }

}
