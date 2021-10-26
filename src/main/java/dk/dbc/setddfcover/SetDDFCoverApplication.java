package dk.dbc.setddfcover;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 * <p>
 * Note: application path must not be "/" if webapp/index.html is to be loaded.
 * </p>
 */
@ApplicationPath("/api")
public class SetDDFCoverApplication extends Application {

    private static final Set<Class<?>> CLASSES = new HashSet<>(asList(
            SetDDFCoverResource.class
    ));

    @Override
    public Set<Class<?>> getClasses() {
        return CLASSES;
    }

}
