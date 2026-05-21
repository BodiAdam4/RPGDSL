package hu.bodiadam.rpg.rpg_backend.dsl.validation;

import hu.bodiadam.rpg.RpgDslParser;
import hu.bodiadam.rpg.rpg_backend.dsl.model.GameDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.definition.ActorDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.definition.ItemDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.definition.PropertyDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.definition.VariableDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.instantiation.ActorInstantiation;
import hu.bodiadam.rpg.rpg_backend.dsl.model.instantiation.ItemInstantiation;
import hu.bodiadam.rpg.rpg_backend.dsl.model.intent.RuleDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.scene.SceneDefinition;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.springframework.stereotype.Component;

@Component
public class GameDefinitionSemanticValidator {

    public SemanticValidationResult validate(RpgDslParser.ProgramContext programContext, GameDefinition definition) {
        return new SemanticValidationResult(new ValidationPass(programContext, definition).run());
    }

    private static final class ValidationPass {
        private final RpgDslParser.ProgramContext programContext;
        private final List<Diagnostic> diagnostics = new ArrayList<>();

        private final Map<String, ItemDefinition> itemDefinitions = new LinkedHashMap<>();
        private final Map<String, ActorDefinition> actorDefinitions = new LinkedHashMap<>();
        private final Map<String, VariableDefinition> variableDefinitions = new LinkedHashMap<>();
        private final Map<String, RuleDefinition> ruleDefinitions = new LinkedHashMap<>();
        private final Map<String, SceneDefinition> sceneDefinitions = new LinkedHashMap<>();
        private final Map<String, String> itemInstances = new LinkedHashMap<>();
        private final Map<String, String> actorInstances = new LinkedHashMap<>();
        private final Map<String, Set<String>> resolvedItemProperties = new HashMap<>();
        private final Map<String, Set<String>> resolvedActorProperties = new HashMap<>();

        private ValidationPass(RpgDslParser.ProgramContext programContext, GameDefinition definition) {
            this.programContext = programContext;
            collectModel(definition);
        }

        private List<Diagnostic> run() {
            validateDefinitions();
            validateStartBlocks();
            validateWorld();
            diagnostics.sort(Comparator
                    .comparingInt(Diagnostic::line)
                    .thenComparingInt(Diagnostic::character)
                    .thenComparing(Diagnostic::code));
            return List.copyOf(diagnostics);
        }

        private void collectModel(GameDefinition definition) {
            for (ItemDefinition item : definition.items().definitions()) {
                itemDefinitions.putIfAbsent(item.name(), item);
            }
            for (ActorDefinition actor : definition.actors().definitions()) {
                actorDefinitions.putIfAbsent(actor.name(), actor);
            }
            for (VariableDefinition variable : definition.variables()) {
                variableDefinitions.putIfAbsent(variable.name(), variable);
            }
            for (RuleDefinition rule : definition.rules()) {
                ruleDefinitions.putIfAbsent(rule.name(), rule);
            }
            for (SceneDefinition scene : definition.world().scenes()) {
                sceneDefinitions.putIfAbsent(scene.name(), scene);
            }
            for (ItemInstantiation instantiation : definition.items().instantiations()) {
                itemInstances.putIfAbsent(instantiation.instanceName(), instantiation.typeName());
            }
            for (ActorInstantiation instantiation : definition.actors().instantiations()) {
                actorInstances.putIfAbsent(instantiation.instanceName(), instantiation.typeName());
            }
            for (ItemInstantiation instantiation : definition.start().itemInstantiations()) {
                itemInstances.putIfAbsent(instantiation.instanceName(), instantiation.typeName());
            }
            for (ActorInstantiation instantiation : definition.start().actorInstantiations()) {
                actorInstances.putIfAbsent(instantiation.instanceName(), instantiation.typeName());
            }
        }

        private void validateDefinitions() {
            for (RpgDslParser.TopLevelBlockContext topLevelBlock : programContext.topLevelBlock()) {
                if (topLevelBlock.itemsBlock() != null) {
                    validateItemBlock(topLevelBlock.itemsBlock());
                } else if (topLevelBlock.actorsBlock() != null) {
                    validateActorBlock(topLevelBlock.actorsBlock());
                } else if (topLevelBlock.variablesBlock() != null) {
                    validateVariablesBlock(topLevelBlock.variablesBlock());
                } else if (topLevelBlock.rulesBlock() != null) {
                    validateRulesBlock(topLevelBlock.rulesBlock());
                }
            }
        }

        private void validateItemBlock(RpgDslParser.ItemsBlockContext block) {
            Set<String> names = new LinkedHashSet<>();
            for (RpgDslParser.ItemDefinitionContext definition : block.itemDefinition()) {
                String name = definition.ID(0).getText();
                if (!names.add(name)) {
                    error(definition.ID(0), "DUPLICATE_ITEM_DEFINITION", "Duplicate item definition '%s'.".formatted(name));
                }
                if (actorDefinitions.containsKey(name)) {
                    error(definition.ID(0), "AMBIGUOUS_TYPE_NAME",
                            "Name '%s' is used both as an item and an actor definition.".formatted(name));
                }
                validateDuplicateProperties(definition.itemBody().propertyDefinition(), "item", name);
                if (definition.ID().size() > 1) {
                    String parentName = definition.ID(1).getText();
                    if (!itemDefinitions.containsKey(parentName)) {
                        error(definition.ID(1), "UNKNOWN_ITEM_PARENT",
                                "Unknown parent item '%s' for item '%s'.".formatted(parentName, name));
                    }
                }
                resolveItemProperties(name, new ArrayDeque<>());
            }

            Set<String> instanceNames = new LinkedHashSet<>();
            for (RpgDslParser.InstantiationContext instantiation : block.instantiation()) {
                String instanceName = instantiation.ID(0).getText();
                String typeName = instantiation.ID(1).getText();
                if (!itemDefinitions.containsKey(typeName)) {
                    error(instantiation.ID(1), "UNKNOWN_ITEM_TYPE",
                            "Unknown item type '%s' in item block.".formatted(typeName));
                }
                if (!instanceNames.add(instanceName)) {
                    error(instantiation.ID(0), "DUPLICATE_INSTANCE", "Duplicate instance '%s'.".formatted(instanceName));
                }
            }
        }

        private void validateActorBlock(RpgDslParser.ActorsBlockContext block) {
            Set<String> names = new LinkedHashSet<>();
            for (RpgDslParser.ActorDefinitionContext definition : block.actorDefinition()) {
                String name = definition.ID(0).getText();
                if (!names.add(name)) {
                    error(definition.ID(0), "DUPLICATE_ACTOR_DEFINITION", "Duplicate actor definition '%s'.".formatted(name));
                }
                if (itemDefinitions.containsKey(name)) {
                    error(definition.ID(0), "AMBIGUOUS_TYPE_NAME",
                            "Name '%s' is used both as an actor and an item definition.".formatted(name));
                }
                validateDuplicateProperties(definition.actorBody().propertyDefinition(), "actor", name);
                if (definition.ID().size() > 1) {
                    String parentName = definition.ID(1).getText();
                    if (!actorDefinitions.containsKey(parentName)) {
                        error(definition.ID(1), "UNKNOWN_ACTOR_PARENT",
                                "Unknown parent actor '%s' for actor '%s'.".formatted(parentName, name));
                    }
                }
                resolveActorProperties(name, new ArrayDeque<>());
            }

            Set<String> instanceNames = new LinkedHashSet<>();
            for (RpgDslParser.InstantiationContext instantiation : block.instantiation()) {
                String instanceName = instantiation.ID(0).getText();
                String typeName = instantiation.ID(1).getText();
                if (!actorDefinitions.containsKey(typeName)) {
                    error(instantiation.ID(1), "UNKNOWN_ACTOR_TYPE",
                            "Unknown actor type '%s' in actor block.".formatted(typeName));
                }
                if (!instanceNames.add(instanceName)) {
                    error(instantiation.ID(0), "DUPLICATE_INSTANCE", "Duplicate instance '%s'.".formatted(instanceName));
                }
            }
        }

        private void validateVariablesBlock(RpgDslParser.VariablesBlockContext block) {
            Set<String> variableNames = new LinkedHashSet<>();
            for (RpgDslParser.VariableDefinitionContext definition : block.variableDefinition()) {
                if (!variableNames.add(definition.ID().getText())) {
                    error(definition.ID(), "DUPLICATE_VARIABLE", "Duplicate variable '%s'.".formatted(definition.ID().getText()));
                }
                if (definition.expression().literal() == null) {
                    error(definition.expression(), "NON_LITERAL_VARIABLE_INITIALIZER",
                            "Variable '%s' must use a literal initializer.".formatted(definition.ID().getText()));
                }
            }
        }

        private void validateRulesBlock(RpgDslParser.RulesBlockContext block) {
            Set<String> ruleNames = new LinkedHashSet<>();
            for (RpgDslParser.RuleDefinitionContext definition : block.ruleDefinition()) {
                if (!ruleNames.add(definition.ID().getText())) {
                    error(definition.ID(), "DUPLICATE_RULE", "Duplicate rule '%s'.".formatted(definition.ID().getText()));
                }
                validateChance(definition.INT(), "rule '" + definition.ID().getText() + "'");

                Set<String> parameters = new LinkedHashSet<>();
                if (definition.parameters() != null) {
                    for (TerminalNode parameter : definition.parameters().ID()) {
                        if (!parameters.add(parameter.getText())) {
                            error(parameter, "DUPLICATE_RULE_PARAMETER",
                                    "Duplicate parameter '%s' in rule '%s'."
                                            .formatted(parameter.getText(), definition.ID().getText()));
                        }
                    }
                }

                validateRequires(definition.ruleBody().requiresBlock(), parameters);
                validateStatements(definition.ruleBody().successBlock().statement(), parameters, null);
                validateStatements(definition.ruleBody().failBlock().statement(), parameters, null);
            }
        }

        private void validateStartBlocks() {
            for (RpgDslParser.TopLevelBlockContext topLevelBlock : programContext.topLevelBlock()) {
                if (topLevelBlock.startBlock() != null) {
                    validateStatements(topLevelBlock.startBlock().statement(), Set.of(), null);
                }
            }
        }

        private void validateWorld() {
            Set<String> sceneNames = new LinkedHashSet<>();
            for (RpgDslParser.SceneDefinitionContext scene : programContext.worldBlock().scenesBlock().sceneDefinition()) {
                if (!sceneNames.add(scene.ID().getText())) {
                    error(scene.ID(), "DUPLICATE_SCENE", "Duplicate scene '%s'.".formatted(scene.ID().getText()));
                }
                validateScene(scene);
            }
        }

        private void validateScene(RpgDslParser.SceneDefinitionContext scene) {
            Set<String> presentActors = new LinkedHashSet<>();
            Set<String> intents = new LinkedHashSet<>();
            Set<String> handlers = new LinkedHashSet<>();

            for (RpgDslParser.SceneElementContext element : scene.sceneBody().sceneElement()) {
                if (element.presentDefinition() != null) {
                    for (TerminalNode actorId : element.presentDefinition().ID()) {
                        validatePresentActor(actorId);
                        presentActors.add(actorId.getText());
                    }
                } else if (element.intentsBlock() != null) {
                    for (RpgDslParser.IntentDefinitionContext intent : element.intentsBlock().intentDefinition()) {
                        if (!intents.add(intent.ID().getText())) {
                            error(intent.ID(), "DUPLICATE_SCENE_INTENT",
                                    "Duplicate intent '%s' in scene '%s'."
                                            .formatted(intent.ID().getText(), scene.ID().getText()));
                        }
                    }
                } else if (element.onIntent() != null) {
                    if (!handlers.add(element.onIntent().ID().getText())) {
                        error(element.onIntent().ID(), "DUPLICATE_INTENT_HANDLER",
                                "Duplicate intent handler '%s' in scene '%s'."
                                        .formatted(element.onIntent().ID().getText(), scene.ID().getText()));
                    }
                }
            }

            for (RpgDslParser.SceneElementContext element : scene.sceneBody().sceneElement()) {
                if (element.onEnter() != null) {
                    validateStatements(element.onEnter().statement(), Set.of(), presentActors);
                } else if (element.onExit() != null) {
                    validateStatements(element.onExit().statement(), Set.of(), presentActors);
                } else if (element.onIntent() != null) {
                    if (!intents.contains(element.onIntent().ID().getText())) {
                        error(element.onIntent().ID(), "UNKNOWN_SCENE_INTENT",
                                "Scene '%s' handles unknown intent '%s'."
                                        .formatted(scene.ID().getText(), element.onIntent().ID().getText()));
                    }
                    validateChance(element.onIntent().INT(),
                            "scene intent handler '%s' in scene '%s'"
                                    .formatted(element.onIntent().ID().getText(), scene.ID().getText()));
                    validateRequires(element.onIntent().intentBody().requiresBlock(), Set.of());
                    validateStatements(element.onIntent().intentBody().successBlock().statement(), Set.of(), presentActors);
                    validateStatements(element.onIntent().intentBody().failBlock().statement(), Set.of(), presentActors);
                }
            }
        }

        private void validateRequires(RpgDslParser.RequiresBlockContext requiresBlock, Set<String> parameters) {
            if (requiresBlock != null) {
                validateCondition(requiresBlock.condition(), parameters);
            }
        }

        private void validateCondition(RpgDslParser.ConditionContext condition, Set<String> parameters) {
            for (RpgDslParser.ConditionTermContext term : condition.conditionTerm()) {
                validateConditionTerm(term, parameters);
            }
        }

        private void validateConditionTerm(RpgDslParser.ConditionTermContext term, Set<String> parameters) {
            if (term.HAS() != null || term.DOES() != null) {
                validateItemReference(term.ID(), "inventory reference");
                return;
            }
            if (term.condition() != null) {
                validateCondition(term.condition(), parameters);
                return;
            }
            validatePropertyReference(term.propertyReference(), parameters);
            validateExpression(term.expression(), parameters);
        }

        private void validateStatements(
                List<RpgDslParser.StatementContext> statements,
                Set<String> parameters,
                Set<String> presentActors) {
            for (RpgDslParser.StatementContext statement : statements) {
                validateStatement(statement, parameters, presentActors);
            }
        }

        private void validateStatement(
                RpgDslParser.StatementContext statement,
                Set<String> parameters,
                Set<String> presentActors) {
            if (statement.giveStatement() != null) {
                if (statement.giveStatement().expression() != null) {
                    validateExpression(statement.giveStatement().expression(), parameters);
                }
                validateItemReference(statement.giveStatement().ID(), "give target");
                return;
            }
            if (statement.consumeStatement() != null) {
                validateExpression(statement.consumeStatement().expression(), parameters);
                validateItemReference(statement.consumeStatement().ID(), "consume target");
                return;
            }
            if (statement.addStatement() != null) {
                validateExpression(statement.addStatement().expression(), parameters);
                validatePropertyReference(statement.addStatement().propertyReference(), parameters);
                return;
            }
            if (statement.subtractStatement() != null) {
                validateExpression(statement.subtractStatement().expression(), parameters);
                validatePropertyReference(statement.subtractStatement().propertyReference(), parameters);
                return;
            }
            if (statement.multiplyStatement() != null) {
                validatePropertyReference(statement.multiplyStatement().propertyReference(), parameters);
                validateExpression(statement.multiplyStatement().expression(), parameters);
                return;
            }
            if (statement.divideStatement() != null) {
                validatePropertyReference(statement.divideStatement().propertyReference(), parameters);
                validateExpression(statement.divideStatement().expression(), parameters);
                return;
            }
            if (statement.randomStatement() != null) {
                validateChance(statement.randomStatement().INT(), "random block");
                validateStatements(statement.randomStatement().statement(), parameters, presentActors);
                return;
            }
            if (statement.saysStatement() != null) {
                if (presentActors == null || !presentActors.contains(statement.saysStatement().ID().getText())) {
                    error(statement.saysStatement().ID(), "UNKNOWN_SPEAKER",
                            "Actor '%s' is not present in the current scene."
                                    .formatted(statement.saysStatement().ID().getText()));
                }
                return;
            }
            if (statement.goToStatement() != null) {
                if (!sceneDefinitions.containsKey(statement.goToStatement().ID().getText())) {
                    error(statement.goToStatement().ID(), "UNKNOWN_SCENE_TARGET",
                            "Unknown scene '%s' in goto.".formatted(statement.goToStatement().ID().getText()));
                }
                return;
            }
            if (statement.useStatement() != null) {
                validateUseStatement(statement.useStatement(), parameters);
                return;
            }
            if (statement.instantiation() != null) {
                validateStartInstantiation(statement.instantiation());
            }
        }

        private void validateUseStatement(RpgDslParser.UseStatementContext useStatement, Set<String> parameters) {
            RuleDefinition rule = ruleDefinitions.get(useStatement.ID().getText());
            if (rule == null) {
                error(useStatement.ID(), "UNKNOWN_RULE", "Unknown rule '%s'.".formatted(useStatement.ID().getText()));
            } else {
                int actualArguments = useStatement.arguments() == null ? 0 : useStatement.arguments().expression().size();
                if (rule.parameters().size() != actualArguments) {
                    error(useStatement.ID(), "RULE_ARITY_MISMATCH",
                            "Rule '%s' expects %d argument(s), but got %d."
                                    .formatted(useStatement.ID().getText(), rule.parameters().size(), actualArguments));
                }
            }
            if (useStatement.arguments() != null) {
                for (RpgDslParser.ExpressionContext argument : useStatement.arguments().expression()) {
                    validateExpression(argument, parameters);
                }
            }
        }

        private void validateExpression(RpgDslParser.ExpressionContext expression, Set<String> parameters) {
            if (expression.literal() != null) {
                return;
            }
            if (expression.propertyReference() != null) {
                validatePropertyReference(expression.propertyReference(), parameters);
                return;
            }
            if (expression.expression() != null) {
                validateExpression(expression.expression(), parameters);
            }
        }

        private void validatePropertyReference(RpgDslParser.PropertyReferenceContext reference, Set<String> parameters) {
            if (reference.ID().size() == 1) {
                String name = reference.ID(0).getText();
                if (!parameters.contains(name) && !variableDefinitions.containsKey(name)) {
                    error(reference.ID(0), "UNKNOWN_VALUE_REFERENCE",
                            "Unknown value reference '%s'.".formatted(name));
                }
                return;
            }

            String root = reference.ID(0).getText();
            String property = reference.ID(1).getText();
            SymbolResolution resolution = resolvePropertyRoot(root);

            if (resolution == SymbolResolution.UNKNOWN) {
                error(reference.ID(0), "UNKNOWN_PROPERTY_ROOT",
                        "Unknown property root '%s'.".formatted(root));
                return;
            }
            if (resolution == SymbolResolution.SCALAR) {
                error(reference.ID(0), "INVALID_PROPERTY_ROOT",
                        "Reference '%s.%s' uses a scalar value as a property root.".formatted(root, property));
                return;
            }
            if (!resolvePropertiesFor(root, resolution).contains(property)) {
                error(reference.ID(1), "UNKNOWN_PROPERTY",
                        "Unknown property '%s' on '%s'.".formatted(property, root));
            }
        }

        private void validateItemReference(TerminalNode itemId, String usage) {
            String name = itemId.getText();
            if (itemDefinitions.containsKey(name) || itemInstances.containsKey(name)) {
                return;
            }
            if (actorDefinitions.containsKey(name) || actorInstances.containsKey(name)) {
                error(itemId, "INVALID_ITEM_REFERENCE",
                        "Reference '%s' in %s points to an actor, not an item.".formatted(name, usage));
                return;
            }
            error(itemId, "UNKNOWN_ITEM_REFERENCE",
                    "Unknown item reference '%s' in %s.".formatted(name, usage));
        }

        private void validatePresentActor(TerminalNode actorId) {
            String name = actorId.getText();
            if (actorDefinitions.containsKey(name) || actorInstances.containsKey(name)) {
                return;
            }
            if (itemDefinitions.containsKey(name) || itemInstances.containsKey(name)) {
                error(actorId, "INVALID_PRESENT_ENTRY",
                        "Present entry '%s' refers to an item, not an actor.".formatted(name));
                return;
            }
            error(actorId, "UNKNOWN_PRESENT_ACTOR", "Unknown present actor '%s'.".formatted(name));
        }

        private void validateStartInstantiation(RpgDslParser.InstantiationContext instantiation) {
            String typeName = instantiation.ID(1).getText();
            if (!itemDefinitions.containsKey(typeName) && !actorDefinitions.containsKey(typeName)) {
                error(instantiation.ID(1), "UNKNOWN_START_TARGET",
                        "Unknown start instantiation target '%s'.".formatted(typeName));
            }
        }

        private void validateChance(TerminalNode chanceNode, String subject) {
            if (chanceNode == null) {
                return;
            }
            int chance = Integer.parseInt(chanceNode.getText());
            if (chance < 0 || chance > 100) {
                error(chanceNode, "INVALID_CHANCE",
                        "Chance for %s must be between 0 and 100, but was %d.".formatted(subject, chance));
            }
        }

        private Set<String> resolveItemProperties(String itemName, Deque<String> path) {
            Set<String> cached = resolvedItemProperties.get(itemName);
            if (cached != null) {
                return cached;
            }
            if (path.contains(itemName)) {
                error("ITEM_INHERITANCE_CYCLE", "Item inheritance cycle detected around '%s'.".formatted(itemName));
                return Set.of();
            }

            path.addLast(itemName);
            ItemDefinition definition = itemDefinitions.get(itemName);
            Set<String> properties = new LinkedHashSet<>();
            if (definition != null) {
                definition.parentName().ifPresent(parentName -> {
                    if (itemDefinitions.containsKey(parentName)) {
                        properties.addAll(resolveItemProperties(parentName, path));
                    }
                });
                for (PropertyDefinition property : definition.properties()) {
                    properties.add(property.name());
                }
            }
            path.removeLast();
            Set<String> resolved = Set.copyOf(properties);
            resolvedItemProperties.put(itemName, resolved);
            return resolved;
        }

        private Set<String> resolveActorProperties(String actorName, Deque<String> path) {
            Set<String> cached = resolvedActorProperties.get(actorName);
            if (cached != null) {
                return cached;
            }
            if (path.contains(actorName)) {
                error("ACTOR_INHERITANCE_CYCLE", "Actor inheritance cycle detected around '%s'.".formatted(actorName));
                return Set.of();
            }

            path.addLast(actorName);
            ActorDefinition definition = actorDefinitions.get(actorName);
            Set<String> properties = new LinkedHashSet<>();
            if (definition != null) {
                definition.parentName().ifPresent(parentName -> {
                    if (actorDefinitions.containsKey(parentName)) {
                        properties.addAll(resolveActorProperties(parentName, path));
                    }
                });
                for (PropertyDefinition property : definition.properties()) {
                    properties.add(property.name());
                }
            }
            path.removeLast();
            Set<String> resolved = Set.copyOf(properties);
            resolvedActorProperties.put(actorName, resolved);
            return resolved;
        }

        private Set<String> resolvePropertiesFor(String rootName, SymbolResolution resolution) {
            return switch (resolution) {
                case ITEM_DEFINITION -> resolveItemProperties(rootName, new ArrayDeque<>());
                case ACTOR_DEFINITION -> resolveActorProperties(rootName, new ArrayDeque<>());
                case ITEM_INSTANCE -> resolveItemProperties(itemInstances.get(rootName), new ArrayDeque<>());
                case ACTOR_INSTANCE -> resolveActorProperties(actorInstances.get(rootName), new ArrayDeque<>());
                case SCALAR, UNKNOWN -> Set.of();
            };
        }

        private SymbolResolution resolvePropertyRoot(String rootName) {
            if (variableDefinitions.containsKey(rootName)) {
                return SymbolResolution.SCALAR;
            }
            if (itemInstances.containsKey(rootName)) {
                return SymbolResolution.ITEM_INSTANCE;
            }
            if (actorInstances.containsKey(rootName)) {
                return SymbolResolution.ACTOR_INSTANCE;
            }
            if (itemDefinitions.containsKey(rootName)) {
                return SymbolResolution.ITEM_DEFINITION;
            }
            if (actorDefinitions.containsKey(rootName)) {
                return SymbolResolution.ACTOR_DEFINITION;
            }
            return SymbolResolution.UNKNOWN;
        }

        private void validateDuplicateProperties(
                List<RpgDslParser.PropertyDefinitionContext> properties,
                String ownerKind,
                String ownerName) {
            Set<String> names = new LinkedHashSet<>();
            for (RpgDslParser.PropertyDefinitionContext property : properties) {
                if (!names.add(property.ID().getText())) {
                    error(property.ID(), "DUPLICATE_PROPERTY",
                            "Duplicate property '%s' on %s '%s'."
                                    .formatted(property.ID().getText(), ownerKind, ownerName));
                }
            }
        }

        private void error(ParserRuleContext context, String code, String message) {
            diagnostics.add(diagnostic(context.getStart(), code, message));
        }

        private void error(TerminalNode node, String code, String message) {
            diagnostics.add(diagnostic(node.getSymbol(), code, message));
        }

        private void error(String code, String message) {
            diagnostics.add(new Diagnostic(0, 0, DiagnosticSeverity.ERROR, code, message));
        }

        private Diagnostic diagnostic(Token token, String code, String message) {
            return new Diagnostic(token.getLine(), token.getCharPositionInLine() + 1, DiagnosticSeverity.ERROR, code, message);
        }
    }

    private enum SymbolResolution {
        UNKNOWN,
        SCALAR,
        ITEM_DEFINITION,
        ACTOR_DEFINITION,
        ITEM_INSTANCE,
        ACTOR_INSTANCE
    }
}
