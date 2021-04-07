package io.spring.sample.dashboard.stats;

import io.spring.sample.dashboard.stats.support.ReverseLookupDescriptor;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.RestTemplate;

@Component
public class ReverseLookupClient {

	private final RestTemplate client;

	public ReverseLookupClient(RestTemplateBuilder builder) {
		this.client = builder.build();
	}

	public ReverseLookupDescriptor freeReverseLookup(String ip) {
		try {
			return this.client.getForObject("http://localhost:8081/reverse-lookup/free/{ip}", ReverseLookupDescriptor.class, ip);
		}
		catch (Forbidden ex) {
			if (getRateLimitRemaining(ex.getResponseHeaders()) == 0) {
				throw new RateLimitException();
			}
			throw ex;
		}
	}

	public ReverseLookupDescriptor payingReverseLookup(String ip) {
		return this.client.getForObject("http://localhost:8081/reverse-lookup/costly/{ip}", ReverseLookupDescriptor.class, ip);
	}

	private int getRateLimitRemaining(HttpHeaders headers) {
		String remaining = (headers != null) ? headers.getFirst("X-RateLimit-Remaining") : null;
		return (StringUtils.hasText(remaining)) ? Integer.parseInt(remaining) : -1;
	}
}
