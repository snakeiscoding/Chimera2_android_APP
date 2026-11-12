package com.jht.rscu

import org.json.JSONObject

class RSCULogin {

    class LoginException extends Exception {
        LoginException(String msg) {
            super(msg)
        }
    }
    private String authToken

    RSCULogin() {
    }

    void login(RSCU rscu, String user, String password) {
        JSONObject loginObject = new JSONObject()
        loginObject.put("email", user)
        loginObject.put("password", password)

        try {
            def loginResponse = rscu.postRSCUAPI("https://am4-api.jfit.co/auth/login", loginObject.toString(), false)
            if(loginResponse == null || loginResponse.id_token == null || loginResponse.id_token.isEmpty()) {
                throw new LoginException("Did not get token from login.")
            }
            authToken = loginResponse.id_token
        }
        catch(RSCU.RSCUException ex) {
            throw new LoginException("Login failed: " + ex.getMessage())
        }
    }

    boolean loggedIn() { return authToken != null && !authToken.isEmpty() }

    String getAuthToken() {
        return authToken
    }



}
