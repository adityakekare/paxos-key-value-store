package service;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for RMI. Extends Remote from Java rmi.
 *
 */
public interface RpcService extends Remote {
    /**
     * Method to get a value from key-value store
     * @param key integer key
     * @return associated string value of key
     * @throws RemoteException if procedure can't be called
     */
    String get(int key) throws RemoteException;

    /**
     * Method to put key-value pair in the key-value store
     * @param key integer key
     * @param value string value
     * @return string success/failure message
     * @throws RemoteException if procedure can't be called
     */
    String put(int key, String value) throws RemoteException;

    /**
     * Method to delete a key from the key-value store
     * @param key integer key
     * @return string success/failure message
     * @throws RemoteException if procedure can't be called
     */
    String delete(int key) throws RemoteException;
}
