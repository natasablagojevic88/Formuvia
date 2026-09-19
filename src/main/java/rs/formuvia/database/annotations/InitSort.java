package rs.formuvia.database.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import rs.formuvia.database.enums.Direction;

@Retention(RUNTIME)
@Target(FIELD)
public @interface InitSort {

	int orderNumber() default 1;
	
	Direction direction() default Direction.ASC;
}
