package com.chat.server;

import com.chat.rmi.ChatService;
import com.chat.rmi.ChatServiceImpl;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

public class ChatServer {
    private static final Logger LOGGER = Logger.getLogger(ChatServer.class.getName());

    public static void main(String[] args) {
        try {
            ChatService chatService = new ChatServiceImpl();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ChatService", chatService);
            LOGGER.info("Chat Server is running...");
        } catch (RemoteException e) {
            LOGGER.severe("Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
