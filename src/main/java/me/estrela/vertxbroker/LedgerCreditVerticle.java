package me.estrela.vertxbroker;

import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

public class LedgerCreditVerticle extends AbstractVerticle {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public static final String LEDGER_CREDIT = "ledger-credit";

    private final LedgerCreditHandler ledgerCreditHandler;

    @Inject
    public LedgerCreditVerticle(LedgerCreditHandler ledgerCreditHandler) {
        this.ledgerCreditHandler = ledgerCreditHandler;
    }

    @Override
    public String deploymentID() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public void start() {
        vertx.eventBus().consumer(LEDGER_CREDIT, ledgerCreditHandler);
        logger.atInfo().log("started verticle id=%s", deploymentID());
    }

    public static class LedgerCreditHandler implements Handler<Message<JsonObject>> {

        private final WebClient webClient;

        @Inject
        public LedgerCreditHandler(WebClient webClient) {
            this.webClient = webClient;
        }

        @Override
        public void handle(Message<JsonObject> event) {
            var body = event.body();
            logger.atInfo().log("communicating position %s", body);
            webClient.post("webhook.site", "/9f671f8e-f764-4644-8561-185b1cf9dc32")
                    .sendJson(body)
                    .onSuccess(response -> logger.atInfo().log("position %s sent", body))
                    .onFailure(err -> logger.atWarning().log("position %s was not sent", body));
        }

    }

}
