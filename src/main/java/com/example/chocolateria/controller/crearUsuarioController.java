package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.usuarioModelo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class crearUsuarioController {

    @FXML private TextField        txtIdUsuario;
    @FXML private TextField        txtUsuario;
    @FXML private TextField        txtPassword;
    @FXML private TextField        txtConfirmarPassword;
    @FXML private ComboBox<String> cbRol;
    @FXML private ComboBox<String> cbDepartamento;
    @FXML private TextField        txtFotoPerfil;

    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private usuarioModelo usuarioCargado = null;
    private final Map<String, Integer> departamentosMap = new LinkedHashMap<>();
    private final conexion             con              = new conexion();

    private static final ObservableList<String> ROLES = FXCollections.observableArrayList(
        "Administrador",
        "Gerente General",
        "Auditor",
        "Encargado de Finanzas",
        "Encargado de RRHH",
        "Vendedor",
        "Encargado de Producción",
        "Operario de Empaque",
        "Inspector de Calidad",
        "Encargado de Almacén",
        "Encargado de Compras",
        "Encargado de Logística",
        "Técnico de Mantenimiento"
    );


    @FXML private Button btnBuscar, btnLimpiar;
    @FXML private Button btnGuardar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnDeshabilitar;

    @FXML
    public void initialize() {
        actualizarBotones(0);
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);

        if (!"Administrador".equalsIgnoreCase(SesionManager.getInstancia().getRol())) {
            mostrarAlerta(Alert.AlertType.ERROR, "Acceso denegado",
                    "Solo los administradores pueden gestionar usuarios del sistema.");
            desactivarFormulario();
            return;
        }

        cbRol.setItems(ROLES);
        cargarDepartamentos();
        generarSiguienteId();
    }

    private void cargarDepartamentos() {
        departamentosMap.clear();
        cbDepartamento.getItems().clear();
        String sql = "SELECT id_departamento, nombre FROM tbl_departamento WHERE estado='Activo' ORDER BY nombre";
        try (Connection c = con.establecerConexion();
             Statement st  = c.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {

            while (rs.next()) {
                String nombre = rs.getString("nombre");
                departamentosMap.put(nombre, rs.getInt("id_departamento"));
                cbDepartamento.getItems().add(nombre);
            }
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudieron cargar los departamentos: " + "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    public void guardarUsuario() {
        if (!validarCampos()) return;

        if (!txtPassword.getText().equals(txtConfirmarPassword.getText())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Contraseñas distintas",
                    "La contraseña y su confirmación no coinciden.");
            return;
        }

        Integer idDept = departamentosMap.get(cbDepartamento.getValue());

        String sql = "INSERT INTO tbl_usuario (usuario, password, foto_perfil, rol, estado, id_departamento) VALUES (?, ?, ?, ?, 'Activo', ?)";
        try (Connection c = con.establecerConexion();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, txtUsuario.getText().trim());
            ps.setString(2, txtPassword.getText());
            ps.setString(3, txtFotoPerfil.getText().trim());
            ps.setString(4, cbRol.getValue());
            if (idDept != null) ps.setInt(5, idDept); else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Usuario creado correctamente.");
            limpiar();

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEditar() {
        actualizarBotones(2);
        if (usuarioCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un usuario por ID primero para editar.");
            return;
        }
        if (!validarCamposEdicion()) return;

        boolean cambiarPassword = !txtPassword.getText().isBlank();
        if (cambiarPassword && !txtPassword.getText().equals(txtConfirmarPassword.getText())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Contraseñas distintas",
                    "La contraseña y su confirmación no coinciden.");
            return;
        }

        Integer idDept = departamentosMap.get(cbDepartamento.getValue());

        String sql = cambiarPassword
            ? "UPDATE tbl_usuario SET usuario=?, password=?, foto_perfil=?, rol=?, id_departamento=? WHERE id_usuario=?"
            : "UPDATE tbl_usuario SET usuario=?, foto_perfil=?, rol=?, id_departamento=? WHERE id_usuario=?";

        try (Connection c = con.establecerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (cambiarPassword) {
                ps.setString(1, txtUsuario.getText().trim());
                ps.setString(2, txtPassword.getText());
                ps.setString(3, txtFotoPerfil.getText().trim());
                ps.setString(4, cbRol.getValue());
                if (idDept != null) ps.setInt(5, idDept); else ps.setNull(5, Types.INTEGER);
                ps.setInt(6, usuarioCargado.getIdUsuario());
            } else {
                ps.setString(1, txtUsuario.getText().trim());
                ps.setString(2, txtFotoPerfil.getText().trim());
                ps.setString(3, cbRol.getValue());
                if (idDept != null) ps.setInt(4, idDept); else ps.setNull(4, Types.INTEGER);
                ps.setInt(5, usuarioCargado.getIdUsuario());
            }
            ps.executeUpdate();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Usuario actualizado correctamente.");
            limpiar();

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al editar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEliminar() {
        if (usuarioCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un usuario por ID primero para eliminar.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar al usuario '" + usuarioCargado.getUsuario() + "'?");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try (Connection c = con.establecerConexion();
                     PreparedStatement ps = c.prepareStatement("DELETE FROM tbl_usuario WHERE id_usuario=?")) {

                    ps.setInt(1, usuarioCargado.getIdUsuario());
                    ps.executeUpdate();
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Usuario eliminado correctamente.");
                    limpiar();

                } catch (SQLException e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar", "Ocurrió un error. Intente de nuevo.");
                }
            }
        });
    }

    @FXML
    private void fnDeshabilitar() {
        if (usuarioCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un usuario por ID primero.");
            return;
        }

        String nuevoEstado = "Activo".equalsIgnoreCase(usuarioCargado.getEstado()) ? "Inactivo" : "Activo";

        try (Connection c = con.establecerConexion();
             PreparedStatement ps = c.prepareStatement("UPDATE tbl_usuario SET estado=? WHERE id_usuario=?")) {

            ps.setString(1, nuevoEstado);
            ps.setInt(2, usuarioCargado.getIdUsuario());
            ps.executeUpdate();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Usuario marcado como " + nuevoEstado + ".");
            limpiar();

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnBuscar() {
        actualizarBotones(0);
        String idTexto = txtIdUsuario.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención",
                    "Escribe un ID en el campo 'ID Usuario' y presiona Buscar.");
            return;
        }

        int idBuscar;
        try {
            idBuscar = Integer.parseInt(idTexto);
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID inválido", "El ID debe ser un número entero.");
            return;
        }

        String sql = "SELECT u.id_usuario, u.usuario, u.rol, u.estado, " +
                     "ISNULL(u.id_departamento, 0) AS id_departamento, ISNULL(d.nombre,'') AS departamento " +
                     "FROM tbl_usuario u " +
                     "LEFT JOIN tbl_departamento d ON u.id_departamento = d.id_departamento " +
                     "WHERE u.id_usuario=?";
        try (Connection c = con.establecerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, idBuscar);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                cargarEnFormulario(new usuarioModelo(
                        rs.getInt("id_usuario"), rs.getString("usuario"),
                        rs.getString("rol"), rs.getString("estado"),
                        rs.getInt("id_departamento"), rs.getString("departamento")));
                actualizarBotones(1);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Encontrado",
                        "Usuario encontrado y cargado en el formulario.");
            } else {
                mostrarAlerta(Alert.AlertType.WARNING, "No encontrado",
                        "No existe ningún usuario con el ID " + idBuscar + ".");
            }
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de búsqueda", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    public void limpiar() {
        actualizarBotones(0);
        txtUsuario.clear();
        txtPassword.clear();
        txtConfirmarPassword.clear();
        txtFotoPerfil.clear();
        cbRol.setValue(null);
        cbDepartamento.setValue(null);
        usuarioCargado = null;
        generarSiguienteId();
    }

    private void cargarEnFormulario(usuarioModelo u) {
        this.usuarioCargado = u;
        txtIdUsuario.setText(String.valueOf(u.getIdUsuario()));
        txtUsuario.setText(u.getUsuario());
        txtPassword.clear();
        txtConfirmarPassword.clear();
        cbRol.setValue(u.getRol());
        cbDepartamento.setValue(u.getDepartamento().isBlank() ? null : u.getDepartamento());
        txtFotoPerfil.clear();
    }

    private void generarSiguienteId() {
        String sql = "SELECT ISNULL(MAX(id_usuario), 0) + 1 AS siguiente FROM tbl_usuario";
        try (Connection c = con.establecerConexion();
             Statement st  = c.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {

            if (rs.next()) txtIdUsuario.setText(String.valueOf(rs.getInt("siguiente")));

        } catch (SQLException e) {
            txtIdUsuario.setText("1");
        }
    }

    private boolean validarCampos() {
        if (txtUsuario.getText().trim().isEmpty() ||
            txtPassword.getText().isBlank()        ||
            cbRol.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos vacíos",
                    "Usuario, contraseña y rol son obligatorios.");
            return false;
        }
        return true;
    }

    private boolean validarCamposEdicion() {
        if (txtUsuario.getText().trim().isEmpty() || cbRol.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos vacíos",
                    "Usuario y rol son obligatorios.");
            return false;
        }
        return true;
    }

    private void desactivarFormulario() {
        if (txtUsuario           != null) txtUsuario.setDisable(true);
        if (txtPassword          != null) txtPassword.setDisable(true);
        if (txtConfirmarPassword != null) txtConfirmarPassword.setDisable(true);
        if (cbRol                != null) cbRol.setDisable(true);
        if (cbDepartamento       != null) cbDepartamento.setDisable(true);
        if (txtFotoPerfil        != null) txtFotoPerfil.setDisable(true);
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // navegacion
    @FXML private void irAInicio(ActionEvent e)              { Navegacion.irA("/vistasFinales/vistaInicio.fxml", e); }
    @FXML private void irAOrdenCliente(ActionEvent e)        { Navegacion.irA("/vistasFinales/vistaOrdenCliente.fxml", e); }
    @FXML private void irAPagoVenta(ActionEvent e)           { Navegacion.irA("/vistasFinales/vistaPagoVenta.fxml", e); }
    @FXML private void irAGestionEnvios(ActionEvent e)       { Navegacion.irA("/vistasFinales/vistaGestionEnvios.fxml", e); }
    @FXML private void irAGestionReclamos(ActionEvent e)     { Navegacion.irA("/vistasFinales/vistaGestionReclamos.fxml", e); }
    @FXML private void irASolicitudProduccion(ActionEvent e) { Navegacion.irA("/vistasFinales/vistaSolicitudDeProduccion.fxml", e); }
    @FXML private void irAOrdenProduccion(ActionEvent e)     { Navegacion.irA("/vistasFinales/vistaOrdenProduccion.fxml", e); }
    @FXML private void irASalidaMateriales(ActionEvent e)    { Navegacion.irA("/vistasFinales/vistaSalidaMateriales.fxml", e); }
    @FXML private void irASalidaProductos(ActionEvent e)     { Navegacion.irA("/vistasFinales/vistaSalidaProductos.fxml", e); }
    @FXML private void irAOrdenProveedor(ActionEvent e)      { Navegacion.irA("/vistasFinales/vistaOrdenProveedor.fxml", e); }
    @FXML private void irAPagoCompra(ActionEvent e)          { Navegacion.irA("/vistasFinales/vistaPagoCompra.fxml", e); }
    @FXML private void irARegistroProducto(ActionEvent e)    { Navegacion.irA("/vistasFinales/vistaRegistroProducto.fxml", e); }
    @FXML private void irARegistroEmpleado(ActionEvent e)    { Navegacion.irA("/vistasFinales/vistaRegistroDeEmpleado.fxml", e); }
    @FXML private void irARegistroCliente(ActionEvent e)     { Navegacion.irA("/vistasFinales/vistaRegistroDeCliente.fxml", e); }
    @FXML private void irARegistroSuplidor(ActionEvent e)    { Navegacion.irA("/vistasFinales/vistaRegistroSuplidor.fxml", e); }
    @FXML private void irARegistroMaquinaria(ActionEvent e)  { Navegacion.irA("/vistasFinales/vistaRegistroMaquinaria.fxml", e); }
    @FXML private void irAReportesVentas(ActionEvent e)      { Navegacion.irA("/vistasFinales/vistaReportes.fxml", e); }
    @FXML private void irAReportesCompras(ActionEvent e)     { Navegacion.irA("/vistasFinales/vistaReportes.fxml", e); }
    @FXML private void irAReportesInventario(ActionEvent e)  { Navegacion.irA("/vistasFinales/vistaReportes.fxml", e); }
    @FXML private void irAReportesProduccion(ActionEvent e)  { Navegacion.irA("/vistasFinales/vistaReportes.fxml", e); }
    @FXML private void irAMantenimientoMaquinaria(ActionEvent e) { Navegacion.irA("/vistasFinales/vistaMantenimientoMaquinaria.fxml", e); }
    @FXML private void irAConsultas(ActionEvent e)           { Navegacion.irA("/vistasFinales/vistaConsultasGenerales.fxml", e); }
    @FXML private void irAGestionUsuarios(ActionEvent e)     { Navegacion.irA("/vistasFinales/vistaGestionUsuarios.fxml", e); }
    @FXML private void irAConsultaUsuarios(ActionEvent e)    { Navegacion.irA("/vistasFinales/vistaConsultaUsuarios.fxml", e); }
    @FXML private void salir(ActionEvent e)                  { Navegacion.salir(e); }

    // estado de botones
    // estado: 0=libre/nuevo 1=encontrado 2=editando
    private void actualizarBotones(int estado) {
        // estado: 0=libre/nuevo 1=encontrado 2=editando
        btnBuscar.setDisable(false);
        btnBuscar.setStyle("-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");
        btnLimpiar.setDisable(false);
        btnLimpiar.setStyle("-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");
        boolean actGuardar = (estado != 1);
        btnGuardar.setDisable(!actGuardar);
        btnGuardar.setStyle(actGuardar ? "-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#c8c8c8; -fx-text-fill:#888; -fx-font-weight:bold; -fx-background-radius:12;");
        boolean actEditar = (estado == 1);
        btnEditar.setDisable(!actEditar);
        btnEditar.setStyle(actEditar ? "-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#c8c8c8; -fx-text-fill:#888; -fx-font-weight:bold; -fx-background-radius:12;");
        boolean actEliminar = (estado != 0);
        btnEliminar.setDisable(!actEliminar);
        btnEliminar.setStyle(actEliminar ? "-fx-background-color:#a83c5b; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#c8c8c8; -fx-text-fill:#888; -fx-font-weight:bold; -fx-background-radius:12;");
        boolean actDesha = (estado == 1);
        btnDeshabilitar.setDisable(!actDesha);
        btnDeshabilitar.setStyle(actDesha ? "-fx-background-color:#7a5c00; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;" : "-fx-background-color:#c8c8c8; -fx-text-fill:#888; -fx-font-weight:bold; -fx-background-radius:12;");
    }

}
