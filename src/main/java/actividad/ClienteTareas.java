package actividad;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class ClienteTareas {

    private static XmlRpcClient client;

    public static void main(String[] args) throws Exception {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL("http://161.132.51.124:8000"));
        client = new XmlRpcClient();
        client.setConfig(config);

        SwingUtilities.invokeLater(ClienteTareas::crearVentanaPrincipal);
    }

    private static void crearVentanaPrincipal() {
        JFrame frame = new JFrame("Cliente de Tareas - RPC");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1300, 400);

        JTextField txtDescripcion = new JTextField(25);
        JButton btnAgregar = new JButton("Agregar Tarea");
        JButton btnListar = new JButton("Listar Tareas");
        JButton btnCompletar = new JButton("Marcar como Completada");
        JButton btnEliminar = new JButton("Eliminar Tarea");
        JTextField txtBuscar = new JTextField(20);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnSalir = new JButton("Salir");  // Nuevo botón
        
        // Configuración de colores para los botones
        btnAgregar.setBackground(new Color(76, 175, 80));     // Verde
        btnAgregar.setForeground(Color.WHITE);
        btnListar.setBackground(new Color(33, 150, 243));      // Azul
        btnListar.setForeground(Color.WHITE);
        btnCompletar.setBackground(new Color(255, 193, 7));    // Amarillo
        btnCompletar.setForeground(Color.BLACK);
        btnEliminar.setBackground(new Color(244, 67, 54));     // Rojo
        btnEliminar.setForeground(Color.WHITE);
        btnBuscar.setBackground(new Color(96, 125, 139));      // Gris azulado
        btnBuscar.setForeground(Color.WHITE);
        btnSalir.setBackground(new Color(156, 39, 176));      // Morado
        btnSalir.setForeground(Color.WHITE);

        JPanel panelSuperior = new JPanel(new FlowLayout());
        panelSuperior.add(new JLabel("Descripción:"));
        panelSuperior.add(txtDescripcion);
        panelSuperior.add(btnAgregar);
        panelSuperior.add(btnListar);
        panelSuperior.add(btnCompletar);
        panelSuperior.add(btnEliminar);
        panelSuperior.add(new JLabel("Buscar ID:"));
        panelSuperior.add(txtBuscar);
        panelSuperior.add(btnBuscar);
        
        // Panel inferior para el botón Salir
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelInferior.add(btnSalir);

        JTable tabla = new JTable();
        JScrollPane scrollPane = new JScrollPane(tabla);
        
        // Organización de los componentes en el frame
        frame.add(panelSuperior, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(panelInferior, BorderLayout.SOUTH);

        // Acción para el botón Salir
        btnSalir.addActionListener(e -> {
            int confirmacion = JOptionPane.showConfirmDialog(
                frame, 
                "¿Está seguro que desea salir?", 
                "Confirmar salida", 
                JOptionPane.YES_NO_OPTION
            );
            
            if (confirmacion == JOptionPane.YES_OPTION) {
                frame.dispose(); // Cierra la ventana
                System.exit(0);  // Termina la aplicación
            }
        });

        // Resto de los action listeners (se mantienen igual)
        btnAgregar.addActionListener(e -> {
            String desc = txtDescripcion.getText().trim();
            if (desc.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Ingrese una descripción para la tarea.");
                return;
            }

            try {
                String id = (String) client.execute("agregar_tarea", new Object[]{desc});
                JOptionPane.showMessageDialog(frame, "Tarea agregada con ID: " + id);
                txtDescripcion.setText("");         // limpiar campo
                btnListar.doClick();                // actualizar tabla
            } catch (Exception ex) {
                mostrarError(frame, ex);
            }
        });

        btnListar.addActionListener(e -> {
            try {
                Object[] tareasRaw = (Object[]) client.execute("listar_tareas", new Object[]{});
                DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Descripción", "Estado"}, 0);

                for (Object obj : tareasRaw) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tarea = (Map<String, Object>) obj;
                    model.addRow(new Object[]{tarea.get("id"), tarea.get("descripcion"), tarea.get("estado")});
                }
                tabla.setModel(model);
            } catch (Exception ex) {
                mostrarError(frame, ex);
            }
        });

        btnCompletar.addActionListener(e -> {
            int selectedRow = tabla.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(frame, "Selecciona una tarea en la tabla.");
                return;
            }

            String id = tabla.getValueAt(selectedRow, 0).toString(); // Columna 0 = ID
            try {
                boolean ok = (Boolean) client.execute("marcar_completada", new Object[]{id});
                JOptionPane.showMessageDialog(frame, ok ? "Tarea marcada como completada." : "Tarea no encontrada.");
                // Refrescar tabla
                btnListar.doClick();
            } catch (Exception ex) {
                mostrarError(frame, ex);
            }
        });

        btnEliminar.addActionListener(e -> {
            int selectedRow = tabla.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(frame, "Selecciona una tarea en la tabla.");
                return;
            }

            String id = tabla.getValueAt(selectedRow, 0).toString(); // Columna 0 = ID
            try {
                boolean ok = (Boolean) client.execute("eliminar_tarea", new Object[]{id});
                JOptionPane.showMessageDialog(frame, ok ? "Tarea eliminada." : "Tarea no encontrada.");
                // Refrescar tabla
                btnListar.doClick();
            } catch (Exception ex) {
                mostrarError(frame, ex);
            }
        });

        btnBuscar.addActionListener(e -> {
            String id = txtBuscar.getText().trim();
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Ingrese un ID para buscar.");
                return;
            }

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> tarea = (Map<String, Object>) client.execute("obtener_tarea_por_id", new Object[]{id});
                if (tarea != null && !tarea.isEmpty()) {
                    DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Descripción", "Estado"}, 0);
                    model.addRow(new Object[]{tarea.get("id"), tarea.get("descripcion"), tarea.get("estado")});
                    tabla.setModel(model);
                } else {
                    JOptionPane.showMessageDialog(frame, "No se encontró una tarea con ese ID.");
                }
            } catch (Exception ex) {
                mostrarError(frame, ex);
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void mostrarError(Component parent, Exception ex) {
        JOptionPane.showMessageDialog(parent, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }
}