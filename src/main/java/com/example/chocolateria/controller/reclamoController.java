package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.reclamoModelo;
import com.example.chocolateria.modelo.ordenClienteModelo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class reclamoController {

    @FXML private TextField        txtId;
    @FXML private TextField        txtIdCliente;
    @FXML private TextField        txtNombreCliente;
    @FXML private TextField        txtIdEmpleado;
    @FXML private TextField        txtNombreEmpleado;
    @FXML private ComboBox<String> cbTipoReclamo;
    @FXML private ComboBox<String> cbEstado;
    @FXML private ComboBox<String> cbOrden;
    @FXML private TextArea         txtDescripcion;

    // radiobuttons prioridad
    @FXML private RadioButton rbAlta;
    @FXML private RadioButton rbMedia;
    @FXML private RadioButton rbBaja;

    private reclamoModelo reclamoCargado = null;
    private final ObservableList<ordenClienteModelo> listaOrdenes = FXCollections.observableArrayList();
    private final conexion con = new conexion();
    private final Map<String, Integer> mapaOrdenes = new HashMap<>();
    private ToggleGroup grupoPrioridad;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    @FXML private Button btnBuscar, btnLimpiar;
    @FXML private Button btnGuardar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    @FXML
    public void initialize() {
        actualizarBotones(0);
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        cbTipoReclamo.setItems(FXCollections.observableArrayList(
            "Producto en mal estado",
            "Producto incorrecto",
            "Entrega tardía",
            "Producto dañado en envio",
            "Cantidad incorrecta",
            "Problema de facturacion",
            "Mal servicio al cliente",
            "Producto no recibido",
            "Calidad no satisfactoria",
            "Otro"
        ));

        cbEstado.setItems(FXCollections.observableArrayList(
            "Abierto", "En proceso", "Resuelto", "Cerrado", "Rechazado"
        ));

        grupoPrioridad = new ToggleGroup();
        rbAlta.setToggleGroup(grupoPrioridad);
        rbMedia.setToggleGroup(grupoPrioridad);
        rbBaja.setToggleGroup(grupoPrioridad);
        rbMedia.setSelected(true);

        txtIdCliente.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) buscarNombreCliente();
        });
        txtIdCliente.setOnAction(e -> buscarNombreCliente());

        txtIdEmpleado.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) buscarNombreEmpleado();
        });
        txtIdEmpleado.setOnAction(e -> buscarNombreEmpleado());

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
        if (idTexto.isEmpty()) {
            txtNombreCliente.clear();
            cbOrden.getItems().clear();
            cbOrden.setValue(null);
            mapaOrdenes.clear();
            listaOrdenes.clear();
            return;
        }
        try {
            int id = Integer.parseInt(idTexto);
            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT nombre + ' ' + apellido AS nombre_completo FROM tbl_cliente WHERE id_cliente = ?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    txtNombreCliente.setText(rs.getString("nombre_completo"));
                    cargarOrdenesCliente(id);
                } else {
                    txtNombreCliente.clear();
                    cbOrden.getItems().clear();
                    cbOrden.setValue(null);
                    mapaOrdenes.clear();
                    listaOrdenes.clear();
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
    private void buscarNombreEmpleado() {
        String idTexto = txtIdEmpleado.getText().trim();
        if (idTexto.isEmpty()) { txtNombreEmpleado.clear(); return; }
        try {
            int id = Integer.parseInt(idTexto);
            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT nombre + ' ' + apellido AS nombre_completo FROM tbl_empleado WHERE id_empleado = ?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    txtNombreEmpleado.setText(rs.getString("nombre_completo"));
                } else {
                    txtNombreEmpleado.clear();
                    mostrarAlerta(Alert.AlertType.WARNING, "Empleado no encontrado",
                        "No existe un empleado con ID " + id + ".");
                }
            }
        } catch (NumberFormatException e) {
            txtNombreEmpleado.clear();
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    private void cargarOrdenesCliente(int idCliente) {
        listaOrdenes.clear();
        cbOrden.getItems().clear();
        mapaOrdenes.clear();

        String sql = "SELECT id_orden, cliente, fecha_registro, fecha_entrega, estado, metodo_pago " +
                     "FROM tbl_orden_cliente WHERE id_cliente = ? ORDER BY fecha_registro DESC";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int idOrden = rs.getInt("id_orden");
                Date dReg   = rs.getDate("fecha_registro");
                Date dEnt   = rs.getDate("fecha_entrega");
                LocalDate fechaReg = dReg != null ? dReg.toLocalDate() : null;
                LocalDate fechaEnt = dEnt != null ? dEnt.toLocalDate() : null;

                listaOrdenes.add(new ordenClienteModelo(
                    idOrden, idCliente, rs.getString("cliente"),
                    fechaReg, fechaEnt,
                    rs.getString("metodo_pago"),
                    rs.getString("estado"),
                    "", ""));

                String item = idOrden + " - " + fechaReg;
                cbOrden.getItems().add(item);
                mapaOrdenes.put(item, idOrden);
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar ordenes", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void guardar() {
        if (estadoActual == 1) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Acción no disponible",
                "Ya hay un registro cargado. Usa 'Editar' para modificarlo o 'Limpiar' para crear uno nuevo.");
            return;
        }
        if (!txtIdCliente.getText().trim().isEmpty() && txtNombreCliente.getText().trim().isEmpty()) buscarNombreCliente();
        if (!txtIdEmpleado.getText().trim().isEmpty() && txtNombreEmpleado.getText().trim().isEmpty()) buscarNombreEmpleado();
        if (!validarCampos()) return;

        int idCliente        = Integer.parseInt(txtIdCliente.getText().trim());
        String nombreCliente = txtNombreCliente.getText().trim();
        String itemOrden     = cbOrden.getValue();
        int idOrden          = itemOrden != null ? mapaOrdenes.getOrDefault(itemOrden, 0) : 0;
        String prioridad     = rbAlta.isSelected() ? "Alta" : rbBaja.isSelected() ? "Baja" : "Media";
        int idEmpleado       = txtIdEmpleado.getText().trim().isEmpty() ? 0
                               : Integer.parseInt(txtIdEmpleado.getText().trim());

        String sql = "INSERT INTO tbl_reclamo(id_cliente,cliente,id_orden,tipo_reclamo,estado,prioridad,descripcion,fecha_reclamo,id_empleado) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idCliente);
            ps.setString(2, nombreCliente);
            ps.setObject(3, idOrden > 0 ? idOrden : null);
            ps.setString(4, cbTipoReclamo.getValue());
            ps.setString(5, cbEstado.getValue());
            ps.setString(6, prioridad);
            ps.setString(7, txtDescripcion.getText().trim());
            ps.setDate(8, Date.valueOf(LocalDate.now()));
            ps.setObject(9, idEmpleado > 0 ? idEmpleado : null);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            int nuevoId = rs.next() ? rs.getInt(1) : 0;
            mostrarAlerta(Alert.AlertType.INFORMATION, "Exito", "Reclamo #" + nuevoId + " registrado correctamente.");

            final int idFinal       = nuevoId;
            final String nomCliente = nombreCliente;
            final String tipo       = cbTipoReclamo.getValue();
            final String prio       = prioridad;
            final String desc       = txtDescripcion.getText().trim();
            final String usuario    = SesionManager.getInstancia().getUsuario();
            new Thread(() ->
                EmailService.notificarNuevoReclamo(idFinal, nomCliente, tipo, prio, desc, usuario)
            ).start();

            limpiar();
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEditar() {
        actualizarBotones(2);
        if (reclamoCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atencion", "Busca un reclamo por ID primero para editar.");
            return;
        }
        if (!txtIdCliente.getText().trim().isEmpty() && txtNombreCliente.getText().trim().isEmpty()) buscarNombreCliente();
        if (!txtIdEmpleado.getText().trim().isEmpty() && txtNombreEmpleado.getText().trim().isEmpty()) buscarNombreEmpleado();
        if (!validarCampos()) return;

        int idCliente        = Integer.parseInt(txtIdCliente.getText().trim());
        String nombreCliente = txtNombreCliente.getText().trim();
        String itemOrden     = cbOrden.getValue();
        int idOrden          = itemOrden != null ? mapaOrdenes.getOrDefault(itemOrden, 0) : 0;
        String prioridad     = rbAlta.isSelected() ? "Alta" : rbBaja.isSelected() ? "Baja" : "Media";
        int idEmpleado       = txtIdEmpleado.getText().trim().isEmpty() ? 0
                               : Integer.parseInt(txtIdEmpleado.getText().trim());

        String sql = "UPDATE tbl_reclamo SET id_cliente=?,cliente=?,id_orden=?,tipo_reclamo=?,estado=?,prioridad=?,descripcion=?,id_empleado=? WHERE id_reclamo=?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setString(2, nombreCliente);
            ps.setObject(3, idOrden > 0 ? idOrden : null);
            ps.setString(4, cbTipoReclamo.getValue());
            ps.setString(5, cbEstado.getValue());
            ps.setString(6, prioridad);
            ps.setString(7, txtDescripcion.getText().trim());
            ps.setObject(8, idEmpleado > 0 ? idEmpleado : null);
            ps.setInt(9, reclamoCargado.getIdReclamo());
            ps.executeUpdate();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Exito", "Reclamo actualizado correctamente.");
            limpiar();
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al editar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEliminar() {
        if (reclamoCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atencion", "Busca un reclamo por ID primero para eliminar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("Eliminar el reclamo #" + reclamoCargado.getIdReclamo() + "?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try (Connection conn = con.establecerConexion();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM tbl_reclamo WHERE id_reclamo=?")) {
                    ps.setInt(1, reclamoCargado.getIdReclamo());
                    ps.executeUpdate();
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Exito", "Reclamo eliminado correctamente.");
                    limpiar();
                } catch (Exception e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar", "Ocurrió un error. Intente de nuevo.");
                }
            }
        });
    }

    @FXML
    private void fnBuscar() {
        actualizarBotones(0);
        String idTexto = txtId.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atencion", "Escribe un ID para buscar.");
            return;
        }
        try {
            int idBuscar = Integer.parseInt(idTexto);
            String sql = "SELECT id_reclamo,id_cliente,cliente,ISNULL(id_orden,0) AS id_orden,tipo_reclamo,estado,prioridad,descripcion,fecha_reclamo,ISNULL(id_empleado,0) AS id_empleado FROM tbl_reclamo WHERE id_reclamo=?";
            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idBuscar);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Date d = rs.getDate("fecha_reclamo");
                    reclamoModelo r = new reclamoModelo(
                        rs.getInt("id_reclamo"), rs.getInt("id_cliente"),
                        rs.getString("cliente"), rs.getInt("id_orden"),
                        rs.getString("tipo_reclamo"), rs.getString("estado"),
                        rs.getString("prioridad"),
                        rs.getString("descripcion") != null ? rs.getString("descripcion") : "",
                        d != null ? d.toLocalDate() : null,
                        rs.getInt("id_empleado"));
                    actualizarBotones(1);
                    cargarEnFormulario(r);
                } else {
                    mostrarAlerta(Alert.AlertType.WARNING, "No encontrado",
                        "No existe reclamo con el ID " + idBuscar + ".");
                }
            }
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID invalido", "El ID debe ser un numero entero.");
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de busqueda", ex.getMessage());
        }
    }

    @FXML
    private void limpiar() {
        actualizarBotones(0);
        txtId.clear();
        txtIdCliente.clear();
        txtNombreCliente.clear();
        txtIdEmpleado.clear();
        txtNombreEmpleado.clear();
        cbTipoReclamo.setValue(null);
        cbEstado.setValue(null);
        cbOrden.getItems().clear();
        cbOrden.setValue(null);
        txtDescripcion.clear();
        rbMedia.setSelected(true);
        listaOrdenes.clear();
        mapaOrdenes.clear();
        reclamoCargado = null;
        generarSiguienteId();
    }

    private void cargarEnFormulario(reclamoModelo r) {
        this.reclamoCargado = r;
        txtId.setText(String.valueOf(r.getIdReclamo()));
        txtIdCliente.setText(String.valueOf(r.getIdCliente()));
        txtNombreCliente.setText(r.getCliente());
        cargarOrdenesCliente(r.getIdCliente());
        if (r.getIdEmpleado() > 0) {
            txtIdEmpleado.setText(String.valueOf(r.getIdEmpleado()));
            buscarNombreEmpleado();
        } else {
            txtIdEmpleado.clear();
            txtNombreEmpleado.clear();
        }
        cbTipoReclamo.setValue(r.getTipoReclamo());
        cbEstado.setValue(r.getEstado());
        txtDescripcion.setText(r.getDescripcion());
        switch (r.getPrioridad()) {
            case "Alta" -> rbAlta.setSelected(true);
            case "Baja" -> rbBaja.setSelected(true);
            default     -> rbMedia.setSelected(true);
        }
        if (r.getIdOrden() > 0) {
            cbOrden.getItems().stream()
                .filter(i -> i.startsWith(r.getIdOrden() + " - "))
                .findFirst().ifPresent(cbOrden::setValue);
        }
    }

    private void generarSiguienteId() {
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT ISNULL(MAX(id_reclamo),0)+1 AS sig FROM tbl_reclamo")) {
            if (rs.next()) txtId.setText(String.valueOf(rs.getInt("sig")));
        } catch (Exception e) {
            txtId.setText("1");
        }
    }

    private boolean validarCampos() {
        if (txtNombreCliente.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Requerido", "Ingresa el ID del cliente.");
            return false;
        }
        if (cbTipoReclamo.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Requerido", "Selecciona el tipo de reclamo.");
            return false;
        }
        if (cbEstado.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Requerido", "Selecciona el estado.");
            return false;
        }
        if (txtDescripcion.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Requerido", "Escribe una descripcion del reclamo.");
            return false;
        }
        return true;
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String msg) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    private void generarReporte() {
        if (reclamoCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un reclamo por ID primero para generar el reporte.");
            return;
        }
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("ID_RECLAMO", reclamoCargado.getIdReclamo());
        params.put("LOGO", getClass().getResourceAsStream("/com/example/chocolateria/logo.png"));
        try (java.sql.Connection conn = new conexion().establecerConexion()) {
            JasperReportUtil.mostrarReporte("/reportes/lachoco_reclamo.jrxml", params, conn);
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al generar reporte", "Ocurrió un error. Intente de nuevo.");
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
    @FXML private void irAConsultaReclamos(javafx.event.ActionEvent e)    { Navegacion.irA("/vistasFinales/vistaConsultaReclamos.fxml", e); }
    @FXML private void irAGestionUsuarios(javafx.event.ActionEvent e)     { Navegacion.irA("/vistasFinales/vistaGestionUsuarios.fxml", e); }
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
        btnGuardar.setStyle(actGuardar
            ? "-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;"
            : "-fx-background-color:#e8d5f0; -fx-text-fill:#9b6baf; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
        boolean actEditar = (estado == 1);
        btnEditar.setDisable(false);
        btnEditar.setStyle(actEditar
            ? "-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;"
            : "-fx-background-color:#e8d5f0; -fx-text-fill:#9b6baf; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
        boolean actEliminar = (estado != 0);
        btnEliminar.setDisable(false);
        btnEliminar.setStyle(actEliminar
            ? "-fx-background-color:#a83c5b; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;"
            : "-fx-background-color:#f5d0da; -fx-text-fill:#c47a8a; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
    }
}
