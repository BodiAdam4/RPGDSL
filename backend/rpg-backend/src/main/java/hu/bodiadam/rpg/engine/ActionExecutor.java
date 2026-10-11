package hu.bodiadam.rpg.engine;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.action.GameAction;
import hu.bodiadam.rpg.runtime.GameState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ActionExecutor {

    @NonNull
    private final ExpressionEvaluator expressionEvaluator;

    public GameState execute(
            GameAction action,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters) {

        //Todo: implement action execution logic here
        return new GameState(null,null,null,false);
    }
}
