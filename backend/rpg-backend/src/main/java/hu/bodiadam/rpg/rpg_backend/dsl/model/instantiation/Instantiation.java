package hu.bodiadam.rpg.rpg_backend.dsl.model.instantiation;

/**
 * Közös ős a név szerinti példányosításokhoz.
 */
public sealed interface Instantiation permits ItemInstantiation, ActorInstantiation {

    String instanceName();

    String typeName();
}
