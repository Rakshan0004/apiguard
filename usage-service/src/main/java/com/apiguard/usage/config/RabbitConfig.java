package com.apiguard.usage.config;

import com.apiguard.common.constant.RabbitConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange usageExchange() {
        return new TopicExchange(RabbitConstants.USAGE_EXCHANGE);
    }

    @Bean
    public Queue usageQueue() {
        return QueueBuilder.durable(RabbitConstants.USAGE_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitConstants.DLX)
                .build();
    }

    @Bean
    public Binding usageBinding(Queue usageQueue, TopicExchange usageExchange) {
        return BindingBuilder.bind(usageQueue).to(usageExchange).with(RabbitConstants.USAGE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
