package ModoConcurrente;

public class HiloClima extends Thread{

    private GestorZonaSeguridad gestor;

    public HiloClima(GestorZonaSeguridad gestor){
        this.gestor=gestor;
    }

    @Override
    public void run(){
        try{
            while(!isInterrupted()){
                gestor.cambiarClima(true);
                Thread.sleep(15000);

                gestor.cambiarClima(false);
                Thread.sleep(10000);
            }
        }catch(InterruptedException e){
            interrupt();
        }
    }
}
