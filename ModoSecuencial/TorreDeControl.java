package ModoSecuencial;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

import ModoSecuencial.AirportState;

public class TorreDeControl {

    private DefaultListModel<String> modeloLogTorre;
    private AirportState airportState; 
    
    // Para el reporte CSV
    private Map<Integer, Long> tiemposFinales;
    // Contador para saber cuándo cerrar en secuencial si se desea automático
    private int avionesRegistrados = 0;

    // Constructor corregido: Recibe el modelo de lista UI y el Estado Global
    public TorreDeControl(DefaultListModel<String> modeloLogTorre, AirportState estado) {
        this.modeloLogTorre = modeloLogTorre;
        this.airportState = estado;
        this.tiemposFinales = new ConcurrentHashMap<>();
    }    

    // --- MÉTODOS DE GESTIÓN (Lógica Secuencial) ---
    // En modo secuencial, como los aviones se ejecutan uno tras otro (Avion.run() directo)
    // o en hilos que se lanzan de uno en uno con join (simulado), 
    // no hay contención real de recursos. Simplemente registramos y avanzamos.

    public synchronized RecursosAsignados solicitarAterrizaje(Avion avion) {
        logTorre("Avión " + avion.getIdAvion() + " solicita aterrizaje.");
        airportState.setEstadoAvion(avion.getIdAvion(), "SOLICITANDO_ATERRIZAJE");
        
        sleep(500); // Simulación de tiempo de gestión
        
        logTorre("Avión " + avion.getIdAvion() + " autorizado a aterrizar en Pista 1.");
        airportState.setEstadoAvion(avion.getIdAvion(), "ATERRIZANDO");
        
        // En secuencial, siempre asignamos Pista 1 y Puerta 1 (simplificación)
        return new RecursosAsignados(1, 1);
    }
    
    public synchronized void liberarPista(Avion avion) {
        // En secuencial no hace falta lógica de semáforos
        logTorre("Avión " + avion.getIdAvion() + " libera Pista.");
    }

    public synchronized void solicitarPuerta(Avion avion) {
        logTorre("Avión " + avion.getIdAvion() + " solicita puerta.");
        airportState.setEstadoAvion(avion.getIdAvion(), "EN_PISTA");
        
        sleep(200);
        
        logTorre("Avión " + avion.getIdAvion() + " entra en Puerta 1.");
        airportState.setEstadoAvion(avion.getIdAvion(), "EN_PUERTA");
    }
    
    public synchronized void liberarPuerta(Avion avion) {
        logTorre("Avión " + avion.getIdAvion() + " libera Puerta.");
    }

    public synchronized void solicitarDespegue(Avion avion) {
        logTorre("Avión " + avion.getIdAvion() + " solicita despegue.");
        airportState.setEstadoAvion(avion.getIdAvion(), "SOLICITANDO_DESPEGUE");
        
        sleep(500);
        
        logTorre("Avión " + avion.getIdAvion() + " despegando por Pista 1.");
        airportState.setEstadoAvion(avion.getIdAvion(), "DESPEGANDO");
    }

    // --- LOGGING Y REPORTE ---

    public void registrarTiempoFinal(int idAvion, long tiempoTotal) {
        tiemposFinales.put(idAvion, tiempoTotal);
        avionesRegistrados++;
        // En el Simulador.java, al ser bucle for secuencial, 
        // se llama a finalizarSimulacion() explícitamente al acabar el bucle.
    }

    public void logTorre(String mensaje) {
        // 1. Escribir en la Interfaz Gráfica
        SwingUtilities.invokeLater(() -> {
            modeloLogTorre.addElement(mensaje);
        });
        
        // 2. Escribir en el Fichero de Texto (Requisito Obligatorio)
        LogManager.log(mensaje);
    }

    public void finalizarSimulacion() {
        generarReporteCSV();
        LogManager.cerrar(); // Cierra el fichero de log
    }

    private void generarReporteCSV() {
        String nombreArchivo = "reporte_aeron_secuencial.csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(nombreArchivo))) {
            pw.println("Avión,Tiempo total (ms),Observaciones");
            
            // Ordenar por tiempo (Ranking)
            List<Map.Entry<Integer, Long>> resultadosOrdenados = 
                tiemposFinales.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toList());
            
            int rank = 1;
            for (Map.Entry<Integer, Long> entry : resultadosOrdenados) {
                pw.println("Avión " + entry.getKey() + "," + entry.getValue() + "," + (rank++) + "°");
            }
            logTorre("Reporte CSV generado: " + nombreArchivo);
            
        } catch (Exception e) {
            e.printStackTrace();
            logTorre("Error al generar CSV: " + e.getMessage());
        }
    }
    
    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}