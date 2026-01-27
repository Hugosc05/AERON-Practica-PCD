package aeronpcd.util;

public class Gate {
    private String id;
    private boolean occupied;

    public Gate(String id, boolean occupied) {
        this.id = id;
        this.occupied = occupied;
    }

    public String getId() { return id; }
    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }
    
    @Override
    public String toString() { return id; }
}