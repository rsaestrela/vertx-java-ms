package me.estrela.vertxbroker;

import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import me.estrela.vertxbroker.generated.tables.daos.StockDao;
import me.estrela.vertxbroker.generated.tables.pojos.Stock;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static java.util.Objects.isNull;
import static me.estrela.vertxbroker.LedgerCreditVerticle.LEDGER_CREDIT;

public class StockVerticle extends AbstractVerticle {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private static final String SERVER_PORT = "server.port";

    private final BuyStockHandler buyStockHandler;

    @Inject
    public StockVerticle(BuyStockHandler buyStockHandler) {
        this.buyStockHandler = buyStockHandler;
    }

    @Override
    public String deploymentID() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public void start() {
        var router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().failureHandler(context -> {
            if (context.statusCode() == 500) {
                logger.atWarning().log("failed handling request");
            }
            context.response().setStatusCode(context.statusCode()).end();
        });
        router.post("/buy").handler(buyStockHandler);
        vertx.createHttpServer().requestHandler(router).listen(config().getInteger(SERVER_PORT));
        logger.atInfo().log("started verticle id=%s", deploymentID());
    }

    public static class BuyStockHandler implements Handler<RoutingContext> {

        private final Vertx vertx;
        private final StockMapper stockMapper;
        private final StockDao stockDao;

        @Inject
        public BuyStockHandler(Vertx vertx,
                               StockMapper stockMapper,
                               StockDao stockDao) {
            this.vertx = vertx;
            this.stockMapper = stockMapper;
            this.stockDao = stockDao;
        }

        @Override
        public void handle(RoutingContext context) {
            var buyStockRequest = BuyStockRequest.fromJson(context.getBodyAsJson());
            if (buyStockRequest == null || !buyStockRequest.isValid()) {
                context.fail(HttpResponseStatus.BAD_REQUEST.code());
                logger.atWarning().log("bad request %s", buyStockRequest);
                return;
            }

            var stock = stockMapper.fromBuyStockRequest(buyStockRequest);
            var response = context.response();
            stockDao.insert(stock)
                    .onComplete(event -> {
                        response.setStatusCode(HttpResponseStatus.CREATED.code())
                                .end(JsonObject.mapFrom(stock).toBuffer());
                        vertx.eventBus().send(LEDGER_CREDIT, JsonObject.mapFrom(stock));
                    })
                    .onFailure(throwable -> logger.atWarning().log(throwable.getMessage()));
        }

    }

    public static class BuyStockRequest {

        public String userId;
        public String market;
        public BigDecimal price;
        public Double quantity;

        public static BuyStockRequest fromJson(JsonObject jsonObject) {
            if (isNull(jsonObject)) {
                throw new IllegalArgumentException("JsonObject cannot be null");
            }
            try {
                return jsonObject.mapTo(BuyStockRequest.class);
            } catch (IllegalArgumentException ignore) {
                return null;
            }
        }

        boolean isValid() {
            return StringUtils.isNoneBlank(userId) &&
                    ObjectUtils.allNotNull(market, price, quantity) &&
                    price.compareTo(BigDecimal.ZERO) > 0 &&
                    quantity > 0;
        }

    }

    public static class StockMapper {

        public Stock fromBuyStockRequest(BuyStockRequest buyStockRequest) {
            Stock stock = new Stock();
            stock.setId(UUID.randomUUID().toString());
            stock.setUserId(buyStockRequest.userId);
            stock.setMarket(buyStockRequest.market);
            stock.setPrice(buyStockRequest.price.doubleValue());
            stock.setQuantity(buyStockRequest.quantity);
            return stock;
        }

    }

}
