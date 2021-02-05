package me.estrela.vertxbroker;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import org.flywaydb.core.Flyway;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

import static io.vertx.pgclient.PgPool.pool;

public class VertxBrokerService {

    private static final String WORKER_POOL_SIZE_KEY = "server.worker.pool.size";
    private static final String WORKER_POOL_NAME = "server.worker.pool.name";
    private static final String DB_URL = "db.url";
    private static final String DB_USER = "db.user";
    private static final String DB_PASSWORD = "db.password";
    private static final String DB_POOL_MAX = "db.pool.max";

    public static void main(String[] args) {
        var vertx = Vertx.vertx();
        getConfigRetriever(vertx).getConfig(json -> {
            JsonObject config = json.result();
            DeploymentOptions options = new DeploymentOptions()
                    .setWorker(true)
                    .setWorkerPoolName(config.getString(WORKER_POOL_NAME))
                    .setWorkerPoolSize(config.getInteger(WORKER_POOL_SIZE_KEY))
                    .setConfig(config);
            var dbConfig = setupDatabase(config);
            var injector = setupDependencies(vertx, config, dbConfig);
            vertx.deployVerticle(injector.getInstance(StockVerticle.class), options);
            vertx.deployVerticle(injector.getInstance(LedgerCreditVerticle.class), options);
        });
    }

    private static ConfigRetriever getConfigRetriever(Vertx vertx) {
        var config = new ConfigStoreOptions()
                .setType("file")
                .setFormat("properties")
                .setConfig(new JsonObject().put("path", "application.properties"));
        return ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(config));
    }

    private static Configuration setupDatabase(JsonObject config) {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl("jdbc:" + config.getString(DB_URL));
        dataSource.setUser(config.getString(DB_USER));
        dataSource.setPassword(config.getString(DB_PASSWORD));
        var defaultConfiguration = new DefaultConfiguration();
        defaultConfiguration.set(new DataSourceConnectionProvider(dataSource));
        defaultConfiguration.set(SQLDialect.POSTGRES);
        defaultConfiguration.set(new Settings().withRenderNameStyle(RenderNameStyle.AS_IS));
        runDbMigrations(dataSource);
        return defaultConfiguration;
    }

    private static void runDbMigrations(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();
    }

    private static Injector setupDependencies(Vertx vertx,
                                              JsonObject config,
                                              Configuration dbConfig) {
        return Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Vertx.class).toInstance(vertx);
                bind(WebClient.class).toInstance(WebClient.create(vertx));
                bind(SqlClient.class).toInstance(getPgPool(vertx, config));
                bind(Configuration.class).toInstance(dbConfig);
                bind(StockVerticle.StockMapper.class);
                bind(StockVerticle.BuyStockHandler.class);
                bind(StockVerticle.class);
                bind(LedgerCreditVerticle.LedgerCreditHandler.class);
                bind(LedgerCreditVerticle.class);
            }
        });
    }

    private static PgPool getPgPool(Vertx vertx, JsonObject config) {
        PgConnectOptions connectOptions = PgConnectOptions.fromUri(config.getString(DB_URL));
        connectOptions.setUser(config.getString(DB_USER));
        connectOptions.setPassword(config.getString(DB_PASSWORD));
        return pool(vertx, connectOptions, new PoolOptions().setMaxSize(config.getInteger(DB_POOL_MAX)));
    }

}
