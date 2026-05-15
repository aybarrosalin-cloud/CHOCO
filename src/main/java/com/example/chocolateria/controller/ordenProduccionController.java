package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.ordenProduccionModelo;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;

import java.sql.*;
import java.time.LocalDate;

public class ordenProduccionController {

    @FXML private TextField    txtIdOrden;
    @FXML private RadioButton  rbPedido;
    @FXML private RadioButton  rbProduccion;
    @FXML private VBox         vboxCliente;
    @FXML private TextField    txtIdCliente;
    @FXML private Label        lblNombreCliente;
    @FXML private DatePicker   dpFechaOrden;
    @FXML private DatePicker   dpFechaInicio;
    @FXML private DatePicker   dpFechaEntrega;
    @FXML private TextField    txtIdResponsable;
    @FXML private Label        lblNombreResponsable;
    @FXML private ComboBox<String> cbEstado;
    @FXML private ComboBox<String> cbPrioridad;
    @FXML private CheckBox     chkBombones;
    @FXML private CheckBox     chkTabletas;
    @FXML private CheckBox     chkTrufas;
    @FXML private CheckBox     chkBaños;
    @FXML private CheckBox     chkRellenos;
    @FXML private TextArea     txtMateriales;
    @FXML private TextArea     txtObservaciones;

    private final conexion con = new conexion();
    private int idClienteSeleccionado  = 0;
    private int idResponsableSeleccionado = 0;
    private ordenProduccionModelo ordenCargada = null;

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

        cbEstado.setItems(FXCollections.observableArrayList(
                "Pendiente", "En proceso", "Completada", "Cancelada"));

        cbPrioridad.setItems(FXCollections.observableArrayList(
                "Alta", "Media", "Baja"));

        ToggleGroup tg = new ToggleGroup();
        rbPedido.setToggleGroup(tg);
        rbProduccion.setToggleGroup(tg);
        rbProduccion.setSelected(true);
        vboxCliente.setVisible(false);
        vboxCliente.setManaged(false);

        tg.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean esPedido = rbPedido.isSelected();
            vboxCliente.setVisible(esPedido);
            vboxCliente.setManaged(esPedido);
            if (!esPedido) {
                txtIdCliente.clear();
                lblNombreCliente.setText("");
                idClienteSeleccionado = 0;
            }
        });


        dpFechaInicio.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        dpFechaInicio.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isBefore(LocalDate.now())) {
                dpFechaInicio.setValue(null);
                mostrarAlerta(Alert.AlertType.WARNING, "Fecha inválida", "La fecha de inicio no puede ser anterior a hoy.");
            }
        });
        dpFechaEntrega.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        dpFechaEntrega.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isBefore(LocalDate.now())) {
                dpFechaEntrega.setValue(null);
                mostrarAlerta(Alert.AlertType.WARNING, "Fecha inválida", "La fecha de entrega no puede ser anterior a hoy.");
            }
        });

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() {
                return null;
            }
        };
        new Thread(cargar).start();
        generarSiguienteId();
    }

    @FXML
    private void abrirBuscadorCliente() {
        popupBuscarClienteController.mostrar(c -> {
            txtIdCliente.setText(String.valueOf(c.getIdCliente()));
            buscarCliente();
        });
    }

    @FXML
    private void buscarCliente() {
        String idTexto = txtIdCliente.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Escribe el ID del cliente.");
            return;
        }
        try {
            int id = Integer.parseInt(idTexto);
            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT nombre, apellido FROM tbl_cliente WHERE id_cliente = ?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    idClienteSeleccionado = id;
                    lblNombreCliente.setText(rs.getString("nombre") + " " + rs.getString("apellido"));
                } else {
                    lblNombreCliente.setText("No encontrado");
                    idClienteSeleccionado = 0;
                }
            }
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID inválido", "El ID debe ser un número.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void buscarResponsable() {
        String idTexto = txtIdResponsable.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Escribe el ID del empleado responsable.");
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
                    idResponsableSeleccionado = id;
                    lblNombreResponsable.setText(rs.getString("nombre") + " " + rs.getString("apellido"));
                } else {
                    lblNombreResponsable.setText("No encontrado");
                    idResponsableSeleccionado = 0;
                }
            }
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID inválido", "El ID debe ser un número.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void guardarOrden() {
        if (estadoActual == 1) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Acción no disponible", "Ya hay un registro cargado. Usa 'Editar' para modificarlo o 'Limpiar' para crear uno nuevo.");
            return;
        }
        if (!validarCampos()) return;

        String categorias = obtenerCategorias();
        String tipoOrden  = rbPedido.isSelected() ? "Pedido" : "Produccion";

        String sql = "INSERT INTO tbl_orden_produccion " +
                "(tipo_orden, id_cliente, fecha_orden, fecha_inicio, fecha_entrega, " +
                "id_responsable, estado, prioridad, categoria, materiales, observaciones) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, tipoOrden);
            ps.setObject(2, idClienteSeleccionado > 0 ? idClienteSeleccionado : null);
            ps.setDate(3, Date.valueOf(dpFechaOrden.getValue()));
            ps.setDate(4, Date.valueOf(dpFechaInicio.getValue()));
            ps.setDate(5, Date.valueOf(dpFechaEntrega.getValue()));
            ps.setInt(6, idResponsableSeleccionado);
            ps.setString(7, cbEstado.getValue());
            ps.setString(8, cbPrioridad.getValue());
            ps.setString(9, categorias);
            ps.setString(10, txtMateriales.getText().trim());
            ps.setString(11, txtObservaciones.getText().trim());
            ps.executeUpdate();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Orden de producción guardada correctamente.");
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
        if (!validarCampos()) return;

        String categorias = obtenerCategorias();
        String tipoOrden  = rbPedido.isSelected() ? "Pedido" : "Produccion";

        String sql = "UPDATE tbl_orden_produccion SET tipo_orden=?, id_cliente=?, fecha_orden=?, " +
                "fecha_inicio=?, fecha_entrega=?, id_responsable=?, estado=?, prioridad=?, " +
                "categoria=?, materiales=?, observaciones=? WHERE id_orden=?";

        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tipoOrden);
            ps.setObject(2, idClienteSeleccionado > 0 ? idClienteSeleccionado : null);
            ps.setDate(3, Date.valueOf(dpFechaOrden.getValue()));
            ps.setDate(4, Date.valueOf(dpFechaInicio.getValue()));
            ps.setDate(5, Date.valueOf(dpFechaEntrega.getValue()));
            ps.setInt(6, idResponsableSeleccionado);
            ps.setString(7, cbEstado.getValue());
            ps.setString(8, cbPrioridad.getValue());
            ps.setString(9, categorias);
            ps.setString(10, txtMateriales.getText().trim());
            ps.setString(11, txtObservaciones.getText().trim());
            ps.setInt(12, ordenCargada.getIdOrden());
            ps.executeUpdate();

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
        confirm.setContentText("¿Eliminar la orden #" + ordenCargada.getIdOrden() + "?");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try (Connection conn = con.establecerConexion();
                     PreparedStatement ps = conn.prepareStatement(
                             "DELETE FROM tbl_orden_produccion WHERE id_orden=?")) {
                    ps.setInt(1, ordenCargada.getIdOrden());
                    ps.executeUpdate();
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
        String idTexto = txtIdOrden.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Escribe un ID en el campo para buscar.");
            return;
        }
        int idBuscar;
        try {
            idBuscar = Integer.parseInt(idTexto);
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID inválido", "El ID debe ser un número entero.");
            return;
        }

        String sql = "SELECT o.id_orden, o.tipo_orden, o.fecha_inicio, o.fecha_orden, o.fecha_entrega, " +
                "o.id_responsable, e.nombre + ' ' + e.apellido AS responsable, " +
                "o.estado, o.prioridad, o.categoria, o.materiales, o.observaciones, " +
                "ISNULL(c.nombre + ' ' + c.apellido, '') AS cliente " +
                "FROM tbl_orden_produccion o " +
                "LEFT JOIN tbl_empleado e ON o.id_responsable = e.id_empleado " +
                "LEFT JOIN tbl_cliente c ON o.id_cliente = c.id_cliente " +
                "WHERE o.id_orden = ?";

        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idBuscar);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ordenCargada = mapearOrden(rs);
                cargarEnFormulario(ordenCargada);
                actualizarBotones(1);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Encontrado", "Orden cargada en el formulario.");
            } else {
                mostrarAlerta(Alert.AlertType.WARNING, "No encontrado", "No existe una orden con el ID " + idBuscar + ".");
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de búsqueda", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void limpiarCampos() {
        actualizarBotones(0);
        txtIdOrden.clear();
        rbProduccion.setSelected(true);
        txtIdCliente.clear();
        lblNombreCliente.setText("");
        idClienteSeleccionado = 0;
        dpFechaOrden.setValue(null);
        dpFechaInicio.setValue(null);
        dpFechaEntrega.setValue(null);
        txtIdResponsable.clear();
        lblNombreResponsable.setText("");
        idResponsableSeleccionado = 0;
        cbEstado.setValue(null);
        cbPrioridad.setValue(null);
        chkBombones.setSelected(false);
        chkTabletas.setSelected(false);
        chkTrufas.setSelected(false);
        chkBaños.setSelected(false);
        chkRellenos.setSelected(false);
        txtMateriales.clear();
        txtObservaciones.clear();
        ordenCargada = null;
        generarSiguienteId();
    }

    private ordenProduccionModelo mapearOrden(ResultSet rs) throws SQLException {
        Date dInicio   = rs.getDate("fecha_inicio");
        Date dOrden    = rs.getDate("fecha_orden");
        Date dEntrega  = rs.getDate("fecha_entrega");
        return new ordenProduccionModelo(
                rs.getInt("id_orden"),
                rs.getString("tipo_orden"),
                dInicio  != null ? dInicio.toLocalDate()  : null,
                dOrden   != null ? dOrden.toLocalDate()   : null,
                dEntrega != null ? dEntrega.toLocalDate() : null,
                rs.getInt("id_responsable"),
                rs.getString("responsable"),
                rs.getString("estado"),
                rs.getString("prioridad"),
                rs.getString("categoria"),
                rs.getString("materiales"),
                rs.getString("observaciones"),
                rs.getString("cliente")
        );
    }

    private void cargarEnFormulario(ordenProduccionModelo o) {
        txtIdOrden.setText(String.valueOf(o.getIdOrden()));
        if ("Pedido".equals(o.getTipoOrden())) {
            rbPedido.setSelected(true);
            lblNombreCliente.setText(o.getCliente());
        } else {
            rbProduccion.setSelected(true);
        }
        dpFechaOrden.setValue(o.getFechaOrden());
        dpFechaInicio.setValue(o.getFechaInicio());
        dpFechaEntrega.setValue(o.getFechaEntrega());
        txtIdResponsable.setText(String.valueOf(o.getIdResponsable()));
        lblNombreResponsable.setText(o.getResponsable());
        idResponsableSeleccionado = o.getIdResponsable();
        cbEstado.setValue(o.getEstado());
        cbPrioridad.setValue(o.getPrioridad());
        String cat = o.getCategoria() != null ? o.getCategoria() : "";
        chkBombones.setSelected(cat.contains("Bombones"));
        chkTabletas.setSelected(cat.contains("Tabletas"));
        chkTrufas.setSelected(cat.contains("Trufas"));
        chkBaños.setSelected(cat.contains("Baños"));
        chkRellenos.setSelected(cat.contains("Rellenos"));
        txtMateriales.setText(o.getMateriales());
        txtObservaciones.setText(o.getObservaciones());
    }

    private void generarSiguienteId() {
        String sql = "SELECT ISNULL(MAX(id_orden), 0) + 1 AS siguiente FROM tbl_orden_produccion";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) txtIdOrden.setText(String.valueOf(rs.getInt("siguiente")));
        } catch (Exception e) {
            txtIdOrden.setText("1");
        }
    }

    private String obtenerCategorias() {
        StringBuilder sb = new StringBuilder();
        if (chkBombones.isSelected()) sb.append("Bombones, ");
        if (chkTabletas.isSelected()) sb.append("Tabletas, ");
        if (chkTrufas.isSelected())   sb.append("Trufas, ");
        if (chkBaños.isSelected())    sb.append("Baños de chocolate, ");
        if (chkRellenos.isSelected()) sb.append("Rellenos, ");
        if (sb.length() > 0) sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private boolean validarCampos() {
        if (dpFechaOrden.getValue() == null || dpFechaInicio.getValue() == null ||
                dpFechaEntrega.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos vacíos", "Por favor completa las fechas.");
            return false;
        }
        if (dpFechaInicio.getValue().isBefore(LocalDate.now())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha inválida", "La fecha de inicio no puede ser anterior a hoy.");
            return false;
        }
        if (dpFechaEntrega.getValue().isBefore(LocalDate.now())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha inválida", "La fecha de entrega no puede ser anterior a hoy.");
            return false;
        }
        if (idResponsableSeleccionado == 0) {
            mostrarAlerta(Alert.AlertType.WARNING, "Responsable requerido", "Busca y selecciona un responsable por ID.");
            return false;
        }
        if (cbEstado.getValue() == null || cbPrioridad.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos vacíos", "Selecciona estado y prioridad.");
            return false;
        }
        if (obtenerCategorias().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Categoría requerida", "Selecciona al menos una categoría.");
            return false;
        }
        if (rbPedido.isSelected() && idClienteSeleccionado == 0) {
            mostrarAlerta(Alert.AlertType.WARNING, "Cliente requerido", "Busca y selecciona un cliente para la orden de pedido.");
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
    @FXML private void irAConsultaOrdenProduccion(javafx.event.ActionEvent e){ Navegacion.irA("/vistasFinales/vistaConsultaOrdenProduccion.fxml", e); }
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

    // estado de botones
    // estado: 0=libre/nuevo 1=encontrado 2=editando
    private void actualizarBotones(int estado) {
        this.estadoActual = estado;
        // estado: 0=libre/nuevo 1=encontrado 2=editando
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
