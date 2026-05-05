package hu.bodiadam.rpg.rpg_backend.dsl.model.instantiation;

/**
 * Egy konkrét actor példány deklarációja.
 */
public record ActorInstantiation(String instanceName, String typeName) implements Instantiation {
}
