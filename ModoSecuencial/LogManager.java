package ModoSecuencial;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {

    private static PrintWriter writer;
    
    public static void inicializar(boolean esSecuencial, int nAviones, int nPistas, int nPuertas, int nOperarios) {
        try {
            File directorio = new File("logs");
            if (!directorio.exists()) {
                directorio.mkdir();
            }

            String modo = esSecuencial ? "Secuencial" : "Concurrente";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            StringBuilder sb = new StringBuilder();
            sb.append("logs/aeron-")
              .append(modo).append("-")
              .append(nAviones).append("AV-")
              .append(nPistas).append("PIS-")
              .append(nPuertas).append("PUE");
            
            if (nOperarios > 0) {
                sb.append("-").append(nOperarios).append("OPE");
            }
            
            sb.append("-").append(timestamp).append(".log");

            writer = new PrintWriter(new BufferedWriter(new FileWriter(sb.toString())));
            
            log(">>> INICIO DE SIMULACIÓN: " + modo + " <<<");
            log("Configuración: " + nAviones + " Aviones, " + nPistas + " Pistas, " + nPuertas + " Puertas.");

        } catch (IOException e) {
            System.err.println("Error al crear el log: " + e.getMessage());
        }
    }

    public static synchronized void log(String mensaje) {
        if (writer != null) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            writer.println("[" + time + "] " + mensaje);
            writer.flush();
        }
    }

    public static void cerrar() {
        if (writer != null) {
            log(">>> FIN DE SIMULACIÓN <<<");
            writer.close();
        }
    }
}