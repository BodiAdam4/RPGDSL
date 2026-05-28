package hu.bodiadam.rpg.dsl.model.instantiation;

/**
 * Közös ős a név szerinti példányosításokhoz.
 */
public sealed interface Instantiation permits ItemInstantiation, ActorInstantiation {

    String instanceName();

    String typeName();
}
