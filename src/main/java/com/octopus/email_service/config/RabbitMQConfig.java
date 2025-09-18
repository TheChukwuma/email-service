package com.octopus.email_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Value("${app.email.queue.name:email.queue}")
    private String emailQueueName;
    
    @Value("${app.email.dlq.name:email.dlq}")
    private String emailDlqName;
    
    @Value("${app.email.exchange.name:email.exchange}")
    private String emailExchangeName;
    
    @Value("${app.email.routing.key:email.send}")
    private String emailRoutingKey;
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
    
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        return factory;
    }
    
    // Email Queue
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(emailQueueName)
                .withArgument("x-dead-letter-exchange", emailExchangeName + ".dlx")
                .withArgument("x-dead-letter-routing-key", emailDlqName)
                .withArgument("x-message-ttl", 300000) // 5 minutes TTL
                .build();
    }
    
    // Dead Letter Queue
    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(emailDlqName).build();
    }
    
    // Email Exchange
    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(emailExchangeName);
    }
    
    // Dead Letter Exchange
    @Bean
    public DirectExchange emailDlx() {
        return new DirectExchange(emailExchangeName + ".dlx");
    }
    
    // Bindings
    @Bean
    public Binding emailQueueBinding() {
        return BindingBuilder.bind(emailQueue()).to(emailExchange()).with(emailRoutingKey);
    }
    
    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder.bind(emailDlq()).to(emailDlx()).with(emailDlqName);
    }
}
