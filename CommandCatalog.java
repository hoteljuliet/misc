package com.comcast.mirs.sixoneone.impl.commands;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.impl.CatalogBase;
import org.apache.commons.chain.impl.ChainBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * A catalog of commands that allows for programmatic access to all commands.
 * This is used for development and testing via the chain controller.
 */
@Component
public class CommandCatalog extends CatalogBase {

    public CommandCatalog() {
        super();
    }

    @Autowired
    private List<Command> commandsList;

    @PostConstruct
    public void postConstruct() {

        for (Command command : commandsList) {
            // exclude chains, which are also commands
            if (command instanceof ChainBase) {
                ;
            }
            // reformat the names of some classes (for examples, ones with @RefreshScope)
            else if (command.getClass().getSimpleName().contains("$$EnhancerBySpringCGLIB")) {
                String[] parts = command.getClass().getSimpleName().split("\\$");
                String simpleName = parts[0];
                addCommand(simpleName, command);
            }
            else {
                addCommand(command.getClass().getSimpleName(), command);
            }
        }
    }
}
