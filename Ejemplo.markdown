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

Ejercicio 4: Vehículo Remolcador Único (Recurso Exclusivo Físico)

Enunciado
El aeropuerto ha instaurado una norma de reducción de ruido. Los aviones ya no pueden ir desde la pista hasta la puerta de embarque por sus propios medios; necesitan ser remolcados.

    Solo existe un (1) vehículo remolcador en todo el aeropuerto.

    El avión debe solicitar el remolcador justo después de liberar la pista de aterrizaje y antes de solicitar la puerta.

    La maniobra de remolque dura 2 segundos, tras los cuales el avión libera el remolcador y continúa su ejecución normal.

Paso 1: Crear GestorRemolque.java
Este monitor actúa casi como un semáforo binario (Mutex), garantizando que solo un hilo tenga el recurso a la vez.
Java

package aeronpcd.model.concurrent;

public class GestorRemolque {
    
    private boolean remolqueLibre = true;

    public synchronized void engancharRemolque() throws InterruptedException {
        while (!remolqueLibre) {
            wait();
        }
        remolqueLibre = false;
    }

    public synchronized void soltarRemolque() {
        remolqueLibre = true;
        notifyAll();
    }
}

Paso 2: Modificar Avion.java
Se añade el gestor al constructor. El cambio clave ocurre en la transición entre la pista y la puerta.
Java

torre.liberarPista(this, recursos.getIdPista());
actualizarEstado(EstadoAvion.EN_PISTA);

gestorRemolque.engancharRemolque();
try {
    Thread.sleep(2000); 
} finally {
    gestorRemolque.soltarRemolque();
}

int idPuerta = torre.solicitarPuerta(this);

Ejercicio 5: Separación de Tráfico (Exclusión Mutua de Grupos)

Enunciado
Para evitar accidentes, se ha decretado que los aviones de Pasajeros y los aviones de Carga no pueden mezclarse en la calle de rodaje principal.

    La calle de rodaje soporta hasta 4 aviones simultáneos.

    Si hay aviones de Pasajeros dentro, pueden entrar más aviones de Pasajeros (hasta 4), pero los de Carga deben esperar.

    Si la calle se vacía, el primer avión que entre (sea del tipo que sea) bloqueará la calle para el grupo contrario.

Paso 1: Crear GestorRodaje.java
Java

package aeronpcd.model.concurrent;

public class GestorRodaje {
    
    private int avionesDentro = 0;
    private Boolean esCallePasajeros = null;

    public synchronized void entrarCalle(boolean esPasajero) throws InterruptedException {
        while (avionesDentro >= 4 || (esCallePasajeros != null && esCallePasajeros != esPasajero)) {
            wait();
        }
        
        if (avionesDentro == 0) {
            esCallePasajeros = esPasajero;
        }
        avionesDentro++;
    }

    public synchronized void salirCalle() {
        avionesDentro--;
        if (avionesDentro == 0) {
            esCallePasajeros = null;
        }
        notifyAll();
    }
}

Paso 2: Modificar Avion.java y Simulador.java
El avión recibe un booleano esPasajero. Llama a entrarCalle(this.esPasajero) antes de cualquier movimiento terrestre y a salirCalle() al terminar. En el simulador, decides aleatoriamente si el avión instanciado es de carga o pasajeros.
Ejercicio 6: Bloqueo Asimétrico por Tormenta

Enunciado
Se debe implementar un sistema de alerta de tormentas. Un HiloTormenta alternará el estado climátológico.

    Si hay tormenta, los despegues quedan totalmente prohibidos. Los aviones que quieran despegar se bloquean.

    Sin embargo, si hay tormenta, los aterrizajes tienen prioridad absoluta. Los aviones en el aire pueden (y deben) seguir aterrizando sin restricciones para ponerse a salvo.

Paso 1: Crear GestorClimaAsimetrico.java
Este monitor solo frena a los aviones que están en una fase concreta, dejando vía libre al resto.
Java

package aeronpcd.model.concurrent;

public class GestorClimaAsimetrico {
    
    private boolean hayTormenta = false;

    public synchronized void solicitarPermisoDespegue() throws InterruptedException {
        while (hayTormenta) {
            wait();
        }
    }

    public synchronized void cambiarClima(boolean tormenta) {
        this.hayTormenta = tormenta;
        notifyAll();
    }
}

Paso 2: Modificar Avion.java
En este caso no hay un método de salida (salir()) del monitor. Funciona simplemente como una barrera de peaje (checkpoint) que detiene al hilo justo antes de despegar si la bandera está levantada.
Java

torre.liberarPuerta(this, idPuerta); 

gestorClima.solicitarPermisoDespegue();

int pistaDespegue = torre.solicitarDespegue(this);
actualizarEstado(EstadoAvion.DESPEGANDO);

