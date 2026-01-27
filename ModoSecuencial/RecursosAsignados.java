package ModoSecuencial;

public class RecursosAsignados {

    private final int idPista;
    private final int idPuerta;

    /**
     * Constructor para crear un nuevo paquete de recursos asignados.
     * @param idPista El ID (índice) de la pista asignada.
     * @param idPuerta El ID (índice) de la puerta de embarque asignada.
     */
    public RecursosAsignados(int idPista, int idPuerta) {
        this.idPista = idPista;
        this.idPuerta = idPuerta;
    }

    /**
     * @return El ID de la pista asignada.
     */
    public int getIdPista() {
        return idPista;
    }

    /**
     * @return El ID de la puerta de embarque asignada.
     */
    public int getIdPuerta() {
        return idPuerta;
    }

    @Override
    public String toString() {
        return "RecursosAsignados{" +
                "idPista=" + idPista +
                ", idPuerta=" + idPuerta +
                '}';
    }
}