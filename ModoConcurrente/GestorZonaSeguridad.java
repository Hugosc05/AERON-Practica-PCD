package ModoConcurrente;

public class GestorZonaSeguridad {
    private int avionesEnZona=0;
    private boolean climaBueno=true;

    public synchronized void entrarZona() throws InterruptedException {
        while (!climaBueno || avionesEnZona >= 2) {
            System.out.println("Bloqueado intentando entrar. Clima: " + climaBueno + " | En zona: " + avionesEnZona);
            wait();
        }
        avionesEnZona++;
        System.out.println("Entra avión. Total en zona: " + avionesEnZona);
    }
    
    public synchronized void salirZona() throws InterruptedException {
        while (!climaBueno) {
            System.out.println("Bloqueado intentando salir. Clima malo.");
            wait();
        }
        avionesEnZona--;
        System.out.println("Sale avión. Total en zona: " + avionesEnZona);
        notifyAll();
    }

    public synchronized void cambiarClima(boolean nuevoClima){
        this.climaBueno=nuevoClima;
        notifyAll();
    }
}
