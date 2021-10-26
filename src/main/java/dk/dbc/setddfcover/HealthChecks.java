package dk.dbc.setddfcover;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import javax.ejb.Singleton;
import javax.enterprise.inject.Produces;

@Singleton
public class HealthChecks {

    @Produces
    @Readiness
    public HealthCheck databaseLookup() {
        return () -> HealthCheckResponse.named("database-lookup")
                .state(true)
                .build();
    }
}

