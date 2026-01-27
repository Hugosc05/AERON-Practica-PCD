Manual de Usuario y Memoria Técnica: Simulador Aeropuerto AERON

1. Introducción

Este proyecto consiste en una simulación completa del tráfico aéreo del aeropuerto AERON. El objetivo principal ha sido poner en práctica los conceptos de programación concurrente para gestionar recursos limitados (pistas y puertas) frente a una alta demanda (20 aviones intentando operar a la vez).

He diseñado el sistema para que sea visual y fácil de entender, alejándome de la típica ejecución en consola, para que podamos ver realmente cómo los aviones compiten por los recursos en tiempo real.

2. Cómo ponerlo en marcha

El simulador está escrito en Java estándar, por lo que no necesitas librerías externas extrañas. Aquí te explico cómo ejecutarlo:

Compilación y Ejecución

Simplemente compila los archivos .java y ejecuta la clase principal Simulador.
Bash

javac *.java
java Simulador

Cambiar entre Modo Secuencial y Concurrente

El enunciado nos pedía dos modos de funcionamiento. Para facilitar el cambio entre ellos sin tener que borrar código, he incluido un "interruptor" en la parte superior de la clase Simulador.java:

Java

// Ponlo en 'true' para ver los aviones uno a uno (Secuencial)
// Ponlo en 'false' para ver el caos ordenado (Concurrente)
private static final boolean MODO_SECUENCIAL = true;

Solo tienes que cambiar esa variable y volver a ejecutar para ver la diferencia radical en el comportamiento del aeropuerto.

O simplemente, ejecutalo en cualquiera de las dos carpetas, cada una esta con el mismo codigo y el interruptor cambiado para mas simplicidad inclusive.

3. ¿Cómo lo he construido? (Arquitectura)

Para resolver este problema, he planteado una arquitectura basada en el modelo Productor-Consumidor y el uso de Monitores.

    Los Aviones (Avion): Son entidades independientes (Hilos/Threads). Cada avión tiene "vida propia": nace, pide aterrizar, espera, desembarca y vuelve a despegar. No saben qué hacen los demás, solo saben que necesitan recursos.

    El Cerebro (TorreDeControl): Aquí es donde ocurre la magia. He implementado la Torre como un Monitor. Es la encargada de garantizar la exclusión mutua.

        Usa synchronized, wait() y notifyAll() para pausar a los aviones cuando no hay pistas libres y despertarlos cuando algo se libera.

        Solución a Interbloqueos (Deadlocks): Para evitar que un avión se quede con una pista bloqueando a otros mientras espera una puerta , he programado la torre para que solo asigne recursos si hay Pista Y Puerta disponibles a la vez. Si no, el avión espera fuera.

4. Interfaz Gráfica: Lo que vas a ver

Al arrancar el programa, verás una ventana dividida en tres paneles para tener control total de la situación:

    Eventos de Aviones (Izquierda): Aquí verás "la voz" de los aviones. Aparecerán mensajes como "Avión 5 solicitando pista" o "Avión 8 ocupando puerta". Es útil para ver qué intenta hacer cada hilo.

    Torre de Control (Centro): Esta es la parte crítica. Verás cómo la torre asigna recursos: "Avión 5 asignado a Pista 1 y Puerta 3". También verás el recuento de recursos libres (ej. P: 2, G: 4). Si ves que los recursos llegan a 0, notarás que los mensajes se pausan hasta que alguien libera algo.

Panel de Vuelos (Derecha): Una tabla en tiempo real. En lugar de leer logs rápidos, aquí puedes ver de un vistazo el estado de los 20 aviones: si están ATERRIZANDO, EMBARCANDO o ESPERANDO.

5. Interpretación de la Simulación

En Modo Secuencial

Verás que la simulación es muy ordenada y lenta. Un avión completa todo su ciclo (aterriza, espera, despega) y desaparece antes de que el siguiente empiece. Aquí no hay concurrencia real, pero sirve para validar que la lógica del ciclo de vida del avión es correcta.

En Modo Concurrente

Aquí empieza la acción. Verás que los logs se disparan.

    Observarás cómo varios aviones pueden estar aterrizando o despegando a la vez (hasta ocupar las 3 pistas).

    Verás colas de espera: si las 5 puertas están llenas, los aviones en el aire esperarán pacientemente (estado wait) hasta que la torre les dé paso.

    Lo más importante: Nunca verás errores de consistencia (como dos aviones en la misma pista).

6. Resultados Finales (CSV)

Cuando el último de los 20 aviones termina su operación y "se va" del sistema, el simulador detecta el fin de la jornada. Automáticamente, se generará un archivo llamado reporte_aeron.csv en la carpeta del proyecto.

Este archivo contiene el tiempo exacto (en milisegundos) que tardó cada avión y un ranking de quién fue el más rápido, tal y como se pedía en la evaluación.

He disfrutado realizando esta práctica porque ver visualmente cómo los hilos se bloquean y desbloquean ayuda mucho a entender la teoría de monitores y semáforos.