package org.manu.wcpregnant;

import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.cloud.stream.poller.fixed-delay=1")
@ImportAutoConfiguration(exclude = {
        KafkaAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class })
@DirtiesContext
class WcpregnantApplicationTest {

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;


    void whenSendingLogMessage_thenMessageIsEnrichedWithPrefix() {
        input.send(MessageBuilder.withPayload("hello world").build(), "enrichLogMessage-in-0");

        Message<byte[]> message = output.receive(1000L, "enrichLogMessage-out-0");

        Assertions.assertThat(message.getPayload()).asString().isEqualTo("[Baeldung] - hello world");
    }
}