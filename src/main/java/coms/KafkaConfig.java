package coms;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class KafkaConfig {

	@Value(value = "${spring.kafka.bootstrap-servers}")
	private String bootstrapAddress;

//	@Autowired
//	private KafkaProperties kafkaProperties;

	@Autowired
	private ObjectMapper objectMapper;

	@Bean
	public KafkaAdmin kafkaAdmin() {
		Map<String, Object> configs = new HashMap<>();
		configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		return new KafkaAdmin(configs);
	}

	@Bean
	public ConsumerFactory<Object, Object> consumerFactory() {
		Map<String, Object> configs = new HashMap<>();
		configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);

		configs.put("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");  
		configs.put("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer");

		//var configs = kafkaProperties.buildConsumerProperties();
		//configs.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, "120000");
		return new DefaultKafkaConsumerFactory<>(configs);
	}

	@Bean(name = "largeOrderContainerFactory")
	public ConcurrentKafkaListenerContainerFactory<Object, Object> largeOrderContainerFactory(
			ConcurrentKafkaListenerContainerFactoryConfigurer configurer) {
		var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
		configurer.configure(factory, consumerFactory());
		factory.setRecordFilterStrategy(new RecordFilterStrategy<Object, Object>() {

			@Override
			public boolean filter(ConsumerRecord<Object, Object> consumerRecord) {
				try {
					//System.out.println("Inside largeOrderContainerFactory(): "+consumerRecord.value().toString());
					Order order = objectMapper.readValue(consumerRecord.value().toString(), Order.class);
					return order.getValue() < 10000;
				} catch (JsonProcessingException e) {
					e.printStackTrace();
					System.out.println("Error in largeOrderContainerFactory(): "+e.getMessage());
					return false;
				}
			}
		});
		return factory;
	}

	@Bean
	public NewTopic testTopic() {
		return new NewTopic("test", 1, (short) 1);
	}
}
