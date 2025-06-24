package com.trackops.server.adapters.output.events.orders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackops.server.domain.events.orders.*;
import com.trackops.server.ports.output.events.orders.OrderEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j

@Service
public class KafkaOrderProducer {


}

