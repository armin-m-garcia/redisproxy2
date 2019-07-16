package io.segment.redis.resp.commands;

import static com.github.tonivade.resp.protocol.RedisToken.string;

import com.github.tonivade.resp.annotation.Command;
import com.github.tonivade.resp.command.Request;
import com.github.tonivade.resp.command.RespCommand;
import com.github.tonivade.resp.protocol.RedisToken;

import io.segment.SpringContext;
import io.segment.redis.RedisService;


@Command("get")
public class GetCommand implements RespCommand {
	
	private RedisService redisService;
	
	protected RedisService getRedisService()
	{
		
		redisService = redisService == null ? SpringContext.getBean(RedisService.class) : redisService;
		return redisService;
	}
	
	@Override
	public RedisToken execute(Request request) {
		return string(getRedisService().get(request.getParam(0).toString()));
	}
}
