package ru.otus.chat.server;

public interface ChatOperations {
    void disconnectingFromChat(ClientHandler clientHandler, String username);
}
