package org.test

import java.util.concurrent.CountDownLatch

@Log
@Configuration
@EnableRabbit
class RabbitExample implements CommandLineRunner {

	private CountDownLatch latch = new CountDownLatch(1)

	@Autowired
	RabbitTemplate rabbitTemplate

	void run(String... args) {
		log.info "Sending RabbitMQ message..."
		rabbitTemplate.convertAndSend("spring-boot", "Greetings from Spring Boot via RabbitMQ")
		latch.await()
	}

	@RabbitListener(queues = 'spring-boot')
	def receive(String message) {
		log.info "Received ${message}"
		latch.countDown()
	}

	@Bean
	Queue queue() {
		new Queue("spring-boot", false)
	}

	@Bean
	TopicExchange exchange() {
		new TopicExchange("spring-boot-exchange")
	}

	/**
	 * The queue and topic exchange cannot be inlined inside this method and have
	 * dynamic creation with Spring AMQP work properly.
	 */
	@Bean
	Binding binding(Queue queue, TopicExchange exchange) {
		BindingBuilder
				.bind(queue)
				.to(exchange)
				.with("spring-boot")
	}

}