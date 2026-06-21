package ModoConcurrente;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class Simulador extends JFrame {

    // ==========================================
    // ÁREA DE CONFIGURACIÓN (CASOS 3, 4, 5 y 6)
    // ==========================================
    private static final int NUM_AVIONES = 20;  // <--- CAMBIAR
    private static final int NUM_PISTAS = 3;    // <--- CAMBIAR
    private static final int NUM_PUERTAS = 5;   // <--- CAMBIAR
    private static final int NUM_OPERARIOS = 5; // <--- CAMBIAR
    // ==========================================

    private JTextArea areaLogAviones;
    private JList<String> listaLogTorre;
    private DefaultListModel<String> modeloLogTorre; 
    private JTable tablaVuelos;
    private DefaultTableModel modeloTabla;

    public Simulador() {
        setTitle("AERON Concurrente - (" + NUM_AVIONES + "Av, " + NUM_PISTAS + "Pi, " + NUM_PUERTAS + "Pu, " + NUM_OPERARIOS + "Op)");
        setSize(1200, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 3)); 

        areaLogAviones = new JTextArea();
        areaLogAviones.setEditable(false);
        JScrollPane scrollAviones = new JScrollPane(areaLogAviones);
        scrollAviones.setBorder(BorderFactory.createTitledBorder("Eventos Aviones"));
        add(scrollAviones);

        modeloLogTorre = new DefaultListModel<>();
        listaLogTorre = new JList<>(modeloLogTorre);
        JScrollPane scrollTorre = new JScrollPane(listaLogTorre);
        scrollTorre.setBorder(BorderFactory.createTitledBorder("Torre Control"));
        add(scrollTorre);

        String[] columnas = {"Avión", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0);
        tablaVuelos = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaVuelos);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("Vuelos"));
        add(scrollTabla);

        for (int i = 1; i <= NUM_AVIONES; i++) {
            modeloTabla.addRow(new Object[]{"Avión " + i, "ESPERANDO..."});
        }

        new Thread(this::ejecutarSimulacion).start();
    }

    private void ejecutarSimulacion() {
        // Inicializar LogManager (Secuencial = false)
        LogManager.inicializar(false, NUM_AVIONES, NUM_PISTAS, NUM_PUERTAS, NUM_OPERARIOS);

        AirportState airportState = new AirportState(NUM_AVIONES);
        
        // Pasamos parámetros a la Torre para que configure sus Semáforos
        TorreDeControl torre = new TorreDeControl(modeloLogTorre, airportState, NUM_PISTAS, NUM_PUERTAS); 

        GestorZonaSeguridad gestor = new GestorZonaSeguridad();
        HiloClima hiloClima = new HiloClima(gestor);

        hiloClima.start();
        modeloLogTorre.addElement(">>> MODO CONCURRENTE <<<");
        LogManager.log("Inicio Simulación Concurrente. Operarios: " + NUM_OPERARIOS);

        // Lanzar Hilos de Aviones
        for (int i = 1; i <= NUM_AVIONES; i++) {
            Avion avion = new Avion(i, torre, areaLogAviones, modeloTabla, gestor);
            new Thread(avion).start();
        }
        
        // NOTA: En modo concurrente, el fin de la simulación suele detectarse
        // dentro de la Torre (cuando cuentan 20 aviones finalizados),
        // así que Torre llamará a finalizarSimulacion().
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Simulador().setVisible(true));
    }
}