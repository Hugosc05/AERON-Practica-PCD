package ModoSecuencial;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class Simulador extends JFrame {

    // ==========================================
    // ÁREA DE CONFIGURACIÓN (CASOS 1 y 2)
    // ==========================================
    // CASO 1: 2 Aviones, 1 Pista, 1 Puerta
    // CASO 2: 20 Aviones, 3 Pistas, 5 Puertas
    
    private static final int NUM_AVIONES = 20; // <--- CAMBIAR AQUI
    private static final int NUM_PISTAS = 3;   // <--- CAMBIAR AQUI
    private static final int NUM_PUERTAS = 5;  // <--- CAMBIAR AQUI
    // ==========================================

    private JTextArea areaLogAviones;
    private JList<String> listaLogTorre;
    private DefaultListModel<String> modeloLogTorre; 
    private JTable tablaVuelos;
    private DefaultTableModel modeloTabla;

    public Simulador() {
        setTitle("AERON Secuencial - (" + NUM_AVIONES + " Av, " + NUM_PISTAS + " Pis, " + NUM_PUERTAS + " Pue)");
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
        // Inicializar LogManager (Secuencial = true, Operarios = 0)
        LogManager.inicializar(true, NUM_AVIONES, NUM_PISTAS, NUM_PUERTAS, 0);

        AirportState airportState = new AirportState(NUM_AVIONES);
        TorreDeControl torre = new TorreDeControl(modeloLogTorre, airportState); 
        
        modeloLogTorre.addElement(">>> MODO SECUENCIAL <<<");
        LogManager.log("Configuración: " + NUM_AVIONES + " Aviones, " + NUM_PISTAS + " Pistas, " + NUM_PUERTAS + " Puertas.");

        for (int i = 1; i <= NUM_AVIONES; i++) {
            Avion avion = new Avion(i, torre, areaLogAviones, modeloTabla);
            avion.run(); 
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }

        modeloLogTorre.addElement(">>> FIN SIMULACIÓN <<<");
        torre.finalizarSimulacion(); 
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Simulador().setVisible(true));
    }
}