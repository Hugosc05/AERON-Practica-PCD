package aeronpcd.util;

public class Runway {
    private String id;
    private boolean available;

    public Runway(String id, boolean available) {
        this.id = id;
        this.available = available;
    }

    public String getId() { return id; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    
    @Override
    public String toString() { return id; }
}