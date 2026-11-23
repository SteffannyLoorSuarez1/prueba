import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServicioTurnos extends Remote {

    Respuesta turno(String token, int ventanilla, int lamport) throws RemoteException;
    Respuesta llamar(String token, int ventanilla, int lamport) throws RemoteException;

    Respuesta estado(String token, int ventanilla, int lamport) throws RemoteException;

    Respuesta latido(String token, int ventanilla, int lamport) throws RemoteException;
}