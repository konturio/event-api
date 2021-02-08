package io.kontur.eventapi.emdat.dto;

public class EmDatAuthorizationResponse {
    private Login data;

    public String getToken() {
        return data.getLogin();
    }

    public void setData(Login data) {
        this.data = data;
    }

    private static class Login {
        private String login;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }
    }
}
