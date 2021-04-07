package io.spring.sample.dashboard.stats;

import io.spring.sample.dashboard.stats.support.ReverseLookupDescriptor;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ReverseLookupClient {

	private final WebClient client;

	public ReverseLookupClient(WebClient.Builder builder) {
		this.client = builder.build();
	}

	public Mono<ReverseLookupDescriptor> freeReverseLookup(String ip) {
		return this.client.get().uri("http://localhost:8081/reverse-lookup/free/{ip}", ip)
				.retrieve().onStatus((status) -> status.equals(HttpStatus.FORBIDDEN), this::parseForbidden)
				.bodyToMono(ReverseLookupDescriptor.class);
	}

	private Mono<? extends Throwable> parseForbidden(ClientResponse response) {
		return response.createException().map((ex) -> {
			if (getRateLimitRemaining(response.headers().asHttpHeaders()) == 0) {
				return new RateLimitException(ex.getRawStatusCode(), ex.getStatusText(),
						ex.getHeaders(), ex.getResponseBodyAsByteArray());
			}
			return ex;
		});
	}

	public Mono<ReverseLookupDescriptor> payingReverseLookup(String ip) {
		return this.client.get().uri("http://localhost:8081/reverse-lookup/costly/{ip}", ip)
				.retrieve().bodyToMono(ReverseLookupDescriptor.class);
	}

	private int getRateLimitRemaining(HttpHeaders headers) {
		String remaining = (headers != null) ? headers.getFirst("X-RateLimit-Remaining") : null;
		return (StringUtils.hasText(remaining)) ? Integer.parseInt(remaining) : -1;
	}
}
