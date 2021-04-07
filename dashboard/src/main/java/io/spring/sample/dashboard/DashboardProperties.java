package io.spring.sample.dashboard;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dashboard")
public class DashboardProperties {

	private final Reverselookup reverselookup = new Reverselookup();

	public Reverselookup getReverselookup() {
		return reverselookup;
	}

	public static class Reverselookup {

		/**
		 * Read timeout for the IP resolver API.
		 */
		private Duration timeout = Duration.ofMillis(500);

		public Duration getTimeout() {
			return timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}
	}

}
