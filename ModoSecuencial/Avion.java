package ModoSecuencial;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.util.Random;

public class Avion implements Runnable {
    //Atributos
    private int idAvion;
    private EstadoAvion estado;
    private TorreDeControl torre;
    private long tiempoInicioOperacion;
    private Random random;

    //Atributos para la UI
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
            
            // 1. Solicitar Aterrizaje
            actualizarEstado(EstadoAvion.SOLICITANDO_ATERRIZAJE);
            logEvento("Solicitando pista para aterrizaje...");
            
            // CORRECCION: Pasamos 'this' en lugar de 'idAvion'
            RecursosAsignados recursos = torre.solicitarAterrizaje(this); 
            
            actualizarEstado(EstadoAvion.ATERRIZANDO);
            logEvento("Aterrizando en Pista " + recursos.getIdPista());
            Thread.sleep(1000 + random.nextInt(500)); 

            // 2. Liberar Pista y Solicitar Puerta
            logEvento("Aterrizaje completado. Liberando pista y solicitando puerta...");
            
            // CORRECCION: Usamos un método unificado o específico pasando 'this'
            torre.liberarPista(this);
            
            actualizarEstado(EstadoAvion.EN_PISTA);
            
            // CORRECCION: Pasamos 'this'
            torre.solicitarPuerta(this);
            
            actualizarEstado(EstadoAvion.EN_PUERTA);
            logEvento("En puerta " + recursos.getIdPuerta() + ". Desembarcando/Embarcando...");
            Thread.sleep(1500 + random.nextInt(500)); 

            // 3. Solicitar Despegue
            actualizarEstado(EstadoAvion.SOLICITANDO_DESPEGUE);
            logEvento("Pasajeros listos. Solicitando pista para despegue...");
            
            // CORRECCION: Pasamos 'this'
            torre.liberarPuerta(this); // Liberamos la puerta antes o durante la solicitud
            torre.solicitarDespegue(this);
            
            actualizarEstado(EstadoAvion.DESPEGANDO);
            logEvento("Despegando...");
            Thread.sleep(1000 + random.nextInt(500)); 
            
            // 4. Fin
            actualizarEstado(EstadoAvion.FIN_CICLO);
            
            // CORRECCION: Liberar pista de despegue
            torre.liberarPista(this);
            
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