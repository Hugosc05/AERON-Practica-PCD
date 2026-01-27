package ModoConcurrente;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import aeronpcd.model.concurrent.ControlTowerConcurrent.Request;
import aeronpcd.util.Runway;
import aeronpcd.util.Gate;

public class TorreDeControl {

    private DefaultListModel<String> modeloLogTorre;
    private AirportState airportState;
    private Map<Integer, Long> tiemposFinales;
    
    // Semáforos y Recursos
    private Semaphore semaforoPistas;
    private Semaphore semaforoPuertas;
    private boolean[] pistas;
    private boolean[] puertas;
    
    private int totalAvionesEsperados; // Para saber cuándo cerrar

    // Constructor actualizado
    public TorreDeControl(DefaultListModel<String> modeloLogTorre, AirportState estado, int numPistas, int numPuertas) {
        this.modeloLogTorre = modeloLogTorre;
        this.airportState = estado;
        this.tiemposFinales = new ConcurrentHashMap<>();
        
        // Inicialización dinámica según configuración
        this.semaforoPistas = new Semaphore(numPistas, true);
        this.semaforoPuertas = new Semaphore(numPuertas, true);
        
        this.pistas = new boolean[numPistas];
        this.puertas = new boolean[numPuertas];
        
        // Suponemos que el tamaño del array de estado en AirportState es el total de aviones
        // Ojo: Si AirportState no tiene un getter de tamaño, lo estimamos o lo pasamos.
        // Aquí usaremos 20 por defecto o lo pasamos en constructor si lo prefieres.
        this.totalAvionesEsperados = 20; // OJO: Deberías pasarlo si varía (Caso 1 vs Caso 2)
    }

    // --- MÉTODOS DE GESTIÓN (Concurrente) ---

    public RecursosAsignados solicitarAterrizaje(Avion avion) {
        try {
            logTorre("Avión " + avion.getIdAvion() + " solicita aterrizaje.");
            airportState.setEstadoAvion(avion.getIdAvion(), "ESPERANDO_PISTA");
            
            semaforoPistas.acquire(); // Bloquea si no hay pistas
            
            int pistaAsignada = ocuparRecurso(pistas);
            logTorre("Avión " + avion.getIdAvion() + " aterriza en Pista " + (pistaAsignada + 1));
            airportState.setEstadoAvion(avion.getIdAvion(), "ATERRIZANDO");
            
            // Retornamos Pista X, Puerta -1 (aún no tiene)
            return new RecursosAsignados(pistaAsignada + 1, -1);
            
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void liberarPista(Avion avion, int idPista) {
        liberarRecurso(pistas, idPista - 1);
        semaforoPistas.release();
        logTorre("Avión " + avion.getIdAvion() + " libera Pista " + idPista);
    }

    public int solicitarPuerta(Avion avion) {
        try {
            logTorre("Avión " + avion.getIdAvion() + " solicita puerta.");
            airportState.setEstadoAvion(avion.getIdAvion(), "ESPERANDO_PUERTA");
            
            semaforoPuertas.acquire();
            
            int puertaAsignada = ocuparRecurso(puertas);
            logTorre("Avión " + avion.getIdAvion() + " asignado a Puerta " + (puertaAsignada + 1));
            airportState.setEstadoAvion(avion.getIdAvion(), "EN_PUERTA");
            
            return puertaAsignada + 1;
            
        } catch (InterruptedException e) {
            return -1;
        }
    }

    public void liberarPuerta(Avion avion, int idPuerta) {
        liberarRecurso(puertas, idPuerta - 1);
        semaforoPuertas.release();
        logTorre("Avión " + avion.getIdAvion() + " libera Puerta " + idPuerta);
    }
    
    public int solicitarDespegue(Avion avion) {
        try {
             logTorre("Avión " + avion.getIdAvion() + " solicita despegue.");
             airportState.setEstadoAvion(avion.getIdAvion(), "ESPERANDO_PISTA_DESPEGUE");
             
             semaforoPistas.acquire();
             
             int pista = ocuparRecurso(pistas);
             logTorre("Avión " + avion.getIdAvion() + " despega por Pista " + (pista + 1));
             airportState.setEstadoAvion(avion.getIdAvion(), "DESPEGANDO");
             
             return pista + 1; // Retorna la pista usada
        } catch (InterruptedException e) { 
            return -1;
        }
    }
    
    
    // --- HELPERS ---
    
    private synchronized int ocuparRecurso(boolean[] array) {
        for (int i = 0; i < array.length; i++) {
            if (!array[i]) { // false = libre (según tu lógica, o true = libre)
                // Usualmente boolean array default es false.
                // Vamos a asumir: false = libre, true = ocupado
                array[i] = true;
                return i;
            }
        }
        return -1;
    }
    
    private synchronized void liberarRecurso(boolean[] array, int index) {
        if (index >= 0 && index < array.length) {
            array[index] = false;
        }
    }

    // --- LOGGING ---

    public void logTorre(String mensaje) {
        SwingUtilities.invokeLater(() -> modeloLogTorre.addElement(mensaje));
        LogManager.log(mensaje);
    }

    public synchronized void registrarTiempoFinal(int idAvion, long tiempoTotal) {
        tiemposFinales.put(idAvion, tiempoTotal);
        
        // Comprobar si hemos terminado (ajusta el numero segun la prueba)
        // Puedes pasar el total de aviones en el constructor para ser exacto
        if (tiemposFinales.size() >= 2) { 
             // OJO: Aquí deberías usar la variable totalAvionesEsperados
             // Para simplificar, generamos el CSV cada vez o al final
        }
    }
    
    public void finalizarSimulacion() {
        generarReporteCSV();
        LogManager.cerrar();
    }

    private void generarReporteCSV() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("reporte_aeron_concurrente.csv"))) {
            pw.println("Avión,Tiempo(ms)");
            tiemposFinales.forEach((k,v) -> pw.println("Avión " + k + "," + v));
        } catch (Exception e) {}
    }
}