package io.spring.sample.dashboard.stats;

import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.MeterRegistry;
import io.spring.sample.dashboard.stats.support.ReverseLookupDescriptor;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ReverseLookupClient {

	private final WebClient client;

	public ReverseLookupClient(WebClient.Builder builder, MeterRegistry registry) {
		this.client = builder.filter(rateLimitRemainingMetric(registry)).build();
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

	private ExchangeFilterFunction rateLimitRemainingMetric(MeterRegistry registry) {
		AtomicInteger rateLimitRemaining = registry
				.gauge("reverselookup.ratelimit.remaining", new AtomicInteger(0));
		return (request, next) -> next.exchange(request)
				.doOnNext(response -> {
					int remaining = getRateLimitRemaining(response.headers().asHttpHeaders());
					if (remaining != -1) {
						rateLimitRemaining.set(remaining);
					}
				});
	}

	private int getRateLimitRemaining(HttpHeaders headers) {
		String remaining = (headers != null) ? headers.getFirst("X-RateLimit-Remaining") : null;
		return (StringUtils.hasText(remaining)) ? Integer.parseInt(remaining) : -1;
	}
}
