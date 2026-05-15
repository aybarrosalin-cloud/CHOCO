package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.empleadoModelo;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.concurrent.Task;

import java.sql.*;

public class empleadoController {

    @FXML private TextField txtIdEmpleado;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtCedula;
    @FXML private TextField txtTelefono;
    @FXML private ComboBox<String> cbTipoEmpleado;
    @FXML private ComboBox<String> cbRol;

    private final conexion con = new conexion();
    private empleadoModelo empleadoCargado = null;

    @FXML private Label lblUsuario;
    @FXML private ImageView imgFotoPerfil;


    @FXML private Button btnBuscar, btnLimpiar;
    @FXML private Button btnGuardar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnDeshabilitar;

    @FXML
    public void initialize() {
        actualizarBotones(0);
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        cbTipoEmpleado.getItems().addAll(
            "Tiempo Completo", "Medio Tiempo", "Contratista", "Pasante"
        );
        cbRol.getItems().addAll(
            "Administrador", "Gerente General", "Auditor",
            "Encargado de Finanzas", "Vendedor", "Encargado de Producción",
            "Operario de Empaque", "Inspector de Calidad", "Encargado de Almacén",
            "Encargado de Compras", "Encargado de Logística", "Técnico de Mantenimiento"
        );


        Task<Void> cargar = new Task<>() {
            @Override protected Void call() {
                return null;
            }
        };
        new Thread(cargar).start();
        generarSiguienteId();
    }

    @FXML
    public void guardarEmpleado() {
        if (estadoActual == 1) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Acción no disponible", "Ya hay un registro cargado. Usa 'Editar' para modificarlo o 'Limpiar' para crear uno nuevo.");
            return;
        }
        if (!validarCampos()) return;

        String sql = "INSERT INTO tbl_empleado (nombre, apellido, cedula, telefono, tipo_empleado, rol, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'Activo')";

        try (Connection c = con.establecerConexion();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, txtNombre.getText().trim());
            ps.setString(2, txtApellido.getText().trim());
            ps.setString(3, txtCedula.getText().trim());
            ps.setString(4, txtTelefono.getText().trim());
            ps.setString(5, cbTipoEmpleado.getValue());
            ps.setString(6, cbRol.getValue());
            ps.executeUpdate();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Empleado registrado correctamente.");
            limpiar();

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEditar() {
        actualizarBotones(2);
        if (empleadoCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un registro por ID primero.");
            return;
        }
        if (!validarCampos()) return;

        String sql = "UPDATE tbl_empleado SET nombre=?, apellido=?, cedula=?, telefono=?, tipo_empleado=?, rol=? WHERE id_empleado=?";

        try (Connection c = con.establecerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, txtNombre.getText().trim());
            ps.setString(2, txtApellido.getText().trim());
            ps.setString(3, txtCedula.getText().trim());
            ps.setString(4, txtTelefono.getText().trim());
            ps.setString(5, cbTipoEmpleado.getValue());
            ps.setString(6, cbRol.getValue());
            ps.setInt(7, empleadoCargado.getIdEmpleado());
            ps.executeUpdate();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Empleado actualizado correctamente.");
            limpiar();

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al editar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEliminar() {
        if (empleadoCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un registro por ID primero.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar a " + empleadoCargado.getNombre() + " " + empleadoCargado.getApellido() + "?");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try (Connection c = con.establecerConexion();
                     PreparedStatement ps = c.prepareStatement("DELETE FROM tbl_empleado WHERE id_empleado=?")) {

                    ps.setInt(1, empleadoCargado.getIdEmpleado());
                    ps.executeUpdate();
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Empleado eliminado correctamente.");
                    limpiar();

                } catch (SQLException e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar", "Ocurrió un error. Intente de nuevo.");
                }
            }
        });
    }

    @FXML
    private void fnDeshabilitar() {
        if (empleadoCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un registro por ID primero.");
            return;
        }

        String nuevoEstado = "Activo".equalsIgnoreCase(empleadoCargado.getEstado()) ? "Inactivo" : "Activo";

        try (Connection c = con.establecerConexion();
             PreparedStatement ps = c.prepareStatement("UPDATE tbl_empleado SET estado=? WHERE id_empleado=?")) {

            ps.setString(1, nuevoEstado);
            ps.setInt(2, empleadoCargado.getIdEmpleado());
            ps.executeUpdate();

            empleadoCargado.setEstado(nuevoEstado);
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Empleado marcado como " + nuevoEstado + ".");

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnBuscar() {
        actualizarBotones(0);
        String idTexto = txtIdEmpleado.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Escribe un ID en el campo 'ID Empleado' y presiona Buscar.");
            return;
        }

        int idBuscar;
        try {
            idBuscar = Integer.parseInt(idTexto);
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID inválido", "El ID debe ser un número entero.");
            return;
        }

        String sql = "SELECT id_empleado, nombre, apellido, cedula, telefono, tipo_empleado, rol, estado FROM tbl_empleado WHERE id_empleado = ?";
        try (Connection c = con.establecerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, idBuscar);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                cargarEnFormulario(new empleadoModelo(
                        rs.getInt("id_empleado"),
                        rs.getString("nombre"),
                        rs.getString("apellido"),
                        rs.getString("cedula"),
                        rs.getString("telefono"),
                        rs.getString("tipo_empleado"),
                        rs.getString("rol"),
                        rs.getString("estado")
                ));
                actualizarBotones(1);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Encontrado", "Empleado encontrado y cargado en el formulario.");
            } else {
                mostrarAlerta(Alert.AlertType.WARNING, "No encontrado", "No existe ningún empleado con el ID " + idBuscar + ".");
            }

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de búsqueda", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    public void limpiar() {
        actualizarBotones(0);
        txtNombre.clear();
        txtApellido.clear();
        txtCedula.clear();
        txtTelefono.clear();
        cbTipoEmpleado.setValue(null);
        cbRol.setValue(null);
        empleadoCargado = null;
        generarSiguienteId();
    }

    private void cargarEnFormulario(empleadoModelo e) {
        this.empleadoCargado = e;
        txtIdEmpleado.setText(String.valueOf(e.getIdEmpleado()));
        txtNombre.setText(e.getNombre());
        txtApellido.setText(e.getApellido());
        txtCedula.setText(e.getCedula());
        txtTelefono.setText(e.getTelefono());
        cbTipoEmpleado.setValue(e.getTipoEmpleado());
        cbRol.setValue(e.getRol());
    }

    private void generarSiguienteId() {
        String sql = "SELECT ISNULL(MAX(id_empleado), 0) + 1 AS siguiente FROM tbl_empleado";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) txtIdEmpleado.setText(String.valueOf(rs.getInt("siguiente")));

        } catch (SQLException e) {
            txtIdEmpleado.setText("1");
        }
    }

    private boolean validarCampos() {
        if (txtNombre.getText().trim().isEmpty()   ||
                txtApellido.getText().trim().isEmpty()  ||
                txtCedula.getText().trim().isEmpty()    ||
                txtTelefono.getText().trim().isEmpty()  ||
                cbTipoEmpleado.getValue() == null       ||
                cbRol.getValue() == null) {

            mostrarAlerta(Alert.AlertType.WARNING, "Campos vacíos", "Por favor completa todos los campos antes de continuar.");
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
    @FXML private void irAConsultaEmpleados(javafx.event.ActionEvent e)   { Navegacion.irA("/vistasFinales/vistaConsultaEmpleados.fxml", e); }
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
        boolean actDesha = (estado == 1);
        btnDeshabilitar.setDisable(false);
        btnDeshabilitar.setStyle(actDesha ? "-fx-background-color:#7a5c00; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#f0e6c0; -fx-text-fill:#9a7a20; -fx-font-weight:bold; -fx-background-radius:12; -fx-cursor:hand;");
    }

}
