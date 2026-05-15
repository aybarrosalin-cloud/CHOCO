package com.example.chocolateria.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Navegacion {

    // mapa fxml -> permiso requerido
    private static final Map<String, PermisoRol.Pantalla> PERMISOS_FXML = new HashMap<>();

    // permisos alternativos (cualquiera del set)
    private static final Map<String, Set<PermisoRol.Pantalla>> EXT_PERMISOS_FXML = new HashMap<>();

    static {
        PERMISOS_FXML.put("/vistasFinales/vistaOrdenCliente.fxml",             PermisoRol.Pantalla.ORDEN_CLIENTE);
        PERMISOS_FXML.put("/vistasFinales/vistaPagoVenta.fxml",                PermisoRol.Pantalla.PAGO_VENTA);
        PERMISOS_FXML.put("/vistasFinales/vistaGestionEnvios.fxml",            PermisoRol.Pantalla.GESTION_ENVIOS);
        PERMISOS_FXML.put("/vistasFinales/vistaGestionReclamos.fxml",          PermisoRol.Pantalla.GESTION_RECLAMOS);
        PERMISOS_FXML.put("/vistasFinales/vistaSolicitudDeProduccion.fxml",    PermisoRol.Pantalla.SOLICITUD_PRODUCCION);
        PERMISOS_FXML.put("/vistasFinales/vistaOrdenProduccion.fxml",          PermisoRol.Pantalla.ORDEN_PRODUCCION);
        PERMISOS_FXML.put("/vistasFinales/vistaRecepcion.fxml",                PermisoRol.Pantalla.RECEPCION);
        PERMISOS_FXML.put("/vistasFinales/vistaOrdenProveedor.fxml",           PermisoRol.Pantalla.ORDEN_PROVEEDOR);
        PERMISOS_FXML.put("/vistasFinales/vistaPagoCompra.fxml",               PermisoRol.Pantalla.PAGO_COMPRA);
        PERMISOS_FXML.put("/vistasFinales/vistaRegistroProducto.fxml",         PermisoRol.Pantalla.REG_PRODUCTO);
        PERMISOS_FXML.put("/vistasFinales/vistaRegistroDeEmpleado.fxml",       PermisoRol.Pantalla.REG_EMPLEADO);
        PERMISOS_FXML.put("/vistasFinales/vistaRegistroDeCliente.fxml",        PermisoRol.Pantalla.REG_CLIENTE);
        PERMISOS_FXML.put("/vistasFinales/vistaRegistroSuplidor.fxml",         PermisoRol.Pantalla.REG_SUPLIDOR);
        PERMISOS_FXML.put("/vistasFinales/vistaRegistroMaquinaria.fxml",       PermisoRol.Pantalla.REG_MAQUINARIA);
        PERMISOS_FXML.put("/vistasFinales/vistaMantenimientoMaquinaria.fxml",  PermisoRol.Pantalla.MANTENIMIENTO);
        PERMISOS_FXML.put("/vistasFinales/vistaGestionUsuarios.fxml",          PermisoRol.Pantalla.GESTION_USUARIOS);
        // todas las vistas de consulta necesitan permiso consultas
        PERMISOS_FXML.put("/vistasFinales/vistaConsultasGenerales.fxml",       PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaReportes.fxml",                    PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaClientes.fxml",         PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaEmpleados.fxml",        PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaProductos.fxml",        PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaSuplidores.fxml",       PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaMaquinarias.fxml",      PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaEnvios.fxml",           PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaReclamos.fxml",         PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaOrdenCliente.fxml",     PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaOrdenProduccion.fxml",  PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaOrdenProveedor.fxml",   PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaPagosVenta.fxml",       PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaPagosCompra.fxml",      PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaRecepcion.fxml",        PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaSolicitudesProduccion.fxml", PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaMantenimientoMaquinaria.fxml", PermisoRol.Pantalla.CONSULTAS);
        PERMISOS_FXML.put("/vistasFinales/vistaConsultaUsuarios.fxml",         PermisoRol.Pantalla.GESTION_USUARIOS);
        PERMISOS_FXML.put("/vistasFinales/vistaSalidaMateriales.fxml",         PermisoRol.Pantalla.SALIDA_MATERIALES);
        PERMISOS_FXML.put("/vistasFinales/vistaSalidaProductos.fxml",          PermisoRol.Pantalla.SALIDA_PRODUCTOS);
        // inicio y principal no necesitan permiso (null = libre)

        // permisos alternativos: acceso desde varios roles
        EXT_PERMISOS_FXML.put("/vistasFinales/vistaRecepcion.fxml",
            Set.of(PermisoRol.Pantalla.SALIDA_MATERIALES, PermisoRol.Pantalla.SALIDA_PRODUCTOS));
    }

    // navegacion principal

    public static void irA(String fxmlPath, ActionEvent event) {
        irA(fxmlPath, event, 1020, 720);
    }

    public static void irA(String fxmlPath, ActionEvent event, double w, double h) {
        // 1. verificar permiso del rol
        if (!tienePermiso(fxmlPath)) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Acceso denegado");
            alerta.setHeaderText(null);
            alerta.setContentText("No tienes permisos para acceder a esta sección.\n"
                    + "Rol actual: " + SesionManager.getInstancia().getRol());
            alerta.showAndWait();
            return;
        }

        // 2. vistas de consulta piden clave admin
        if (esVistaConsulta(fxmlPath)) {
            if (!VerificarClave.pedirClaveAdmin()) {
                Alert alerta = new Alert(Alert.AlertType.WARNING);
                alerta.setTitle("Clave incorrecta");
                alerta.setHeaderText(null);
                alerta.setContentText("Clave incorrecta o cancelada.\nSolo los administradores pueden acceder a las consultas.");
                alerta.showAndWait();
                return;
            }
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(Navegacion.class.getResource(fxmlPath));
            Parent root = loader.load();
            stage.setResizable(false);
            stage.setScene(new Scene(root, w, h));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de navegación");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo abrir la pantalla:\n" + ex.getMessage());
            alert.showAndWait();
        }
    }

    public static void salir(ActionEvent event) {
        SesionManager.getInstancia().cerrarSesion();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(Navegacion.class.getResource("/vistasFinales/vistaPrincipal.fxml"));
            Parent root = loader.load();
            stage.setResizable(false);
            stage.setScene(new Scene(root, 949, 533));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // verificacion de permisos

    private static boolean tienePermiso(String fxmlPath) {
        String rol = SesionManager.getInstancia().getRol();

        // permiso principal
        PermisoRol.Pantalla pantallaRequerida = PERMISOS_FXML.get(fxmlPath);
        if (pantallaRequerida != null && PermisoRol.tieneAcceso(rol, pantallaRequerida)) return true;

        // permisos alternativos
        Set<PermisoRol.Pantalla> alternativas = EXT_PERMISOS_FXML.get(fxmlPath);
        if (alternativas != null) {
            for (PermisoRol.Pantalla p : alternativas) {
                if (PermisoRol.tieneAcceso(rol, p)) return true;
            }
        }

        // si no esta en ningun mapa, acceso libre
        return pantallaRequerida == null && alternativas == null;
    }

    // true si es vista de consulta individual (protegida con clave), excluye el hub general
    private static boolean esVistaConsulta(String fxmlPath) {
        String nombre = fxmlPath.toLowerCase();
        return nombre.contains("/vistaconsulta") && !nombre.contains("consultasgenerales");
    }
}
