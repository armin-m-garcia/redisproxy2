package io.segment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListenerAdapter;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class RedisSystemTests {
	final Logger logger = LoggerFactory.getLogger(RedisSystemTests.class);

    @LocalServerPort
    private int port;
    @Autowired
    private ApplicationConfiguration configuration;
    @Autowired
    private TestRestTemplate clientToHttpProxy;
    private RedisClient clientToRedis = null;
    private RedisClient clientToRedisProxy = null;
    
    private RedisCommands<String, String> commandsToRedisProxy = null;
    private RedisCommands<String, String> commandsToRedis = null;
   
    @Before
    public void setUp()
    {
    	// Setup the client to the local Redis proxy
    	clientToRedisProxy = RedisClient.create("redis://localhost:"+configuration.getProperties().getProxy().getResp().getPort());
		commandsToRedisProxy = clientToRedisProxy.connect().sync();		
		
    	// Setup the client to the remote Redis instance 
		clientToRedis = RedisClient.create(configuration.getProperties().getHost().toString());
		commandsToRedis = clientToRedis.connect().sync();
    }
    
    @After
    public void shutDown()
    {
    	clientToRedisProxy.shutdown();
    	clientToRedis.shutdown();
    }
    
    /**
     * Test the cache's ability to expire based on the configured period.
     * 
     * Strategy:
     * 1. Set and get an element
     * 2. Register an expiration event listener
     * 3. Wait for twice the expiration period
     * 4. Check to see the element was expired
     * 5. Check to see whether the element was not prematurely expired  
     * 
     * @throws Exception
     */
    @Test
    public void entryShouldNotExistAfterExpiration() throws Exception
    {
    	String key = UUID.randomUUID().toString();
    	String value = UUID.randomUUID().toString();
    	String result = null;    	
    	StopWatch sw = new StopWatch();
    	
    	long expirationPeriodInSeconds = configuration.getProperties().getProxy().getCache().getExpiryPeriod().getSeconds();

    	// Use a latch to notify the main test thread of when the expiration has occurred
    	CountDownLatch expiryLatch = new CountDownLatch(1);

    	// Create a listener to capture the expiration events
    	CacheEventListenerAdapter listener = new CacheEventListenerAdapter() {			
			@Override
			public void notifyElementExpired(Ehcache cache, Element element) {
				
				// Is the expired element the one we set, if so notify the main test thread to proceed with validations
				if( element.getObjectKey().toString().equals(key))
				{;
					sw.stop();
					expiryLatch.countDown();
				}
			}
		};
		
		// Register a cache event listener to mark the moment the element is expired.
    	configuration.ehCacheManager().getEhcache("redisCache").getCacheEventNotificationService().registerListener(listener);
    	
		try
		{	    	
	    	// Set the value in the Redis instance
	    	commandsToRedis.set(key,value);

	    	// Start the stop watch
	    	sw.start();
	    	// Get & check the value via the http proxy thereby setting off the cache
			result = this.clientToHttpProxy.getForObject("http://localhost:" + port + "/?key="+key,String.class);
			assertEquals(result,value);
	    	
			// Wait for 2X the amount of time as configured for the element in the cache to expire.
	    	expiryLatch.await(2*expirationPeriodInSeconds,TimeUnit.SECONDS);
	    	sw.stop();
		}
    	finally
    	{
    		// Remove the cache event listener
    		configuration.ehCacheManager().getEhcache("redisCache").getCacheEventNotificationService().unregisterListener(listener);
    	}
		
		// Grab the expired element
		Element expiredElement = configuration.ehCacheManager().getEhcache("redisCache").get(key);
		
		// Check to make sure the element is expired
		if(expiredElement != null )
			assertTrue("Waited "+2*expirationPeriodInSeconds+"s. The element never expired.",expiredElement.isExpired());
		    	
    	// Check for premature expiration
    	assertFalse("The element expired prematurely at "+sw.getTotalTimeSeconds()+"s. Expiration period="+expirationPeriodInSeconds+"s.",sw.getTotalTimeSeconds() < expirationPeriodInSeconds);
    	    	
    	// However, we cannot discern small deltas due to OS behaviors such as context switching.
    	if( sw.getTotalTimeSeconds() > expirationPeriodInSeconds & sw.getTotalTimeSeconds() < 2*expirationPeriodInSeconds )
    		logger.warn("The expiration occured after the specified time ["+expirationPeriodInSeconds+"s] but less than the upper bound of 2X.");
    }
    
    @Test
    public void sequentialGetsShouldBeCached() throws Exception {
    	    	             
    	String key = UUID.randomUUID().toString();
    	String value = UUID.randomUUID().toString();
    	String result = null;    	
    	// Use a latch to notify the main test thread of when the expiration has occurred
    	CountDownLatch putLatch = new CountDownLatch(1);

		// Create a listener to capture the put event
    	CacheEventListenerAdapter listener = new CacheEventListenerAdapter() {			
			@Override
			public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
		    				
				// Is the put element is the one we set, notify the main test thread to proceed with validations
				if( element.getObjectKey().toString().equals(key))
				{
					putLatch.countDown();
				}
			}
		};

		// Set the value in the Redis instance
    	commandsToRedis.set(key,value);

		// Register a cache event listener to mark the moment the element is put.
    	configuration.ehCacheManager().getEhcache("redisCache").getCacheEventNotificationService().registerListener(listener);
    	try
    	{
        	// Get & check the value via the http proxy
    		result = this.clientToHttpProxy.getForObject("http://localhost:" + port + "/?key="+key,String.class);
    		assertEquals(result,value);
    		
    		// Wait to make sure the cache was upated
    		boolean isCached = putLatch.await(10, TimeUnit.SECONDS);
    		assertTrue("The cache was not updated on a get!",isCached);
    	}
    	finally
    	{
    		// Remove the cache event listener
    		configuration.ehCacheManager().getEhcache("redisCache").getCacheEventNotificationService().unregisterListener(listener);
    	}		

    	// Get & check the value via the resp proxy    	
		result = commandsToRedisProxy.get(key);
		assertEquals(result,value);		
    }
    public static class Tuple
    {
    	protected String key = null;
    	protected String value = null;
    	
		public Tuple(String key, String value) {
			super();
			this.key = key;
			this.value = value;
		}
    	
    	public String key()
    	{
    		return this.key;
    	}
    	
    	public String value()
    	{
    		return this.value;
    	}    	
    }
    
    public static Tuple[] newElements(int count)
    {
    	Tuple[] elements = new Tuple[count];
    	
    	for(int x=0; x<count;x++)
    	{
    		elements[x] = new Tuple(UUID.randomUUID().toString(),UUID.randomUUID().toString());
    	}
    	
    	return elements;
    }
    
    public void setNGet(String key, String value)
    {
    	// Set the value in the Redis instance
    	commandsToRedis.set(key,value);

    	// Get & check the value via the http proxy
		String result = this.clientToHttpProxy.getForObject("http://localhost:" + port + "/?key="+key,String.class);
		assertEquals(result,value);
    }
    
    public void setNGet(Tuple tuple)
    {
    	this.setNGet(tuple.key(), tuple.value());
    }
    
    /**
     * Validate the capacity of the cache is as configured, and when the capacity is exceeded,
     * the least recently used element is evicted.
     * 
     * Strategy:
     * 
     * 1. Create N+1 elements of test data
     * 2. For elements 0 thru N-1, set and get the cache entries
     * 3. For elements 1 thru N-1, get the cache entries
     * 4. Set and get element N
     * 5.    * an eviction must take place
     * 6.    * the evicted item must be element 0
     * 
     * @throws Exception
     */
    @Test
    public void evictionsShouldHappenOnCapacityAndLRUShouldBe86d() throws Exception
    {
    	// Get the capacity of the cache
    	int cacheCapacity = this.configuration.getProperties().getProxy().getCache().getCapacity();
    	
    	// Generate cacheCapacity+1 data entries
    	Tuple[] tuples = newElements(cacheCapacity+1);
    	
    	// Change the TTL in order to have enough time to perform all the updates and gets
    	configuration.ehCacheManager().getEhcache("redisCache").getCacheConfiguration().setTimeToLiveSeconds(60*5);
    	
    	// Use a latch to notify the main test thread of when the eviction has occurred
    	CountDownLatch evictionLatch = new CountDownLatch(1);

		// Create a listener to capture the eviction event
    	CacheEventListenerAdapter listener = new CacheEventListenerAdapter() {			
			@Override
			 public void notifyElementEvicted(Ehcache cache, Element element) {
		    				
				// Is the first element the one being evicted?
				if( element.getObjectKey().toString().equals(tuples[0].key))
				{
					evictionLatch.countDown();
				}
			}
		};
		
    	// Register the listener on the redis cache
		configuration.ehCacheManager().getEhcache("redisCache").getCacheEventNotificationService().registerListener(listener);
		
    	try {
    		
    		// Set and get the cache entries
    		for(int x=0;x<cacheCapacity;x++)
    		{
    			this.setNGet(tuples[x]);
    		}
    		    		
    		// Hammer the gets
    		for(int y=0;y<30;y++)
    		{
            	// Get & check the value via the http proxy for all but the 0th entry
        		for(int x=1;x<cacheCapacity;x++)
        		{
            		String result = this.clientToHttpProxy.getForObject("http://localhost:" + port + "/?key="+tuples[x].key(),String.class);
            		assertEquals(result,tuples[x].value);
        		}
    		}
    		
    		// Set the last entry.  This should evict the first element
    		this.setNGet(tuples[cacheCapacity]);
    		    		
    		boolean wasEvicted = evictionLatch.await(8, TimeUnit.SECONDS);
    		assertTrue("The least recently used item (index=0) was not evicted when the capacity was exceeded.",wasEvicted);
    	}
    	finally
    	{
    		// Reset the cache configuration
    		configuration.ehCacheManager().getEhcache("redisCache").getCacheConfiguration().setTimeToLiveSeconds(this.configuration.getProperties().getProxy().getCache().getExpiryPeriod().getSeconds());
    		
    		// Remove the cache event listener
    		configuration.ehCacheManager().getEhcache("redisCache").getCacheEventNotificationService().unregisterListener(listener);
    	}
    }
}
