package io.segment.redis.resp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.tonivade.resp.RespServer;

import io.segment.ApplicationProperties;
import io.segment.redis.resp.commands.CommandSuite;

@Service
public class RedisServer {

	@Autowired
	private ApplicationProperties properties;

	protected RespServer server = null;

	@PostConstruct
	public void startUp()
	{
		// Start up the TCP server and begin listerning for Redis commands
		server = RespServer.builder().host("localhost").port(properties.getProxy().getResp().getPort()).commands(new CommandSuite()).build();
		server.start();
	}
	
	@PreDestroy
	public void shutDown()
	{
		// Stop and tear down the redis server
		server.stop();
	}
}
