package dk.dbc.setddfcover;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * To obtain an {@link EntityManager} for the Set DDF Cover database simply say
 * <pre>
 * {@literal @}Inject {@literal @}SetDDFCoverEntityManager EntityManager em
 * </pre>
 */
@SuppressWarnings("PMD.UnusedPrivateField")
public class EntityManagerProducer {
    @Produces
    @SetDDFCoverEntityManager
    @PersistenceContext(unitName = "pg_set_ddf_cover_PU")
    private EntityManager entityManager;
}
