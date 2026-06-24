package uno.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the JPA {@link EntityManagerFactory} for the {@code uno-pu} persistence
 * unit and hands out {@link EntityManager}s. This is the single place the
 * persistence framework is bootstrapped.
 *
 * <p>{@link #open()} uses the file-based H2 database declared in
 * {@code persistence.xml}; {@link #open(Map)} lets tests override the JDBC url
 * (e.g. an in-memory database) without changing configuration files.
 *
 * <p>Hibernate's logger is held and pinned to {@code WARNING} so its startup
 * banners never clutter the player-facing CLI output.
 */
public final class Database implements AutoCloseable {

    public static final String UNIT = "uno-pu";

    // Strong reference so the level we set is not lost to logger GC.
    private static final Logger HIBERNATE_LOGGER = Logger.getLogger("org.hibernate");

    static {
        System.setProperty("org.jboss.logging.provider", "jdk");
        // Keep Hibernate's framework chatter (e.g. the dev connection-pool notice)
        // off the CLI; genuine errors still surface.
        HIBERNATE_LOGGER.setLevel(Level.SEVERE);
    }

    private final EntityManagerFactory emf;

    private Database(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /** Open the default (file-based H2) database. */
    public static Database open() {
        return new Database(Persistence.createEntityManagerFactory(UNIT));
    }

    /** Open with property overrides (used by tests for an in-memory database). */
    public static Database open(Map<String, Object> overrides) {
        return new Database(Persistence.createEntityManagerFactory(UNIT, overrides));
    }

    public EntityManager em() {
        return emf.createEntityManager();
    }

    public EntityManagerFactory factory() {
        return emf;
    }

    @Override
    public void close() {
        if (emf.isOpen()) {
            emf.close();
        }
    }
}
