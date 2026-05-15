package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.sql.*;
import java.util.Optional;

import static com.example.chocolateria.controller.PermisoRol.Consulta.*;

public class consultasController {

    @FXML private TextField                         txtBuscarGeneral;
    @FXML private Label                             lblConsultaActiva;
    @FXML private Label                             lblTotal;
    @FXML private TableView<ObservableList<String>> tablaResultados;

    @FXML private Button btnClientes, btnVentas, btnCompras, btnProduccion;
    @FXML private Button btnInventario, btnPedidos, btnIngresos, btnMasVendidos;
    @FXML private Button btnMantenimiento;
    @FXML private Button btnSalidaMateriales;
    @FXML private Button btnSalidaProductos;
    @FXML private Label     lblUsuario;
    @FXML private ImageView imgFotoPerfil;

    private final conexion con = new conexion();
    private String sqlActual   = "";
    private String tituloActual = "";

    private static String consultaPendiente = null;

    public static void setConsultaPendiente(String consulta) {
        consultaPendiente = consulta;
    }

    @FXML
    public void initialize() {
        CargarPerfil.aplicar(lblUsuario, imgFotoPerfil);
        aplicarPermisosConsultas();
        if (consultaPendiente != null) {
            String cp = consultaPendiente;
            consultaPendiente = null;
            switch (cp) {
                case "salidaMateriales" -> consultarSalidaMateriales();
                case "salidaProductos"  -> consultarSalidaProductos();
            }
        }
    }

    // permisos de los botones de consulta
    private void aplicarPermisosConsultas() {
        String rol = SesionManager.getInstancia().getRol();

        configurarBoton(btnClientes,    rol, CLIENTES);
        configurarBoton(btnVentas,      rol, VENTAS);
        configurarBoton(btnCompras,     rol, COMPRAS);
        configurarBoton(btnProduccion,  rol, PRODUCCION);
        configurarBoton(btnInventario,  rol, INVENTARIO);
        configurarBoton(btnPedidos,     rol, PEDIDOS);
        configurarBoton(btnMantenimiento, rol, MANTENIMIENTO);
        configurarBoton(btnSalidaMateriales, rol, INVENTARIO);
        configurarBoton(btnSalidaProductos, rol, INVENTARIO);

        // ingresos y mas vendidos: libre, con clave o bloqueado
        configurarBotonFinanciero(btnIngresos,    rol, INGRESOS);
        configurarBotonFinanciero(btnMasVendidos, rol, MAS_VENDIDOS);
    }

    private void configurarBoton(Button btn, String rol, PermisoRol.Consulta consulta) {
        if (btn == null) return;
        boolean tieneAcceso = PermisoRol.tieneAccesoConsulta(rol, consulta);
        btn.setVisible(tieneAcceso);
        btn.setManaged(tieneAcceso);
    }

    private void configurarBotonFinanciero(Button btn, String rol, PermisoRol.Consulta consulta) {
        if (btn == null) return;
        if (PermisoRol.esRolFinanciero(rol)) {
            // libre
            btn.setVisible(true);
            btn.setManaged(true);
        } else if (PermisoRol.requiereClaveFinanciera(rol)) {
            // visible pero con candado
            btn.setVisible(true);
            btn.setManaged(true);
            btn.setText(btn.getText() + "  🔐");
        } else {
            // sin acceso
            btn.setVisible(false);
            btn.setManaged(false);
        }
    }

    // validar acceso financiero con clave gerencial
    private boolean validarClaveGerencial() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Acceso restringido");
        dialog.setHeaderText("Esta consulta requiere autorización gerencial.");
        dialog.setContentText("Ingresa la contraseña de un Gerente o Administrador:");

        // ocultar el texto
        PasswordField campoOculto = new PasswordField();
        campoOculto.setPromptText("Contraseña gerencial");
        dialog.getDialogPane().setContent(campoOculto);

        Optional<String> resultado = dialog.showAndWait();
        if (resultado.isEmpty()) return false;

        String claveIngresada = campoOculto.getText();
        if (claveIngresada == null || claveIngresada.isBlank()) return false;

        // verificar contra la bd: cualquier admin o gerente activo
        String sql = "SELECT COUNT(*) FROM tbl_usuario " +
                     "WHERE password=? AND estado='Activo' " +
                     "AND rol IN ('Administrador','Gerente General')";
        try (Connection c = con.establecerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, claveIngresada);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return true;
        } catch (SQLException e) {
            mostrarAlerta("Error al verificar la clave: " + "Ocurrió un error. Intente de nuevo.");
        }

        mostrarAlerta("Contraseña incorrecta o usuario sin permiso.");
        return false;
    }

    // consultas

    @FXML private void consultarClientes() {
        sqlActual    = "SELECT id_cliente AS ID, nombre AS Nombre, apellido AS Apellido, " +
                       "cedula AS Cedula, telefono AS Telefono, email AS Email, " +
                       "direccion AS Direccion, estado AS Estado " +
                       "FROM tbl_cliente ORDER BY nombre";
        tituloActual = "Consulta de Clientes";
        resaltarBoton(btnClientes);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML private void consultarVentas() {
        sqlActual    = "SELECT v.id_venta AS ID, ISNULL(o.cliente,'') AS Cliente, " +
                       "v.fecha_venta AS Fecha, v.subtotal AS Subtotal, " +
                       "v.descuento AS Descuento, v.itbis AS ITBIS, " +
                       "v.monto_total AS Total, v.monto_pagado AS Pagado, " +
                       "v.balance_pendiente AS Balance, v.estado_pago AS Estado, " +
                       "v.tipo_pago AS [Tipo Pago], ISNULL(CAST(v.id_comprobante AS VARCHAR),'') AS NCF " +
                       "FROM tbl_venta v " +
                       "LEFT JOIN tbl_orden_cliente o ON v.id_orden = o.id_orden " +
                       "ORDER BY v.fecha_venta DESC";
        tituloActual = "Consulta de Ventas";
        resaltarBoton(btnVentas);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML private void consultarCompras() {
        sqlActual    = "SELECT o.codigo AS ID, o.proveedor AS Proveedor, " +
                       "o.rnc_proveedor AS RNC, o.fecha_requerida AS [Fecha Requerida], " +
                       "o.prioridad AS Prioridad, o.categoria AS Categoria, " +
                       "o.monto_total AS [Monto Total], o.estado_pago AS [Estado Pago] " +
                       "FROM tbl_orden_proveedor o ORDER BY o.codigo DESC";
        tituloActual = "Consulta de Compras";
        resaltarBoton(btnCompras);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML private void consultarProduccion() {
        sqlActual    = "SELECT o.id_orden AS ID, o.tipo_orden AS Tipo, " +
                       "ISNULL(c.nombre + ' ' + c.apellido,'') AS Cliente, " +
                       "o.fecha_orden AS [Fecha Orden], o.fecha_inicio AS Inicio, " +
                       "o.fecha_entrega AS Entrega, " +
                       "ISNULL(e.nombre + ' ' + e.apellido,'') AS Responsable, " +
                       "o.estado AS Estado, o.prioridad AS Prioridad, o.categoria AS Categoria " +
                       "FROM tbl_orden_produccion o " +
                       "LEFT JOIN tbl_cliente c ON o.id_cliente = c.id_cliente " +
                       "LEFT JOIN tbl_empleado e ON o.id_responsable = e.id_empleado " +
                       "ORDER BY o.fecha_orden DESC";
        tituloActual = "Consulta de Produccion";
        resaltarBoton(btnProduccion);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML private void consultarInventario() {
        sqlActual    = "SELECT p.codigo AS Codigo, p.nombre AS Producto, " +
                       "p.categoria AS Categoria, p.tipo AS Tipo, " +
                       "p.unidad_medida AS Unidad, p.stock AS Stock, " +
                       "p.precio_unitario AS [Precio Unit.], p.precio_mayor AS [Precio Mayor] " +
                       "FROM tbl_producto p ORDER BY p.categoria, p.nombre";
        tituloActual = "Consulta de Inventario";
        resaltarBoton(btnInventario);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML private void consultarPedidos() {
        sqlActual    = "SELECT o.id_orden AS ID, o.cliente AS Cliente, " +
                       "o.fecha_registro AS [F. Registro], o.fecha_entrega AS [F. Entrega], " +
                       "o.metodo_pago AS [Metodo Pago], o.estado AS Estado, " +
                       "o.observaciones AS Observaciones " +
                       "FROM tbl_orden_cliente o ORDER BY o.fecha_registro DESC";
        tituloActual = "Consulta de Pedidos";
        resaltarBoton(btnPedidos);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML private void consultarIngresos() {
        String rol = SesionManager.getInstancia().getRol();
        if (PermisoRol.requiereClaveFinanciera(rol) && !validarClaveGerencial()) return;

        sqlActual    = "SELECT YEAR(v.fecha_venta) AS Anio, MONTH(v.fecha_venta) AS Mes, " +
                       "COUNT(*) AS [Num Ventas], " +
                       "SUM(v.subtotal) AS Subtotal, " +
                       "SUM(v.descuento) AS Descuentos, " +
                       "SUM(v.itbis) AS ITBIS, " +
                       "SUM(v.monto_total) AS [Total Bruto], " +
                       "SUM(v.monto_pagado) AS [Total Cobrado], " +
                       "SUM(v.balance_pendiente) AS [Por Cobrar] " +
                       "FROM tbl_venta v " +
                       "GROUP BY YEAR(v.fecha_venta), MONTH(v.fecha_venta) " +
                       "ORDER BY Anio DESC, Mes DESC";
        tituloActual = "Reporte de Ingresos por Mes";
        resaltarBoton(btnIngresos);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML private void consultarMasVendidos() {
        String rol = SesionManager.getInstancia().getRol();
        if (PermisoRol.requiereClaveFinanciera(rol) && !validarClaveGerencial()) return;

        sqlActual    = "SELECT d.producto AS Producto, d.codigo AS Codigo, " +
                       "SUM(d.cantidad) AS [Total Vendido], " +
                       "COUNT(DISTINCT d.id_orden) AS [Num Ordenes], " +
                       "p.precio_unitario AS [Precio Unit.], p.categoria AS Categoria " +
                       "FROM tbl_orden_detalle d " +
                       "LEFT JOIN tbl_producto p ON d.codigo = p.codigo " +
                       "GROUP BY d.producto, d.codigo, p.precio_unitario, p.categoria " +
                       "ORDER BY [Total Vendido] DESC";
        tituloActual = "Reporte de Productos mas Vendidos";
        resaltarBoton(btnMasVendidos);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML private void consultarSalidaMateriales() {
        sqlActual    = "SELECT sm.id_salida AS ID, sm.id_solicitud AS [ID Solicitud], " +
                       "CONVERT(VARCHAR, sm.fecha_salida, 103) AS Fecha, " +
                       "ISNULL(sm.responsable,'') AS Responsable, " +
                       "CAST((SELECT COUNT(*) FROM tbl_salida_materiales_detalle d WHERE d.id_salida=sm.id_salida) AS VARCHAR) AS Items, " +
                       "ISNULL(sm.observaciones,'') AS Observaciones " +
                       "FROM tbl_salida_materiales sm ORDER BY sm.fecha_salida DESC";
        tituloActual = "Consulta de Salida de Materiales";
        resaltarBoton(btnSalidaMateriales);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML private void consultarSalidaProductos() {
        sqlActual    = "SELECT sp.id_salida AS ID, sp.id_orden_cliente AS [ID Orden], " +
                       "ISNULL(sp.cliente,'') AS Cliente, " +
                       "CONVERT(VARCHAR, sp.fecha_salida, 103) AS Fecha, " +
                       "ISNULL(sp.responsable,'') AS Responsable, " +
                       "CAST((SELECT COUNT(*) FROM tbl_salida_productos_detalle d WHERE d.id_salida=sp.id_salida) AS VARCHAR) AS Items, " +
                       "CAST(ISNULL(sp.total,0) AS VARCHAR) AS Total, " +
                       "ISNULL(sp.observaciones,'') AS Observaciones " +
                       "FROM tbl_salida_productos sp ORDER BY sp.fecha_salida DESC";
        tituloActual = "Consulta de Salida de Productos";
        resaltarBoton(btnSalidaProductos);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    @FXML private void consultarMantenimiento() {
        sqlActual    = "SELECT m.id_mantenimiento AS ID, maq.nombre AS Maquinaria, " +
                       "m.tipo_mantenimiento AS Tipo, m.fecha_mantenimiento AS Fecha, " +
                       "m.tecnico AS Tecnico, m.costo AS Costo, " +
                       "m.descripcion AS Descripcion, " +
                       "ISNULL(CONVERT(VARCHAR, m.fecha_proximo_mantenimiento, 23),'N/A') AS [Prox. Mantenimiento] " +
                       "FROM tbl_mantenimiento_maquinaria m " +
                       "LEFT JOIN tbl_maquinaria maq ON m.id_maquinaria = maq.id_maquinaria " +
                       "ORDER BY m.fecha_mantenimiento DESC";
        tituloActual = "Historial de Mantenimiento";
        resaltarBoton(btnMantenimiento);
        ejecutarConsulta(sqlActual, tituloActual);
    }

    // busqueda y motor generico
    @FXML private void ejecutarBusqueda() {
        String filtro = txtBuscarGeneral.getText().trim();
        if (sqlActual.isEmpty()) { mostrarAlerta("Selecciona una consulta primero."); return; }
        if (filtro.isEmpty())    { ejecutarConsulta(sqlActual, tituloActual); return; }

        lblTotal.setText("Filtrando…");
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws Exception {
                ObservableList<ObservableList<String>> todos = FXCollections.observableArrayList();
                try (Connection conn = con.establecerConexion();
                     Statement st   = conn.createStatement();
                     ResultSet rs   = st.executeQuery(sqlActual)) {

                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    String filtroMin = filtro.toLowerCase();
                    while (rs.next()) {
                        ObservableList<String> fila = FXCollections.observableArrayList();
                        boolean coincide = false;
                        for (int i = 1; i <= cols; i++) {
                            String val = rs.getString(i) != null ? rs.getString(i) : "";
                            fila.add(val);
                            if (val.toLowerCase().contains(filtroMin)) coincide = true;
                        }
                        if (coincide) todos.add(fila);
                    }
                }
                Platform.runLater(() -> {
                    tablaResultados.setItems(todos);
                    lblTotal.setText("Resultados filtrados: " + todos.size());
                });
                return null;
            }
            @Override protected void failed() {
                Platform.runLater(() -> mostrarAlerta("Error al filtrar: " + getException().getMessage()));
            }
        };
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML private void limpiarBusqueda() {
        txtBuscarGeneral.clear();
        if (!sqlActual.isEmpty()) ejecutarConsulta(sqlActual, tituloActual);
    }

    private void ejecutarConsulta(String sql, String titulo) {
        tablaResultados.getColumns().clear();
        tablaResultados.getItems().clear();
        lblConsultaActiva.setText(titulo + "  (cargando…)");
        lblTotal.setText("");

        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ObservableList<TableColumn<ObservableList<String>, String>> columnas =
                        FXCollections.observableArrayList();
                ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();

                try (Connection conn = con.establecerConexion();
                     Statement st   = conn.createStatement();
                     ResultSet rs   = st.executeQuery(sql)) {

                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();

                    for (int i = 1; i <= cols; i++) {
                        final int idx = i - 1;
                        TableColumn<ObservableList<String>, String> col =
                                new TableColumn<>(meta.getColumnLabel(i));
                        col.setCellValueFactory(d -> new SimpleStringProperty(
                                d.getValue().size() > idx ? d.getValue().get(idx) : ""));
                        col.setStyle("-fx-background-color:#48295a; -fx-text-fill:white;" +
                                "-fx-font-weight:bold; -fx-alignment:CENTER;");
                        col.setMinWidth(90);
                        col.setPrefWidth(140);
                        columnas.add(col);
                    }

                    while (rs.next()) {
                        ObservableList<String> fila = FXCollections.observableArrayList();
                        for (int i = 1; i <= cols; i++)
                            fila.add(rs.getString(i) != null ? rs.getString(i) : "");
                        data.add(fila);
                    }
                }

                // actualizar tabla desde el hilo de javafx
                Platform.runLater(() -> {
                    tablaResultados.getColumns().setAll(columnas);
                    tablaResultados.setItems(data);
                    lblConsultaActiva.setText(titulo);
                    lblTotal.setText("Total de registros: " + data.size());
                });
                return null;
            }

            @Override
            protected void failed() {
                Platform.runLater(() ->
                        mostrarAlerta("Error al ejecutar la consulta: " + getException().getMessage()));
            }
        };

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    // resaltar boton activo
    private void resaltarBoton(Button activo) {
        String normal  = "-fx-background-color:#6d3c87; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:10;";
        String verde   = "-fx-background-color:#2e7d32; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:10;";
        String oscuro  = "-fx-background-color:#48295a; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:10;";
        String activos = "-fx-background-color:#f0e6ff; -fx-text-fill:#3B1A5C; -fx-font-weight:bold; -fx-background-radius:10; -fx-border-color:#6d3c87; -fx-border-width:2;";

        if (btnClientes    != null) btnClientes.setStyle(normal);
        if (btnVentas      != null) btnVentas.setStyle(normal);
        if (btnCompras     != null) btnCompras.setStyle(normal);
        if (btnProduccion  != null) btnProduccion.setStyle(normal);
        if (btnInventario  != null) btnInventario.setStyle(normal);
        if (btnPedidos     != null) btnPedidos.setStyle(normal);
        if (btnIngresos    != null) btnIngresos.setStyle(verde);
        if (btnMasVendidos != null) btnMasVendidos.setStyle(verde);
        if (btnMantenimiento != null) btnMantenimiento.setStyle(oscuro);
        if (btnSalidaMateriales != null) btnSalidaMateriales.setStyle(normal);
        if (btnSalidaProductos != null) btnSalidaProductos.setStyle(normal);

        if (activo != null) activo.setStyle(activos);
    }

    private void mostrarAlerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Aviso"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
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
    @FXML private void salir(javafx.event.ActionEvent e)                  { Navegacion.salir(e); }
}
