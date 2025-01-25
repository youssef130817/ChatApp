package com.chat.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ClientCallback extends Remote {
    void receiveMessage(String sender, String encryptedMessage) throws RemoteException;
    void updateUserList(List<String> users) throws RemoteException;
}
