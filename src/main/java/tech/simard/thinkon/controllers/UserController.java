package tech.simard.thinkon.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import tech.simard.thinkon.accessors.UserAccessor;
import tech.simard.thinkon.db.DBConnection;
import tech.simard.thinkon.models.DBTable;
import tech.simard.thinkon.models.User;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class UserController {

    @GetMapping("/users")
    public List<DBTable> getUsers() throws SQLException {
        DBConnection db = null;
        try {
            // Establish a connection to the DB
            db = new DBConnection();

            // Get all user records from the DB
            UserAccessor accessor = new UserAccessor();
            List<DBTable> users = accessor.getAll(db);

            // Commit any changes
            db.finish(true);

            return users;
        } catch (SQLException e) {
            db.finish(false);
            return new ArrayList<>();
        } catch (NoSuchFieldException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/users/{id}")
    public User getUserByID(@PathVariable UUID id) throws SQLException {
        DBConnection db = null;
        try {
            // Establish a connection to the DB
            db = new DBConnection();

            // Get a user record by the provided id
            UserAccessor accessor = new UserAccessor();
            DBTable user = accessor.getById(db, id);

            // Commit any changes
            db.finish(true);

            return (User) user;
        } catch (SQLException e) {
            db.finish(false);
            return null;
        } catch (NoSuchFieldException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/users")
    public User createUser(@RequestBody String body) throws SQLException, JsonProcessingException {
        DBConnection db = null;
        try {
            // Convert the body JSON to a User instance
            User tempUser = (new ObjectMapper()).readValue(body, User.class);

            // Establish a connection to the DB
            db = new DBConnection();

            // Create a new user record using the request's JSON
            UserAccessor accessor = new UserAccessor();
            DBTable user = accessor.create(db, tempUser);

            // Commit the new record
            db.finish(true);

            return (User) user;
        } catch (SQLException e) {
            db.finish(false);
            return null;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/users/{id}")
    public User updateUser(@PathVariable UUID id, @RequestBody String body) throws SQLException, JsonProcessingException {
        DBConnection db = null;
        try {
            // Convert the body JSON to a User instance
            User tempUser = (new ObjectMapper()).readValue(body, User.class);

            // Establish a connection to the DB
            db = new DBConnection();

            // Update existing user record using the request's JSON
            UserAccessor accessor = new UserAccessor();
            accessor.update(db, id, tempUser);

            // Retrieve the record so we can send it back to the client
            DBTable user = accessor.getById(db, id);

            // Commit changes to the updated record
            db.finish(true);

            return (User) user;
        } catch (SQLException e) {
            db.finish(false);
            return null;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable UUID id) throws SQLException {
        DBConnection db = null;
        try {
            // Establish a connection to the DB
            db = new DBConnection();

            // Update existing user record using the request's JSON
            UserAccessor accessor = new UserAccessor();
            accessor.delete(db, id);

            // Commit changes to the updated record
            db.finish(true);
        } catch (SQLException e) {
            db.finish(false);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
