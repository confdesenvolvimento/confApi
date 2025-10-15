package com.confApi.dto;

public class TokenDTO {
    private String login;
    private String token;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public TokenDTO(String login, String token) {
        super();
        this.login = login;
        this.token = token;
    }

    public TokenDTO() {
        // TODO Auto-generated constructor stub
    }
}
