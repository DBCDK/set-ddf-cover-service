package dk.dbc.setddfcover;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

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

