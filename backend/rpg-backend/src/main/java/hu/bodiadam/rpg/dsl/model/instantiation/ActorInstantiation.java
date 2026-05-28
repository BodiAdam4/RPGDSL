package hu.bodiadam.rpg.dsl.model.instantiation;

/**
 * Egy konkrét actor példány deklarációja.
 */
public record ActorInstantiation(String instanceName, String typeName) implements Instantiation {
}
