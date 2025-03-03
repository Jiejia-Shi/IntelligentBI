package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.yupi.springbootinit.constant.BIConstant;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class BIMqInitMain {
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // create exchange
        String exchangeName = BIConstant.BI_EXCHANGE_NAME;
        channel.exchangeDeclare(exchangeName, "direct");

        // create queue
        String queueName = BIConstant.BI_QUEUE_NAME;
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, exchangeName, BIConstant.BI_ROUTING_KEY);
    }
}
