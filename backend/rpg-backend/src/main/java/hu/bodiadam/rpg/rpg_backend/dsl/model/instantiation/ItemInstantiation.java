package hu.bodiadam.rpg.rpg_backend.dsl.model.instantiation;

/**
 * Egy konkrét item példány deklarációja.
 */
public record ItemInstantiation(String instanceName, String typeName) implements Instantiation {
}
