package com.example.fireapp.notifications;

public class Token {
    //FCM token or registrati0n token id servers send this to app to let it receive messages
    String token;


    public Token() {
    }

    public Token(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
