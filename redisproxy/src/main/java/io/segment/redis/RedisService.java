package io.segment.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import io.segment.ApplicationConfiguration;

@Service
public class RedisService {

	@Autowired
	private ApplicationConfiguration configuration;
	
	@Cacheable(value = "redisCache",key = "#key",unless="#result==null")
	public String get(String key)
	{
		return configuration.stringRedisTemplate().opsForValue().get(key);
//		return "Hello World!";
	}
}