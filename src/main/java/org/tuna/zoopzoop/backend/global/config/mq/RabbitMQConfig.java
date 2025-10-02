package org.tuna.zoopzoop.backend.global.config.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    private static final String EXCHANGE_NAME = "zoopzoop.exchange";
    private static final String QUEUE_NAME = "graph.update.queue";
    private static final String ROUTING_KEY = "graph.update.#";

    private static final String DLQ_EXCHANGE_NAME = EXCHANGE_NAME + ".dlx";
    private static final String DLQ_QUEUE_NAME = QUEUE_NAME + ".dlq";
    private static final String DLQ_ROUTING_KEY = "graph.update.dlq";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue queue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE_NAME) // 실패 시 메시지를 보낼 Exchange
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY) // 실패 시 사용할 라우팅 키
                .build();
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    // ================= DLQ 인프라 구성 추가 ================= //

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLQ_EXCHANGE_NAME);
    }

    @Bean
    public Queue dlqQueue() {
        return new Queue(DLQ_QUEUE_NAME);
    }

    @Bean
    public Binding dlqBinding(Queue dlqQueue, TopicExchange dlqExchange) {
        return BindingBuilder.bind(dlqQueue).to(dlqExchange).with(DLQ_ROUTING_KEY);
    }

    // ================= DLQ 인프라 구성 추가 ================= //
    @Bean
    public MessageConverter messageConverter() {
        // 메시지를 JSON 형식으로 직렬화/역직렬화하는 컨버터
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
