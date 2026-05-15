package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.pagoVentaModelo;
import com.example.chocolateria.modelo.ventaModelo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.sql.*;
import java.time.LocalDate;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
public class ventaController {

    private static final double ITBIS_PORCENTAJE = 0.18;

    @FXML private TextField        txtIdVenta;
    @FXML private TextField        txtIdOrden;
    @FXML private Label            lblInfoOrden;
    @FXML private DatePicker       dpFechaVenta;
    @FXML private TextField        txtIdEmpleado;
    @FXML private Label            lblNombreEmpleado;
    @FXML private ComboBox<String> cbTipoPago;
    @FXML private ComboBox<String> cbMetodoPago;
    @FXML private ComboBox<String> cbTipoNcf;
    @FXML private TextField        txtSubtotal;
    @FXML private TextField        txtDescuento;
    @FXML private TextField        txtItbis;
    @FXML private TextField        txtTotal;
    @FXML private TextField        txtMontoPagado;
    @FXML private TextField        txtBalancePendiente;
    @FXML private TextField        txtEstadoPago;
    @FXML private TextField        txtNcf;
    @FXML private TextField        txtBuscarTabla;

    @FXML private DatePicker       dpFechaPago;
    @FXML private TextField        txtMontoAbono;
    @FXML private ComboBox<String> cbMetodoPagoAbono;
    @FXML private TextField        txtReferencia;

    @FXML private TableView<pagoVentaModelo>                tablaPagos;
    @FXML private TableColumn<pagoVentaModelo, Number>      colPagoId;
    @FXML private TableColumn<pagoVentaModelo, LocalDate>   colPagoFecha;
    @FXML private TableColumn<pagoVentaModelo, Number>      colPagoMonto;
    @FXML private TableColumn<pagoVentaModelo, String>      colPagoMetodo;
    @FXML private TableColumn<pagoVentaModelo, String>      colPagoRef;

    @FXML private TableView<ventaModelo>                    tablaVentas;
    @FXML private TableColumn<ventaModelo, Number>          colId;
    @FXML private TableColumn<ventaModelo, String>          colCliente;
    @FXML private TableColumn<ventaModelo, LocalDate>       colFecha;
    @FXML private TableColumn<ventaModelo, Number>          colSubtotal;
    @FXML private TableColumn<ventaModelo, Number>          colItbis;
    @FXML private TableColumn<ventaModelo, Number>          colDescuento;
    @FXML private TableColumn<ventaModelo, Number>          colTotal;
    @FXML private TableColumn<ventaModelo, Number>          colPagado;
    @FXML private TableColumn<ventaModelo, Number>          colBalance;
    @FXML private TableColumn<ventaModelo, String>          colEstado;
    @FXML private TableColumn<ventaModelo, String>          colNcf;

    private final ObservableList<ventaModelo>     listaVentas = FXCollections.observableArrayList();
    private final ObservableList<pagoVentaModelo> listaPagos  = FXCollections.observableArrayList();
    private final conexion con = new conexion();
    private int    idVentaSeleccionada = 0;
    private int    idEmpleadoSeleccionado = 0;
    private double subtotalBase = 0.0;

    @FXML private Label lblUsuario;
    @FXML private ImageView imgFotoPerfil;


    @FXML private Button btnBuscar, btnLimpiar;
    @FXML private Button btnRegistrar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnAbono;

    @FXML
    public void initialize() {
        actualizarBotones(0);
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);
        cbTipoPago.setItems(FXCollections.observableArrayList("Contado", "CrÃ©dito"));
        cbMetodoPago.setItems(FXCollections.observableArrayList("Efectivo", "Transferencia", "Tarjeta", "Cheque"));
        cbMetodoPagoAbono.setItems(FXCollections.observableArrayList("Efectivo", "Transferencia", "Tarjeta", "Cheque"));
        cbTipoNcf.setItems(FXCollections.observableArrayList("B01 - CrÃ©dito Fiscal", "B02 - Consumidor Final"));
        cbTipoNcf.setValue("B02 - Consumidor Final");

        // calcular itbis y total cuando cambia el descuento
        txtDescuento.textProperty().addListener((obs, o, n) -> calcularMontos());

        // columnas historial ventas (solo si existe la tabla)
        if (tablaVentas != null) {
            colId.setCellValueFactory(d       -> d.getValue().idVentaProperty());
            colCliente.setCellValueFactory(d  -> d.getValue().clienteProperty());
            colFecha.setCellValueFactory(d    -> d.getValue().fechaVentaProperty());
            colSubtotal.setCellValueFactory(d -> d.getValue().subtotalProperty());
            colItbis.setCellValueFactory(d    -> d.getValue().itbisProperty());
            colDescuento.setCellValueFactory(d-> d.getValue().descuentoProperty());
            colTotal.setCellValueFactory(d    -> d.getValue().montoTotalProperty());
            colPagado.setCellValueFactory(d   -> d.getValue().montoPagadoProperty());
            colBalance.setCellValueFactory(d  -> d.getValue().balancePendienteProperty());
            colEstado.setCellValueFactory(d   -> d.getValue().estadoPagoProperty());
            colNcf.setCellValueFactory(d      -> d.getValue().ncfProperty());
        }

        // columnas pagos
        colPagoId.setCellValueFactory(d    -> d.getValue().idPagoProperty());
        colPagoFecha.setCellValueFactory(d -> d.getValue().fechaPagoProperty());
        colPagoMonto.setCellValueFactory(d -> d.getValue().montoPagadoProperty());
        colPagoMetodo.setCellValueFactory(d-> d.getValue().metodoPagoProperty());
        colPagoRef.setCellValueFactory(d   -> d.getValue().numeroReferenciaProperty());
        tablaPagos.setItems(listaPagos);

        // color por estado y filtro (solo si existe)
        if (tablaVentas != null) {
            tablaVentas.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(ventaModelo v, boolean empty) {
                    super.updateItem(v, empty);
                    if (v == null || empty) { setStyle(""); return; }
                    switch (v.getEstadoPago()) {
                        case "Pagado"   -> setStyle("-fx-background-color:#e8f5e9;");
                        case "Pendiente"-> setStyle("-fx-background-color:#fde8e8;");
                        case "Parcial"  -> setStyle("-fx-background-color:#fff8e1;");
                        default         -> setStyle("");
                    }
                }
            });

            FilteredList<ventaModelo> listaFiltrada = new FilteredList<>(listaVentas, p -> true);
            if (txtBuscarTabla != null) {
                txtBuscarTabla.textProperty().addListener((obs, oldVal, newVal) ->
                        listaFiltrada.setPredicate(v -> {
                            if (newVal == null || newVal.isBlank()) return true;
                            String f = newVal.toLowerCase();
                            return v.getCliente().toLowerCase().contains(f)
                                    || v.getEstadoPago().toLowerCase().contains(f)
                                    || v.getNcf().toLowerCase().contains(f);
                        })
                );
            }
            tablaVentas.setItems(listaFiltrada);

            tablaVentas.getSelectionModel().selectedItemProperty().addListener(
                    (obs, old, sel) -> {
                        if (sel != null) {
                            cargarEnFormulario(sel);
                            cargarPagos(sel.getIdVenta());
                            actualizarBotones(1);
                        }
                    }
            );
        }

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() {
                cargarVentas();
                return null;
            }
        };
        new Thread(cargar).start();
        generarSiguienteId();
    }

    @FXML
    private void abrirBuscadorOrden() {
        popupBuscarOrdenClienteController.mostrar(id -> {
            txtIdOrden.setText(id);
            cargarDesdeOrden();
        });
    }

    @FXML
    private void cargarDesdeOrden() {
        String idTexto = txtIdOrden.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "AtenciÃ³n", "Escribe el ID de la orden.");
            return;
        }
        try {
            int id = Integer.parseInt(idTexto);
            // primero trae los datos generales de la orden
            String sqlOrden = "SELECT o.cliente, o.metodo_pago, " +
                    "ISNULL(SUM(d.precio * d.cantidad), 0) AS subtotal_calc, " +
                    "STUFF((SELECT ', ' + d2.producto FROM tbl_orden_detalle d2 " +
                    "       WHERE d2.id_orden = o.id_orden FOR XML PATH('')), 1, 2, '') AS productos " +
                    "FROM tbl_orden_cliente o " +
                    "LEFT JOIN tbl_orden_detalle d ON o.id_orden = d.id_orden " +
                    "WHERE o.id_orden = ? " +
                    "GROUP BY o.id_orden, o.cliente, o.metodo_pago";
            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(sqlOrden)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String cliente   = rs.getString("cliente");
                    String productos = rs.getString("productos");
                    subtotalBase     = rs.getDouble("subtotal_calc");

                    String resumen = (productos != null && !productos.isBlank())
                            ? cliente + " â€” " + productos
                            : cliente;
                    lblInfoOrden.setText(resumen);
                    txtSubtotal.setText(String.format("%.2f", subtotalBase));
                    txtDescuento.setText("0.00");
                    if (rs.getString("metodo_pago") != null)
                        cbMetodoPago.setValue(rs.getString("metodo_pago"));
                    dpFechaVenta.setValue(LocalDate.now());
                    calcularMontos();
                } else {
                    mostrarAlerta(Alert.AlertType.WARNING, "No encontrado",
                            "No existe una orden con el ID " + id + ".");
                }
            }
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID invÃ¡lido", "El ID debe ser un nÃºmero.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrio un error inesperado. Intente nuevamente.");
        }
    }

    @FXML
    private void buscarEmpleado() {
        String idTexto = txtIdEmpleado.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "AtenciÃ³n", "Escribe el ID del empleado.");
            return;
        }
        try {
            int id = Integer.parseInt(idTexto);
            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT nombre, apellido FROM tbl_empleado WHERE id_empleado = ?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    idEmpleadoSeleccionado = id;
                    lblNombreEmpleado.setText(rs.getString("nombre") + " " + rs.getString("apellido"));
                } else {
                    idEmpleadoSeleccionado = 0;
                    lblNombreEmpleado.setText("No encontrado");
                }
            }
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID invÃ¡lido", "El ID debe ser un nÃºmero.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrio un error inesperado. Intente nuevamente.");
        }
    }

    @FXML
    private void registrarVenta() {
        if (estadoActual == 1) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "AcciÃ³n no disponible", "Ya hay un registro cargado. Usa 'Limpiar' para registrar uno nuevo.");
            return;
        }
        if (!validarCamposVenta()) return;

        try {
            double subtotal  = Double.parseDouble(txtSubtotal.getText().trim());
            double descuento = Double.parseDouble(txtDescuento.getText().trim());
            double itbis     = Double.parseDouble(txtItbis.getText().trim());
            double total     = Double.parseDouble(txtTotal.getText().trim());
            String tipoPago  = cbTipoPago.getValue();
            String estadoPago = "Contado".equals(tipoPago) ? "Pagado" : "Pendiente";
            double montoPagado = "Contado".equals(tipoPago) ? total : 0.0;
            double balance     = total - montoPagado;

            // generar ncf
            String ncf = generarNcf(cbTipoNcf.getValue());

            String sql = "INSERT INTO tbl_venta (fecha_venta, monto_total, id_empleado, descuento, " +
                    "subtotal, itbis, tipo_pago, estado_pago, monto_pagado, balance_pendiente, " +
                    "id_orden) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setDate(1, Date.valueOf(dpFechaVenta.getValue()));
                ps.setDouble(2, total);
                ps.setInt(3, idEmpleadoSeleccionado);
                ps.setDouble(4, descuento);
                ps.setDouble(5, subtotal);
                ps.setDouble(6, itbis);
                ps.setString(7, tipoPago);
                ps.setString(8, estadoPago);
                ps.setDouble(9, montoPagado);
                ps.setDouble(10, balance);
                ps.setObject(11, txtIdOrden.getText().trim().isEmpty() ? null
                        : Integer.parseInt(txtIdOrden.getText().trim()));
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                int nuevoId = rs.next() ? rs.getInt(1) : 0;
                idVentaSeleccionada = nuevoId;
                txtIdVenta.setText(String.valueOf(nuevoId));
                txtNcf.setText(ncf);

                // actualizar id_comprobante con el ncf
                try (PreparedStatement psNcf = conn.prepareStatement(
                        "UPDATE tbl_venta SET id_comprobante = ? WHERE id_venta = ?")) {
                    psNcf.setString(1, ncf);
                    psNcf.setInt(2, nuevoId);
                    psNcf.executeUpdate();
                }

                // para ventas de contado registrar el pago automaticamente
                if ("Contado".equals(tipoPago) && cbMetodoPago.getValue() != null) {
                    try (PreparedStatement psPago = conn.prepareStatement(
                            "INSERT INTO tbl_pago_venta (id_venta, fecha_pago, monto_pagado, metodo_pago, numero_referencia) VALUES (?, ?, ?, ?, ?)")) {
                        psPago.setInt(1, nuevoId);
                        psPago.setDate(2, Date.valueOf(dpFechaVenta.getValue()));
                        psPago.setDouble(3, total);
                        psPago.setString(4, cbMetodoPago.getValue());
                        psPago.setString(5, ncf);
                        psPago.executeUpdate();
                    }
                    listaPagos.add(new pagoVentaModelo(0, nuevoId, dpFechaVenta.getValue(),
                            total, cbMetodoPago.getValue(), ncf, "Pago contado"));
                }

                String cliente = lblInfoOrden.getText().contains("-")
                        ? lblInfoOrden.getText().split("-")[0].trim() : lblInfoOrden.getText();

                ventaModelo nueva = new ventaModelo(nuevoId,
                        txtIdOrden.getText().trim().isEmpty() ? 0 : Integer.parseInt(txtIdOrden.getText().trim()),
                        dpFechaVenta.getValue(), cliente, subtotal, descuento, itbis, total,
                        montoPagado, balance, tipoPago, estadoPago,
                        cbMetodoPago.getValue() != null ? cbMetodoPago.getValue() : "",
                        ncf, idEmpleadoSeleccionado, lblNombreEmpleado.getText());
                listaVentas.add(0, nueva);

                actualizarResumenUI(montoPagado, total, estadoPago);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Ã‰xito",
                        "Venta registrada. NCF: " + ncf + "\nTotal: RD$ " + String.format("%.2f", total));
            }

        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Error", "Verifica los montos ingresados.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al registrar venta", "Ocurrio un error inesperado. Intente nuevamente.");
        }
    }

    @FXML
    private void registrarAbono() {
        if (idVentaSeleccionada == 0) {
            mostrarAlerta(Alert.AlertType.WARNING, "AtenciÃ³n",
                    "Selecciona una venta de la tabla o regÃ­strala primero.");
            return;
        }
        if (!validarCamposAbono()) return;

        try {
            double montoAbono = Double.parseDouble(txtMontoAbono.getText().trim());
            double balance    = Double.parseDouble(txtBalancePendiente.getText().trim());

            if (montoAbono > balance) {
                mostrarAlerta(Alert.AlertType.WARNING, "Monto excedido",
                        String.format("El abono (RD$ %.2f) supera el balance pendiente (RD$ %.2f).",
                                montoAbono, balance));
                return;
            }

            String sql = "INSERT INTO tbl_pago_venta (id_venta, fecha_pago, monto_pagado, metodo_pago, numero_referencia) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, idVentaSeleccionada);
                ps.setDate(2, Date.valueOf(dpFechaPago.getValue()));
                ps.setDouble(3, montoAbono);
                ps.setString(4, cbMetodoPagoAbono.getValue());
                ps.setString(5, txtReferencia.getText().trim());
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                int nuevoId = rs.next() ? rs.getInt(1) : 0;

                // actualizar monto_pagado y balance en tbl_venta
                double nuevoPagado = Double.parseDouble(txtMontoPagado.getText().trim()) + montoAbono;
                double total       = Double.parseDouble(txtTotal.getText().trim());
                double nuevoBalance= total - nuevoPagado;
                String nuevoEstado = nuevoBalance <= 0 ? "Pagado"
                        : nuevoPagado > 0  ? "Parcial" : "Pendiente";

                try (PreparedStatement psUpd = conn.prepareStatement(
                        "UPDATE tbl_venta SET monto_pagado=?, balance_pendiente=?, estado_pago=? WHERE id_venta=?")) {
                    psUpd.setDouble(1, nuevoPagado);
                    psUpd.setDouble(2, nuevoBalance);
                    psUpd.setString(3, nuevoEstado);
                    psUpd.setInt(4, idVentaSeleccionada);
                    psUpd.executeUpdate();
                }

                listaPagos.add(new pagoVentaModelo(nuevoId, idVentaSeleccionada,
                        dpFechaPago.getValue(), montoAbono,
                        cbMetodoPagoAbono.getValue(), txtReferencia.getText().trim(), ""));

                // actualizar fila en tabla (si existe)
                for (ventaModelo v : listaVentas) {
                    if (v.getIdVenta() == idVentaSeleccionada) {
                        v.setMontoPagado(nuevoPagado);
                        v.setBalancePendiente(nuevoBalance);
                        v.setEstadoPago(nuevoEstado);
                        if (tablaVentas != null) tablaVentas.refresh();
                        break;
                    }
                }

                actualizarResumenUI(nuevoPagado, total, nuevoEstado);
                limpiarCamposAbono();
                mostrarAlerta(Alert.AlertType.INFORMATION, "Ã‰xito",
                        String.format("Abono de RD$ %.2f registrado. Estado: %s", montoAbono, nuevoEstado));
            }

        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Monto invÃ¡lido", "El monto debe ser un nÃºmero vÃ¡lido.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al registrar abono", "Ocurrio un error inesperado. Intente nuevamente.");
        }
    }

    @FXML
    private void fnBuscar() {
        String idTexto = txtIdVenta.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "AtenciÃ³n", "Escribe un ID para buscar.");
            return;
        }
        int idBuscar;
        try {
            idBuscar = Integer.parseInt(idTexto);
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID invÃ¡lido", "El ID debe ser un nÃºmero entero.");
            return;
        }
        String sql = "SELECT v.id_venta, v.id_orden, v.fecha_venta, " +
                "ISNULL(o.cliente, '') AS cliente, " +
                "v.subtotal, v.descuento, v.itbis, v.monto_total, " +
                "v.monto_pagado, v.balance_pendiente, v.tipo_pago, " +
                "v.estado_pago, " +
                "ISNULL(CAST(v.id_comprobante AS VARCHAR), '') AS ncf, " +
                "v.id_empleado, " +
                "ISNULL(e.nombre + ' ' + e.apellido, '') AS empleado " +
                "FROM tbl_venta v " +
                "LEFT JOIN tbl_orden_cliente o ON v.id_orden = o.id_orden " +
                "LEFT JOIN tbl_empleado e ON v.id_empleado = e.id_empleado " +
                "WHERE v.id_venta = ?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idBuscar);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Date d = rs.getDate("fecha_venta");
                ventaModelo v = new ventaModelo(
                        rs.getInt("id_venta"), rs.getInt("id_orden"),
                        d != null ? d.toLocalDate() : null,
                        rs.getString("cliente"),
                        rs.getDouble("subtotal"), rs.getDouble("descuento"),
                        rs.getDouble("itbis"), rs.getDouble("monto_total"),
                        rs.getDouble("monto_pagado"), rs.getDouble("balance_pendiente"),
                        rs.getString("tipo_pago"), rs.getString("estado_pago"),
                        "", rs.getString("ncf"),
                        rs.getInt("id_empleado"), rs.getString("empleado"));
                cargarEnFormulario(v);
                cargarPagos(v.getIdVenta());
                if (tablaVentas != null) {
                    for (ventaModelo tv : listaVentas) {
                        if (tv.getIdVenta() == idBuscar) {
                            tablaVentas.getSelectionModel().select(tv);
                            tablaVentas.scrollTo(tv);
                            break;
                        }
                    }
                }
                actualizarBotones(1);
            } else {
                mostrarAlerta(Alert.AlertType.WARNING, "No encontrado",
                        "No existe una venta con el ID " + idBuscar + ".");
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al buscar", "Ocurrio un error inesperado. Intente nuevamente.");
        }
    }

    @FXML
    private void fnEditar() {
        if (estadoActual != 1) {
            mostrarAlerta(Alert.AlertType.WARNING, "AtenciÃ³n", "Busca una venta primero para poder editarla.");
            return;
        }
        if (dpFechaVenta.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha requerida", "Selecciona la fecha de venta.");
            return;
        }
        if (cbTipoPago.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Tipo pago requerido", "Selecciona el tipo de pago.");
            return;
        }
        try {
            double descuento = txtDescuento.getText().trim().isEmpty() ? 0.0
                    : Double.parseDouble(txtDescuento.getText().trim());
            double subtotal  = Double.parseDouble(txtSubtotal.getText().trim());
            double itbis     = (subtotal - descuento) * ITBIS_PORCENTAJE;
            double total     = (subtotal - descuento) + itbis;

            String sql = "UPDATE tbl_venta SET fecha_venta=?, tipo_pago=?, descuento=?, itbis=?, monto_total=?, id_empleado=? WHERE id_venta=?";
            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(dpFechaVenta.getValue()));
                ps.setString(2, cbTipoPago.getValue());
                ps.setDouble(3, descuento);
                ps.setDouble(4, itbis);
                ps.setDouble(5, total);
                ps.setInt(6, idEmpleadoSeleccionado);
                ps.setInt(7, idVentaSeleccionada);
                ps.executeUpdate();
            }
            for (ventaModelo v : listaVentas) {
                if (v.getIdVenta() == idVentaSeleccionada) {
                    listaVentas.remove(v);
                    break;
                }
            }
            mostrarAlerta(Alert.AlertType.INFORMATION, "Ã‰xito", "Venta actualizada correctamente.");
            limpiar();
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Error", "Verifica los montos ingresados.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al editar venta", "Ocurrio un error inesperado. Intente nuevamente.");
        }
    }

    @FXML
    private void fnEliminar() {
        ventaModelo sel = (tablaVentas != null)
                ? tablaVentas.getSelectionModel().getSelectedItem()
                : listaVentas.stream()
                    .filter(v -> String.valueOf(v.getIdVenta()).equals(txtIdVenta.getText().trim()))
                    .findFirst().orElse(null);
        if (sel == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "AtenciÃ³n", "Busca una venta por ID para poder eliminarla.");
            return;
        }
        if (!"Pendiente".equals(sel.getEstadoPago())) {
            mostrarAlerta(Alert.AlertType.WARNING, "No permitido",
                    "Solo se pueden eliminar ventas en estado Pendiente sin pagos.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminaciÃ³n");
        confirm.setHeaderText(null);
        confirm.setContentText("Â¿Eliminar la venta #" + sel.getIdVenta() + "?");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try (Connection conn = con.establecerConexion();
                     PreparedStatement ps = conn.prepareStatement(
                             "DELETE FROM tbl_venta WHERE id_venta=?")) {
                    ps.setInt(1, sel.getIdVenta());
                    ps.executeUpdate();
                    listaVentas.remove(sel);
                    listaPagos.clear();
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Ã‰xito", "Venta eliminada.");
                    limpiar();
                } catch (Exception e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar", "Ocurrio un error inesperado. Intente nuevamente.");
                }
            }
        });
    }

    @FXML
    private void limpiar() {
        actualizarBotones(0);
        txtIdVenta.clear();
        txtIdOrden.clear();
        lblInfoOrden.setText("");
        dpFechaVenta.setValue(null);
        txtIdEmpleado.clear();
        lblNombreEmpleado.setText("");
        idEmpleadoSeleccionado = 0;
        cbTipoPago.setValue(null);
        cbMetodoPago.setValue(null);
        cbTipoNcf.setValue("B02 - Consumidor Final");
        txtSubtotal.clear();
        txtDescuento.clear();
        txtItbis.clear();
        txtTotal.clear();
        txtMontoPagado.clear();
        txtBalancePendiente.clear();
        txtEstadoPago.clear();
        txtNcf.clear();
        txtEstadoPago.setStyle("-fx-background-color:#f0eaf8; -fx-font-weight:bold; -fx-border-color:purple; -fx-border-radius:6; -fx-background-radius:6;");
        subtotalBase = 0.0;
        idVentaSeleccionada = 0;
        listaPagos.clear();
        limpiarCamposAbono();
        if (tablaVentas != null) tablaVentas.getSelectionModel().clearSelection();
        generarSiguienteId();
    }

    private void limpiarCamposAbono() {
        dpFechaPago.setValue(null);
        txtMontoAbono.clear();
        cbMetodoPagoAbono.setValue(null);
        txtReferencia.clear();
    }

    private void calcularMontos() {
        try {
            double descuento = txtDescuento.getText().trim().isEmpty()
                    ? 0.0 : Double.parseDouble(txtDescuento.getText().trim());
            double base  = subtotalBase - descuento;
            double itbis = base * ITBIS_PORCENTAJE;
            double total = base + itbis;
            txtItbis.setText(String.format("%.2f", itbis));
            txtTotal.setText(String.format("%.2f", total));
        } catch (NumberFormatException ignored) {}
    }

    private String generarNcf(String tipoNcf) throws Exception {
        String codigo = tipoNcf.startsWith("B01") ? "B01" : "B02";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE tbl_secuencia_ncf SET secuencia = secuencia + 1 OUTPUT INSERTED.secuencia WHERE tipo_ncf = ?")) {
            ps.setString(1, codigo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int seq = rs.getInt(1);
                return String.format("%s%08d", codigo, seq);
            }
        }
        return codigo + "00000001";
    }

    private double buscarPrecioProducto(Connection conn, String nombreProducto) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT precio_unitario FROM tbl_producto WHERE nombre = ?")) {
            ps.setString(1, nombreProducto);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Double.parseDouble(rs.getString("precio_unitario"));
        } catch (Exception ignored) {}
        return 0.0;
    }

    private void cargarVentas() {
        listaVentas.clear();
        String sql = "SELECT v.id_venta, v.id_orden, v.fecha_venta, " +
                "ISNULL(o.cliente, '') AS cliente, " +
                "v.subtotal, v.descuento, v.itbis, v.monto_total, " +
                "v.monto_pagado, v.balance_pendiente, v.tipo_pago, " +
                "v.estado_pago, " +
                "ISNULL(CAST(v.id_comprobante AS VARCHAR), '') AS ncf, " +
                "v.id_empleado, " +
                "ISNULL(e.nombre + ' ' + e.apellido, '') AS empleado " +
                "FROM tbl_venta v " +
                "LEFT JOIN tbl_orden_cliente o ON v.id_orden = o.id_orden " +
                "LEFT JOIN tbl_empleado e ON v.id_empleado = e.id_empleado " +
                "ORDER BY v.fecha_venta DESC";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date d = rs.getDate("fecha_venta");
                listaVentas.add(new ventaModelo(
                        rs.getInt("id_venta"),
                        rs.getInt("id_orden"),
                        d != null ? d.toLocalDate() : null,
                        rs.getString("cliente"),
                        rs.getDouble("subtotal"),
                        rs.getDouble("descuento"),
                        rs.getDouble("itbis"),
                        rs.getDouble("monto_total"),
                        rs.getDouble("monto_pagado"),
                        rs.getDouble("balance_pendiente"),
                        rs.getString("tipo_pago"),
                        rs.getString("estado_pago"),
                        "",
                        rs.getString("ncf"),
                        rs.getInt("id_empleado"),
                        rs.getString("empleado")
                ));
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar ventas", "Ocurrio un error inesperado. Intente nuevamente.");
        }
    }

    private void cargarPagos(int idVenta) {
        listaPagos.clear();
        idVentaSeleccionada = idVenta;
        String sql = "SELECT id_pago, id_venta, fecha_pago, monto_pagado, metodo_pago, " +
                "numero_referencia, observaciones FROM tbl_pago_venta WHERE id_venta = ?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date d = rs.getDate("fecha_pago");
                listaPagos.add(new pagoVentaModelo(
                        rs.getInt("id_pago"),
                        rs.getInt("id_venta"),
                        d != null ? d.toLocalDate() : null,
                        rs.getDouble("monto_pagado"),
                        rs.getString("metodo_pago"),
                        rs.getString("numero_referencia") != null ? rs.getString("numero_referencia") : "",
                        rs.getString("observaciones") != null ? rs.getString("observaciones") : ""
                ));
            }
            if (!listaPagos.isEmpty())
                cbMetodoPago.setValue(listaPagos.get(0).getMetodoPago());
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar pagos", "Ocurrio un error inesperado. Intente nuevamente.");
        }
    }

    private void cargarEnFormulario(ventaModelo v) {
        txtIdVenta.setText(String.valueOf(v.getIdVenta()));
        txtIdOrden.setText(String.valueOf(v.getIdOrden()));
        lblInfoOrden.setText(v.getCliente());
        dpFechaVenta.setValue(v.getFechaVenta());
        txtIdEmpleado.setText(String.valueOf(v.getIdEmpleado()));
        lblNombreEmpleado.setText(v.getEmpleado());
        idEmpleadoSeleccionado = v.getIdEmpleado();
        cbTipoPago.setValue(v.getTipoPago());
        cbMetodoPago.setValue(v.getMetodoPago());
        txtNcf.setText(v.getNcf());
        subtotalBase = v.getSubtotal();
        txtSubtotal.setText(String.format("%.2f", v.getSubtotal()));
        txtDescuento.setText(String.format("%.2f", v.getDescuento()));
        txtItbis.setText(String.format("%.2f", v.getItbis()));
        txtTotal.setText(String.format("%.2f", v.getMontoTotal()));
        actualizarResumenUI(v.getMontoPagado(), v.getMontoTotal(), v.getEstadoPago());
        idVentaSeleccionada = v.getIdVenta();
    }

    private void actualizarResumenUI(double pagado, double total, String estado) {
        double balance = total - pagado;
        txtMontoPagado.setText(String.format("%.2f", pagado));
        txtBalancePendiente.setText(String.format("%.2f", balance));
        txtEstadoPago.setText(estado);
        String color = switch (estado) {
            case "Pagado"    -> "-fx-text-fill:#2e7d32;";
            case "Parcial"   -> "-fx-text-fill:#e65100;";
            default          -> "-fx-text-fill:#c0392b;";
        };
        txtEstadoPago.setStyle("-fx-background-color:#f0eaf8; -fx-font-weight:bold; " +
                "-fx-border-color:purple; -fx-border-radius:6; " +
                "-fx-background-radius:6; " + color);
    }

    private void generarSiguienteId() {
        String sql = "SELECT ISNULL(MAX(id_venta), 0) + 1 AS siguiente FROM tbl_venta";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) txtIdVenta.setText(String.valueOf(rs.getInt("siguiente")));
        } catch (Exception e) {
            txtIdVenta.setText("1");
        }
    }

    private boolean validarCamposVenta() {
        if (dpFechaVenta.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha requerida", "Selecciona la fecha de venta.");
            return false;
        }
        if (idEmpleadoSeleccionado == 0) {
            mostrarAlerta(Alert.AlertType.WARNING, "Empleado requerido", "Busca y selecciona el empleado responsable.");
            return false;
        }
        if (cbTipoPago.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Tipo pago requerido", "Selecciona el tipo de pago.");
            return false;
        }
        if (txtSubtotal.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Subtotal requerido", "Carga una orden o ingresa el subtotal.");
            return false;
        }
        return true;
    }

    private boolean validarCamposAbono() {
        if (dpFechaPago.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha requerida", "Selecciona la fecha del abono.");
            return false;
        }
        if (txtMontoAbono.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Monto requerido", "Ingresa el monto del abono.");
            return false;
        }
        if (cbMetodoPagoAbono.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "MÃ©todo requerido", "Selecciona el mÃ©todo de pago.");
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
    // generar reporte Jasper

    @FXML
    private void generarReporte() {
        if (idVentaSeleccionada == 0) {
            mostrarAlerta(Alert.AlertType.WARNING, "AtenciÃ³n", "Selecciona una venta de la tabla antes de generar el reporte.");
            return;
        }
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("ID_VENTA", idVentaSeleccionada);
        params.put("LOGO", getClass().getResourceAsStream("/com/example/chocolateria/logo.png"));
        try (java.sql.Connection conn = new conexion().establecerConexion()) {
            JasperReportUtil.mostrarReporte("/reportes/facturalachoco.jrxml", params, conn);
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al generar reporte", "Ocurrio un error inesperado. Intente nuevamente.");
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
    @FXML private void irAConsultaPagosVenta(javafx.event.ActionEvent e)  { Navegacion.irA("/vistasFinales/vistaConsultaPagosVenta.fxml", e); }
    @FXML private void salir(javafx.event.ActionEvent e)                  { Navegacion.salir(e); }

    private int estadoActual = 0;

    // estado de botones
    // estado: 0=libre/nuevo 1=encontrado 2=editando
    private void actualizarBotones(int estado) {
        this.estadoActual = estado;
        // estado: 0=libre/nuevo 1=encontrado 2=editando
        btnBuscar.setDisable(false);
        btnBuscar.setStyle("-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");
        btnLimpiar.setDisable(false);
        btnLimpiar.setStyle("-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");
        boolean actRegistrar = (estado == 0);
        btnRegistrar.setDisable(false);
        btnRegistrar.setStyle(actRegistrar ? "-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#e8d5f0; -fx-text-fill:#9b6baf; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
        boolean actEditar = (estado != 0);
        btnEditar.setDisable(false);
        btnEditar.setStyle(actEditar ? "-fx-background-color:#5a2d82; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#d9c8f0; -fx-text-fill:#9b6baf; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
        boolean actEliminar = (estado != 0);
        btnEliminar.setDisable(false);
        btnEliminar.setStyle(actEliminar ? "-fx-background-color:#a83c5b; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#f5d0da; -fx-text-fill:#c47a8a; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
        boolean actAbono = (estado != 0);
        btnAbono.setDisable(false);
        btnAbono.setStyle(actAbono ? "-fx-background-color:#2e7d32; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#c8e6c9; -fx-text-fill:#5a9060; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
    }
}
