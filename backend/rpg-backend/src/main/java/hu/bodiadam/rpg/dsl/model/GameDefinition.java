package hu.bodiadam.rpg.dsl.model;

import hu.bodiadam.rpg.dsl.model.definition.ActorDefinition;
import hu.bodiadam.rpg.dsl.model.definition.DefinitionBlock;
import hu.bodiadam.rpg.dsl.model.definition.ItemDefinition;
import hu.bodiadam.rpg.dsl.model.definition.VariableDefinition;
import hu.bodiadam.rpg.dsl.model.instantiation.ActorInstantiation;
import hu.bodiadam.rpg.dsl.model.instantiation.ItemInstantiation;
import hu.bodiadam.rpg.dsl.model.intent.RuleDefinition;
import hu.bodiadam.rpg.dsl.model.scene.StartBlock;
import hu.bodiadam.rpg.dsl.model.scene.World;
import java.util.List;
import java.util.Optional;

/**
 * A scriptből felépített teljes domain definíció.
 */

public record GameDefinition(
        Optional<String> initPrompt,
        DefinitionBlock<ItemDefinition, ItemInstantiation> items,
        DefinitionBlock<ActorDefinition, ActorInstantiation> actors,
        List<VariableDefinition> variables,
        StartBlock start,
        List<RuleDefinition> rules,
        World world) {

    public GameDefinition {
        initPrompt = initPrompt == null ? Optional.empty() : initPrompt;
        items = items == null ? DefinitionBlock.empty() : items;
        actors = actors == null ? DefinitionBlock.empty() : actors;
        variables = variables == null ? List.of() : List.copyOf(variables);
        start = start == null ? new StartBlock(List.of(), List.of(), List.of()) : start;
        rules = rules == null ? List.of() : List.copyOf(rules);
        world = world == null ? new World(List.of()) : world;
    }
}
