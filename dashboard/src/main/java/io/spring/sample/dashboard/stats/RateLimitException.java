package io.spring.sample.dashboard.stats;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Thrown when a service hits its rate limit.
 *
 * @author Stephane Nicoll
 */
public final class RateLimitException extends WebClientResponseException {

	public RateLimitException(int statusCode, String statusText, HttpHeaders headers, byte[] body) {
		super(statusCode, statusText, headers, body, null);
	}
}
