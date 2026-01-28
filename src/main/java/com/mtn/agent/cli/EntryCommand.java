package com.mtn.agent.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
        name = "auction",
        mixinStandardHelpOptions = true,
        subcommands = {
                SimulationCommand.class,
                ServerCommand.class,
                AgentCommand.class
        })
public class EntryCommand {
}
