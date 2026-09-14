package com.hestia.vault.dto;

public class AuthDto {

    public static class LoginRequest {
        private String usernameOrEmail;
        private String password;

        public String getUsernameOrEmail() { return usernameOrEmail; }
        public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class SetPasswordRequest {
        private Object userId;
        private String email;
        private String username;
        private String newPassword;

        public Object getUserId() { return userId; }
        public void setUserId(Object userId) { this.userId = userId; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class GoogleAuthRequest {
        private String idToken;
        private String email;
        private String name;
        private String username;

        public String getIdToken() { return idToken; }
        public void setIdToken(String idToken) { this.idToken = idToken; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    public static class UpdateUsernameRequest {
        private Object userId;
        private String email;
        private String newUsername;

        public Object getUserId() { return userId; }
        public void setUserId(Object userId) { this.userId = userId; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getNewUsername() { return newUsername; }
        public void setNewUsername(String newUsername) { this.newUsername = newUsername; }
    }
}
