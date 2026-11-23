import java.io.FileInputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PruebaCarga {

    public static void main(String[] args) {

        ExecutorService ejecutor = Executors.newFixedThreadPool(30);

        try {
            Properties config = new Properties();

            try (FileInputStream archivo = new FileInputStream("config.properties"))
            {
                config.load(archivo);
            }

            String host = config.getProperty("host", "localhost");

            int puerto = Integer.parseInt(config.getProperty("puerto", "1099"));

            String token = config.getProperty("token");

            Registry registro = LocateRegistry.getRegistry(host, puerto);

            ServicioTurnos servicio = (ServicioTurnos) registro.lookup("TurnoJusto");

            AtomicInteger asignadas = new AtomicInteger();
            AtomicInteger rechazadas = new AtomicInteger();

            Set<Integer> numeros = ConcurrentHashMap.newKeySet();

            CountDownLatch inicio = new CountDownLatch(1);
            CountDownLatch fin = new CountDownLatch(30);

            for (int i = 0; i < 30; i++) {

                final int ventanilla = (i / 10) + 1;

                ejecutor.submit(() -> {

                    try {
                        inicio.await();

                        Respuesta respuesta =
                                servicio.turno(
                                        token, ventanilla, 1);

                        if (respuesta.resultado
                                .equals("Asignado")) {

                            asignadas.incrementAndGet();
                            numeros.add(respuesta.numero);

                        } else {
                            rechazadas.incrementAndGet();
                        }

                    } catch (Exception e) {
                        System.err.println("Solicitud fallida: " + e.getMessage());
                    } finally {
                        fin.countDown();
                    }
                });
            }

            System.out.println("Iniciando las 30 solicitudes");
            inicio.countDown();
            fin.await();

            Respuesta estado = servicio.estado(token, 1, 1);

            List<Integer> ordenados = new ArrayList<>(numeros);

            Collections.sort(ordenados);

            System.out.println();
            System.out.println("Resultados de la prueba:");
            System.out.println("Rechazadas " + rechazadas.get());
            System.out.println("Asignadas: " + asignadas.get());
            System.out.println("Numeros " + ordenados);
            System.out.println("Numeros distintos: " + numeros.size());
            System.out.println("Emiidos coordinador" + estado.emitidos);
            System.out.println("Cupo: "+ estado.cupo);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ejecutor.shutdownNow();
        }
    }
}
