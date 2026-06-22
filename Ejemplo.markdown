Ejercicio 1: Protocolo de Emergencia VIP
Enunciado

La dirección del aeropuerto exige implementar un protocolo para vuelos VIP (ej. autoridades o emergencias). Se debe crear un GestorPrioridad que controle el acceso al aeropuerto con estas reglas:

    Capacidad máxima simultánea de operaciones: 3 aviones.

    Si un avión VIP está esperando para operar, ningún avión normal (no VIP) puede iniciar una nueva operación, incluso si hay capacidad disponible. Los aviones normales deben esperar a que todos los VIP hayan entrado.

Paso 1: Crear GestorPrioridad.java

Crea este archivo en el paquete aeronpcd.model.concurrent. Este monitor gestiona la barrera de entrada diferenciando el tipo de avión.
Java

package aeronpcd.model.concurrent;

public class GestorPrioridad {
    
    private int avionesOperando = 0;
    private int vipEsperando = 0;

    public synchronized void solicitarEntrada(boolean esVip) throws InterruptedException {
        if (esVip) {
            vipEsperando++;
        }
        
        while (avionesOperando >= 3 || (!esVip && vipEsperando > 0)) {
            wait();
        }
        
        if (esVip) {
            vipEsperando--;
        }
        avionesOperando++;
    }

    public synchronized void salir() {
        avionesOperando--;
        notifyAll();
    }
}

Paso 2: Modificar Avion.java

Añade la variable booleana para saber si es VIP, el gestor por parámetro y rodea toda la operación del hilo (desde que solicita aterrizaje hasta que libera la pista de despegue) con el monitor.
Java

private GestorPrioridad gestorPrioridad;
private boolean esVip;

public Avion(int id, TorreDeControl torre, JTextArea logArea, DefaultTableModel modeloTabla, GestorPrioridad gestorPrioridad, boolean esVip) {
    this.idAvion = id;
    this.torre = torre;
    this.estado = EstadoAvion.EN_AIRE;
    this.logVentanaAviones = logArea;
    this.modeloTablaVuelos = modeloTabla;
    this.random = new Random();
    this.gestorPrioridad = gestorPrioridad;
    this.esVip = esVip;
}

@Override
public void run() {
    try {
        gestorPrioridad.solicitarEntrada(this.esVip);
        try {
            this.tiempoInicioOperacion = System.currentTimeMillis();
            
            actualizarEstado(EstadoAvion.SOLICITANDO_ATERRIZAJE);
            RecursosAsignados recursos = torre.solicitarAterrizaje(this); 
            if (recursos == null) return; 

            actualizarEstado(EstadoAvion.ATERRIZANDO);
            Thread.sleep(1000 + random.nextInt(500)); 

            torre.liberarPista(this, recursos.getIdPista());
            int idPuerta = torre.solicitarPuerta(this);
            if (idPuerta == -1) return; 
            
            actualizarEstado(EstadoAvion.EN_PUERTA);
            Thread.sleep(1500 + random.nextInt(500)); 

            torre.liberarPuerta(this, idPuerta); 
            int pistaDespegue = torre.solicitarDespegue(this);
            
            actualizarEstado(EstadoAvion.DESPEGANDO);
            Thread.sleep(1000 + random.nextInt(500)); 
            
            actualizarEstado(EstadoAvion.FIN_CICLO);
            torre.liberarPista(this, pistaDespegue);
            
            long tiempoTotal = System.currentTimeMillis() - tiempoInicioOperacion;
            torre.registrarTiempoFinal(this.idAvion, tiempoTotal);

        } finally {
            gestorPrioridad.salir();
        }

    } catch(InterruptedException e){
        System.out.println("Avion " + idAvion + " interrumpido");
    }
}

Paso 3: Modificar Simulador.java

Instancia el gestor y asigna aleatoriamente si un avión es VIP o no.
Java

GestorPrioridad gestorPrioridad = new GestorPrioridad();
Random rand = new Random();

for (int i = 1; i <= NUM_AVIONES; i++) {
    boolean esVip = rand.nextInt(10) > 7; 
    Avion avion = new Avion(i, torre, areaLogAviones, modeloTabla, gestorPrioridad, esVip);
    new Thread(avion).start();
}

Ejercicio 2: Bloqueo por Mantenimiento de Puertas
Enunciado

Implementa un sistema de limpieza de puertas. Un hilo independiente HiloLimpieza se activará cada 20 segundos para limpiar la terminal, tardando 5 segundos.

    Para iniciar la limpieza, no puede haber ningún avión en la fase de puerta (desembarcando/embarcando).

    Si la limpieza está activa, ningún avión puede acceder a una puerta de embarque, debiendo esperar en la pista tras aterrizar.

Paso 1: Crear GestorLimpieza.java

Este monitor controla exclusivamente el acceso a la zona de puertas.
Java

package aeronpcd.model.concurrent;

public class GestorLimpieza {
    
    private int avionesEnPuerta = 0;
    private boolean limpiezaActiva = false;

    public synchronized void entrarPuerta() throws InterruptedException {
        while (limpiezaActiva) {
            wait();
        }
        avionesEnPuerta++;
    }

    public synchronized void salirPuerta() {
        avionesEnPuerta--;
        if (avionesEnPuerta == 0) {
            notifyAll();
        }
    }

    public synchronized void iniciarLimpieza() throws InterruptedException {
        limpiezaActiva = true;
        while (avionesEnPuerta > 0) {
            wait();
        }
    }

    public synchronized void terminarLimpieza() {
        limpiezaActiva = false;
        notifyAll();
    }
}

Paso 2: Crear HiloLimpieza.java

Este hilo llamará al monitor periódicamente.
Java

package aeronpcd.model.concurrent;

public class HiloLimpieza extends Thread {
    
    private GestorLimpieza gestor;

    public HiloLimpieza(GestorLimpieza gestor) {
        this.gestor = gestor;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                Thread.sleep(20000);
                
                gestor.iniciarLimpieza();
                Thread.sleep(5000);
                gestor.terminarLimpieza();
            }
        } catch (InterruptedException e) {
            interrupt();
        }
    }
}

Paso 3: Modificar Avion.java

El gestor solo debe rodear la fase donde el avión está en la puerta.
Java

private GestorLimpieza gestorLimpieza;

public Avion(int id, TorreDeControl torre, JTextArea logArea, DefaultTableModel modeloTabla, GestorLimpieza gestorLimpieza) {
    // ... inicialización
    this.gestorLimpieza = gestorLimpieza;
}

@Override
public void run() {
    try {
        // ... Aterrizaje previo
        
        int idPuerta = torre.solicitarPuerta(this);
        if (idPuerta == -1) return; 
        
        actualizarEstado(EstadoAvion.EN_PUERTA);
        
        gestorLimpieza.entrarPuerta();
        try {
            Thread.sleep(1500 + random.nextInt(500)); 
        } finally {
            gestorLimpieza.salirPuerta();
        }

        torre.liberarPuerta(this, idPuerta); 
        
        // ... Despegue posterior

Ejercicio 3: Suministro de Combustible Compartido
Enunciado

El aeropuerto cuenta con una reserva compartida de 5000 litros de combustible. Cada avión necesita una cantidad aleatoria (entre 1000 y 2000 litros) para despegar.

    Si no hay combustible suficiente en el depósito, el avión debe quedarse bloqueado en la puerta esperando.

    Un HiloCamion rellena la reserva en bloques de 3000 litros cada 10 segundos.

Paso 1: Crear GestorCombustible.java

Este monitor evalúa la cantidad del parámetro del hilo contra el recurso global.
Java

package aeronpcd.model.concurrent;

public class GestorCombustible {
    
    private int reservaTotal = 5000;

    public synchronized void consumirCombustible(int cantidad) throws InterruptedException {
        while (reservaTotal < cantidad) {
            wait();
        }
        reservaTotal -= cantidad;
    }

    public synchronized void rellenarReserva(int cantidad) {
        reservaTotal += cantidad;
        notifyAll();
    }
}

Paso 2: Crear HiloCamion.java
Java

package aeronpcd.model.concurrent;

public class HiloCamion extends Thread {
    
    private GestorCombustible gestor;

    public HiloCamion(GestorCombustible gestor) {
        this.gestor = gestor;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                Thread.sleep(10000);
                gestor.rellenarReserva(3000);
            }
        } catch (InterruptedException e) {
            interrupt();
        }
    }
}

Paso 3: Modificar Avion.java

El avión debe calcular su combustible y pedirlo antes de despegar. Aquí no hace falta finally porque no hay un método de salida (el combustible se consume y punto).
Java

private GestorCombustible gestorCombustible;

public Avion(int id, TorreDeControl torre, JTextArea logArea, DefaultTableModel modeloTabla, GestorCombustible gestorCombustible) {
    // ... inicialización
    this.gestorCombustible = gestorCombustible;
}

@Override
public void run() {
    try {
        // ... Aterrizaje y Puerta previos
        
        torre.liberarPuerta(this, idPuerta); 
        int pistaDespegue = torre.solicitarDespegue(this);
        
        int combustibleNecesario = 1000 + random.nextInt(1001);
        gestorCombustible.consumirCombustible(combustibleNecesario);
        
        actualizarEstado(EstadoAvion.DESPEGANDO);
        Thread.sleep(1000 + random.nextInt(500)); 
        
        actualizarEstado(EstadoAvion.FIN_CICLO);
        torre.liberarPista(this, pistaDespegue);
        
        // ... Fin

    }
}