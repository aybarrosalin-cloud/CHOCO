package com.example.chocolateria.controller;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Map;

public class JasperReportUtil {

    public static JasperPrint compilarYLlenar(String ruta, Map<String, Object> params, Connection conn) {
        try {
            InputStream jrxmlStream = JasperReportUtil.class.getResourceAsStream(ruta);
            if (jrxmlStream == null) throw new RuntimeException("No se encontró el reporte: " + ruta);
            JasperReport report = JasperCompileManager.compileReport(jrxmlStream);
            return JasperFillManager.fillReport(report, params, conn);
        } catch (Exception e) {
            throw new RuntimeException("Error al compilar reporte: " + e.getClass().getSimpleName() + " - " + "Ocurrió un error. Intente de nuevo.", e);
        }
    }

    public static void mostrarReporte(String ruta, Map<String, Object> params, Connection conn) {
        JasperPrint print = compilarYLlenar(ruta, params, conn);
        System.out.println("[JASPER DEBUG] Páginas generadas: " + print.getPages().size());
        JasperViewer.viewReport(print, false);
    }

    public static void exportarPDF(String ruta, Map<String, Object> params, Connection conn, String destino) {
        try {
            JasperPrint print = compilarYLlenar(ruta, params, conn);
            JasperExportManager.exportReportToPdfFile(print, destino);
        } catch (Exception e) {
            throw new RuntimeException("Error al exportar PDF: " + e.getClass().getSimpleName() + " - " + "Ocurrió un error. Intente de nuevo.", e);
        }
    }
}
