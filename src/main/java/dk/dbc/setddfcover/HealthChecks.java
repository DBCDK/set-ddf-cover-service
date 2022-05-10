package dk.dbc.setddfcover;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class HealthChecks {

    @Produces
    @Readiness
    public HealthCheck databaseLookup() {
        return () -> HealthCheckResponse.named("database-lookup")
                .status(true)
                .build();
    }
}

