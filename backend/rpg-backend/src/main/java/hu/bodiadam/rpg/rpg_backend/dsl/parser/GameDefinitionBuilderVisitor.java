package hu.bodiadam.rpg.rpg_backend.dsl.parser;

import hu.bodiadam.rpg.RpgDslBaseVisitor;
import hu.bodiadam.rpg.RpgDslParser;
import hu.bodiadam.rpg.rpg_backend.dsl.model.GameDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.action.AddAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.action.ConsumeAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.action.DivideAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.action.GameAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.action.GiveAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.action.GoToAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.action.MultiplyAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.action.NarrateAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.action.RandomAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.action.SaysAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.action.SubtractAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.action.UseAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.condition.ComparisonCondition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.condition.ComparisonOperator;
import hu.bodiadam.rpg.rpg_backend.dsl.model.condition.Condition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.condition.HasCondition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.condition.LogicalCondition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.condition.LogicalOperator;
import hu.bodiadam.rpg.rpg_backend.dsl.model.definition.ActorDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.definition.DefinitionBlock;
import hu.bodiadam.rpg.rpg_backend.dsl.model.definition.ItemDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.definition.PropertyDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.definition.VariableDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.expression.Expression;
import hu.bodiadam.rpg.rpg_backend.dsl.model.expression.LiteralExpression;
import hu.bodiadam.rpg.rpg_backend.dsl.model.expression.ReferenceExpression;
import hu.bodiadam.rpg.rpg_backend.dsl.model.instantiation.ActorInstantiation;
import hu.bodiadam.rpg.rpg_backend.dsl.model.instantiation.Instantiation;
import hu.bodiadam.rpg.rpg_backend.dsl.model.instantiation.ItemInstantiation;
import hu.bodiadam.rpg.rpg_backend.dsl.model.intent.IntentDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.intent.IntentHandler;
import hu.bodiadam.rpg.rpg_backend.dsl.model.intent.RuleDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.scene.ActorReference;
import hu.bodiadam.rpg.rpg_backend.dsl.model.scene.SceneDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.scene.StartBlock;
import hu.bodiadam.rpg.rpg_backend.dsl.model.scene.World;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * <p>Itt még nem végzünk komoly szemantikai ellenőrzést. A feladat kizárólag az,
 * hogy a parse tree-t stabil, jól használható domain objektumokká fordítsuk.
 */
final class GameDefinitionBuilderVisitor extends RpgDslBaseVisitor<Object> {

    private static final int DEFAULT_RULE_CHANCE = 100;

    GameDefinition build(RpgDslParser.ProgramContext context) {
        return (GameDefinition) visit(context);
    }

    @Override
    public Object visitProgram(RpgDslParser.ProgramContext ctx) {
        // Először összegyűjtjük a top-level blokkokat külön változókba,
        // majd a végén egy GameDefinition objektumot állítunk össze.
        Optional<String> initPrompt = Optional.empty();
        DefinitionBlock<ItemDefinition, ItemInstantiation> items = DefinitionBlock.empty();
        DefinitionBlock<ActorDefinition, ActorInstantiation> actors = DefinitionBlock.empty();
        List<VariableDefinition> variables = new ArrayList<>();
        StartBlock start = new StartBlock(List.of(), List.of(), List.of());
        RpgDslParser.StartBlockContext startBlockContext = null;
        List<RuleDefinition> rules = new ArrayList<>();
        boolean initPromptSeen = false;
        boolean itemsSeen = false;
        boolean actorsSeen = false;
        boolean variablesSeen = false;
        boolean startSeen = false;
        boolean rulesSeen = false;

        for (RpgDslParser.TopLevelBlockContext block : ctx.topLevelBlock()) {

            if (block.initialPromptBlock() != null) {
                if (initPromptSeen) {
                    throw unsupported(block, "Duplicate top-level block: initPrompt");
                }
                initPromptSeen = true;
                initPrompt = Optional.of(unquote(block.initialPromptBlock().STRING().getText()));
            } else if (block.itemsBlock() != null) {
                if (itemsSeen) {
                    throw unsupported(block, "Duplicate top-level block: items");
                }
                itemsSeen = true;
                items = mapItemsBlock(block.itemsBlock());
            } else if (block.actorsBlock() != null) {
                if (actorsSeen) {
                    throw unsupported(block, "Duplicate top-level block: actors");
                }
                actorsSeen = true;
                actors = mapActorsBlock(block.actorsBlock());
            } else if (block.variablesBlock() != null) {
                if (variablesSeen) {
                    throw unsupported(block, "Duplicate top-level block: vars");
                }
                variablesSeen = true;
                variables = mapVariables(block.variablesBlock());
            } else if (block.startBlock() != null) {
                if (startSeen) {
                    throw unsupported(block, "Duplicate top-level block: start");
                }
                startSeen = true;
                startBlockContext = block.startBlock();
            } else if (block.rulesBlock() != null) {
                if (rulesSeen) {
                    throw unsupported(block, "Duplicate top-level block: rules");
                }
                rulesSeen = true;
                rules = mapRules(block.rulesBlock());
            }
        }

        if (startBlockContext != null) {
            start = mapStartBlock(startBlockContext, items, actors);
        }

        return new GameDefinition(
                initPrompt,
                items,
                actors,
                variables,
                start,
                rules,
                mapWorld(ctx.worldBlock(), actors, start));
    }

    private DefinitionBlock<ItemDefinition, ItemInstantiation> mapItemsBlock(RpgDslParser.ItemsBlockContext ctx) {
        // Az items blokk vegyesen tartalmazhat item definíciókat és explicit példányosításokat.
        List<ItemDefinition> definitions = new ArrayList<>();
        List<ItemInstantiation> instantiations = new ArrayList<>();

        for (var definition : ctx.itemDefinition()) {
            definitions.add(mapItemDefinition(definition));
        }
        for (var instantiation : ctx.instantiation()) {
            instantiations.add(mapItemInstantiation(instantiation));
        }
        return new DefinitionBlock<>(definitions, instantiations);
    }

    private DefinitionBlock<ActorDefinition, ActorInstantiation> mapActorsBlock(RpgDslParser.ActorsBlockContext ctx) {
        List<ActorDefinition> definitions = new ArrayList<>();
        List<ActorInstantiation> instantiations = new ArrayList<>();

        for (var definition : ctx.actorDefinition()) {
            definitions.add(mapActorDefinition(definition));
        }
        for (var instantiation : ctx.instantiation()) {
            instantiations.add(mapActorInstantiation(instantiation));
        }
        return new DefinitionBlock<>(definitions, instantiations);
    }

    private List<VariableDefinition> mapVariables(RpgDslParser.VariablesBlockContext ctx) {
        // A vars blokkban minden sorból egy név + expression típusú domain elem készül.
        List<VariableDefinition> variables = new ArrayList<>();
        for (var definition : ctx.variableDefinition()) {
            variables.add(new VariableDefinition(
                    definition.ID().getText(),
                    mapExpression(definition.expression())));
        }
        return variables;
    }

    private StartBlock mapStartBlock(
            RpgDslParser.StartBlockContext ctx,
            DefinitionBlock<ItemDefinition, ItemInstantiation> items,
            DefinitionBlock<ActorDefinition, ActorInstantiation> actors) {
        // A start blokkban a példányosításokat és az actionöket külön gyűjtjük
        List<ItemInstantiation> itemInstantiations = new ArrayList<>();
        List<ActorInstantiation> actorInstantiations = new ArrayList<>();
        List<GameAction> actions = new ArrayList<>();
        for (var statement : ctx.statement()) {
            if (statement.instantiation() != null) {
                Instantiation instantiation = mapStartInstantiation(statement.instantiation(), items, actors);
                if (instantiation instanceof ItemInstantiation itemInstantiation) {
                    itemInstantiations.add(itemInstantiation);
                } else if (instantiation instanceof ActorInstantiation actorInstantiation) {
                    actorInstantiations.add(actorInstantiation);
                }
            } else {
                actions.add(mapAction(statement));
            }
        }
        return new StartBlock(itemInstantiations, actorInstantiations, actions);
    }

    private List<RuleDefinition> mapRules(RpgDslParser.RulesBlockContext ctx) {
        List<RuleDefinition> rules = new ArrayList<>();
        for (var definition : ctx.ruleDefinition()) {
            // A paraméterlista
            List<String> parameters = definition.parameters() == null
                    ? List.of()
                    : definition.parameters().ID().stream().map(TerminalNode::getText).toList();
            rules.add(new RuleDefinition(
                    definition.ID().getText(),
                    parameters,
                    parseOptionalChance(definition.INT()),
                    Optional.ofNullable(definition.ruleBody().requiresBlock()).map(this::mapRequires),
                    mapActions(definition.ruleBody().successBlock().statement()),
                    mapActions(definition.ruleBody().failBlock().statement())));
        }
        return rules;
    }

    private World mapWorld(
            RpgDslParser.WorldBlockContext ctx,
            DefinitionBlock<ActorDefinition, ActorInstantiation> actors,
            StartBlock start) {
        // A world scene-ek listájára fordul le.
        List<SceneDefinition> scenes = new ArrayList<>();
        for (var scene : ctx.scenesBlock().sceneDefinition()) {
            scenes.add(mapScene(scene, actors, start));
        }
        return new World(scenes);
    }

    private ItemDefinition mapItemDefinition(RpgDslParser.ItemDefinitionContext ctx) {
        // Egy item definíció stackelhetőséget, öröklést és tetszőleges property-ket is hordozhat.
        boolean stackable = ctx.STACKABLE() != null;
        List<PropertyDefinition> properties = new ArrayList<>(mapProperties(ctx.itemBody().propertyDefinition()));
        if (stackable && properties.stream().noneMatch(property -> property.name().equals("count"))) {
            // A stackelhető itemek automatikusan kapnak count property-t már a modellépítés során.
            properties.add(new PropertyDefinition("count", new LiteralExpression(0)));
        }
        return new ItemDefinition(
                ctx.ID(0).getText(),
                stackable,
                optionalText(ctx.ID(), 1),
                unquote(ctx.itemBody().descriptionDefinition().STRING().getText()),
                properties);
    }

    private ActorDefinition mapActorDefinition(RpgDslParser.ActorDefinitionContext ctx) {
        // Az actor definíció szerkezetileg hasonló az itemhez, de külön domain típust kap.
        return new ActorDefinition(
                ctx.ID(0).getText(),
                optionalText(ctx.ID(), 1),
                unquote(ctx.actorBody().descriptionDefinition().STRING().getText()),
                mapProperties(ctx.actorBody().propertyDefinition()));
    }

    private List<PropertyDefinition> mapProperties(List<RpgDslParser.PropertyDefinitionContext> contexts) {
        // A property egy egységes név-érték pár, függetlenül attól, milyen definíción van.
        List<PropertyDefinition> properties = new ArrayList<>();
        for (var property : contexts) {
            properties.add(new PropertyDefinition(
                    property.ID().getText(),
                    mapExpression(property.expression())));
        }
        return properties;
    }

    private SceneDefinition mapScene(
            RpgDslParser.SceneDefinitionContext ctx,
            DefinitionBlock<ActorDefinition, ActorInstantiation> actors,
            StartBlock start) {
        // A scene elemei vegyes típusúak, ezért lépésről lépésre szedjük ki belőlük a megfelelő részeket.
        List<ActorReference> presentActors = new ArrayList<>();
        List<GameAction> onEnterActions = List.of();
        List<GameAction> onExitActions = List.of();
        List<IntentDefinition> intents = new ArrayList<>();
        List<IntentHandler> intentHandlers = new ArrayList<>();
        boolean endScene = false;

        for (var element : ctx.sceneBody().sceneElement()) {
            if (element.presentDefinition() != null) {
                for (TerminalNode id : element.presentDefinition().ID()) {
                    presentActors.add(mapPresentEntry(id.getText(), actors, start));
                }
            } else if (element.onEnter() != null) {
                onEnterActions = mapActions(element.onEnter().statement());
            } else if (element.onExit() != null) {
                onExitActions = mapActions(element.onExit().statement());
            } else if (element.intentsBlock() != null) {
                for (var intent : element.intentsBlock().intentDefinition()) {
                    intents.add(new IntentDefinition(
                            intent.ID().getText(),
                            unquote(intent.descriptionDefinition().STRING().getText())));
                }
            } else if (element.onIntent() != null) {
                intentHandlers.add(mapIntentHandler(element.onIntent()));
            } else if (element.endScene() != null) {
                endScene = true;
            }
        }

        return new SceneDefinition(
                ctx.ID().getText(),
                unquote(ctx.sceneBody().descriptionDefinition().STRING().getText()),
                presentActors,
                onEnterActions,
                onExitActions,
                intents,
                intentHandlers,
                endScene);
    }

    private ActorReference mapPresentEntry(
            String name,
            DefinitionBlock<ActorDefinition, ActorInstantiation> actors,
            StartBlock start) {
        boolean isActorType = actors.definitions().stream().anyMatch(actor -> actor.name().equals(name));
        boolean isActorInstance = actors.instantiations().stream().anyMatch(actor -> actor.instanceName().equals(name))
                || start.actorInstantiations().stream().anyMatch(actor -> actor.instanceName().equals(name));
        if (isActorType && !isActorInstance) {
            return new ActorReference(name);
        }
        if (isActorInstance && !isActorType) {
            return new ActorReference(name);
        }
        if (isActorType) {
            return new ActorReference(name);
        }
        throw new DslParseException("Unknown present entry: " + name);
    }

    private IntentHandler mapIntentHandler(RpgDslParser.OnIntentContext ctx) {
        // Ez képezi le az `on <intent> chance <n>` blokkot a domain modellre.
        return new IntentHandler(
                ctx.ID().getText(),
                parseChance(ctx.INT()),
                Optional.ofNullable(ctx.intentBody().requiresBlock()).map(this::mapRequires),
                mapActions(ctx.intentBody().successBlock().statement()),
                mapActions(ctx.intentBody().failBlock().statement()));
    }

    private Condition mapRequires(RpgDslParser.RequiresBlockContext ctx) {
        return mapCondition(ctx.condition());
    }

    private Condition mapCondition(RpgDslParser.ConditionContext ctx) {
        // A grammar balról jobbra adja vissza a feltételrészeket,
        // itt ebből explicit logikai fát építünk.
        Condition current = mapConditionTerm(ctx.conditionTerm(0));
        for (int i = 1; i < ctx.conditionTerm().size(); i++) {
            LogicalOperator operator = ctx.getChild(2 * i - 1).getText().equals("and")
                    ? LogicalOperator.AND
                    : LogicalOperator.OR;
            current = new LogicalCondition(current, operator, mapConditionTerm(ctx.conditionTerm(i)));
        }
        return current;
    }

    private Condition mapConditionTerm(RpgDslParser.ConditionTermContext ctx) {
        // A condition legkisebb egysége lehet egyszerű birtoklásvizsgálat,
        // zárójelezett feltétel vagy konkrét összehasonlítás.
        if (ctx.HAS() != null) {
            return new HasCondition(ctx.ID().getText(), false);
        }
        if (ctx.DOES() != null) {
            return new HasCondition(ctx.ID().getText(), true);
        }
        if (ctx.condition() != null) {
            return mapCondition(ctx.condition());
        }
        return new ComparisonCondition(
                mapReference(ctx.propertyReference()),
                mapComparison(ctx.comparison()),
                mapExpression(ctx.expression()));
    }

    private ComparisonOperator mapComparison(RpgDslParser.ComparisonContext ctx) {
        // A DSL szöveges összehasonlító szerkezeteit belső enumokra fordítjuk.
        String text = ctx.getText();
        return switch (text) {
            case "islessthan" -> ComparisonOperator.LESS_THAN;
            case "isgreaterthan" -> ComparisonOperator.GREATER_THAN;
            case "isequalto" -> ComparisonOperator.EQUAL_TO;
            case "isnotequalto" -> ComparisonOperator.NOT_EQUAL_TO;
            case "isatleast" -> ComparisonOperator.AT_LEAST;
            case "isatmost" -> ComparisonOperator.AT_MOST;
            default -> throw unsupported(ctx, "Unknown comparison operator: " + text);
        };
    }

    private List<GameAction> mapActions(List<RpgDslParser.StatementContext> contexts) {
        List<GameAction> actions = new ArrayList<>();
        for (var statement : contexts) {
            actions.add(mapAction(statement));
        }
        return actions;
    }

    private GameAction mapAction(RpgDslParser.StatementContext ctx) {
        if (ctx.giveStatement() != null) {
            return new GiveAction(
                    Optional.ofNullable(ctx.giveStatement().expression()).map(this::mapExpression),
                    ctx.giveStatement().ID().getText());
        }
        if (ctx.consumeStatement() != null) {
            return new ConsumeAction(
                    mapExpression(ctx.consumeStatement().expression()),
                    ctx.consumeStatement().ID().getText());
        }
        if (ctx.addStatement() != null) {
            return new AddAction(
                    mapExpression(ctx.addStatement().expression()),
                    mapReference(ctx.addStatement().propertyReference()));
        }
        if (ctx.subtractStatement() != null) {
            return new SubtractAction(
                    mapExpression(ctx.subtractStatement().expression()),
                    mapReference(ctx.subtractStatement().propertyReference()));
        }
        if (ctx.multiplyStatement() != null) {
            return new MultiplyAction(
                    mapReference(ctx.multiplyStatement().propertyReference()),
                    mapExpression(ctx.multiplyStatement().expression()));
        }
        if (ctx.divideStatement() != null) {
            return new DivideAction(
                    mapReference(ctx.divideStatement().propertyReference()),
                    mapExpression(ctx.divideStatement().expression()));
        }
        if (ctx.narrateStatement() != null) {
            return new NarrateAction(unquote(ctx.narrateStatement().STRING().getText()));
        }
        if (ctx.randomStatement() != null) {
            return new RandomAction(
                    parseChance(ctx.randomStatement().INT()),
                    mapActions(ctx.randomStatement().statement()));
        }
        if (ctx.saysStatement() != null) {
            return new SaysAction(
                    ctx.saysStatement().ID().getText(),
                    unquote(ctx.saysStatement().STRING().getText()));
        }
        if (ctx.goToStatement() != null) {
            return new GoToAction(ctx.goToStatement().ID().getText());
        }
        if (ctx.useStatement() != null) {
            List<Expression> arguments = ctx.useStatement().arguments() == null
                    ? List.of()
                    : ctx.useStatement().arguments().expression().stream().map(this::mapExpression).toList();
            return new UseAction(ctx.useStatement().ID().getText(), arguments);
        }
        throw unsupported(ctx, "Unknown action type");
    }

    private Expression mapExpression(RpgDslParser.ExpressionContext ctx) {
        // Az expression lehet literál, referencia vagy zárójelezett expression.
        if (ctx.literal() != null) {
            return mapLiteral(ctx.literal());
        }
        if (ctx.propertyReference() != null) {
            return mapReference(ctx.propertyReference());
        }
        if (ctx.expression() != null) {
            return mapExpression(ctx.expression());
        }
        throw unsupported(ctx, "Unknown expression type");
    }

    private Expression mapLiteral(RpgDslParser.LiteralContext ctx) {
        if (ctx.INT() != null) {
            return new LiteralExpression(Integer.parseInt(ctx.INT().getText()));
        }
        if (ctx.BOOLEAN() != null) {
            return new LiteralExpression(Boolean.parseBoolean(ctx.BOOLEAN().getText()));
        }
        if (ctx.STRING() != null) {
            return new LiteralExpression(unquote(ctx.STRING().getText()));
        }
        throw unsupported(ctx, "Unknown literal type");
    }

    private ReferenceExpression mapReference(RpgDslParser.PropertyReferenceContext ctx) {
        // A referencia lehet egyszerű (`hp`) vagy összetett (`Potion.count`) alakú.
        return new ReferenceExpression(
                ctx.ID(0).getText(),
                optionalText(ctx.ID(), 1));
    }

    private ItemInstantiation mapItemInstantiation(RpgDslParser.InstantiationContext ctx) {
        return new ItemInstantiation(ctx.ID(0).getText(), ctx.ID(1).getText());
    }

    private ActorInstantiation mapActorInstantiation(RpgDslParser.InstantiationContext ctx) {
        return new ActorInstantiation(ctx.ID(0).getText(), ctx.ID(1).getText());
    }

    private Instantiation mapStartInstantiation(
            RpgDslParser.InstantiationContext ctx,
            DefinitionBlock<ItemDefinition, ItemInstantiation> items,
            DefinitionBlock<ActorDefinition, ActorInstantiation> actors) {
        String instanceName = ctx.ID(0).getText();
        String typeName = ctx.ID(1).getText();
        boolean isItem = items.definitions().stream().anyMatch(definition -> definition.name().equals(typeName));
        boolean isActor = actors.definitions().stream().anyMatch(definition -> definition.name().equals(typeName));
        if (isItem && !isActor) {
            return new ItemInstantiation(instanceName, typeName);
        }
        if (isActor && !isItem) {
            return new ActorInstantiation(instanceName, typeName);
        }
        if (isItem) {
            throw unsupported(ctx, "Ambiguous start instantiation target type: " + typeName);
        }
        throw unsupported(ctx, "Unknown start instantiation target type: " + typeName);
    }

    private Optional<String> optionalText(List<TerminalNode> nodes, int index) {
        return index < nodes.size() ? Optional.of(nodes.get(index).getText()) : Optional.empty();
    }

    private int parseChance(TerminalNode node) {
        return Integer.parseInt(node.getText());
    }

    private int parseOptionalChance(TerminalNode node) {
        return node == null ? DEFAULT_RULE_CHANCE : parseChance(node);
    }

    private String unquote(String text) {
        return text.substring(1, text.length() - 1)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private DslParseException unsupported(ParserRuleContext ctx, String message) {
        // Váratlan szerkezetnél sorinformációval együtt dobunk hibát.
        return new DslParseException(message + " at line " + ctx.getStart().getLine());
    }
}
