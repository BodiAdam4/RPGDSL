package hu.bodiadam.rpg.runtime;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import java.util.UUID;

public record GameSession(UUID id, GameDefinition definition, GameState state) {
    public GameSession {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
    }
}