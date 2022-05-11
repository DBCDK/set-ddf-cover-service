package dk.dbc.setddfcover;

import dk.dbc.commons.exceptionhandling.WebAppExceptionHandler;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.annotation.security.DeclareRoles;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 * <p>
 * Note: application path must not be "/" if webapp/index.html is to be loaded.
 * </p>
 */
@ApplicationPath("/")
@DeclareRoles("authenticated-user")
public class CoverApplication extends Application {

    private static final Set<Class<?>> CLASSES = Set.of(CoverResource.class, JacksonFeature.class, WebAppExceptionHandler.class);

    @Override
    public Set<Class<?>> getClasses() {
        return CLASSES;
    }

}
