package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;


public class EmailService {

    private static final String REMITENTE    = "lachocory@gmail.com";
    private static final String APP_PASSWORD = "nmce jfjf acdp zrif"; // app password de Gmail
    private static final String ADMIN_EMAIL  = "aybarrosalin@gmail.com";
    private static final String SMTP_HOST    = "smtp.gmail.com";
    private static final int    SMTP_PORT    = 587;

    // mapeo de tipo de reclamo a departamento
    public static String departamentoPorTipo(String tipoReclamo) {
        if (tipoReclamo == null) return "Administración";
        return switch (tipoReclamo) {
            case "Producto en mal estado",
                 "Calidad no satisfactoria",
                 "Producto incorrecto"      -> "Calidad";
            case "Entrega tardía",
                 "Producto dañado en envio",
                 "Producto no recibido"     -> "Logística";
            case "Cantidad incorrecta"      -> "Almacén";
            case "Problema de facturacion"  -> "Finanzas";
            case "Mal servicio al cliente"  -> "Ventas";
            default                         -> "Administración";
        };
    }

    private static String rolResponsable(String departamento) {
        return switch (departamento) {
            case "Calidad"   -> "Inspector de Calidad";
            case "Logística" -> "Encargado de Logística";
            case "Almacén"   -> "Encargado de Almacén";
            case "Finanzas"  -> "Encargado de Finanzas";
            case "Ventas"    -> "Vendedor / Gerente";
            default          -> "Administrador";
        };
    }

    private static String colorDept(String departamento) {
        return switch (departamento) {
            case "Calidad"   -> "#1abc9c";
            case "Logística" -> "#2980b9";
            case "Almacén"   -> "#8e44ad";
            case "Finanzas"  -> "#c0392b";
            case "Ventas"    -> "#e67e22";
            default          -> "#6d3c87";
        };
    }

    // busca el email del encargado en tbl_usuario
    private static String emailEncargado(String rol) {
        String sql = "SELECT TOP 1 email FROM tbl_usuario WHERE rol = ? AND estado = 'Activo' AND email IS NOT NULL AND email <> ''";
        try (Connection c = new conexion().establecerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, rol);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("email");
            }
        } catch (Exception ignored) {}
        return null;
    }
//INICIA CORREO
    public static void notificarNuevoReclamo(int idReclamo, String cliente,
                                             String tipo, String prioridad,
                                             String descripcion, String empleado) {

        String colorPrioridad = switch (prioridad) {
            case "Alta"  -> "#c0392b";
            case "Media" -> "#e67e22";
            default      -> "#27ae60";
        };

        String dept     = departamentoPorTipo(tipo);
        String rol      = rolResponsable(dept);
        String colorDep = colorDept(dept);

        String asunto = "Reclamo #" + idReclamo + " [" + prioridad + "] - Dept. " + dept;
        String cuerpo =
            "<div style='font-family:Arial,sans-serif; max-width:560px; margin:auto;'>" +
            "<div style='background:" + colorDep + "; padding:20px; border-radius:8px 8px 0 0;'>" +
            "<h2 style='color:white; margin:0;'>Sistema Chocolatería</h2>" +
            "<p style='color:rgba(255,255,255,0.85); margin:4px 0 0 0; font-size:13px;'>Notificación automática de reclamo</p>" +
            "</div>" +
            "<div style='background:#fff8f5; padding:24px; border:1px solid #f0d0c0; border-radius:0 0 8px 8px;'>" +
            "<div style='background:" + colorDep + "22; border-left:4px solid " + colorDep + ";" +
            " padding:10px 14px; border-radius:4px; margin-bottom:14px;'>" +
            "<b style='color:" + colorDep + "; font-size:13px;'>Departamento responsable: " + dept + "</b><br/>" +
            "<span style='color:#555; font-size:12px;'>Asignar a: <b>" + rol + "</b></span>" +
            "</div>" +
            "<h3 style='color:#a83c5b; margin-top:0;'>Nuevo reclamo registrado</h3>" +
            "<table style='width:100%; border-collapse:collapse;'>" +
            "<tr><td style='padding:6px 0; font-weight:bold; color:#6d3c87; width:130px;'>Reclamo #:</td>" +
            "<td style='padding:6px 0; font-weight:bold;'>" + idReclamo + "</td></tr>" +
            "<tr><td style='padding:6px 0; font-weight:bold; color:#6d3c87;'>Cliente:</td>" +
            "<td style='padding:6px 0;'>" + cliente + "</td></tr>" +
            "<tr><td style='padding:6px 0; font-weight:bold; color:#6d3c87;'>Tipo:</td>" +
            "<td style='padding:6px 0;'>" + tipo + "</td></tr>" +
            (empleado != null && !empleado.isBlank() ?
            "<tr><td style='padding:6px 0; font-weight:bold; color:#6d3c87;'>Registrado por:</td>" +
            "<td style='padding:6px 0;'>" + empleado + "</td></tr>" : "") +
            "<tr><td style='padding:6px 0; font-weight:bold; color:#6d3c87;'>Prioridad:</td>" +
            "<td style='padding:6px 0;'><span style='background:" + colorPrioridad +
            "; color:white; padding:2px 10px; border-radius:12px; font-size:12px;'>" +
            prioridad + "</span></td></tr>" +
            "<tr><td style='padding:6px 0; font-weight:bold; color:#6d3c87;'>Fecha:</td>" +
            "<td style='padding:6px 0;'>" + new java.util.Date() + "</td></tr>" +
            "</table>" +
            "<div style='margin-top:14px; background:#fff0f0; border-left:4px solid #a83c5b;" +
            " padding:10px 14px; border-radius:4px;'>" +
            "<b style='color:#a83c5b;'>Descripción:</b><br/>" +
            "<span style='color:#333;'>" + descripcion + "</span></div>" +
            "<p style='color:#888; font-size:11px; margin-top:16px;'>" +
            "Ingresa al sistema para gestionar este reclamo.</p>" +
            "</div></div>";

        // enviar siempre al admin
        enviarEnHiloSegundo(ADMIN_EMAIL, asunto, cuerpo);
        // enviar tambien al encargado si tiene email
        String emailDept = emailEncargado(rol);
        if (emailDept != null && !emailDept.equalsIgnoreCase(ADMIN_EMAIL)) {
            enviarEnHiloSegundo(emailDept, asunto, cuerpo);
        }
    }

    // sobrecarga para compatibilidad hacia atras
    public static void notificarNuevoReclamo(int idReclamo, String cliente,
                                             String tipo, String prioridad,
                                             String descripcion) {
        notificarNuevoReclamo(idReclamo, cliente, tipo, prioridad, descripcion, "");
    }


    public static void notificarNuevoUsuario(String usuario, String rol) {
        String asunto = "Nuevo usuario creado";
        String cuerpo =
            "<div style='font-family:Arial,sans-serif; max-width:520px; margin:auto;'>" +
            "<div style='background:#6d3c87; padding:20px; border-radius:8px 8px 0 0;'>" +
            "<h2 style='color:white; margin:0;'>Sistema Chocolatería</h2></div>" +
            "<div style='background:#f9f5ff; padding:24px; border:1px solid #e0d0f0; border-radius:0 0 8px 8px;'>" +
            "<h3 style='color:#3B1A5C;'>Nuevo usuario registrado en el sistema</h3>" +
            "<table style='width:100%; border-collapse:collapse;'>" +
            "<tr><td style='padding:6px 0; font-weight:bold; color:#6d3c87; width:100px;'>Usuario:</td>" +
            "<td style='padding:6px 0;'>" + usuario + "</td></tr>" +
            "<tr><td style='padding:6px 0; font-weight:bold; color:#6d3c87;'>Rol:</td>" +
            "<td style='padding:6px 0;'>" + rol + "</td></tr>" +
            "</table>" +
            "<p style='color:#888; font-size:11px; margin-top:16px;'>" +
            "Notificación automática del sistema de gestión.</p>" +
            "</div></div>";

        enviarEnHiloSegundo(ADMIN_EMAIL, asunto, cuerpo);
    }

    public static void notificarRestablecimientoPassword(String usuario, String rol) {
        String asunto = "Contraseña restablecida - " + usuario;
        String cuerpo =
            "<div style='font-family:Arial,sans-serif; max-width:520px; margin:auto;'>" +
            "<div style='background:#2c3e50; padding:20px; border-radius:8px 8px 0 0;'>" +
            "<h2 style='color:white; margin:0;'>Sistema Chocolatería</h2></div>" +
            "<div style='background:#f5f5f5; padding:24px; border:1px solid #ddd; border-radius:0 0 8px 8px;'>" +
            "<h3 style='color:#e74c3c;'>Contraseña restablecida por el administrador</h3>" +
            "<table style='width:100%; border-collapse:collapse;'>" +
            "<tr><td style='padding:6px 0; font-weight:bold; color:#2c3e50; width:100px;'>Usuario:</td>" +
            "<td style='padding:6px 0;'><b>" + usuario + "</b></td></tr>" +
            "<tr><td style='padding:6px 0; font-weight:bold; color:#2c3e50;'>Rol:</td>" +
            "<td style='padding:6px 0;'>" + rol + "</td></tr>" +
            "<tr><td style='padding:6px 0; font-weight:bold; color:#2c3e50;'>Fecha:</td>" +
            "<td style='padding:6px 0;'>" + new java.util.Date() + "</td></tr>" +
            "</table>" +
            "<p style='color:#888; font-size:11px; margin-top:16px;'>" +
            "Si no reconoces esta acción, contacta al administrador del sistema.</p>" +
            "</div></div>";

        enviarEnHiloSegundo(ADMIN_EMAIL, asunto, cuerpo);
    }


    private static void enviarEnHiloSegundo(String destinatario, String asunto, String htmlCuerpo) {
        Thread hilo = new Thread(() -> {
            try {
                enviar(destinatario, asunto, htmlCuerpo);
            } catch (Exception e) {
                System.err.println("[EmailService] No se pudo enviar: " + "Ocurrió un error. Intente de nuevo.");
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    private static void enviar(String destinatario, String asunto, String htmlCuerpo)
            throws MessagingException, java.io.UnsupportedEncodingException {

        Properties props = new Properties();
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(REMITENTE, APP_PASSWORD);
            }
        });

        Message mensaje = new MimeMessage(session);
        mensaje.setFrom(new InternetAddress(REMITENTE, "Sistema Chocolatería", "UTF-8"));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        mensaje.setSubject(asunto);
        mensaje.setContent(htmlCuerpo, "text/html; charset=UTF-8");

        Transport.send(mensaje);
    }
}

