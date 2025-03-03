package com.yupi.springbootinit.mq;

import com.yupi.springbootinit.constant.BIConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BIMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(BIConstant.BI_EXCHANGE_NAME, BIConstant.BI_ROUTING_KEY, message);
    }

}
