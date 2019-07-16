package io.segment;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="redis")
public class ApplicationProperties {

	private URI host;
	private Proxy proxy = new Proxy();
	
	public static class Proxy
	{
		private URI host = null;
		private Cache cache = new Cache();
		private Resp resp = new Resp();
		
		public static class Resp
		{
			private int port;

			public int getPort() {
				return port;
			}

			public void setPort(int port) {
				this.port = port;
			}		
		}
		
		public static class Cache
		{
			private Duration expiryPeriod;
			private int capacity;
			
			public Duration getExpiryPeriod() {
				return expiryPeriod;
			}

			public void setExpiryPeriod(Duration expiryPeriod) {
				this.expiryPeriod = expiryPeriod;
			}
			
			public int getCapacity() {
				return capacity;
			}
			
			public void setCapacity(int capacity) {
				this.capacity = capacity;
			}
						
		}

		public URI getHost() {
			return host;
		}

		public void setHost(URI host) {
			this.host = host;
		}

		public Cache getCache() {
			return cache;
		}

		public void setCache(Cache cache) {
			this.cache = cache;
		}

		public Resp getResp() {
			return resp;
		}

		public void setResp(Resp resp) {
			this.resp = resp;
		}		
	}

	public URI getHost() {
		return host;
	}

	public void setHost(URI host) {
		this.host = host;
	}

	public Proxy getProxy() {
		return proxy;
	}

	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}
}
