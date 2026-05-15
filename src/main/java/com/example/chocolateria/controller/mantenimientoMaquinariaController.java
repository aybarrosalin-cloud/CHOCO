package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.mantenimientoMaquinariaModelo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.concurrent.Task;

import java.sql.*;
import java.time.LocalDate;

public class mantenimientoMaquinariaController {

    @FXML private TextField        txtId;
    @FXML private DatePicker       dpFechaMantenimiento;
    @FXML private DatePicker       dpProximoMantenimiento;
    @FXML private ComboBox<String> cbMaquina;
    @FXML private ComboBox<String> cbTecnico;
    @FXML private TextField        txtCosto;
    @FXML private ComboBox<String> cbEstadoMaquina;
    @FXML private ComboBox<String> cbTipoMantenimiento;
    @FXML private TextArea         taObservaciones;
    @FXML private Label            lblAlerta;

    private final conexion con = new conexion();
    private mantenimientoMaquinariaModelo mantenimientoCargado = null;

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

        cbEstadoMaquina.setItems(FXCollections.observableArrayList("Operativa", "En reparación", "Fuera de servicio"));
        cbTipoMantenimiento.setItems(FXCollections.observableArrayList("Preventivo", "Correctivo"));


        cbMaquina.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                cbTecnico.setValue(null);
                return;
            }
            try (Connection conn = con.establecerConexion();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT tecnico FROM tbl_asignacion_tecnico WHERE maquina = ?")) {
                ps.setString(1, newVal);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    cbTecnico.setValue(rs.getString("tecnico"));
                } else {
                    cbTecnico.setValue(null);
                }
            } catch (Exception e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo cargar el técnico asignado.");
            }
        });

        verificarAlertas();

        Task<Void> cargar = new Task<>() {
            @Override protected Void call() {
            cargarMaquinas();
            cargarTecnicos();
                return null;
            }
        };
        new Thread(cargar).start();

        dpProximoMantenimiento.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        dpProximoMantenimiento.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isBefore(LocalDate.now())) {
                dpProximoMantenimiento.setValue(null);
                mostrarAlerta(Alert.AlertType.WARNING, "Fecha inválida", "La fecha del próximo mantenimiento no puede ser anterior a hoy.");
            }
        });
    }

    private void cargarMaquinas() {
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement("SELECT nombre FROM tbl_maquinaria")) {
            ResultSet rs = ps.executeQuery();
            ObservableList<String> items = FXCollections.observableArrayList();
            while (rs.next()) items.add(rs.getString("nombre"));
            cbMaquina.setItems(items);
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudieron cargar las máquinas.");
        }
    }

    private void cargarTecnicos() {
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement("SELECT nombre FROM tbl_tecnico")) {
            ResultSet rs = ps.executeQuery();
            ObservableList<String> items = FXCollections.observableArrayList();
            while (rs.next()) items.add(rs.getString("nombre"));
            cbTecnico.setItems(items);
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudieron cargar los técnicos.");
        }
    }

    private void verificarAlertas() {
        String sql = "SELECT COUNT(DISTINCT maquina) AS cnt FROM tbl_mantenimiento_maquinaria " +
                "WHERE DATEDIFF(day, fecha_mantenimiento, GETDATE()) > 30";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                long cnt = rs.getLong("cnt");
                if (lblAlerta != null) {
                    lblAlerta.setText(cnt > 0
                        ? " " + cnt + " máquina(s) sin mantenimiento en más de 30 días"
                        : "");
                }
            }
        } catch (Exception ignored) {
            if (lblAlerta != null) lblAlerta.setText("");
        }
    }

    @FXML
    private void guardarMantenimiento() {
        if (estadoActual == 1) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Acción no disponible", "Ya hay un registro cargado. Usa 'Editar' para modificarlo o 'Limpiar' para crear uno nuevo.");
            return;
        }
        if (!validarCampos()) return;

        String sql = "INSERT INTO tbl_mantenimiento_maquinaria " +
                "(fecha_mantenimiento, fecha_proximo_mantenimiento, maquina, tecnico, costo, estado_maquina, tipo_mantenimiento, observaciones) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            double    costo   = Double.parseDouble(txtCosto.getText().trim());
            LocalDate proximo = dpProximoMantenimiento.getValue();

            ps.setDate(1, Date.valueOf(dpFechaMantenimiento.getValue()));
            ps.setDate(2, proximo != null ? Date.valueOf(proximo) : null);
            ps.setString(3, cbMaquina.getValue());
            ps.setString(4, cbTecnico.getValue());
            ps.setDouble(5, costo);
            ps.setString(6, cbEstadoMaquina.getValue());
            ps.setString(7, cbTipoMantenimiento.getValue());
            ps.setString(8, taObservaciones.getText().trim());
            ps.executeUpdate();

            verificarAlertas();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Mantenimiento guardado correctamente.");
            limpiarCampos();

        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Costo inválido", "El costo debe ser un número válido.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEditar() {
        actualizarBotones(2);
        if (mantenimientoCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un registro por ID antes de editar.");
            return;
        }
        if (!validarCampos()) return;

        String sql = "UPDATE tbl_mantenimiento_maquinaria " +
                "SET fecha_mantenimiento=?, fecha_proximo_mantenimiento=?, maquina=?, tecnico=?, " +
                "costo=?, estado_maquina=?, tipo_mantenimiento=?, observaciones=? WHERE id=?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            double    costo   = Double.parseDouble(txtCosto.getText().trim());
            LocalDate proximo = dpProximoMantenimiento.getValue();

            ps.setDate(1, Date.valueOf(dpFechaMantenimiento.getValue()));
            ps.setDate(2, proximo != null ? Date.valueOf(proximo) : null);
            ps.setString(3, cbMaquina.getValue());
            ps.setString(4, cbTecnico.getValue());
            ps.setDouble(5, costo);
            ps.setString(6, cbEstadoMaquina.getValue());
            ps.setString(7, cbTipoMantenimiento.getValue());
            ps.setString(8, taObservaciones.getText().trim());
            ps.setInt(9, mantenimientoCargado.getId());
            ps.executeUpdate();

            verificarAlertas();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Mantenimiento actualizado correctamente.");
            limpiarCampos();

        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Costo inválido", "El costo debe ser un número válido.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al editar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEliminar() {
        if (mantenimientoCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un registro por ID antes de eliminar.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar el mantenimiento de " + mantenimientoCargado.getMaquina()
                + " del " + mantenimientoCargado.getFechaMantenimiento() + "?");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try (Connection conn = con.establecerConexion();
                     PreparedStatement ps = conn.prepareStatement(
                             "DELETE FROM tbl_mantenimiento_maquinaria WHERE id=?")) {
                    ps.setInt(1, mantenimientoCargado.getId());
                    ps.executeUpdate();
                    mantenimientoCargado = null;
                    verificarAlertas();
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Registro eliminado correctamente.");
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
        String idTexto = txtId.getText().trim();
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

        String sql = "SELECT id, fecha_mantenimiento, fecha_proximo_mantenimiento, maquina, tecnico, costo, " +
                "estado_maquina, tipo_mantenimiento, observaciones " +
                "FROM tbl_mantenimiento_maquinaria WHERE id=?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idBuscar);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Date dProximo = rs.getDate("fecha_proximo_mantenimiento");
                mantenimientoCargado = new mantenimientoMaquinariaModelo(
                        rs.getInt("id"),
                        rs.getDate("fecha_mantenimiento").toLocalDate(),
                        dProximo != null ? dProximo.toLocalDate() : null,
                        rs.getString("maquina"),
                        rs.getString("tecnico"),
                        rs.getDouble("costo"),
                        rs.getString("estado_maquina"),
                        rs.getString("tipo_mantenimiento"),
                        rs.getString("observaciones"));
                cargarEnFormulario(mantenimientoCargado);
                actualizarBotones(1);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Encontrado", "Registro cargado en el formulario.");
            } else {
                mostrarAlerta(Alert.AlertType.WARNING, "No encontrado", "No existe un registro con el ID " + idBuscar + ".");
            }

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de búsqueda", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void limpiarCampos() {
        actualizarBotones(0);
        txtId.clear();
        dpFechaMantenimiento.setValue(null);
        dpProximoMantenimiento.setValue(null);
        cbMaquina.setValue(null);
        cbTecnico.setValue(null);
        txtCosto.clear();
        cbEstadoMaquina.setValue(null);
        cbTipoMantenimiento.setValue(null);
        taObservaciones.clear();
        mantenimientoCargado = null;
    }

    private void cargarEnFormulario(mantenimientoMaquinariaModelo m) {
        txtId.setText(String.valueOf(m.getId()));
        dpFechaMantenimiento.setValue(m.getFechaMantenimiento());
        dpProximoMantenimiento.setValue(m.getFechaProximoMantenimiento());
        cbMaquina.setValue(m.getMaquina());
        cbTecnico.setValue(m.getTecnico());
        txtCosto.setText(String.valueOf(m.getCosto()));
        cbEstadoMaquina.setValue(m.getEstadoMaquina());
        cbTipoMantenimiento.setValue(m.getTipoMantenimiento());
        taObservaciones.setText(m.getObservaciones());
    }

    private boolean validarCampos() {
        if (dpFechaMantenimiento.getValue() == null ||
                cbMaquina.getValue() == null            ||
                cbTecnico.getValue() == null            ||
                txtCosto.getText().trim().isEmpty()      ||
                cbEstadoMaquina.getValue() == null       ||
                cbTipoMantenimiento.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos vacíos", "Por favor completa todos los campos obligatorios.");
            return false;
        }
        if (dpProximoMantenimiento.getValue() != null && dpProximoMantenimiento.getValue().isBefore(LocalDate.now())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha inválida", "La fecha del próximo mantenimiento no puede ser anterior a hoy.");
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
    @FXML private void irAConsultaMantenimientoMaquinaria(javafx.event.ActionEvent e) { Navegacion.irA("/vistasFinales/vistaConsultaMantenimientoMaquinaria.fxml", e); }
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
    // 0=nuevo 1=viendo 2=editando
    private void actualizarBotones(int estado) {
        this.estadoActual = estado;
        // 0=nuevo 1=viendo 2=editando
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
