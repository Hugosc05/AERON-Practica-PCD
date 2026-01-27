🛡️ Guía de Defensa y Modificaciones Rápidas - Proyecto AERON

Este documento recopila los posibles cambios que el profesor puede pedir durante la defensa y cómo implementarlos en tiempo real sin romper el código.

⚠️ NOTA: Ten este archivo abierto durante la defensa. Todos los cambios son reversibles (Ctrl+Z).

🟢 NIVEL 1: Cambios de Configuración (Básicos)

1. "Aumenta el número de aviones o reduce las pistas"

Objetivo: Ver si el sistema colapsa o si se gestiona dinámicamente.
Archivo: ModoConcurrente/Simulador.java (o ModoSecuencial/Simulador.java).
Líneas: Al principio de la clase (Constantes).

// ANTES
private static final int NUM_AVIONES = 20;
private static final int NUM_PISTAS = 3;

// MODIFICACIÓN (Ej: Poner 50 aviones y solo 1 pista para estresar el sistema)
private static final int NUM_AVIONES = 50; 
private static final int NUM_PISTAS = 1; 


Explicación: "El sistema está parametrizado. Al cambiar estas constantes, la Torre inicializa los semáforos con menos permisos, lo que provocará que más hilos (aviones) se queden bloqueados en estado ESPERANDO_PISTA."

2. "Haz que la simulación vaya más rápida o más lenta"

Objetivo: Ver si controlas el ciclo de vida del hilo.
Archivo: ModoConcurrente/Avion.java
Método: run()

Busca los Thread.sleep(...).

// ANTES (Tarda entre 1 y 1.5 segundos)
Thread.sleep(1000 + random.nextInt(500)); 

// MODIFICACIÓN (Hacerlo muy lento - 5 segundos)
Thread.sleep(5000); 

// MODIFICACIÓN (Hacerlo instantáneo - Modo "Turbo")
Thread.sleep(50);


Explicación: "El Thread.sleep simula el tiempo que tarda la operación real (aterrizar, desembarcar). Al reducirlo, aumentamos la contención porque los aviones liberan y piden recursos mucho más rápido."

🟡 NIVEL 2: Cambios de Lógica Específica

3. "Haz que el Avión número 5 tarde mucho más que los demás"

Objetivo: Ver si sabes identificar hilos específicos dentro de la lógica concurrente.
Archivo: ModoConcurrente/Avion.java
Método: run() (Sección de aterrizaje o despegue).

// ANTES
Thread.sleep(1000 + random.nextInt(500));

// MODIFICACIÓN (Condición específica por ID)
if (this.idAvion == 5) {
    Thread.sleep(10000); // El avión 5 tarda 10 segundos
    logEvento("Tengo problemas mecánicos, tardaré más...");
} else {
    Thread.sleep(1000 + random.nextInt(500));
}


Explicación: "Introducimos una condición basada en el ID del hilo para simular una incidencia técnica específica en una aeronave sin afectar al resto."

4. "Cambia el mensaje que sale en el Log cuando despegan"

Objetivo: Comprobar si sabes dónde se generan los mensajes de la interfaz.
Archivo: ModoConcurrente/Avion.java o TorreDeControl.java (según dónde esté el log).
Método: run() o solicitarDespegue().

// ANTES
logEvento("Despegando por Pista " + pistaDespegue + "...");

// MODIFICACIÓN
logEvento("¡VUELO " + this.idAvion + " RUMBO A LAS BAHAMAS! Pista " + pistaDespegue);


🟠 NIVEL 3: Gestión de Recursos (Semáforos)

5. "Haz que los aviones no respeten el orden de llegada (Quitar FIFO)"

Objetivo: Pregunta sobre Semáforos y justicia (fairness).
Archivo: ModoConcurrente/TorreDeControl.java
Constructor: TorreDeControl(...)

// ANTES (Fairness = true, estricto orden de llegada)
this.semaforoPistas = new Semaphore(numPistas, true);

// MODIFICACIÓN (Fairness = false, el que pille el recurso se lo queda)
this.semaforoPistas = new Semaphore(numPistas, false);


Explicación: "Al poner el booleano a false, el semáforo no garantiza el orden FIFO. Si un avión llega y justo se libera una pista, podría 'colarse' delante de otro que lleva esperando más tiempo."

6. "Haz que un avión se impaciente y se vaya si no hay pista" (Balking)

Objetivo: Ver si conoces tryAcquire().
Archivo: ModoConcurrente/TorreDeControl.java
Método: solicitarAterrizaje

// ANTES (Bloqueante)
semaforoPistas.acquire(); 

// MODIFICACIÓN (No bloqueante / Intento)
if (semaforoPistas.tryAcquire()) {
    // Código normal de asignar pista...
    int pistaAsignada = ocuparRecurso(pistas);
    // ...
    return new RecursosAsignados(pistaAsignada + 1, -1);
} else {
    // Si no hay hueco, no espero. Retorno null.
    logTorre("Avión " + avion.getIdAvion() + " SE DESVÍA por tráfico.");
    return null; 
}


Nota: Si haces esto, el profesor verá que el avión desaparece del log o da error en Avion.java si no controlas el null. Lo ideal es decir: "Cambiaría el acquire por tryAcquire, y si devuelve false, el avión aborta la operación."

🔵 NIVEL 4: Reportes y Datos

7. "Añade una columna nueva al CSV (ej: Tipo de Avión)"

Objetivo: Ver si controlas la generación de ficheros.
Archivo: ModoConcurrente/TorreDeControl.java
Método: generarReporteCSV()

// ANTES
pw.println("Avión " + k + "," + v);

// MODIFICACIÓN
pw.println("Avión " + k + ", BOEING-747, " + v); 
// (Añadimos un dato fijo o variable en la cadena de texto)


🟣 NIVEL 5: Prioridad VIP (Aviones Preferentes)

8. "Haz que el Avión 1 sea VIP y tenga prioridad sobre los demás"

Objetivo: Demostrar control sobre la planificación de hilos (Concurrente) y el flujo de ejecución (Secuencial).

A) En Modo CONCURRENTE (Prioridad de Hilo):
Archivo: ModoConcurrente/Simulador.java
Línea: Dentro del bucle for, donde se crea y lanza el hilo.

// ANTES
new Thread(avion).start();

// MODIFICACIÓN
Thread t = new Thread(avion);
if (i == 1) { // Si es el Avión 1
    t.setPriority(Thread.MAX_PRIORITY); // Le damos prioridad máxima (10)
    System.out.println("¡Avión 1 es VIP!");
} else {
    t.setPriority(Thread.MIN_PRIORITY); // Los demás prioridad mínima (1)
}
t.start();


Explicación: "Usamos setPriority para sugerir al Planificador de Hilos de Java que asigne más tiempo de CPU al avión VIP, intentando que se ejecute y consiga recursos antes que los demás."

B) En Modo SECUENCIAL (Orden de Ejecución):
Archivo: ModoSecuencial/Simulador.java
Línea: Método ejecutarModoSecuencial.

// ANTES (Un solo bucle para todos)
for (int i = 1; i <= 20; i++) { ... }

// MODIFICACIÓN (Ejecutar VIP explícitamente primero)
// 1. Ejecutar VIP (Avión 1)
Avion avionVip = new Avion(1, torre, areaLogAviones, modeloTabla);
avionVip.run();

// 2. Ejecutar el resto (Empezamos en 2)
for (int i = 2; i <= 20; i++) {
    Avion avion = new Avion(i, torre, areaLogAviones, modeloTabla);
    avion.run();
}


Explicación: "En secuencial no hay hilos compitiendo. La 'prioridad' se implementa alterando el orden determinista de ejecución: ejecutamos el VIP antes de entrar en el bucle del resto."

🛠️ Solución de Errores en Directo

Si al hacer un cambio algo se rompe (ej. NullPointerException):

Mira la consola: Te dirá la línea exacta en rojo.

Causa probable: Seguramente cambiaste algo en la Torre (ej. devolver null) pero no lo controlaste en el Avion.java.

Deshacer: Ctrl + Z es tu mejor amigo. No intentes arreglar un bug complejo en directo. Deshaz el cambio y explica la lógica teórica.

📝 Chuleta de Comandos (Terminal)

Si el IDE falla y tienes que usar consola:

Compilar todo:

javac ModoConcurrente/*.java ModoSecuencial/*.java aeronpcd/util/*.java aeronpcd/model/concurrent/*.java


Ejecutar Concurrente:

java ModoConcurrente.Simulador
