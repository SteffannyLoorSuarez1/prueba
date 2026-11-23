import com.sun.jdi.event.StepEvent;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import java.util.Scanner;

public class Ventanilla {
    private static int id;
    private static int lamport = 0;
    private static String token;
    private static ServicioTurnos servicio;

    private static synchronized int enviar() {
        return ++lamport;
    }

    private static synchronized void recibir(int remoto) {
        lamport = Math.max(lamport, remoto) + 1;
    }

    private static synchronized void registrar(String evento, String detalle) {
        int tiempo = ++lamport;

        try {
            Files.createDirectories(Path.of("logs"));

            String linea = tiempo + ";" + id + ";" + evento + ";" + detalle + System.lineSeparator();

            Files.writeString(
                    Path.of("logs/ventanilla" + id + ".log"),
                    linea,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void procesar(String comando) {

        try {
            Respuesta respuesta;

            registrar("Envio", comando);

            int tiempo = enviar();

            switch (comando) {
                case "turno":
                    respuesta = servicio.turno(token, id, tiempo);
                    break;

                case "llamar":
                    respuesta = servicio.llamar(token, id, tiempo);
                    break;

                case "estado":
                    respuesta = servicio.estado(token, id, tiempo);
                    break;

                default:
                    System.out.println("No se reconoce");
                    return;
            }

            recibir(respuesta.lamport);
            registrar("Recepc", respuesta.resultado);

            System.out.println();
            System.out.println(respuesta);
            System.out.println();

        } catch (Exception e) {
            registrar("Error", "El coordinador no està disponible");

            System.out.println("No se puede contactar al coordinador.");
            System.out.println("No se asignan los turnos sin el coordinador.");
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: Ventanilla 1");
            return;
        }

        try {
            id = Integer.parseInt(args[0]);

            if (id < 1 || id > 3) {
                System.out.println( "La ventanilla debe ser 1, 2 o incluso 3");
                return;
            }

            Properties config = new Properties();

            try (FileInputStream archivo = new FileInputStream("config.properties"))
            {
                config.load(archivo);
            }
            token = config.getProperty("token");

            String host = config.getProperty("host", "localhost");

            int puerto = Integer.parseInt(config.getProperty("puerto", "1099"));
            Registry registro= LocateRegistry.getRegistry(host, puerto);

            servicio = (ServicioTurnos) registro.lookup("TurnoJusto");

            Thread latidos = new Thread(() -> {

                while (!Thread.currentThread().isInterrupted()) {

                    try {
                        registrar("Envio", "Latido");

                        Respuesta respuesta = servicio.latido(
                                token, id, enviar());

                        recibir(respuesta.lamport);

                        registrar("Recepcion", "Latido");

                    } catch (Exception e) {
                        registrar("Error",
                                "Hubo un fallo al registrar el latido");
                    }
                    try
                    {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            latidos.setDaemon(true);
            latidos.start();

            Scanner teclado = new Scanner(System.in);

            System.out.println("Ventanilla V" + id);
            System.out.println("Los comandos: turno, llamar, estado, salir");

            while (true) {

                System.out.print("V" + id + "> ");
                String comando = teclado.nextLine()
                        .trim().toLowerCase();

                if (comando.equals("salir")) {
                    registrar("Salida", "Ventanilla cerrada");
                    break;
                }
                procesar(comando);
            }

            teclado.close();

        } catch (Exception e) {
            System.err.println(
                    "Error: " + e.getMessage());
            e.printStackTrace();
        }


    }

}