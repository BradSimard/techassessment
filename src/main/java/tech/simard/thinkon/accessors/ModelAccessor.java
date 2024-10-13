package tech.simard.thinkon.accessors;

import tech.simard.thinkon.db.DBConnection;
import tech.simard.thinkon.models.DBTable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModelAccessor {
    protected Class<?> cls;
    protected String tableName;

    public ModelAccessor(Class<?> cls) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.cls = cls;

        // Figure out the table name based on the field
        this.tableName = this.cls.getDeclaredField("TABLE_NAME").get(this.cls.getDeclaredConstructor().newInstance()).toString();
    }

    /**
     * Dynamically create an instance of the accessor's class and populate its fields using the data from the database result.
     * @param dbResults the results provided from DBConnection.query
     * @param meta database metadata (contains column names)
     * @return an instance of the class provided to the accessor
     */
    private DBTable dynamicSet(ResultSet dbResults, ResultSetMetaData meta) {
        DBTable returnObject = null;

        try {
            // Create an instance of our dbtable class so we can populate the field values from the DB
            returnObject = (DBTable) this.cls.getDeclaredConstructor().newInstance();

            // Go through each field in the resulting dataset
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                // Figure out the name of the column and its value from the db
                String columnName = meta.getColumnName(i);
                Object columnValue = dbResults.getObject(columnName);

                // Assign the column's value to the object instance
                // NOTE: If the column names don't match the object field names, it'll throw an exception
                // TODO: It'd probably be better to output a warning if a field isn't found rather than an exception
                Field field = this.cls.getDeclaredField(columnName);
                field.set(returnObject, columnValue);
            }
        } catch (NoSuchFieldException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException | SQLException e) {
            throw new RuntimeException(e);
        }

        return returnObject;
    }

    /**
     * Provided with a UUID for the record desired, return the entire record from the database
     * @param db A DBConnection instance
     * @param id The UUID of the record to be found
     * @return an instance of the class provided to the accessor
     */
    public DBTable getById(DBConnection db, UUID id) {
        try {
            // Build the select by id statement
            PreparedStatement stmt = db.prepareStatement(String.format("SELECT * FROM %s WHERE id = ?", this.tableName));
            stmt.setObject(1, id);

            // Execute the select all statement
            ResultSet dbResults = db.query(stmt);

            // Get the metadata from the database results (we'll use this to dynamically find and assign fields to our object instance)
            ResultSetMetaData meta = dbResults.getMetaData();

            // If there's a result, create the resulting DBTable object and return them
            if (dbResults.next()) {
                return dynamicSet(dbResults, meta);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    /**
     * Find all records for the class of the accessor
     * @param db A DBConnection instance
     * @return instance(s) of the class provided to the accessor
     */
    public List<DBTable> getAll(DBConnection db) {
        List<DBTable> returnObjects = new ArrayList<>();
        try {
            // Build the select all statement
            PreparedStatement stmt = db.prepareStatement(String.format("SELECT * FROM %s", this.tableName));

            // Execute the select all statement
            ResultSet dbResults = db.query(stmt);

            // Get the metadata from the database results (we'll use this to dynamically find and assign fields to our object instance)
            ResultSetMetaData meta = dbResults.getMetaData();

            // Iterate over results, create resulting DBTable objects and return them
            while (dbResults.next()) {
                returnObjects.add(dynamicSet(dbResults, meta));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return returnObjects;
    }

    /**
     * Given a local instance of the class of the accessor, insert the record data into the related database table
     * @param db A DBConnection instance
     * @param incomingData Data to be used to insert a new record into the db
     * @return the newly created record as an instance of the class provided to the accessor
     */
    public DBTable create(DBConnection db, DBTable incomingData) {
        try {
            // Get the name of the table based on the class
            String tableName = this.tableName;

            // Get all the fields and values
            List<String> fieldNames = new ArrayList<>();
            List<Object> fieldValues = new ArrayList<>();
            for (Field field : incomingData.getClass().getDeclaredFields()) {
                Object value = field.get(incomingData);

                // Ignore the id field as we're inserting, the db will give us an id
                if (field.getName().equals("id")) {
                    continue;
                }

                // Check for annotations on the current field
                // We want to filter out anything that doesn't have JsonProperty
                // We're using JsonProperty to mean it's a DB field
                boolean ignoreField = true;
                Annotation[] annotations = field.getAnnotations();
                for (Annotation ant : annotations) {
                    if (ant.annotationType().getSimpleName().equals("JsonProperty")) {
                        ignoreField = false;
                        break;
                    }
                }
                if (ignoreField) {
                    continue;
                }

                // We only want non-null fields to be inserted into the DB
                if (value != null) {
                    fieldNames.add(field.getName());
                    fieldValues.add(value);
                }
            }

            // Can't do an insert if we have no fields!
            if (fieldNames.isEmpty()) {
                return null;
            }

            // Dynamically build the insert query
            StringBuilder queryString = new StringBuilder(String.format("INSERT INTO %s (", tableName));
            for (String fieldName : fieldNames) {
                queryString.append(String.format("%s,", fieldName));
            }
            queryString.setLength(queryString.length() - 1); // trim trailing comma
            queryString.append(") VALUES (");
            for (Object fieldValue : fieldValues) {
                    // Looks a little nasty, but surround strings/uuids with single quotes. Otherwise, should be ok to just output the value
                    queryString.append(fieldValue.getClass().equals(String.class) || fieldValue.getClass().equals(UUID.class) ? String.format("'%s',", fieldValue.toString()) : String.format("%s,", fieldValue.toString()));
            }
            queryString.setLength(queryString.length() - 1); // trim trailing comma
            queryString.append(") RETURNING *");
            PreparedStatement stmt = db.prepareStatement(queryString.toString());

            // Execute the insert statement (we're going to get the newly created record back)
            ResultSet dbResults = db.query(stmt);

            // Get the metadata from the database results (we'll use this to dynamically find and assign fields to our object instance)
            ResultSetMetaData meta = dbResults.getMetaData();

            // If there's a result, create the resulting DBTable object and return them
            if (dbResults.next()) {
                return dynamicSet(dbResults, meta);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Perform an update on an existing record in the database
     * @param db A DBConnection instance
     * @param recordId the UUID of the record that already exists in the database
     * @param incomingData Data to be used to update the chosen record
     */
    public void update(DBConnection db, UUID recordId, DBTable incomingData) {
        try {
            // Get the name of the table based on the class
            String tableName = this.tableName;

            // Get all the fields and values
            List<String> fieldNames = new ArrayList<>();
            List<Object> fieldValues = new ArrayList<>();
            for (Field field : incomingData.getClass().getDeclaredFields()) {
                Object value = field.get(incomingData);

                // Check for annotations on the current field
                // We want to filter out anything that doesn't have JsonProperty
                // We're using JsonProperty to mean it's a DB field
                boolean ignoreField = true;
                Annotation[] annotations = field.getAnnotations();
                for (Annotation ant : annotations) {
                    if (ant.annotationType().getSimpleName().equals("JsonProperty")) {
                        ignoreField = false;
                        break;
                    }
                }
                if (ignoreField) {
                    continue;
                }

                // We only want non-null fields to be inserted into the DB
                if (value != null) {
                    fieldNames.add(field.getName());
                    fieldValues.add(value);
                }
            }

            // Can't do an insert if we have no fields!
            if (fieldNames.isEmpty()) {
                return;
            }

            // Dynamically build the update query
            StringBuilder queryString = new StringBuilder(String.format("UPDATE %s SET", tableName));
            for (int i = 0; i < fieldNames.size(); i++) {
                String fieldName = fieldNames.get(i);
                Object fieldValue = fieldValues.get(i);

                queryString.append(String.format(" %s=", fieldName));

                // Looks a little nasty, but surround strings/uuids with single quotes. Otherwise, should be ok to just output the value
                queryString.append(fieldValue.getClass().equals(String.class) || fieldValue.getClass().equals(UUID.class) ? String.format("'%s',", fieldValue.toString()) : String.format("%s,", fieldValue.toString()));
            }
            queryString.setLength(queryString.length() - 1); // trim trailing comma
            queryString.append(String.format(" WHERE id='%s'", recordId.toString()));
            PreparedStatement stmt = db.prepareStatement(queryString.toString());

            // Execute the update statement
            db.query(stmt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete an existing record in the database
     * @param db A DBConnection instance
     * @param id the UUID of the record to be deleted
     */
    public void delete(DBConnection db, UUID id) {
        try {
            // Build the delete statement
            PreparedStatement stmt = db.prepareStatement(String.format("DELETE FROM %s WHERE id = ?", this.tableName));
            stmt.setObject(1, id);

            // Execute the delete statement
            db.query(stmt);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
