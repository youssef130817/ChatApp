package com.chat.client;

import com.chat.rmi.ChatService;
import com.chat.rmi.ClientCallback;
import com.chat.security.AESEncryption;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.List;
import java.util.Random;

public class ChatClient extends UnicastRemoteObject implements ClientCallback {
    private final JFrame frame;
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final Map<String, ChatPanel> chatPanels;
    private final Map<String, Integer> unreadMessages;
    private final UserListPanel userListPanel;
    private final ChatService chatService;
    private final String username;
    private final AESEncryption encryption;

    public ChatClient(String username) throws Exception {
        this.username = username;
        this.encryption = new AESEncryption();
        this.chatPanels = new HashMap<>();
        this.unreadMessages = new HashMap<>();

        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        chatService = (ChatService) registry.lookup("ChatService");

        FlatLightLaf.setup();
        frame = new JFrame("Chat - " + username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 600);  

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        userListPanel = new UserListPanel();
        mainPanel.add(userListPanel, "USERS");

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        chatService.registerClient(username, this);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    chatService.unregisterClient(username);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void receiveMessage(String sender, String encryptedMessage) throws RemoteException {
        try {
            String decryptedMessage = encryption.decrypt(encryptedMessage);
            SwingUtilities.invokeLater(() -> {
                ChatPanel chatPanel = getChatPanel(sender);
                if (!chatPanel.isVisible()) {
                    unreadMessages.put(sender, unreadMessages.getOrDefault(sender, 0) + 1);
                    userListPanel.updateUnreadCount(sender, unreadMessages.get(sender));
                }
                chatPanel.addMessage(sender, decryptedMessage);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateUserList(List<String> users) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            users.remove(username);
            userListPanel.updateUsers(users);
        });
    }

    private ChatPanel getChatPanel(String otherUser) {
        ChatPanel panel = chatPanels.get(otherUser);
        if (panel == null) {
            panel = new ChatPanel(otherUser);
            chatPanels.put(otherUser, panel);
            mainPanel.add(panel, otherUser);
        }
        return panel;
    }

    private class UserListPanel extends JPanel {
        private final DefaultListModel<UserListItem> listModel;
        private final JList<UserListItem> userList;

        public UserListPanel() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel headerLabel = new JLabel("Conversations", JLabel.CENTER);
            headerLabel.setFont(new Font("Arial", Font.BOLD, 18));
            headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            add(headerLabel, BorderLayout.NORTH);

            listModel = new DefaultListModel<>();
            userList = new JList<>(listModel);
            userList.setCellRenderer(new UserListCellRenderer());
            userList.setFixedCellHeight(50);
            userList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    UserListItem item = userList.getSelectedValue();
                    if (item != null) {
                        // Réinitialiser le compteur de messages non lus
                        unreadMessages.put(item.username, 0);
                        userListPanel.updateUnreadCount(item.username, 0);
                        
                        // Obtenir et afficher le panel de chat
                        ChatPanel panel = getChatPanel(item.username);
                        mainPanel.add(panel, item.username);
                        cardLayout.show(mainPanel, item.username);
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(userList);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            add(scrollPane, BorderLayout.CENTER);
        }

        public void updateUsers(List<String> users) {
            listModel.clear();
            for (String user : users) {
                listModel.addElement(new UserListItem(user, unreadMessages.getOrDefault(user, 0)));
            }
        }

        public void updateUnreadCount(String username, int count) {
            for (int i = 0; i < listModel.size(); i++) {
                UserListItem item = listModel.getElementAt(i);
                if (item.username.equals(username)) {
                    item.unreadCount = count;
                    listModel.set(i, item);
                    break;
                }
            }
        }
    }

    private class ChatPanel extends JPanel {
        private final JTextArea chatArea;
        private final JTextField messageField;
        private final String otherUser;

        public ChatPanel(String otherUser) {
            this.otherUser = otherUser;
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(new Color(240, 240, 240));
            headerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

            JButton backButton = new JButton("←");
            backButton.setFont(new Font("Arial", Font.BOLD, 16));
            backButton.setContentAreaFilled(false);
            backButton.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            backButton.addActionListener(e -> {
                cardLayout.show(mainPanel, "USERS");
            });
            
            JLabel userLabel = new JLabel(otherUser, JLabel.CENTER);
            userLabel.setFont(new Font("Arial", Font.BOLD, 14));
            
            headerPanel.add(backButton, BorderLayout.WEST);
            headerPanel.add(userLabel, BorderLayout.CENTER);
            add(headerPanel, BorderLayout.NORTH);

            chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            chatArea.setFont(new Font("Arial", Font.PLAIN, 13));
            chatArea.setBackground(new Color(250, 250, 250));
            JScrollPane scrollPane = new JScrollPane(chatArea);
            add(scrollPane, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));
            bottomPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
            messageField = new JTextField();
            messageField.setFont(new Font("Arial", Font.PLAIN, 13));
            messageField.addActionListener(e -> sendMessage());
            
            JButton sendButton = new JButton("Send");
            sendButton.setFont(new Font("Arial", Font.BOLD, 14));
            sendButton.setContentAreaFilled(false);
            sendButton.addActionListener(e -> sendMessage());
            
            bottomPanel.add(messageField, BorderLayout.CENTER);
            bottomPanel.add(sendButton, BorderLayout.EAST);
            add(bottomPanel, BorderLayout.SOUTH);
        }

        public void sendMessage() {
            String message = messageField.getText().trim();
            if (!message.isEmpty()) {
                try {
                    String encryptedMessage = encryption.encrypt(message);
                    chatService.sendPrivateMessage(username, otherUser, encryptedMessage);
                    addMessage(username, message);
                    messageField.setText("");
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                        "Erreur lors de l'envoi du message: " + e.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        public void addMessage(String sender, String message) {
            String timestamp = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
            String formattedMessage = String.format("[%s] %s: %s%n",
                timestamp,
                sender.equals(username) ? "Moi" : sender,
                message);
            chatArea.append(formattedMessage);
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

    private static class UserListItem {
        String username;
        int unreadCount;

        public UserListItem(String username, int unreadCount) {
            this.username = username;
            this.unreadCount = unreadCount;
        }

        @Override
        public String toString() {
            return username;
        }
    }

    private static class UserListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            
            UserListItem item = (UserListItem) value;
            
            JLabel nameLabel = new JLabel(item.username);
            nameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            
            if (item.unreadCount > 0) {
                JLabel badge = new JLabel(String.valueOf(item.unreadCount));
                badge.setFont(new Font("Arial", Font.BOLD, 12));
                badge.setForeground(Color.WHITE);
                badge.setBackground(new Color(231, 76, 60)); // Rouge vif
                badge.setOpaque(true);
                badge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                badge.setHorizontalAlignment(JLabel.CENTER);
                panel.add(badge, BorderLayout.EAST);
            }
            
            panel.add(nameLabel, BorderLayout.CENTER);
            
            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
                panel.setForeground(list.getSelectionForeground());
                nameLabel.setForeground(list.getSelectionForeground());
            } else {
                panel.setBackground(list.getBackground());
                panel.setForeground(list.getForeground());
                nameLabel.setForeground(list.getForeground());
            }
            
            return panel;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            String defaultUsername = "User" + new Random().nextInt(1000);
            new ChatClient(defaultUsername);
            System.out.println("Connecté avec le nom d'utilisateur: " + defaultUsername);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, 
                "Erreur lors du démarrage du client: " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
