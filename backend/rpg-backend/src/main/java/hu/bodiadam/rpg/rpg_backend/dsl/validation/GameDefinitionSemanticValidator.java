package hu.bodiadam.rpg.rpg_backend.dsl.validation;

import hu.bodiadam.rpg.rpg_backend.dsl.model.GameDefinition;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * névütközések:
 * - duplikált item / actor / variable / rule / scene nevek
 * - duplikált item és actor instance nevek
 * - duplikált start instance nevek
 * - duplikált scene intent nevek
 * - duplikált scene intent handlerek
 * - duplikált item és actor property-k
 * - duplikált rule paraméterek
 *
 * referenciák:
 * - ismeretlen item / actor parent
 * - ismeretlen item / actor instantiation target
 * - ismeretlen scene target go to esetén
 * - ismeretlen rule név és rule parameterszam eltérés
 * - ismeretlen actor says esetén
 * - ismeretlen present actor
 * - ismeretlen item referencia
 * - ismeretlen property root
 * - ismeretlen property név itemen / actoron / instance-on
 *
 * öröklés:
 * - item inheritance
 * - actor inheritance
 * - örökölt property-k figyelembevétele property referencia validáláskor
 *
 * - has / does not have céljának ellenőrzése
 * - comparison bal és jobb oldal referenciáinak ellenőrzése
 *
 * akciók:
 * - add / subtract / multiply / divide referenciák validálása
 * - give / consume cél item-jellegének ellenőrzése
 *
 *
 * - chance értékek tartománya rule / intent / random esetén
 * - nem literál variable inicializálók tiltása
 */
@Component
public class GameDefinitionSemanticValidator {

    public SemanticValidationResult validate(GameDefinition definition) {
        return new SemanticValidationResult(List.of());
    }
}
