package com.jva.ERP.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Seed credentials loaded from application.properties.
 * Used exclusively by DataSeeder to create the initial ADMIN and MANAGER accounts.
 * Never exposed through any API endpoint.
 */
@Component
@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {

    private UserSeed admin = new UserSeed();
    private UserSeed manager = new UserSeed();

    public UserSeed getAdmin() { return admin; }
    public void setAdmin(UserSeed admin) { this.admin = admin; }

    public UserSeed getManager() { return manager; }
    public void setManager(UserSeed manager) { this.manager = manager; }

    public static class UserSeed {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }
}
