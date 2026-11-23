
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public class OrdenTotal {

    public static void main(String[] args) {

        List<String> eventos = new ArrayList<>();

        try
        {
            Path carpeta = Path.of("logs");

            Files.createDirectories(carpeta);

            String[] archivos = {
                    "coordinador.log",
                    "ventanilla1.log",
                    "ventanilla2.log",
                    "ventanilla3.log"
            };

            for (String nombre : archivos)
            {
                Path archivo = carpeta.resolve(nombre);

                if (Files.exists(archivo)) {
                    eventos.addAll(Files.readAllLines(archivo));
                }
            }

            eventos.removeIf(linea -> {
                String[] partes = linea.split(";", 4);

                if (partes.length < 4) {
                    return true;
                }

                try
                {
                    Integer.parseInt(partes[0]);
                    Integer.parseInt(partes[1]);
                    return false;
                } catch (NumberFormatException e)
                {
                    return true;
                }
            });

            eventos.sort((a, b) -> {

                String[] x = a.split(";", 4);
                String[] y = b.split(";", 4);

                int relojX = Integer.parseInt(x[0]);
                int relojY = Integer.parseInt(y[0]);

                int comparacion =
                        Integer.compare(relojX, relojY);

                if (comparacion != 0) {
                    return comparacion;
                }

                int idX = Integer.parseInt(x[1]);
                int idY = Integer.parseInt(y[1]);

                return Integer.compare(idX, idY);
            });

            Path salida = carpeta.resolve("orden_total.log");

            Files.write(salida, eventos,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("El orden total fue generado correctamente.");

            System.out.println("Archivo: " + salida.toAbsolutePath());

            System.out.println("Eventos ordenados: " + eventos.size());

        } catch (IOException e)
        { System.err.println("Error: " + e.getMessage());}
    }
}
