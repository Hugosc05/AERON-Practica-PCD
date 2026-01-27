package aeronpcd.model.concurrent;

/**
 * Clase contenedora para satisfacer el import:
 * aeronpcd.model.concurrent.ControlTowerConcurrent.Request
 */
public class ControlTowerConcurrent {
    
    public static class Request {
        public enum Type { 
            LANDING, LANDED, BOARDED, TAKEOFF, DEPARTED 
        }

        public Type type;
        public String plane;

        public Request(Type type, String plane) {
            this.type = type;
            this.plane = plane;
        }
        
        @Override
        public String toString() {
            return type + " - " + plane;
        }
    }
}