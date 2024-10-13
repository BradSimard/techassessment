package tech.simard.thinkon.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class User extends DBTable {
    @JsonIgnore
    public final String TABLE_NAME = "user";

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("username")
    public String username;

    @JsonProperty("firstname")
    public String firstname;

    @JsonProperty("lastname")
    public String lastname;

    @JsonProperty("email")
    public String email;

    @JsonProperty("phone")
    public String phone;

    public User(UUID id, String username, String firstname, String lastname, String email, String phone) {
        this.id = id;
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phone = phone;
    }

    public User() {}
}
