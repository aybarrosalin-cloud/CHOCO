package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.abonoCompraModelo;
import com.example.chocolateria.modelo.deudaCompraModelo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.sql.*;
import java.time.LocalDate;

public class pagoCompraController {

    // datos generales de la deuda
    @FXML private TextField    txtIdDeuda;
    @FXML private TextField    txtIdRecepcion;
    @FXML private Label        lblInfoRecepcion;
    @FXML private TextField    txtRncSuplidor;
    @FXML private DatePicker   dpFechaDeuda;
    @FXML private TextField    txtMontoTotal;
    @FXML private TextField    txtMontoPagado;
    @FXML private TextField    txtMontoPendiente;
    @FXML private TextField    txtEstado;
    @FXML private TextArea     txtObservaciones;
    @FXML private TextField    txtBuscarTabla;

    // abono
    @FXML private DatePicker       dpFechaAbono;
    @FXML private TextField        txtMontoAbono;
    @FXML private ComboBox<String> cbMetodoPago;
    @FXML private TextField        txtNumeroReferencia;

    // tabla abonos
    @FXML private TableView<abonoCompraModelo>                tablaAbonos;
    @FXML private TableColumn<abonoCompraModelo, Number>      colAbonoId;
    @FXML private TableColumn<abonoCompraModelo, LocalDate>   colAbonoFecha;
    @FXML private TableColumn<abonoCompraModelo, Number>      colAbonoMonto;
    @FXML private TableColumn<abonoCompraModelo, String>      colAbonoMetodo;
    @FXML private TableColumn<abonoCompraModelo, String>      colAbonoRef;

    // tabla deudas
    @FXML private TableView<deudaCompraModelo>                tablaDeudas;
    @FXML private TableColumn<deudaCompraModelo, Number>      colId;
    @FXML private TableColumn<deudaCompraModelo, String>      colOrden;
    @FXML private TableColumn<deudaCompraModelo, String>      colRnc;
    @FXML private TableColumn<deudaCompraModelo, LocalDate>   colFecha;
    @FXML private TableColumn<deudaCompraModelo, Number>      colTotal;
    @FXML private TableColumn<deudaCompraModelo, Number>      colPagado;
    @FXML private TableColumn<deudaCompraModelo, Number>      colPendiente;
    @FXML private TableColumn<deudaCompraModelo, String>      colEstado;

    private final ObservableList<deudaCompraModelo>  listaDeudas  = FXCollections.observableArrayList();
    private final ObservableList<abonoCompraModelo>  listaAbonos  = FXCollections.observableArrayList();
    private final conexion con = new conexion();
    private int idDeudaSeleccionada = 0;

    @FXML private Label lblUsuario;
    @FXML private ImageView imgFotoPerfil;


    @FXML private Button btnBuscar, btnLimpiar;
    @FXML private Button btnRegistrar;
    @FXML private Button btnEliminar;
    @FXML private Button btnAbono;

    @FXML
    public void initialize() {
        actualizarBotones(0);
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);
        cbMetodoPago.setItems(FXCollections.observableArrayList(
                "Efectivo", "Transferencia", "Cheque", "Tarjeta de crédito", "Tarjeta de débito"));

        // columnas deudas (solo si existe la tabla)
        if (tablaDeudas != null) {
            colId.setCellValueFactory(d        -> d.getValue().idDeudaProperty());
            colOrden.setCellValueFactory(d     -> d.getValue().numeroOrdenProperty());
            colRnc.setCellValueFactory(d       -> d.getValue().rncSuplidorProperty());
            colFecha.setCellValueFactory(d     -> d.getValue().fechaDeudaProperty());
            colTotal.setCellValueFactory(d     -> d.getValue().montoTotalProperty());
            colPagado.setCellValueFactory(d    -> d.getValue().montoPagadoProperty());
            colPendiente.setCellValueFactory(d -> d.getValue().montoPendienteProperty());
            colEstado.setCellValueFactory(d    -> d.getValue().estadoProperty());
        }

        // columnas abonos
        colAbonoId.setCellValueFactory(d    -> d.getValue().idAbonoProperty());
        colAbonoFecha.setCellValueFactory(d -> d.getValue().fechaAbonoProperty());
        colAbonoMonto.setCellValueFactory(d -> d.getValue().montoAbonoProperty());
        colAbonoMetodo.setCellValueFactory(d-> d.getValue().metodoPagoProperty());
        colAbonoRef.setCellValueFactory(d   -> d.getValue().numeroReferenciaProperty());
        tablaAbonos.setItems(listaAbonos);

        // color por estado y filtro en tabla deudas
        if (tablaDeudas != null) {
            tablaDeudas.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(deudaCompraModelo d, boolean empty) {
                    super.updateItem(d, empty);
                    if (d == null || empty) { setStyle(""); return; }
                    switch (d.getEstado()) {
                        case "Saldado"   -> setStyle("-fx-background-color:#e8f5e9;");
                        case "Pendiente" -> setStyle("-fx-background-color:#fde8e8;");
                        case "Parcial"   -> setStyle("-fx-background-color:#fff8e1;");
                        default          -> setStyle("");
                    }
                }
            });

            FilteredList<deudaCompraModelo> listaFiltrada = new FilteredList<>(listaDeudas, p -> true);
            if (txtBuscarTabla != null) {
                txtBuscarTabla.textProperty().addListener((obs, oldVal, newVal) ->
                        listaFiltrada.setPredicate(d -> {
                            if (newVal == null || newVal.isBlank()) return true;
                            String f = newVal.toLowerCase();
                            return d.getRncSuplidor().toLowerCase().contains(f)
                                    || d.getEstado().toLowerCase().contains(f)
                                    || d.getNumeroOrden().toLowerCase().contains(f);
                        })
                );
            }
            tablaDeudas.setItems(listaFiltrada);

            tablaDeudas.getSelectionModel().selectedItemProperty().addListener(
                    (obs, old, sel) -> {
                        if (sel != null) {
                            cargarEnFormulario(sel);
                            cargarAbonos(sel.getIdDeuda());
                            actualizarBotones(1);
                        }
                    }
            );
        }

        cargarDeudas();
        generarSiguienteId();
    }

    @FXML
    private void abrirBuscadorRecepcion() {
        popupBuscarRecepcionController.mostrar(id -> {
            txtIdRecepcion.setText(id);
            cargarDesdeRecepcion();
        });
    }

    // cargar datos desde recepcion
    @FXML
    private void cargarDesdeRecepcion() {
        String idTexto = txtIdRecepcion.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Escribe el ID de la recepción.");
            return;
        }
        try {
            int id = Integer.parseInt(idTexto);
            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT rnc_proveedor, numero_orden, monto_total, fecha_recepcion FROM tbl_recepcion WHERE id_recepcion = ?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    txtRncSuplidor.setText(rs.getString("rnc_proveedor"));
                    txtMontoTotal.setText(String.format("%.2f", rs.getDouble("monto_total")));
                    lblInfoRecepcion.setText("Orden: " + rs.getString("numero_orden"));
                    dpFechaDeuda.setValue(LocalDate.now());
                    actualizarMontos(0.0, rs.getDouble("monto_total"));
                } else {
                    mostrarAlerta(Alert.AlertType.WARNING, "No encontrado",
                            "No existe una recepción con el ID " + id + ".");
                }
            }
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID inválido", "El ID debe ser un número.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    // registrar deuda nueva
    @FXML
    private void registrarDeuda() {
        if (estadoActual == 1) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Acción no disponible", "Ya hay un registro cargado. Usa 'Limpiar' para registrar uno nuevo.");
            return;
        }
        if (!validarCamposDeuda()) return;

        String sql = "INSERT INTO tbl_deuda_compra (numero_orden, id_recepcion, rnc_suplidor, fecha_deuda, monto_total, observaciones) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String idRec = txtIdRecepcion.getText().trim();
            ps.setString(1, lblInfoRecepcion.getText().replace("Orden: ", ""));
            ps.setObject(2, idRec.isEmpty() ? null : Integer.parseInt(idRec));
            ps.setString(3, txtRncSuplidor.getText().trim());
            ps.setDate(4, Date.valueOf(dpFechaDeuda.getValue()));
            ps.setDouble(5, Double.parseDouble(txtMontoTotal.getText().trim()));
            ps.setString(6, txtObservaciones.getText().trim());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            int nuevoId = rs.next() ? rs.getInt(1) : 0;
            idDeudaSeleccionada = nuevoId;
            txtIdDeuda.setText(String.valueOf(nuevoId));

            double total = Double.parseDouble(txtMontoTotal.getText().trim());
            listaDeudas.add(0, new deudaCompraModelo(nuevoId,
                    lblInfoRecepcion.getText().replace("Orden: ", ""),
                    idRec.isEmpty() ? 0 : Integer.parseInt(idRec),
                    txtRncSuplidor.getText().trim(),
                    dpFechaDeuda.getValue(), total, 0.0, total, "Pendiente",
                    txtObservaciones.getText().trim()));

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito",
                    "Deuda registrada. Ahora puedes ir registrando los abonos.");

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al registrar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    // registrar abono a la deuda seleccionada
    @FXML
    private void registrarAbono() {
        if (idDeudaSeleccionada == 0) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención",
                    "Selecciona una deuda de la tabla o regístrala primero.");
            return;
        }
        if (!validarCamposAbono()) return;

        try {
            double montoAbono = Double.parseDouble(txtMontoAbono.getText().trim());

            // verificar que el abono no supere el pendiente
            double pendiente = Double.parseDouble(txtMontoPendiente.getText().trim());
            if (montoAbono > pendiente) {
                mostrarAlerta(Alert.AlertType.WARNING, "Monto excedido",
                        String.format("El abono (RD$ %.2f) supera el monto pendiente (RD$ %.2f).", montoAbono, pendiente));
                return;
            }

            String sql = "INSERT INTO tbl_abono_compra (id_deuda, fecha_abono, monto_abono, metodo_pago, numero_referencia, observaciones) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, idDeudaSeleccionada);
                ps.setDate(2, Date.valueOf(dpFechaAbono.getValue()));
                ps.setDouble(3, montoAbono);
                ps.setString(4, cbMetodoPago.getValue());
                ps.setString(5, txtNumeroReferencia.getText().trim());
                ps.setString(6, "");
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                int nuevoAbonoId = rs.next() ? rs.getInt(1) : 0;

                // actualizar monto_pagado en la tabla
                double nuevoPagado = Double.parseDouble(txtMontoPagado.getText().trim()) + montoAbono;
                double total       = Double.parseDouble(txtMontoTotal.getText().trim());
                String nuevoEstado = nuevoPagado >= total ? "Saldado"
                        : nuevoPagado > 0     ? "Parcial"
                        : "Pendiente";

                try (PreparedStatement psUpd = conn.prepareStatement(
                        "UPDATE tbl_deuda_compra SET monto_pagado=?, estado=? WHERE id_deuda=?")) {
                    psUpd.setDouble(1, nuevoPagado);
                    psUpd.setString(2, nuevoEstado);
                    psUpd.setInt(3, idDeudaSeleccionada);
                    psUpd.executeUpdate();
                }

                // actualizar ui
                listaAbonos.add(new abonoCompraModelo(nuevoAbonoId, idDeudaSeleccionada,
                        dpFechaAbono.getValue(), montoAbono, cbMetodoPago.getValue(),
                        txtNumeroReferencia.getText().trim(), ""));

                actualizarMontos(nuevoPagado, total);
                txtEstado.setText(nuevoEstado);
                colorearEstado(nuevoEstado);

                // actualizar fila en tabla principal
                for (deudaCompraModelo d : listaDeudas) {
                    if (d.getIdDeuda() == idDeudaSeleccionada) {
                        d.setMontoPagado(nuevoPagado);
                        d.setMontoPendiente(total - nuevoPagado);
                        d.setEstado(nuevoEstado);
                        tablaDeudas.refresh();
                        break;
                    }
                }

                limpiarCamposAbono();
                mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito",
                        String.format("Abono de RD$ %.2f registrado. Estado: %s", montoAbono, nuevoEstado));
            }

        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "Monto inválido", "El monto debe ser un número válido.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al registrar abono", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnBuscar() {
        actualizarBotones(0);
        String idTexto = txtIdDeuda.getText().trim();
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
        for (deudaCompraModelo d : listaDeudas) {
            if (d.getIdDeuda() == idBuscar) {
                if (tablaDeudas != null) {
                    tablaDeudas.getSelectionModel().select(d);
                    tablaDeudas.scrollTo(d);
                }
                cargarEnFormulario(d);
                cargarAbonos(d.getIdDeuda());
                actualizarBotones(1);
                return;
            }
        }
        mostrarAlerta(Alert.AlertType.WARNING, "No encontrado", "No existe una deuda con el ID " + idBuscar + ".");
    }

    @FXML
    private void fnEliminar() {
        deudaCompraModelo sel = (tablaDeudas != null)
                ? tablaDeudas.getSelectionModel().getSelectedItem()
                : listaDeudas.stream()
                    .filter(d -> String.valueOf(d.getIdDeuda()).equals(txtIdDeuda.getText().trim()))
                    .findFirst().orElse(null);
        if (sel == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca una deuda por ID para poder eliminarla.");
            return;
        }
        if (!"Pendiente".equals(sel.getEstado())) {
            mostrarAlerta(Alert.AlertType.WARNING, "No permitido",
                    "Solo se pueden eliminar deudas en estado Pendiente sin abonos.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar la deuda #" + sel.getIdDeuda() + "?");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try (Connection conn = con.establecerConexion();
                     PreparedStatement ps = conn.prepareStatement(
                             "DELETE FROM tbl_deuda_compra WHERE id_deuda=?")) {
                    ps.setInt(1, sel.getIdDeuda());
                    ps.executeUpdate();
                    listaDeudas.remove(sel);
                    listaAbonos.clear();
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Deuda eliminada correctamente.");
                    limpiar();
                } catch (Exception e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar", "Ocurrió un error. Intente de nuevo.");
                }
            }
        });
    }

    @FXML
    private void limpiar() {
        actualizarBotones(0);
        txtIdDeuda.clear();
        txtIdRecepcion.clear();
        lblInfoRecepcion.setText("");
        txtRncSuplidor.clear();
        dpFechaDeuda.setValue(null);
        txtMontoTotal.clear();
        txtMontoPagado.clear();
        txtMontoPendiente.clear();
        txtEstado.clear();
        txtEstado.setStyle("-fx-background-color:#f0eaf8; -fx-font-weight:bold; -fx-border-color:purple; -fx-border-radius:6; -fx-background-radius:6;");
        txtObservaciones.clear();
        limpiarCamposAbono();
        listaAbonos.clear();
        idDeudaSeleccionada = 0;
        tablaDeudas.getSelectionModel().clearSelection();
        generarSiguienteId();
    }

    private void limpiarCamposAbono() {
        dpFechaAbono.setValue(null);
        txtMontoAbono.clear();
        cbMetodoPago.setValue(null);
        txtNumeroReferencia.clear();
    }

    private void cargarDeudas() {
        listaDeudas.clear();
        String sql = "SELECT id_deuda, numero_orden, id_recepcion, rnc_suplidor, fecha_deuda, " +
                "monto_total, monto_pagado, monto_pendiente, estado, observaciones " +
                "FROM tbl_deuda_compra ORDER BY fecha_deuda DESC";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date d = rs.getDate("fecha_deuda");
                listaDeudas.add(new deudaCompraModelo(
                        rs.getInt("id_deuda"),
                        rs.getString("numero_orden") != null ? rs.getString("numero_orden") : "",
                        rs.getInt("id_recepcion"),
                        rs.getString("rnc_suplidor"),
                        d != null ? d.toLocalDate() : null,
                        rs.getDouble("monto_total"),
                        rs.getDouble("monto_pagado"),
                        rs.getDouble("monto_pendiente"),
                        rs.getString("estado"),
                        rs.getString("observaciones") != null ? rs.getString("observaciones") : ""
                ));
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar deudas", "Ocurrió un error. Intente de nuevo.");
        }
    }

    private void cargarAbonos(int idDeuda) {
        listaAbonos.clear();
        idDeudaSeleccionada = idDeuda;
        String sql = "SELECT id_abono, id_deuda, fecha_abono, monto_abono, metodo_pago, numero_referencia, observaciones " +
                "FROM tbl_abono_compra WHERE id_deuda = ? ORDER BY fecha_abono";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDeuda);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date d = rs.getDate("fecha_abono");
                listaAbonos.add(new abonoCompraModelo(
                        rs.getInt("id_abono"),
                        rs.getInt("id_deuda"),
                        d != null ? d.toLocalDate() : null,
                        rs.getDouble("monto_abono"),
                        rs.getString("metodo_pago"),
                        rs.getString("numero_referencia") != null ? rs.getString("numero_referencia") : "",
                        rs.getString("observaciones") != null ? rs.getString("observaciones") : ""
                ));
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar abonos", "Ocurrió un error. Intente de nuevo.");
        }
    }

    private void cargarEnFormulario(deudaCompraModelo d) {
        txtIdDeuda.setText(String.valueOf(d.getIdDeuda()));
        txtIdRecepcion.setText(String.valueOf(d.getIdRecepcion()));
        lblInfoRecepcion.setText("Orden: " + d.getNumeroOrden());
        txtRncSuplidor.setText(d.getRncSuplidor());
        dpFechaDeuda.setValue(d.getFechaDeuda());
        txtMontoTotal.setText(String.format("%.2f", d.getMontoTotal()));
        txtObservaciones.setText(d.getObservaciones());
        actualizarMontos(d.getMontoPagado(), d.getMontoTotal());
        txtEstado.setText(d.getEstado());
        colorearEstado(d.getEstado());
        idDeudaSeleccionada = d.getIdDeuda();
    }

    private void actualizarMontos(double pagado, double total) {
        double pendiente = total - pagado;
        txtMontoPagado.setText(String.format("%.2f", pagado));
        txtMontoPendiente.setText(String.format("%.2f", pendiente));
    }

    private void colorearEstado(String estado) {
        String color = switch (estado) {
            case "Saldado"   -> "-fx-text-fill:#2e7d32;";
            case "Parcial"   -> "-fx-text-fill:#e65100;";
            default          -> "-fx-text-fill:#c0392b;";
        };
        txtEstado.setStyle("-fx-background-color:#f0eaf8; -fx-font-weight:bold; " +
                "-fx-border-color:purple; -fx-border-radius:6; -fx-background-radius:6; " + color);
    }

    private void generarSiguienteId() {
        String sql = "SELECT ISNULL(MAX(id_deuda), 0) + 1 AS siguiente FROM tbl_deuda_compra";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) txtIdDeuda.setText(String.valueOf(rs.getInt("siguiente")));
        } catch (Exception e) {
            txtIdDeuda.setText("1");
        }
    }

    private boolean validarCamposDeuda() {
        if (txtRncSuplidor.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "RNC requerido", "Ingresa el RNC del suplidor.");
            return false;
        }
        if (dpFechaDeuda.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha requerida", "Selecciona la fecha de la deuda.");
            return false;
        }
        if (txtMontoTotal.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Monto requerido", "Ingresa el monto total de la deuda.");
            return false;
        }
        try {
            Double.parseDouble(txtMontoTotal.getText().trim());
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Monto inválido", "El monto total debe ser un número válido.");
            return false;
        }
        return true;
    }

    private boolean validarCamposAbono() {
        if (dpFechaAbono.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha requerida", "Selecciona la fecha del abono.");
            return false;
        }
        if (txtMontoAbono.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Monto requerido", "Ingresa el monto del abono.");
            return false;
        }
        if (cbMetodoPago.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Método requerido", "Selecciona el método de pago.");
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
    @FXML private void irAConsultaPagosCompra(javafx.event.ActionEvent e) { Navegacion.irA("/vistasFinales/vistaConsultaPagosCompra.fxml", e); }
    @FXML private void salir(javafx.event.ActionEvent e)                  { Navegacion.salir(e); }

    private int estadoActual = 0;

    // estado de botones
    // 0=nuevo 1=viendo 2=editando
    private void actualizarBotones(int estado) {
        this.estadoActual = estado;
        // 0=nuevo 1=viendo 2=editando
        btnBuscar.setDisable(false);
        btnBuscar.setStyle("-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");
        btnLimpiar.setDisable(false);
        btnLimpiar.setStyle("-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");
        boolean actRegistrar = (estado == 0);
        btnRegistrar.setDisable(false);
        btnRegistrar.setStyle(actRegistrar ? "-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#e8d5f0; -fx-text-fill:#9b6baf; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
        boolean actEliminar = (estado != 0);
        btnEliminar.setDisable(false);
        btnEliminar.setStyle(actEliminar ? "-fx-background-color:#a83c5b; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#f5d0da; -fx-text-fill:#c47a8a; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
        boolean actAbono = (estado != 0);
        btnAbono.setDisable(false);
        btnAbono.setStyle(actAbono ? "-fx-background-color:#2e7d32; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#c8e6c9; -fx-text-fill:#5a9060; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
    }
}
