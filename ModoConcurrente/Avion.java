package ModoConcurrente;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.util.Random;


public class Avion implements Runnable {
    // Atributos
    private int idAvion;
    private EstadoAvion estado;
    private TorreDeControl torre;
    private long tiempoInicioOperacion;
    private Random random;

    // Atributos para la UI
    private JTextArea logVentanaAviones;
    private DefaultTableModel modeloTablaVuelos;

    public Avion(int id, TorreDeControl torre, JTextArea logArea, DefaultTableModel modeloTabla) {
        this.idAvion = id;
        this.torre = torre;
        this.estado = EstadoAvion.EN_AIRE;
        this.logVentanaAviones = logArea;
        this.modeloTablaVuelos = modeloTabla;
        this.random = new Random();
    }

    public int getIdAvion(){
        return idAvion;
    }

    @Override
    public void run() {
        try {
            this.tiempoInicioOperacion = System.currentTimeMillis();
            
            // 1. SOLICITAR ATERRIZAJE
            actualizarEstado(EstadoAvion.SOLICITANDO_ATERRIZAJE);
            logEvento("Solicitando pista para aterrizaje...");
            
            // CORRECCIÓN: Pasamos 'this' en lugar de 'idAvion'
            RecursosAsignados recursos = torre.solicitarAterrizaje(this); 
            
            if (recursos == null) return; // Seguridad

            actualizarEstado(EstadoAvion.ATERRIZANDO);
            logEvento("Aterrizando en Pista " + recursos.getIdPista());
            Thread.sleep(1000 + random.nextInt(500)); 

            // 2. LIBERAR PISTA Y SOLICITAR PUERTA
            logEvento("Aterrizaje completado. Liberando pista " + recursos.getIdPista() + "...");
            
            // CORRECCIÓN: Pasamos 'this' y el ID de la pista
            torre.liberarPista(this, recursos.getIdPista());
            
            actualizarEstado(EstadoAvion.EN_PISTA);
            logEvento("Solicitando puerta de embarque...");
            
            // CORRECCIÓN: Pasamos 'this'
            int idPuerta = torre.solicitarPuerta(this);
            
            if (idPuerta == -1) return; // Seguridad
            
            actualizarEstado(EstadoAvion.EN_PUERTA);
            logEvento("En puerta " + idPuerta + ". Desembarcando/Embarcando...");
            Thread.sleep(1500 + random.nextInt(500)); 

            // 3. SOLICITAR DESPEGUE
            actualizarEstado(EstadoAvion.SOLICITANDO_DESPEGUE);
            logEvento("Pasajeros listos. Solicitando pista para despegue...");
            
            // CORRECCIÓN: Liberar puerta pasando 'this' y el ID
            torre.liberarPuerta(this, idPuerta); 
            
            // CORRECCIÓN: Pasamos 'this' y capturamos la pista asignada
            int pistaDespegue = torre.solicitarDespegue(this);
            
            actualizarEstado(EstadoAvion.DESPEGANDO);
            logEvento("Despegando por Pista " + pistaDespegue + "...");
            Thread.sleep(1000 + random.nextInt(500)); 
            
            // 4. FIN
            actualizarEstado(EstadoAvion.FIN_CICLO);
            
            // CORRECCIÓN: Liberar la pista de despegue
            torre.liberarPista(this, pistaDespegue);
            
            long tiempoFinOperacion = System.currentTimeMillis();
            long tiempoTotal = tiempoFinOperacion - tiempoInicioOperacion;
            
            logEvento("Ciclo completado en " + tiempoTotal + " ms.");
            torre.registrarTiempoFinal(this.idAvion, tiempoTotal);

        } catch(InterruptedException e){
            System.out.println("Avion " + idAvion + " interrumpido");
        }
    }

    private void actualizarEstado(EstadoAvion nuevoEstado) {
        this.estado = nuevoEstado;
        SwingUtilities.invokeLater(() -> {
            if (modeloTablaVuelos.getRowCount() >= idAvion) {
                modeloTablaVuelos.setValueAt(nuevoEstado.toString(), idAvion - 1, 1);
            }
        });
    }
    
    private void logEvento(String mensaje) {
        String log = "[Avión " + this.idAvion + "]: " + mensaje + "\n";
        SwingUtilities.invokeLater(() -> {
            logVentanaAviones.append(log);
            logVentanaAviones.setCaretPosition(logVentanaAviones.getDocument().getLength());
        });
    }
}