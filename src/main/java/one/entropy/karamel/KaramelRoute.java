package one.entropy.karamel;

import io.vertx.reactivex.core.eventbus.EventBus;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KaramelRoute extends RouteBuilder {

    protected static final String CONSUMER_ROUTE_ID = "consumerRoute";

    @Inject
    EventBus bus;

    @Override
    public void configure() throws Exception {
        from("kafka:.*?topicIsPattern=true&autoOffsetReset=earliest")
                .routeId(CONSUMER_ROUTE_ID)
                .process(this::processIncoming);

        from("direct:message")
                .process(this::processOutgoing)
                .toD("kafka:${header.kafka.TOPIC}");
    }

    private void processIncoming(Exchange exchange) {
        KaramelMessage message = new KaramelMessage(
                exchange.getIn().getHeader(KafkaConstants.TOPIC, String.class),
                exchange.getIn().getHeader(KafkaConstants.PARTITION, Long.class),
                exchange.getIn().getHeader(KafkaConstants.OFFSET, Long.class),
                exchange.getIn().getHeader(KafkaConstants.TIMESTAMP, Long.class),
                exchange.getIn().getHeader(KafkaConstants.KEY, String.class),
                exchange.getIn().getBody(String.class)
        );
        bus.publish("kmessage", message);
    }

    private void processOutgoing(Exchange exchange) {
        KaramelMessage message = exchange.getIn().getBody(KaramelMessage.class);
        exchange.getIn().setHeader(KafkaConstants.TOPIC, message.getTopic());
        exchange.getIn().setHeader(KafkaConstants.OFFSET, message.getOffset());
        exchange.getIn().setHeader(KafkaConstants.KEY, message.getKey());
        exchange.getIn().setBody(message.getValue());
    }
}
