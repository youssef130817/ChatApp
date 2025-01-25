package com.chat.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ChatServiceImpl extends UnicastRemoteObject implements ChatService {
    private static final Logger LOGGER = Logger.getLogger(ChatServiceImpl.class.getName());
    private final Map<String, ClientCallback> clients;

    public ChatServiceImpl() throws RemoteException {
        this.clients = new HashMap<>();
    }

    @Override
    public synchronized void registerClient(String username, ClientCallback client) throws RemoteException {
        clients.put(username, client);
        LOGGER.info("New client registered: " + username);
        broadcastUserList();
    }

    @Override
    public synchronized void unregisterClient(String username) throws RemoteException {
        clients.remove(username);
        LOGGER.info("Client unregistered: " + username);
        broadcastUserList();
    }

    @Override
    public synchronized void sendPrivateMessage(String sender, String recipient, String message) throws RemoteException {
        ClientCallback recipientClient = clients.get(recipient);
        if (recipientClient != null) {
            try {
                recipientClient.receiveMessage(sender, message);
                LOGGER.info("Private message sent from " + sender + " to " + recipient);
            } catch (RemoteException e) {
                LOGGER.warning("Failed to send private message to: " + recipient);
                throw e;
            }
        } else {
            LOGGER.warning("Recipient not found: " + recipient);
        }
    }

    @Override
    public List<String> getConnectedUsers() throws RemoteException {
        return new ArrayList<>(clients.keySet());
    }

    private void broadcastUserList() throws RemoteException {
        List<String> userList = getConnectedUsers();
        for (ClientCallback client : clients.values()) {
            try {
                client.updateUserList(userList);
            } catch (RemoteException e) {
                LOGGER.warning("Failed to update user list for a client");
            }
        }
    }
}
