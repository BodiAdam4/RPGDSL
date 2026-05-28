package hu.bodiadam.rpg.dsl.model.scene;

import java.util.List;

/**
 * A world blokk reprezentációja.
 */
public record World(List<SceneDefinition> scenes) {
    public World {
        scenes = scenes == null ? List.of() : List.copyOf(scenes);
    }
}
