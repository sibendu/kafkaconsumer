package coms;

import org.apache.kafka.common.errors.SerializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/orders")
public class OrderController {

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@GetMapping("/message")
	public String find() {
		return "Hello order";
	}

	@GetMapping("/generate")
	@Operation(summary = "Post a message to topic")
	public String sendMessages() {
		System.out.println("Inside ProcessController.sendMessages()");
		String jsonString = null;
		for (int i = 1; i < 7; i++) {
			Order order = new Order();
			order.setOrderNum("O20011" + i);
			order.setDelay(new Long(5000));
			order.setErrorType(i == 3 ? "RETRY" : "");
			order.setValue(i == 4? new Long(20000): new Long(1000));

			jsonString = new Gson().toJson(order);
			System.out.println("Generating order: "+jsonString);
			this.sendMessage(jsonString);
		}
		return "success";
	}

	@PostMapping("/send")
	@Operation(summary = "Post a message to topic")
	public String send(@RequestBody Order order) {
		System.out.println("Inside ProcessController.send()");
		try {
			String jsonString = new Gson().toJson(order);
			this.sendMessage(jsonString);
			return "success";
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	@RetryableTopic(attempts = "3", topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE, backoff = @Backoff(delay = 2000, multiplier = 3.0), exclude = {
			SerializationException.class, DeserializationException.class }

	)
	@KafkaListener(topics = "order", groupId = "order-processor-group-1", concurrency = "2")
	public void listen(String message) throws Exception {

		System.out.println("Received message : " + message);
		Order order = new Gson().fromJson(message, Order.class);
		// var order = objectMapper.readValue(message, Order.class);

		Thread.sleep(order.getDelay());

		if (order.getErrorType() != null && order.getErrorType().equalsIgnoreCase("RETRY")) {
			System.out.println("Order "+order.getOrderNum()+" have error and should be retried");
			throw new RuntimeException("Test exception");
		} else {
			System.out.println("Processed Order : " + order.getOrderNum());
		}
	}

	@DltHandler
	public void handleDlt(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		// Order order = new Gson().fromJson(message, Order.class);
		System.out.println("Message handled by dlq topic: " + topic + " :::: " + message);
	}

	@KafkaListener(topics = "order", groupId = "order-processor-group-2", containerFactory = "largeOrderContainerFactory")
	public void processLargeOrder(String message) throws Exception {
		//System.out.println("processLargeOrder(): " + message);
		Order order = new Gson().fromJson(message, Order.class);
		System.out.println("Processed large order: "+order.getOrderNum());
	}

	public void sendMessage(String msg) {
		kafkaTemplate.send("order", msg);
	}
}
