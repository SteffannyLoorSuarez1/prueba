
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public class Coordinador extends UnicastRemoteObject implements ServicioTurnos {

    private final int cupo;
    private final String tokenCorrecto;
    private final boolean inseguro;

    private int emitidos = 0;
    private int siguiente = 1;
    private int lamport = 0;

    private final Queue<Integer> pendientes = new ArrayDeque<>();
    private final Map<Integer, Long> latidos = new HashMap<>();
    private final Map<Integer, Boolean> activos = new HashMap<>();

    public Coordinador(int cupo, String token, boolean inseguro) throws RemoteException
    {
        super();
        this.cupo = cupo;
        this.tokenCorrecto = token;
        this.inseguro = inseguro;

        for (int i = 1; i <= 3; i++) {
            activos.put(i, false);
        }

        Thread monitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    verificarLatidos();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        monitor.setDaemon(true);
        monitor.start();
    }

    private synchronized void recibir(int recibido)
    {
        lamport = Math.max(lamport, recibido) + 1;
    }

    private synchronized int enviar()
    {
        return ++lamport;
    }

    private synchronized void registrar(String evento, String detalle)
    {

        int tiempo = ++lamport;

        try {
            Files.createDirectories(Path.of("logs"));

            String linea = tiempo + ";0;" + evento
                    + ";" + detalle + System.lineSeparator();

            Files.writeString(
                    Path.of("logs/coordinador.log"), linea, StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private boolean validar(String token) {
        if (!tokenCorrecto.equals(token)) {
            registrar("Rechazada", "Token invalido");
            return false;
        }
        return true;
    }

    private synchronized Respuesta responder(String resultado, String motivo, int numero, String detalle)
    {
        registrar(resultado, detalle);

        return new Respuesta(resultado, motivo, numero, emitidos, cupo, enviar(),detalle);
    }

    @Override
    public Respuesta turno(String token, int ventanilla, int reloj) throws RemoteException {
        if (inseguro) {
            return turnoInseguro(token, ventanilla, reloj);
        }
        synchronized (this) {
            recibir(reloj);

            if (!validar(token)) {
                return responder("Rechazada",
                        "El token es invàlido", 0, "");
            }

            if (emitidos >= cupo) {
                return responder("Rechazada",
                        "El cupo està agotado", 0, "");
            }

            int numero = ++emitidos;
            pendientes.add(numero);

            return responder("Asignado", "",
                    numero, "V" + ventanilla);
        }
    }

    private Respuesta turnoInseguro(
            String token, int ventanilla, int reloj) {

        synchronized (this) {
            recibir(reloj);

            if (!validar(token)) {
                return responder("Rechazado",
                        "El token es invalido", 0, "");
            }
        }

        int numero = emitidos + 1;

        if (numero > cupo) {
            synchronized (this) {
                return responder("Rechzado",
                        "El cupo està agotado", 0, "");
            }
        }

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        emitidos = numero;

        synchronized (this) {
            pendientes.add(numero);
            return responder("Asignado", "",
                    numero, "Modo inseguro V" + ventanilla);
        }
    }

    @Override
    public synchronized Respuesta llamar(String token, int ventanilla, int reloj) throws RemoteException
    {
        recibir(reloj);

        if (!validar(token)) {
            return responder("Rechazada",
                    "Token invalido", 0, "");
        }

        if (pendientes.isEmpty()) {
            return responder("Rechazada",
                    "No hay turnos en espera", 0, "");
        }

        int numero = pendientes.poll();

        return responder("LLamado", "",
                numero, "Atendido por V" + ventanilla);
    }

    @Override
    public synchronized Respuesta estado(
            String token, int ventanilla, int reloj)
            throws RemoteException {

        recibir(reloj);

        if (!validar(token)) {
            return responder("Rechazada",
                    "El token es invalido", 0, "");
        }

        StringBuilder detalle = new StringBuilder();

        detalle.append("Pendientes: ")
                .append(pendientes);

        for (int i = 1; i <= 3; i++) {
            detalle.append(" | V").append(i)
                    .append(": ")
                    .append(activos.get(i)
                            ? "Activa" : "Caida");
        }

        return responder("Estado", "",
                0, detalle.toString());
    }

    @Override
    public synchronized Respuesta latido(String token, int ventanilla, int reloj) throws RemoteException
    {
        recibir(reloj);
        if (!validar(token)) {
            return responder("Rechazado",
                    "El token es invàlido", 0, "");
        }
        latidos.put(ventanilla,
                System.currentTimeMillis());

        if (!activos.getOrDefault(ventanilla, false))
        {
            activos.put(ventanilla, true);
            registrar("Activa", "V" + ventanilla);
        }

        return responder("Latido", "",
                0, "V" + ventanilla);
    }

    private synchronized void verificarLatidos() {
        long ahora = System.currentTimeMillis();

        for (int i = 1; i <= 3; i++) {
            Long ultimo = latidos.get(i);

            if (ultimo != null
                    && ahora - ultimo >= 6000
                    && activos.getOrDefault(i, false)) {

                activos.put(i, false);
                registrar("Caida", "V" + i);
            }
        }
    }
}
