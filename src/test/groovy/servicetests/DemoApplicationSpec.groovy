package servicetests

import com.example.kafkaFourthProducer.DemoApplication
import com.example.kafkaFourthProducer.model.Address
import com.example.kafkaFourthProducer.model.Player
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.IntegerDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.platform.commons.util.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Specification

import static org.springframework.boot.test.util.TestPropertyValues.of

@Testcontainers
@ContextConfiguration(initializers = [Initializer.class])
@SpringBootTest(classes = DemoApplication.class)
class DemoApplicationSpec extends Specification {

    public static KafkaContainer kafka

    @Autowired
    ObjectMapper objectMapper

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        def postgreSQLContainer = new PostgreSQLContainer("postgres:13.1")
                .withDatabaseName("int-test-db")
                .withUsername("sa")
                .withPassword("sa")

        void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            postgreSQLContainer.start()
            of(
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword(),
                    "spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers()
            )
                    .applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    def setupSpec(){
        kafka = new KafkaContainer("5.3.0")
        kafka.start()
    }


    def 'Order status message is produced and is consumed by the order status consumer' () {
        given: 'a kafka template'
        def configs = new HashMap<>(KafkaTestUtils.producerProps(kafka.bootstrapServers))
        def factory = new DefaultKafkaProducerFactory<String, String>(configs, new StringSerializer(), new StringSerializer())
        def template = new KafkaTemplate<String, String>(factory, true)
        def consumer = configureConsumer("earliest")

        and: 'an order status to be sent to order status consumer'
        def testPlayer = new Player('richard', 'krasowski', 01234567, new Address('Goethestraße 11', 'Dresden', 1139))

        and: 'order status kafka producer'
        ProducerRecord<String, String> record = new ProducerRecord<>('players', objectMapper.writeValueAsString(testPlayer))

        when: 'order status message is produced on the topic'
        template.send(record)

        then: 'consumer is able to read the message'
        ConsumerRecord<String, Player> singleRecord = KafkaTestUtils.getOneRecord(kafka.bootstrapServers,'meineGruppe', "players",0,false,true,10000)
        StringUtils.isNotBlank(singleRecord.value() as String)
        Thread.sleep(100)


        and:
        def playersTopicPartition = new TopicPartition('players', 0)
        consumer.endOffsets([playersTopicPartition]) == Map<TopicPartition, Long>.of(playersTopicPartition,1L)
        KafkaTestUtils.getCurrentOffset(kafka.bootstrapServers, "meineGruppe", 'players', 0).offset() == 1L
    }

    def Consumer<String, String> configureConsumer(String offsetReset){
        def configs = meineConfigurationFuerKafka(offsetReset)
        def factory = new DefaultKafkaConsumerFactory(configs, new StringDeserializer(), new StringDeserializer())
        def consumer = factory.createConsumer()
        consumer.subscribe(['players'])
        consumer
    }

    def Map<String, Object> meineConfigurationFuerKafka(String offsetReset){
        Map<String, Object> props = new HashMap();
        props.put("bootstrap.servers", kafka.bootstrapServers);
        props.put("group.id", "meineGruppe");
        props.put("session.timeout.ms", "60000");
        props.put("key.deserializer", StringDeserializer.class);
        props.put("value.deserializer", StringDeserializer.class);
        props

    }
}