package io.segment;

import java.net.URI;

//import org.ehcache.expiry.Duration;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cache.CacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import net.sf.ehcache.config.CacheConfiguration;

@Configuration
@EnableCaching
public class ApplicationConfiguration extends CachingConfigurerSupport {
	
	public final static String REDIS_CACHE="redisCache";
	protected final static String MEMORY_STORE_EVICTION_POLICY="LRU";
	
	private ApplicationProperties properties;	    

	public ApplicationProperties getProperties()
	{
		return this.properties;
	}
	
	@Autowired
	public ApplicationConfiguration(ApplicationProperties properties)
	{
		this.properties = properties;
	}
	
	@Bean
	JedisConnectionFactory jedisConnectionFactory() {
	
		// Get the URI for the remote Redis service
		URI redisURI = properties.getHost();
		
		// Create a factory that will connect to the Redis server		
		RedisStandaloneConfiguration redisStandaloneConfiguration = 
				new RedisStandaloneConfiguration(redisURI.getHost(),redisURI.getPort());
	    
		// If a password is specified, use it
		if( redisURI.getUserInfo() != null)
		{ redisStandaloneConfiguration.setPassword(RedisPassword.of(redisURI.getUserInfo())); }
		
		// Create the factory
	    return new JedisConnectionFactory(redisStandaloneConfiguration);
	}
	
	@Bean
	public StringRedisTemplate stringRedisTemplate() {				
		StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(jedisConnectionFactory());
		stringRedisTemplate.setEnableTransactionSupport(true);
		return stringRedisTemplate;
	}
	
	@Bean
	public net.sf.ehcache.CacheManager ehCacheManager() {
		
		// Create the configuration for the redis cache
		CacheConfiguration cache = new CacheConfiguration();
		cache.setName(REDIS_CACHE);
		cache.setMemoryStoreEvictionPolicy(MEMORY_STORE_EVICTION_POLICY);
		cache.setMaxEntriesLocalHeap(this.getProperties().getProxy().getCache().getCapacity());
		cache.setTimeToLiveSeconds(this.getProperties().getProxy().getCache().getExpiryPeriod().getSeconds());		

		// Create the configuration containing all the caches (in our case one).
		net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
		config.addCache(cache);
		
		// Create the ehcache cache manager
		return net.sf.ehcache.CacheManager.newInstance(config);
	}
	
	@Bean
	@Override
	public CacheManager cacheManager() {
		return new EhCacheCacheManager(ehCacheManager());
	}
}