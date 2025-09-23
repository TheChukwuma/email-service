package com.octopus.email_service.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audited
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    
    /**
     * The action being performed (e.g., "CREATE_TEMPLATE", "DELETE_USER")
     */
    String action();
    
    /**
     * The type of resource being acted upon (e.g., "TEMPLATE", "USER", "API_KEY")
     */
    String resourceType() default "";
    
    /**
     * Description of the action for logging purposes
     */
    String description() default "";
}
