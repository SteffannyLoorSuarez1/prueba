
import java.io.Serializable;

public class Respuesta implements Serializable {

    private static final long serialVersionUID = 1L;

    public String resultado;
    public String motivo;
    public int numero;
    public int emitidos;
    public int cupo;
    public int lamport;
    public String detalle;

    public Respuesta(String resultado, String motivo, int numero, int emitidos, int cupo, int lamport, String detalle)
    {
        this.resultado = resultado;
        this.motivo = motivo;
        this.numero = numero;
        this.emitidos = emitidos;
        this.cupo = cupo;
        this.lamport = lamport;
        this.detalle = detalle;
    }

    @Override
    public String toString()
    {
        return "Resulatdo: " + resultado + "El motivo: " + motivo + " Numero: " + numero + " latidos: "
                + emitidos + " / " + cupo + " Lamport: " + lamport + " Detalles: " + detalle;
    }
}
