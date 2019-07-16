package io.segment.redis.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.segment.redis.RedisService;

@RestController
public class ProxyController {

	@Autowired
    private RedisService redisService;

    @RequestMapping(path="/",method=RequestMethod.GET)    
    public String get(@RequestParam(value="key") String key) {
        return redisService.get(key);
    }
}
