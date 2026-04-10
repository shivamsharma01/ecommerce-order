package com.mcart.order.service;

import tools.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.mcart.order.dto.OrderItemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "order.pubsub.enabled", havingValue = "true", matchIfMissing = false)
public class OrderPaidPublisher {

	private static final String EVENT_TYPE = "ORDER_PAID";

	private final PubSubTemplate pubSubTemplate;
	private final ObjectMapper objectMapper;

	@Value("${order.pubsub.topic:order-paid-events}")
	private String topicName;

	public void publish(
			UUID orderId,
			UUID userId,
			String checkoutReferenceId,
			String paymentId,
			String customerEmail,
			long totalAmountMinor,
			List<OrderItemResponse> items
	) {
		try {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("eventType", EVENT_TYPE);
			body.put("orderId", orderId.toString());
			body.put("userId", userId.toString());
			body.put("checkoutReferenceId", checkoutReferenceId);
			body.put("paymentId", paymentId);
			if (customerEmail != null && !customerEmail.isBlank()) {
				body.put("customerEmail", customerEmail.trim());
			}
			body.put("totalAmount", totalAmountMinor);
			List<Map<String, Object>> lineItems = new ArrayList<>();
			for (OrderItemResponse it : items) {
				Map<String, Object> line = new LinkedHashMap<>();
				line.put("productId", it.productId());
				line.put("quantity", it.quantity());
				line.put("unitPriceMinor", it.unitPrice());
				line.put("lineTotalMinor", it.lineTotal());
				lineItems.add(line);
			}
			body.put("items", lineItems);
			String json = objectMapper.writeValueAsString(body);
			pubSubTemplate.publish(topicName, json);
			log.debug("Published {} for order {}", EVENT_TYPE, orderId);
		} catch (Exception ex) {
			log.error("Failed to publish {} for order {} (email will not be sent for this checkout)", EVENT_TYPE, orderId, ex);
		}
	}
}
