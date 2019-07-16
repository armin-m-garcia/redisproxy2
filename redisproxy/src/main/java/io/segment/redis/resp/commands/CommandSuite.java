package io.segment.redis.resp.commands;

import com.github.tonivade.resp.command.CommandWrapperFactory;
import com.github.tonivade.resp.command.DefaultCommandWrapperFactory;

public class CommandSuite extends com.github.tonivade.resp.command.CommandSuite {

	public CommandSuite() {
		this(new DefaultCommandWrapperFactory());
	}

	public CommandSuite(CommandWrapperFactory factory) {
		super(factory);
		addCommand(GetCommand.class);
	}
}