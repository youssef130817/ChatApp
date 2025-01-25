package com.chat.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatService extends Remote {
    void registerClient(String username, ClientCallback client) throws RemoteException;
    void unregisterClient(String username) throws RemoteException;
    void sendPrivateMessage(String sender, String recipient, String message) throws RemoteException;
    List<String> getConnectedUsers() throws RemoteException;
}
