package io.spring.sample.dashboard.stats;

import java.time.Duration;
import java.util.List;

import io.spring.sample.dashboard.DashboardProperties;
import io.spring.sample.dashboard.stats.support.Event;
import io.spring.sample.dashboard.stats.support.GenerationStatistics;
import io.spring.sample.dashboard.stats.support.GeneratorClient;
import io.spring.sample.dashboard.stats.support.ReverseLookupDescriptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;

@Service
public class StatsService {

	private final StatsClient statsClient;

	private final ReverseLookupClient lookupClient;

	private final Duration timeout;

	public StatsService(StatsClient statsClient, ReverseLookupClient lookupClient,
			DashboardProperties properties) {
		this.statsClient = statsClient;
		this.lookupClient = lookupClient;
		this.timeout = properties.getReverselookup().getTimeout();
	}

	public Mono<StatsContainer> fetchStats(String fromDate, String toDate) {
		Mono<GenerationStatistics> generationStats = statsClient.fetchGenerationStats(fromDate, toDate);
		Mono<List<Event>> events = statsClient.fetchEvents(fromDate, toDate).collectList();
		Mono<List<ReverseLookupDescriptor>> topClients = fetchTopClients(fromDate, toDate).collectList();
		return Mono.zip(generationStats, events, topClients).map(tuple -> {
			StatsContainer.Builder builder = StatsContainer.range(fromDate, toDate);
			builder.addSeries(tuple.getT1().getRecords());
			builder.addAnnotations(tuple.getT2());
			builder.addTopClients(tuple.getT3());
			return builder.build();
		});
	}

	private Flux<ReverseLookupDescriptor> fetchTopClients(String fromDate, String toDate) {
		return statsClient.fetchGeneratorClients(fromDate, toDate)
				.flatMap(client -> lookupClient.freeReverseLookup(client.getIp())
						.onErrorResume(RateLimitException.class, (ex) -> fallbackReverseLookup(client))
						.timeout(this.timeout, fallbackReverseLookup(client))
				);
	}

	private Mono<ReverseLookupDescriptor> fallbackReverseLookup(GeneratorClient client) {
		return this.lookupClient.payingReverseLookup(client.getIp());
	}

}
