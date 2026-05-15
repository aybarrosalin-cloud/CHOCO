package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.solicitudDetalleModelo;
import com.example.chocolateria.modelo.solicitudProduccionModelo;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class solicitudProduccionController {

    // datos generales
    @FXML private TextField        txtIdSolicitud;
    @FXML private DatePicker       dpFechaSolicitud;
    @FXML private DatePicker       dpFechaProduccion;
    @FXML private TextField        txtIdResponsable;
    @FXML private Label            lblNombreResponsable;
    @FXML private ComboBox<String> cbPrioridad;
    @FXML private ComboBox<String> cbEstado;
    @FXML private TextArea         txtObservaciones;
    @FXML private TextField        txtBuscarTabla;

    // detalle productos
    @FXML private TextField txtCodigoDetalle;
    @FXML private Label     lblProductoDetalle;
    @FXML private TextField txtCantidadDetalle;

    @FXML private TableView<solicitudDetalleModelo>               tablaDetalle;
    @FXML private TableColumn<solicitudDetalleModelo, String>     colDetCodigo;
    @FXML private TableColumn<solicitudDetalleModelo, String>     colDetProducto;
    @FXML private TableColumn<solicitudDetalleModelo, Number>     colDetCantidad;

    // historial solicitudes
    @FXML private TableView<solicitudProduccionModelo>                  tablaSolicitudes;
    @FXML private TableColumn<solicitudProduccionModelo, Number>        colId;
    @FXML private TableColumn<solicitudProduccionModelo, LocalDate>     colFechaSolicitud;
    @FXML private TableColumn<solicitudProduccionModelo, LocalDate>     colFechaProduccion;
    @FXML private TableColumn<solicitudProduccionModelo, String>        colResponsable;
    @FXML private TableColumn<solicitudProduccionModelo, String>        colPrioridad;
    @FXML private TableColumn<solicitudProduccionModelo, String>        colEstado;
    @FXML private TableColumn<solicitudProduccionModelo, String>        colProductos;

    private final ObservableList<solicitudProduccionModelo> lista        = FXCollections.observableArrayList();
    private final ObservableList<solicitudDetalleModelo>    listaDetalle = FXCollections.observableArrayList();
    private final conexion con = new conexion();
    // cache de productos para evitar n+1 queries al renderizar
    private final Map<Integer, String> cacheProductos = new HashMap<>();

    private int    idResponsableSeleccionado = 0;
    private String productoDetalleSeleccionado = "";

    // ══════════════════════════════════════════════════════════════════════════
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

        cbPrioridad.setItems(FXCollections.observableArrayList("Alta", "Media", "Baja"));
        cbEstado.setItems(FXCollections.observableArrayList("Pendiente", "En proceso", "Completada", "Cancelada"));

        // columnas historial (solo si existe la tabla)
        if (tablaSolicitudes != null) {
            colId.setCellValueFactory(d              -> d.getValue().idProperty());
            colFechaSolicitud.setCellValueFactory(d  -> d.getValue().fechaSolicitudProperty());
            colFechaProduccion.setCellValueFactory(d -> d.getValue().fechaProduccionProperty());
            colResponsable.setCellValueFactory(d     -> d.getValue().responsableProperty());
            colPrioridad.setCellValueFactory(d       -> d.getValue().prioridadProperty());
            colEstado.setCellValueFactory(d          -> d.getValue().estadoProperty());
            // usa el cache para no abrir conexion por cada fila
            colProductos.setCellValueFactory(d       -> new SimpleStringProperty(
                    cacheProductos.getOrDefault(d.getValue().getId(), "")));
        }

        // columnas detalle
        colDetCodigo.setCellValueFactory(d    -> d.getValue().codigoProductoProperty());
        colDetProducto.setCellValueFactory(d  -> d.getValue().productoProperty());
        colDetCantidad.setCellValueFactory(d  -> d.getValue().cantidadProperty());
        tablaDetalle.setItems(listaDetalle);

        // color por estado y filtro en historial (solo si existe)
        if (tablaSolicitudes != null) {
            tablaSolicitudes.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(solicitudProduccionModelo s, boolean empty) {
                    super.updateItem(s, empty);
                    if (s == null || empty) { setStyle(""); return; }
                    switch (s.getEstado()) {
                        case "Completada" -> setStyle("-fx-background-color:#e8f5e9;");
                        case "Cancelada"  -> setStyle("-fx-background-color:#fde8e8;");
                        case "En proceso" -> setStyle("-fx-background-color:#fff8e1;");
                        default           -> setStyle("");
                    }
                }
            });

            FilteredList<solicitudProduccionModelo> listaFiltrada = new FilteredList<>(lista, p -> true);
            if (txtBuscarTabla != null) {
                txtBuscarTabla.textProperty().addListener((obs, oldVal, newVal) ->
                        listaFiltrada.setPredicate(s -> {
                            if (newVal == null || newVal.isBlank()) return true;
                            String f = newVal.toLowerCase();
                            return s.getResponsable().toLowerCase().contains(f)
                                    || s.getEstado().toLowerCase().contains(f)
                                    || s.getPrioridad().toLowerCase().contains(f);
                        })
                );
            }
            tablaSolicitudes.setItems(listaFiltrada);

            tablaSolicitudes.getSelectionModel().selectedItemProperty().addListener(
                    (obs, old, sel) -> {
                        if (sel != null) {
                            cargarEnFormulario(sel);
                            cargarDetalle(sel.getId());
                            actualizarBotones(1);
                        }
                    }
            );
        }

        // carga los datos en un hilo secundario para no bloquear
        Task<Void> cargar = new Task<>() {
            @Override protected Void call() {
                cargarSolicitudes();
                return null;
            }
        };
        new Thread(cargar).start();

        dpFechaSolicitud.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        dpFechaSolicitud.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isBefore(LocalDate.now())) {
                dpFechaSolicitud.setValue(null);
                mostrarAlerta(Alert.AlertType.WARNING, "Fecha inválida", "La fecha de solicitud no puede ser anterior a hoy.");
            }
        });
        dpFechaProduccion.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        dpFechaProduccion.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isBefore(LocalDate.now())) {
                dpFechaProduccion.setValue(null);
                mostrarAlerta(Alert.AlertType.WARNING, "Fecha inválida", "La fecha de producción no puede ser anterior a hoy.");
            }
        });

        generarSiguienteId();
    }

    // buscar responsable por id
    @FXML
    private void buscarResponsable() {
        String idTexto = txtIdResponsable.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Escribe el ID del responsable.");
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
                    idResponsableSeleccionado = 0;
                    lblNombreResponsable.setText("No encontrado");
                }
            }
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID inválido", "El ID debe ser un número.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void abrirBuscadorProductoSolicitud() {
        popupBuscarProductoController.mostrar(codigo -> {
            txtCodigoDetalle.setText(codigo);
            buscarProductoDetalle();
        });
    }

    // buscar producto para el detalle
    @FXML
    private void buscarProductoDetalle() {
        String codigo = txtCodigoDetalle.getText().trim();
        if (codigo.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Escribe el código del producto.");
            return;
        }
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT nombre FROM tbl_producto WHERE codigo = ?")) {
            ps.setString(1, codigo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                productoDetalleSeleccionado = rs.getString("nombre");
                lblProductoDetalle.setText(productoDetalleSeleccionado);
            } else {
                productoDetalleSeleccionado = "";
                lblProductoDetalle.setText("No encontrado");
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error. Intente de nuevo.");
        }
    }

    // agregar producto al detalle temporal
    @FXML
    private void agregarProducto() {
        if (productoDetalleSeleccionado.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca un producto antes de agregar.");
            return;
        }
        if (txtCantidadDetalle.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Ingresa la cantidad.");
            return;
        }
        try {
            int cantidad = Integer.parseInt(txtCantidadDetalle.getText().trim());
            if (cantidad <= 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "Cantidad inválida", "La cantidad debe ser mayor a 0.");
                return;
            }
            String codigo = txtCodigoDetalle.getText().trim();

            // ver si ya existe el producto en el detalle
            for (solicitudDetalleModelo d : listaDetalle) {
                if (d.getCodigoProducto().equals(codigo)) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Duplicado",
                            "Este producto ya está en la lista. Quítalo y agrégalo de nuevo si quieres cambiar la cantidad.");
                    return;
                }
            }

            listaDetalle.add(new solicitudDetalleModelo(0, 0, codigo,
                    productoDetalleSeleccionado, cantidad));

            txtCodigoDetalle.clear();
            lblProductoDetalle.setText("");
            txtCantidadDetalle.clear();
            productoDetalleSeleccionado = "";

        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "Cantidad inválida", "La cantidad debe ser un número entero.");
        }
    }

    // quitar producto seleccionado del detalle
    @FXML
    private void quitarProducto() {
        solicitudDetalleModelo sel = tablaDetalle.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Selecciona un producto de la lista para quitarlo.");
            return;
        }
        listaDetalle.remove(sel);
    }

    // guardar solicitud + detalle
    @FXML
    private void guardar() {
        if (estadoActual == 1) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Acción no disponible", "Ya hay un registro cargado. Usa 'Editar' para modificarlo o 'Limpiar' para crear uno nuevo.");
            return;
        }
        if (!validarCampos()) return;

        String sqlMaestro = "INSERT INTO tbl_solicitud_produccion " +
                "(fecha_solicitud, fecha_produccion, prioridad, estado, id_responsable, observaciones) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sqlMaestro, Statement.RETURN_GENERATED_KEYS)) {

            ps.setDate(1, Date.valueOf(dpFechaSolicitud.getValue()));
            ps.setDate(2, Date.valueOf(dpFechaProduccion.getValue()));
            ps.setString(3, cbPrioridad.getValue());
            ps.setString(4, cbEstado.getValue());
            ps.setInt(5, idResponsableSeleccionado);
            ps.setString(6, txtObservaciones.getText().trim());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (!rs.next()) throw new SQLException("No se obtuvo el ID de la solicitud.");
            int nuevoId = rs.getInt(1);

            // insertar detalle
            String sqlDetalle = "INSERT INTO tbl_solicitud_detalle (id_solicitud, codigo_producto, producto, cantidad) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psDet = conn.prepareStatement(sqlDetalle)) {
                for (solicitudDetalleModelo det : listaDetalle) {
                    psDet.setInt(1, nuevoId);
                    psDet.setString(2, det.getCodigoProducto());
                    psDet.setString(3, det.getProducto());
                    psDet.setInt(4, det.getCantidad());
                    psDet.addBatch();
                }
                psDet.executeBatch();
            }

            solicitudProduccionModelo nuevo = new solicitudProduccionModelo(
                    nuevoId, dpFechaSolicitud.getValue(), dpFechaProduccion.getValue(),
                    cbPrioridad.getValue(), cbEstado.getValue(),
                    idResponsableSeleccionado, lblNombreResponsable.getText(),
                    txtObservaciones.getText().trim());
            lista.add(0, nuevo);

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito",
                    "Solicitud guardada con " + listaDetalle.size() + " producto(s).");
            limpiar();

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    // editar solicitud (datos generales)
    @FXML
    private void fnEditar() {
        actualizarBotones(2);
        solicitudProduccionModelo sel = tablaSolicitudes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Selecciona una solicitud de la tabla para editar.");
            return;
        }
        if (!validarCampos()) return;

        String sql = "UPDATE tbl_solicitud_produccion SET fecha_solicitud=?, fecha_produccion=?, " +
                "prioridad=?, estado=?, id_responsable=?, observaciones=? WHERE id_solicitud=?";

        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(dpFechaSolicitud.getValue()));
            ps.setDate(2, Date.valueOf(dpFechaProduccion.getValue()));
            ps.setString(3, cbPrioridad.getValue());
            ps.setString(4, cbEstado.getValue());
            ps.setInt(5, idResponsableSeleccionado);
            ps.setString(6, txtObservaciones.getText().trim());
            ps.setInt(7, sel.getId());
            ps.executeUpdate();

            // reemplazar detalle: borrar el anterior e insertar el nuevo
            try (PreparedStatement psDel = conn.prepareStatement(
                    "DELETE FROM tbl_solicitud_detalle WHERE id_solicitud=?")) {
                psDel.setInt(1, sel.getId());
                psDel.executeUpdate();
            }
            String sqlDet = "INSERT INTO tbl_solicitud_detalle (id_solicitud, codigo_producto, producto, cantidad) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psDet = conn.prepareStatement(sqlDet)) {
                for (solicitudDetalleModelo det : listaDetalle) {
                    psDet.setInt(1, sel.getId());
                    psDet.setString(2, det.getCodigoProducto());
                    psDet.setString(3, det.getProducto());
                    psDet.setInt(4, det.getCantidad());
                    psDet.addBatch();
                }
                psDet.executeBatch();
            }

            sel.setFechaSolicitud(dpFechaSolicitud.getValue());
            sel.setFechaProduccion(dpFechaProduccion.getValue());
            sel.setPrioridad(cbPrioridad.getValue());
            sel.setEstado(cbEstado.getValue());
            sel.setIdResponsable(idResponsableSeleccionado);
            sel.setResponsable(lblNombreResponsable.getText());
            sel.setObservaciones(txtObservaciones.getText().trim());

            tablaSolicitudes.refresh();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Solicitud actualizada correctamente.");
            limpiar();

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al editar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    // eliminar solicitud + detalle
    @FXML
    private void fnEliminar() {
        solicitudProduccionModelo sel = (tablaSolicitudes != null)
                ? tablaSolicitudes.getSelectionModel().getSelectedItem()
                : lista.stream()
                    .filter(s -> String.valueOf(s.getId()).equals(txtIdSolicitud.getText().trim()))
                    .findFirst().orElse(null);
        if (sel == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca una solicitud por ID para poder eliminarla.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar la solicitud #" + sel.getId() + " y todos sus productos?");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try (Connection conn = con.establecerConexion()) {
                    try (PreparedStatement psDet = conn.prepareStatement(
                            "DELETE FROM tbl_solicitud_detalle WHERE id_solicitud=?")) {
                        psDet.setInt(1, sel.getId());
                        psDet.executeUpdate();
                    }
                    try (PreparedStatement psMae = conn.prepareStatement(
                            "DELETE FROM tbl_solicitud_produccion WHERE id_solicitud=?")) {
                        psMae.setInt(1, sel.getId());
                        psMae.executeUpdate();
                    }
                    lista.remove(sel);
                    listaDetalle.clear();
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Solicitud eliminada correctamente.");
                    limpiar();
                } catch (Exception e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar", "Ocurrió un error. Intente de nuevo.");
                }
            }
        });
    }

    // buscar por id
    @FXML
    private void fnBuscar() {
        actualizarBotones(0);
        String idTexto = txtIdSolicitud.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Escribe un ID para buscar.");
            return;
        }
        int idBuscar;
        try {
            idBuscar = Integer.parseInt(idTexto);
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "ID inválido", "El ID debe ser un número entero.");
            return;
        }

        for (solicitudProduccionModelo s : lista) {
            if (s.getId() == idBuscar) {
                if (tablaSolicitudes != null) {
                    tablaSolicitudes.getSelectionModel().select(s);
                    tablaSolicitudes.scrollTo(s);
                }
                cargarEnFormulario(s);
                cargarDetalle(s.getId());
                actualizarBotones(1);
                return;
            }
        }
        mostrarAlerta(Alert.AlertType.WARNING, "No encontrado", "No existe una solicitud con el ID " + idBuscar + ".");
    }

    // convertir a orden de produccion
    @FXML
    private void convertirAOrden() {
        solicitudProduccionModelo sel = (tablaSolicitudes != null)
                ? tablaSolicitudes.getSelectionModel().getSelectedItem()
                : lista.stream()
                    .filter(s -> String.valueOf(s.getId()).equals(txtIdSolicitud.getText().trim()))
                    .findFirst().orElse(null);
        if (sel == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Busca una solicitud por ID para convertirla.");
            return;
        }
        if ("Completada".equals(sel.getEstado()) || "Cancelada".equals(sel.getEstado())) {
            mostrarAlerta(Alert.AlertType.WARNING, "No permitido",
                    "Solo se pueden convertir solicitudes Pendientes o En proceso.");
            return;
        }

        // construir resumen de productos para la categoria
        StringBuilder productos = new StringBuilder();
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT producto FROM tbl_solicitud_detalle WHERE id_solicitud = ?")) {
            ps.setInt(1, sel.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (productos.length() > 0) productos.append(", ");
                productos.append(rs.getString("producto"));
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error. Intente de nuevo.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Convertir a Orden");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Convertir la solicitud #" + sel.getId() + " en una Orden de Producción?");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                String sql = "INSERT INTO tbl_orden_produccion " +
                        "(tipo_orden, fecha_orden, fecha_inicio, fecha_entrega, " +
                        "id_responsable, estado, prioridad, categoria, materiales, observaciones) " +
                        "VALUES ('Produccion', ?, ?, ?, ?, 'Pendiente', ?, ?, '', ?)";
                try (Connection conn = con.establecerConexion();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setDate(1, Date.valueOf(LocalDate.now()));
                    ps.setDate(2, sel.getFechaSolicitud() != null
                            ? Date.valueOf(sel.getFechaSolicitud()) : Date.valueOf(LocalDate.now()));
                    ps.setDate(3, sel.getFechaProduccion() != null
                            ? Date.valueOf(sel.getFechaProduccion()) : Date.valueOf(LocalDate.now().plusDays(7)));
                    ps.setInt(4, sel.getIdResponsable());
                    ps.setString(5, sel.getPrioridad());
                    ps.setString(6, productos.toString());
                    ps.setString(7, sel.getObservaciones());
                    ps.executeUpdate();

                    try (PreparedStatement ps2 = conn.prepareStatement(
                            "UPDATE tbl_solicitud_produccion SET estado='En proceso' WHERE id_solicitud=?")) {
                        ps2.setInt(1, sel.getId());
                        ps2.executeUpdate();
                        sel.setEstado("En proceso");
                        tablaSolicitudes.refresh();
                    }

                    mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito",
                            "Solicitud convertida a Orden de Producción con " + listaDetalle.size() + " producto(s).");

                } catch (Exception e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error al convertir", "Ocurrió un error. Intente de nuevo.");
                }
            }
        });
    }

    // limpiar
    @FXML
    private void limpiar() {
        actualizarBotones(0);
        txtIdSolicitud.clear();
        dpFechaSolicitud.setValue(null);
        dpFechaProduccion.setValue(null);
        txtIdResponsable.clear();
        lblNombreResponsable.setText("");
        idResponsableSeleccionado = 0;
        cbPrioridad.setValue(null);
        cbEstado.setValue(null);
        txtObservaciones.clear();
        txtCodigoDetalle.clear();
        lblProductoDetalle.setText("");
        txtCantidadDetalle.clear();
        productoDetalleSeleccionado = "";
        listaDetalle.clear();
        tablaSolicitudes.getSelectionModel().clearSelection();
        generarSiguienteId();
    }

    // metodos de soporte

    private void cargarSolicitudes() {
        ObservableList<solicitudProduccionModelo> temporal = FXCollections.observableArrayList();
        Map<Integer, String> tempCache = new HashMap<>();

        // un solo join trae solicitudes y resumen sin queries extra
        String sql = "SELECT s.id_solicitud, s.fecha_solicitud, s.fecha_produccion, " +
                "s.prioridad, s.estado, s.id_responsable, " +
                "ISNULL(e.nombre + ' ' + e.apellido, '') AS responsable, " +
                "ISNULL(s.observaciones, '') AS observaciones, " +
                "ISNULL(STUFF((SELECT ', ' + d.producto " +
                "              FROM tbl_solicitud_detalle d " +
                "              WHERE d.id_solicitud = s.id_solicitud " +
                "              FOR XML PATH('')), 1, 2, ''), '') AS resumen_productos " +
                "FROM tbl_solicitud_produccion s " +
                "LEFT JOIN tbl_empleado e ON s.id_responsable = e.id_empleado " +
                "ORDER BY s.fecha_solicitud DESC";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date dSol  = rs.getDate("fecha_solicitud");
                Date dProd = rs.getDate("fecha_produccion");
                int idSol  = rs.getInt("id_solicitud");
                temporal.add(new solicitudProduccionModelo(
                        idSol,
                        dSol  != null ? dSol.toLocalDate()  : null,
                        dProd != null ? dProd.toLocalDate() : null,
                        rs.getString("prioridad"),
                        rs.getString("estado"),
                        rs.getInt("id_responsable"),
                        rs.getString("responsable"),
                        rs.getString("observaciones")
                ));
                tempCache.put(idSol, rs.getString("resumen_productos"));
            }
        } catch (Exception e) {
            Platform.runLater(() ->
                mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar solicitudes", "Ocurrió un error. Intente de nuevo."));
            return;
        }
        // actualiza la ui desde el hilo de javafx
        Platform.runLater(() -> {
            cacheProductos.clear();
            cacheProductos.putAll(tempCache);
            lista.setAll(temporal);
        });
    }

    private void cargarDetalle(int idSolicitud) {
        listaDetalle.clear();
        String sql = "SELECT id_detalle, id_solicitud, codigo_producto, producto, cantidad " +
                "FROM tbl_solicitud_detalle WHERE id_solicitud = ?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSolicitud);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                listaDetalle.add(new solicitudDetalleModelo(
                        rs.getInt("id_detalle"),
                        rs.getInt("id_solicitud"),
                        rs.getString("codigo_producto"),
                        rs.getString("producto"),
                        rs.getInt("cantidad")
                ));
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar detalle", "Ocurrió un error. Intente de nuevo.");
        }
    }

    private String cargarResumenProductos(int idSolicitud) {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT producto FROM tbl_solicitud_detalle WHERE id_solicitud = ?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSolicitud);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(rs.getString("producto"));
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    private void cargarEnFormulario(solicitudProduccionModelo s) {
        txtIdSolicitud.setText(String.valueOf(s.getId()));
        dpFechaSolicitud.setValue(s.getFechaSolicitud());
        dpFechaProduccion.setValue(s.getFechaProduccion());
        txtIdResponsable.setText(String.valueOf(s.getIdResponsable()));
        lblNombreResponsable.setText(s.getResponsable());
        idResponsableSeleccionado = s.getIdResponsable();
        cbPrioridad.setValue(s.getPrioridad());
        cbEstado.setValue(s.getEstado());
        txtObservaciones.setText(s.getObservaciones());
    }

    private void generarSiguienteId() {
        String sql = "SELECT ISNULL(MAX(id_solicitud), 0) + 1 AS siguiente FROM tbl_solicitud_produccion";
        try (Connection conn = con.establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) txtIdSolicitud.setText(String.valueOf(rs.getInt("siguiente")));
        } catch (Exception e) {
            txtIdSolicitud.setText("1");
        }
    }

    private boolean validarCampos() {
        if (dpFechaSolicitud.getValue() == null || dpFechaProduccion.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fechas requeridas", "Completa las fechas de solicitud y producción.");
            return false;
        }
        if (dpFechaSolicitud.getValue().isBefore(LocalDate.now())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha inválida", "La fecha de solicitud no puede ser anterior a hoy.");
            return false;
        }
        if (dpFechaProduccion.getValue().isBefore(LocalDate.now())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha inválida", "La fecha de producción no puede ser anterior a hoy.");
            return false;
        }
        if (idResponsableSeleccionado == 0) {
            mostrarAlerta(Alert.AlertType.WARNING, "Responsable requerido", "Busca y selecciona un responsable por ID.");
            return false;
        }
        if (cbPrioridad.getValue() == null || cbEstado.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos requeridos", "Selecciona prioridad y estado.");
            return false;
        }
        if (listaDetalle.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Productos requeridos",
                    "Agrega al menos un producto antes de guardar.");
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
    @FXML private void irAConsultas(javafx.event.ActionEvent e)              { Navegacion.irA("/vistasFinales/vistaConsultasGenerales.fxml", e); }
    @FXML private void irAConsultaSolicitudesProduccion(javafx.event.ActionEvent e) { Navegacion.irA("/vistasFinales/vistaConsultaSolicitudesProduccion.fxml", e); }
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
