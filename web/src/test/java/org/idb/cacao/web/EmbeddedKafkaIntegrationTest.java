package org.idb.cacao.web;

import java.util.concurrent.TimeUnit;

import org.idb.cacao.web.utils.KafkaMockConsumer;
import org.idb.cacao.web.utils.KafkaMockProducer;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class EmbeddedKafkaIntegrationTest {

	@Autowired
	private KafkaMockConsumer consumer;

	@Autowired
	private KafkaMockProducer producer;

	@Value("${test.topic}")
	private String topic;

	@Test
	public void givenEmbeddedKafkaBroker_whenSendingtoSimpleProducer_thenMessageReceived() throws Exception {
		producer.send(topic, "Sending with own simple KafkaProducer");
		consumer.getLatch().await(10000, TimeUnit.MILLISECONDS);

		assertThat(consumer.getLatch().getCount(), equalTo(0L));
		assertThat(consumer.getPayload(), containsString("cacao-test-topic"));
	}
}
