package com.example.chocolateria.controller;

import com.example.chocolateria.baseDeDatos.conexion;
import com.example.chocolateria.modelo.productoModelo;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import java.sql.*;

public class productoController {

    @FXML private TextField        txtCodigo;
    @FXML private TextField        txtNombre;
    @FXML private TextField        txtPrecioUnitario;
    @FXML private TextField        txtPrecioMayor;
    @FXML private TextArea         txtDescripcion;
    @FXML private ComboBox<String> cbUnidadMedida;

    @FXML private CheckBox chkBombones, chkTabletas, chkTrufas, chkBanos;
    @FXML private CheckBox chkRellenos, chkCoberturas, chkGranolas, chkMateriaPrima, chkEmpaque;
    @FXML private CheckBox chkTerminado, chkSemielaborado, chkInsumo;
    @FXML private CheckBox chkMateriaP, chkEmpaqueT, chkSubproducto;

    private productoModelo productoCargado = null;
    private final conexion con = new conexion();

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
        cbUnidadMedida.setItems(FXCollections.observableArrayList(
            "Unidad", "Caja", "Paquete", "Kilogramo", "Gramo",
            "Litro", "Mililitro", "Bolsa", "Barra", "Docena"));

        for (CheckBox cb : new CheckBox[]{chkBombones,chkTabletas,chkTrufas,chkBanos,
                chkRellenos,chkCoberturas,chkGranolas,chkMateriaPrima,chkEmpaque}) {
            cb.selectedProperty().addListener((obs, old, nv) -> {
                if (productoCargado == null)
                    actualizarCodigo();
            });
        }

        txtCodigo.setText("Selecciona categoría");
    }

    private void actualizarCodigo() {
        String p = getPrefijo();
        if (p == null) { txtCodigo.setText("Selecciona categoría"); return; }
        generarCodigo(p);
    }

    private String getPrefijo() {
        if (chkBombones.isSelected())     return "CHOC";
        if (chkTabletas.isSelected())     return "TABL";
        if (chkTrufas.isSelected())       return "TRUF";
        if (chkBanos.isSelected())        return "BAÑO";
        if (chkRellenos.isSelected())     return "RLLE";
        if (chkCoberturas.isSelected())   return "COBE";
        if (chkGranolas.isSelected())     return "GRAN";
        if (chkMateriaPrima.isSelected()) return "MATP";
        if (chkEmpaque.isSelected())      return "EMPA";
        return null;
    }

    private void generarCodigo(String prefijo) {
        String sql = "SELECT MAX(CAST(SUBSTRING(codigo,6,LEN(codigo)) AS INT)) AS ultimo " +
                     "FROM tbl_producto WHERE codigo LIKE ?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefijo + "-%");
            ResultSet rs = ps.executeQuery();
            int ultimo = rs.next() ? rs.getInt("ultimo") : 0;
            txtCodigo.setText(String.format("%s-%03d", prefijo, ultimo + 1));
        } catch (Exception e) {
            txtCodigo.setText(prefijo + "-001");
        }
    }

    @FXML
    private void guardar() {
        if (estadoActual == 1) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Acción no disponible", "Ya hay un registro cargado. Usa 'Editar' para modificarlo o 'Limpiar' para crear uno nuevo.");
            return;
        }
        if (!validarCampos()) return;
        String categoria = getSeleccionados(chkBombones,chkTabletas,chkTrufas,chkBanos,
                                            chkRellenos,chkCoberturas,chkGranolas,chkMateriaPrima,chkEmpaque);
        String tipo      = getSeleccionados(chkTerminado,chkSemielaborado,chkInsumo,
                                            chkMateriaP,chkEmpaqueT,chkSubproducto);
        if (categoria.isEmpty()) { mostrarAlerta(Alert.AlertType.WARNING,"Categoría requerida","Selecciona al menos una categoría."); return; }
        if (tipo.isEmpty())      { mostrarAlerta(Alert.AlertType.WARNING,"Tipo requerido","Selecciona al menos un tipo."); return; }
        if (txtCodigo.getText().equals("Selecciona categoría")) {
            mostrarAlerta(Alert.AlertType.WARNING,"Código","Selecciona una categoría para generar el código."); return; }

        String sql = "INSERT INTO tbl_producto(codigo,nombre,precio_unitario,precio_mayor," +
                     "descripcion,unidad_medida,categoria,tipo) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txtCodigo.getText().trim());
            ps.setString(2, txtNombre.getText().trim());
            ps.setDouble(3, Double.parseDouble(txtPrecioUnitario.getText().trim()));
            ps.setDouble(4, Double.parseDouble(txtPrecioMayor.getText().trim()));
            ps.setString(5, txtDescripcion.getText().trim());
            ps.setString(6, cbUnidadMedida.getValue() != null ? cbUnidadMedida.getValue() : "");
            ps.setString(7, categoria);
            ps.setString(8, tipo);
            ps.executeUpdate();
            mostrarAlerta(Alert.AlertType.INFORMATION,"Éxito","Producto guardado: " + txtCodigo.getText().trim());
            limpiar();
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING,"Precios inválidos","Los precios deben ser números válidos.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR,"Error al guardar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEditar() {
        actualizarBotones(2);
        if (productoCargado == null) { mostrarAlerta(Alert.AlertType.WARNING,"Atención","Busca un producto por código primero para editar."); return; }
        if (!validarCampos()) return;
        String categoria = getSeleccionados(chkBombones,chkTabletas,chkTrufas,chkBanos,
                                            chkRellenos,chkCoberturas,chkGranolas,chkMateriaPrima,chkEmpaque);
        String tipo      = getSeleccionados(chkTerminado,chkSemielaborado,chkInsumo,
                                            chkMateriaP,chkEmpaqueT,chkSubproducto);
        if (categoria.isEmpty()) { mostrarAlerta(Alert.AlertType.WARNING,"Categoría requerida","Selecciona al menos una categoría."); return; }
        if (tipo.isEmpty())      { mostrarAlerta(Alert.AlertType.WARNING,"Tipo requerido","Selecciona al menos un tipo."); return; }

        String sql = "UPDATE tbl_producto SET nombre=?,precio_unitario=?,precio_mayor=?," +
                     "descripcion=?,unidad_medida=?,categoria=?,tipo=? WHERE codigo=?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txtNombre.getText().trim());
            ps.setDouble(2, Double.parseDouble(txtPrecioUnitario.getText().trim()));
            ps.setDouble(3, Double.parseDouble(txtPrecioMayor.getText().trim()));
            ps.setString(4, txtDescripcion.getText().trim());
            ps.setString(5, cbUnidadMedida.getValue() != null ? cbUnidadMedida.getValue() : "");
            ps.setString(6, categoria);
            ps.setString(7, tipo);
            ps.setString(8, productoCargado.getCodigo());
            ps.executeUpdate();
            mostrarAlerta(Alert.AlertType.INFORMATION,"Éxito","Producto actualizado correctamente.");
            limpiar();
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING,"Precios inválidos","Los precios deben ser números válidos.");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR,"Error al editar", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void fnEliminar() {
        if (productoCargado == null) { mostrarAlerta(Alert.AlertType.WARNING,"Atención","Busca un producto por código primero para eliminar."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar " + productoCargado.getNombre() + " (" + productoCargado.getCodigo() + ")?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try (Connection conn = con.establecerConexion();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM tbl_producto WHERE codigo=?")) {
                    ps.setString(1, productoCargado.getCodigo());
                    ps.executeUpdate();
                    mostrarAlerta(Alert.AlertType.INFORMATION,"Éxito","Producto eliminado.");
                    limpiar();
                } catch (Exception e) {
                    mostrarAlerta(Alert.AlertType.ERROR,"Error al eliminar", "Ocurrió un error. Intente de nuevo.");
                }
            }
        });
    }

    @FXML
    private void fnBuscar() {
        actualizarBotones(0);
        String cod = txtCodigo.getText().trim();
        if (cod.isEmpty() || cod.equals("Selecciona categoría")) {
            mostrarAlerta(Alert.AlertType.WARNING,"Atención","Escribe un código para buscar."); return; }

        String sql = "SELECT codigo,nombre,precio_unitario,precio_mayor,descripcion,unidad_medida,categoria,tipo,stock FROM tbl_producto WHERE codigo=?";
        try (Connection conn = con.establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cod);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                cargarEnFormulario(new productoModelo(
                    rs.getString("codigo"), rs.getString("nombre"),
                    rs.getDouble("precio_unitario"), rs.getDouble("precio_mayor"),
                    rs.getString("descripcion") != null ? rs.getString("descripcion") : "",
                    rs.getString("unidad_medida") != null ? rs.getString("unidad_medida") : "",
                    rs.getString("categoria"), rs.getString("tipo"), rs.getInt("stock")));
                actualizarBotones(1);
                mostrarAlerta(Alert.AlertType.INFORMATION,"Encontrado","Producto encontrado y cargado en el formulario.");
            } else {
                mostrarAlerta(Alert.AlertType.WARNING,"No encontrado","No existe producto con código " + cod + ".");
            }
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR,"Error de búsqueda", "Ocurrió un error. Intente de nuevo.");
        }
    }

    @FXML
    private void limpiar() {
        actualizarBotones(0);
        txtCodigo.setText("Selecciona categoría");
        txtNombre.clear(); txtPrecioUnitario.clear();
        txtPrecioMayor.clear(); txtDescripcion.clear();
        cbUnidadMedida.setValue(null);
        for (CheckBox cb : new CheckBox[]{chkBombones,chkTabletas,chkTrufas,chkBanos,
            chkRellenos,chkCoberturas,chkGranolas,chkMateriaPrima,chkEmpaque,
            chkTerminado,chkSemielaborado,chkInsumo,chkMateriaP,chkEmpaqueT,chkSubproducto})
            cb.setSelected(false);
        productoCargado = null;
    }

    private void cargarEnFormulario(productoModelo p) {
        this.productoCargado = p;
        txtCodigo.setText(p.getCodigo());
        txtNombre.setText(p.getNombre());
        txtPrecioUnitario.setText(String.valueOf(p.getPrecioUnitario()));
        txtPrecioMayor.setText(String.valueOf(p.getPrecioMayor()));
        txtDescripcion.setText(p.getDescripcion());
        cbUnidadMedida.setValue(p.getUnidadMedida());
        String cat = p.getCategoria() != null ? p.getCategoria() : "";
        chkBombones.setSelected(cat.contains("Bombones"));
        chkTabletas.setSelected(cat.contains("Tabletas"));
        chkTrufas.setSelected(cat.contains("Trufas"));
        chkBanos.setSelected(cat.contains("Baños"));
        chkRellenos.setSelected(cat.contains("Rellenos"));
        chkCoberturas.setSelected(cat.contains("Coberturas"));
        chkGranolas.setSelected(cat.contains("Granolas"));
        chkMateriaPrima.setSelected(cat.contains("Materia prima"));
        chkEmpaque.setSelected(cat.contains("Empaque / Presentación"));
        String tip = p.getTipo() != null ? p.getTipo() : "";
        chkTerminado.setSelected(tip.contains("Terminado"));
        chkSemielaborado.setSelected(tip.contains("Semielaborado"));
        chkInsumo.setSelected(tip.contains("Insumo"));
        chkMateriaP.setSelected(tip.contains("Materia prima"));
        chkEmpaqueT.setSelected(tip.contains("Empaque"));
        chkSubproducto.setSelected(tip.contains("Subproducto"));
    }

    private String getSeleccionados(CheckBox... cbs) {
        StringBuilder sb = new StringBuilder();
        for (CheckBox cb : cbs) if (cb.isSelected()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(cb.getText());
        }
        return sb.toString();
    }

    private boolean validarCampos() {
        if (txtNombre.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING,"Campo requerido","El nombre es obligatorio."); return false; }
        if (txtPrecioUnitario.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING,"Campo requerido","El precio unitario es obligatorio."); return false; }
        if (txtPrecioMayor.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING,"Campo requerido","El precio al por mayor es obligatorio."); return false; }
        try {
            Double.parseDouble(txtPrecioUnitario.getText().trim());
            Double.parseDouble(txtPrecioMayor.getText().trim());
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING,"Precios inválidos","Los precios deben ser números válidos."); return false; }
        return true;
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String msg) {
        Alert a = new Alert(tipo); a.setTitle(titulo); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
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
    @FXML private void irAConsultaProductos(javafx.event.ActionEvent e)   { Navegacion.irA("/vistasFinales/vistaConsultaProductos.fxml", e); }
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
