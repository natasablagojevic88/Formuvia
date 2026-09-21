package rs.formuvia.database.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import rs.formuvia.utils.DatabaseListen;

@Retention(RUNTIME)
@Target(FIELD)
public @interface ComboboxList {
	
	DatabaseListen value();

}
