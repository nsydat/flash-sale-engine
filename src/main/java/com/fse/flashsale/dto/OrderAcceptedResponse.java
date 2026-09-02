package com.fse.flashsale.dto;

/** Returned immediately; PostgreSQL persistence happens asynchronously through Kafka. */
public record OrderAcceptedResponse(String orderCode) {
}
