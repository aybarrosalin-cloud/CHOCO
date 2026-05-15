package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class loginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;

    @FXML
    private void iniciarSesion() {
        String usuario  = txtUsuario.getText();
        String password = txtPassword.getText();

        if (usuario.isBlank() || password.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Por favor ingresa usuario y contraseña.").show();
            return;
        }

        if (validarCredenciales(usuario, password)) {
            abrirMenuPrincipal();
        } else {
            new Alert(Alert.AlertType.ERROR, "Usuario o contraseña incorrectos.").show();
            txtPassword.clear();
        }
    }

    private boolean validarCredenciales(String usuario, String password) {
        try (Connection conn = new conexion().establecerConexion();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id_usuario, usuario, password, foto_perfil, rol, estado FROM tbl_usuario WHERE usuario = ?")) {

            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                new Alert(Alert.AlertType.ERROR, "Usuario o contraseña incorrectos.").show();
                return false;
            }

            String estado = rs.getString("estado");
            if ("Inactivo".equalsIgnoreCase(estado)) {
                new Alert(Alert.AlertType.WARNING, "Tu cuenta está inactiva. Contacta al administrador.").show();
                return false;
            }

            String storedPassword = rs.getString("password");
            if (!CifradoUtil.verificarPassword(password, storedPassword)) {
                new Alert(Alert.AlertType.ERROR, "Usuario o contraseña incorrectos.").show();
                return false;
            }

            // si la clave aun no tiene hash, la ciframos para la proxima
            if (!CifradoUtil.esHashSeguro(storedPassword)) {
                String nuevoHash = CifradoUtil.hashPassword(password);
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE tbl_usuario SET password = ? WHERE id_usuario = ?")) {
                    upd.setString(1, nuevoHash);
                    upd.setInt(2, rs.getInt("id_usuario"));
                    upd.executeUpdate();
                }
            }

            SesionManager.getInstancia().iniciarSesion(
                    rs.getString("usuario"),
                    rs.getString("foto_perfil"),
                    rs.getString("rol"));
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo conectar a la base de datos.").show();
            return false;
        }
    }

    private void abrirMenuPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistasFinales/vistaInicio.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) txtUsuario.getScene().getWindow();
            stage.setScene(new Scene(root, 1020, 720));
            stage.setTitle("Chocolatería - Sistema de Gestión");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al abrir el menú: " + "Ocurrió un error. Intente de nuevo.").show();
        }
    }
}
