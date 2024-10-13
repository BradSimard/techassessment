package tech.simard.thinkon.accessors;

import tech.simard.thinkon.models.User;

import java.lang.reflect.InvocationTargetException;

public class UserAccessor extends ModelAccessor {
    public UserAccessor() throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        super(User.class);
    }

    // If you wanted other custom queries for this class, this is where you'd put them
    // We're just using generic ones though, so none are defined here
}
