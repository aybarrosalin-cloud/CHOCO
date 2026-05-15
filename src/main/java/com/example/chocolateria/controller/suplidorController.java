package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.suplidorModelo;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.concurrent.Task;

import java.sql.*;

public class suplidorController {

    @FXML private TextField        txtId;
    @FXML private TextField        txtNombre;
    @FXML private TextField        txtApellido;
    @FXML private TextField        txtRnc;
    @FXML private TextField        txtTelefono;
    @FXML private TextField        txtCorreo;
    @FXML private ComboBox<String> cbCiudad;

    private final conexion con = new conexion();
    private suplidorModelo suplidorCargado = null;

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

        cbCiudad.setItems(FXCollections.observableArrayList(
            "Santo Domingo", "Santiago", "La Vega", "San Pedro de Macorís",
            "San Cristóbal", "La Romana", "Puerto Plata", "Higüey",
            "San Francisco de Macorís", "Moca", "Bonao", "Barahona",
            "Azua", "Monte Cristi", "Nagua", "Samaná", "Hato Mayor",
            "Cotuí", "Dajabón", "Pedernales"
        ));


        Task<Void> cargar = new Task<>() {
            @Override protected Void call() {
                return null;
            }
        };
        new Thread(cargar).start();
        generarSiguienteId();
    }

    @FXML
    private void guardar() {
        if (estadoActual == 1) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Acción no disponible", "Ya hay un registro cargado. Usa 'Editar' para modificarlo o 'Limpiar' para crear uno nuevo.");
            return;
        }
        if (!validarCampos()) return;

        String sql = "INSERT INTO tbl_suplidor (nombre, apellido, rnc, telefono, correo, ciudad) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, txtNombre.getText().trim());
            ps.setString(2, txtApellido.getText().trim());
            ps.setString(3, txtRnc.getText().trim());
            ps.setString(4, txtTelefono.getText().trim());
            ps.setString(5, txtCorreo.getText().trim());
            ps.setString(6, cbCiudad.getValue() != null ? cbCiudad.getValue() : "");
            ps.executeUpdate();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Suplidor registrado correctamente.");
            limpiar();

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEditar() {
        actualizarBotones(2);
        if (suplidorCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un registro por ID primero.");
            return;
        }
        if (!validarCampos()) return;

        String sql = "UPDATE tbl_suplidor SET nombre=?, apellido=?, rnc=?, telefono=?, correo=?, ciudad=? " +
                     "WHERE id_suplidor=?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, txtNombre.getText().trim());
            ps.setString(2, txtApellido.getText().trim());
            ps.setString(3, txtRnc.getText().trim());
            ps.setString(4, txtTelefono.getText().trim());
            ps.setString(5, txtCorreo.getText().trim());
            ps.setString(6, cbCiudad.getValue() != null ? cbCiudad.getValue() : "");
            ps.setInt(7, suplidorCargado.getIdSuplidor());
            ps.executeUpdate();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Suplidor actualizado correctamente.");
            limpiar();

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al editar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEliminar() {
        if (suplidorCargado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un registro por ID primero.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar al suplidor " + suplidorCargado.getNombre() + " " + suplidorCargado.getApellido() + "?");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try (Connection conn = con.establecerConexion();
                     PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM tbl_suplidor WHERE id_suplidor=?")) {
                    ps.setInt(1, suplidorCargado.getIdSuplidor());
                    ps.executeUpdate();
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Suplidor eliminado correctamente.");
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

        String sql = "SELECT id_suplidor, nombre, apellido, rnc, telefono, correo, ciudad " +
                     "FROM tbl_suplidor WHERE id_suplidor = ?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idBuscar);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                cargarEnFormulario(new suplidorModelo(
                    rs.getInt("id_suplidor"),
                    rs.getString("nombre"),
                    rs.getString("apellido"),
                    rs.getString("rnc"),
                    rs.getString("telefono") != null ? rs.getString("telefono") : "",
                    rs.getString("correo")   != null ? rs.getString("correo")   : "",
                    rs.getString("ciudad")   != null ? rs.getString("ciudad")   : ""
                ));
                actualizarBotones(1);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Encontrado", "Suplidor encontrado y cargado en el formulario.");
            } else {
                mostrarAlerta(Alert.AlertType.WARNING, "No encontrado",
                    "No existe un suplidor con el ID " + idBuscar + ".");
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de búsqueda", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void limpiar() {
        actualizarBotones(0);
        txtId.clear();
        txtNombre.clear();
        txtApellido.clear();
        txtRnc.clear();
        txtTelefono.clear();
        txtCorreo.clear();
        cbCiudad.setValue(null);
        suplidorCargado = null;
        generarSiguienteId();
    }

    private void cargarEnFormulario(suplidorModelo s) {
        this.suplidorCargado = s;
        txtId.setText(String.valueOf(s.getIdSuplidor()));
        txtNombre.setText(s.getNombre());
        txtApellido.setText(s.getApellido());
        txtRnc.setText(s.getRnc());
        txtTelefono.setText(s.getTelefono());
        txtCorreo.setText(s.getCorreo());
        cbCiudad.setValue(s.getCiudad());
    }

    private void generarSiguienteId() {
        String sql = "SELECT ISNULL(MAX(id_suplidor), 0) + 1 AS siguiente FROM tbl_suplidor";
        try (Connection c = con.establecerConexion();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) txtId.setText(String.valueOf(rs.getInt("siguiente")));

        } catch (SQLException e) {
            txtId.setText("1");
        }
    }

    private boolean validarCampos() {
        if (txtNombre.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo requerido", "El nombre es obligatorio.");
            return false;
        }
        if (txtApellido.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo requerido", "El apellido es obligatorio.");
            return false;
        }
        if (txtRnc.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo requerido", "El RNC es obligatorio.");
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
    @FXML private void irAConsultaSuplidores(javafx.event.ActionEvent e)  { Navegacion.irA("/vistasFinales/vistaConsultaSuplidores.fxml", e); }
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
