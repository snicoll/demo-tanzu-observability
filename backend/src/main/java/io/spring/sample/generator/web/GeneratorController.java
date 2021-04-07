package io.spring.sample.generator.web;

import java.time.LocalDate;
import java.util.List;

import io.github.bucket4j.Bucket;
import io.spring.sample.generator.DateRange;
import io.spring.sample.generator.Event;
import io.spring.sample.generator.GenerationStatistics;
import io.spring.sample.generator.Generator;
import io.spring.sample.generator.GeneratorClient;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeneratorController {

	private final Generator generator;

	public GeneratorController(Generator generator) {
		this.generator = generator;
	}

	@GetMapping("/statistics/{from}/{to}")
	public GenerationStatistics statistics(@PathVariable LocalDate from,
			@PathVariable LocalDate to) {
		DateRange range = new DateRange(from, to);
		return this.generator.generateStatistics(range);
	}

	@GetMapping("/events/{from}/{to}")
	public List<Event> events(@PathVariable LocalDate from,
			@PathVariable LocalDate to) {
		DateRange range = new DateRange(from, to);
		return this.generator.getEvents(range);
	}

	@GetMapping("/top-ips/{from}/{to}")
	public List<GeneratorClient> topIps(@PathVariable LocalDate from,
			@PathVariable LocalDate to) {
		DateRange range = new DateRange(from, to);
		return this.generator.getTopIps(range);
	}

	@GetMapping("/reverse-lookup/costly/{ip}")
	public ReverseLookupDescriptor costlyReverseLookup(@PathVariable String ip) {
		return reverseLookup(ip);
	}

	@GetMapping("/reverse-lookup/free/{ip}")
	public ResponseEntity<ReverseLookupDescriptor> freeReverseLookup(
			@RequestAttribute("RateLimiterBucket") Bucket bucket,
			@PathVariable String ip) throws InterruptedException {
		if (bucket.tryConsume(1)) {
			Thread.sleep(this.generator.getLatency().randomLatency().toMillis());
			return ResponseEntity.ok()
					.header("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()))
					.body(reverseLookup(ip));
		}
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.header("X-RateLimit-Remaining", "0")
				.build();
	}

	private ReverseLookupDescriptor reverseLookup(String ip) {
		return new ReverseLookupDescriptor(ip, ip + ".example.com");
	}

}
