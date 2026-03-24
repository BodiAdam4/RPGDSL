# 1. Áttekintés

Az RPG DSL egy domain-specifikus nyelv, amely narratív alapú szerepjátékok (RPG-k) leírására van tervezve.

A nyelv segítségével strukturáltan definiálhatók:

- jelenetek és akciók
- újrahasznosítható szabályok
- tárgyak
- szereplők
- játékállapot és logika

A DSL célja, hogy deklaratív történetmesélést kombináljon szabályalapú működéssel, így jól használható AI-alapú narrációval és elágazó játékmenettel.

## 1.1. Program felépítése

Egy RPG DSL fájl az alábbiakat tartalmazza:

- 0 vagy több top-level deklarációs blokkot
- egy `world` blokkot

A top-level deklarációk globálisak, azaz a teljes játék során elérhetők.

A `world` blokk tartalmazza a játék konkrét definícióját, beleértve a jeleneteket és az azok közötti interakciókat.

## 1.2. Literálok

A nyelv az alábbi literál típusokat támogatja:

- Egész szám (`integer`)  
  Példa: `42`

- Logikai érték (`boolean`)  
  Példa: `true`, `false`

- Sztring (`string`)  
  Példa: `"Egy titokzatos ajtó"`

- Azonosító (`identifier`)  
  Példa: `HealingPotion`, `hp`, `guard1`

# 2. Top-level blokkok

A top-level blokkok a program globális definícióit tartalmazzák.  
Ezek a blokkok a `world` blokkon kívül helyezkednek el, és a teljes játék során elérhetők.

A nyelv az alábbi top-level blokkokat támogatja:

- `initPrompt`
- `items`
- `actors`
- `vars`
- `start`
- `rules`

Ezek a blokkok opcionálisak.

---

## 2.1. `initPrompt`

Az `initPrompt` blokk a játékvilág alapvető kontextusát írja le.

Ez az egyik legfontosabb top-level blokk, mert a benne megadott szöveg folyamatos kontextusként szolgál az AI számára a játék során. Az itt definiált információk meghatározzák, hogy az AI milyen stílusban, milyen világértelmezéssel és milyen narratív keretben generáljon válaszokat.

Az `initPrompt` célja tehát nem egy kezdőszöveg megadása, hanem a teljes világ, hangulat és narratív háttér kontextusba helyezése.

Ide kerülhet például:

- a világ hangulata
- a történet tónusa
- a környezet jellege
- az AI narrációs viselkedésére vonatkozó elvárások

### Példa

```dsl
initPrompt {
  "This world is a ruined dark fantasy kingdom shaped by fear, superstition, and the remains of forgotten gods.
  The tone is tense, mysterious, and oppressive. Responses should reflect uncertainty, moral ambiguity, and the harsh reality of survival."
}
```

---

## 2.2. `items`

Az `items` blokk definiálja a játékban használható itemtípusokat.

Kétféle item definiálható:

- **`item`** — nem stackelhető item
- **`stackable item`** — stackelhető item

Minden item:

- rendelkezik névvel
- kötelező `description` mezővel rendelkezik
- tartalmazhat tetszőleges további mezőket
- opcionálisan származhat egy másik itemből az `is` kulcsszó segítségével

> A `stackable item` típusok automatikusan rendelkeznek egy rejtett `count` attribútummal.  
> Ezt a nyelv automatikusan kezeli, de hivatkozni lehet rá `itemId.count` formában.

A nem stackelhető itemek a háttérben példányként léteznek. Amikor a játékos kap egy ilyen itemet, a rendszer automatikusan létrehoz egy példányt belőle, akkor is, ha ez nincs külön névvel ellátva a DSL-ben. Szükség esetén nem stackelhető itemek explicit módon is példányosíthatók.

### Példa — nem stackelhető item

```dsl
items {
  item RustyKey {
    description: "An old iron key"
    rarity: "Common"
  }
}
```

### Példa — stackelhető item

```dsl
items {
  stackable item HealingPotion {
    description: "Restores health"
    healAmount: 4
    rarity: "Common"
  }
}
```

### Származtatás

```dsl
items {
  item Weapon {
    description: "Base weapon"
    damage: 1
    rarity: "Common"
  }

  item FineDagger is Weapon {
    description: "A balanced dagger"
    damage: 5
    rarity: "Rare"
  }
}
```

### Hivatkozás item property-re

Az item mezőire `itemId.property` formában lehet hivatkozni:

```
HealingPotion.healAmount
FineDagger.damage
HealingPotion.count
```

### Implicit és explicit példányosítás

A nem stackelhető itemek kétféleképpen jelenhetnek meg a játékban.

**Implicit példányosítás** — a rendszer automatikusan létrehoz egy példányt a háttérben:

```dsl
give RustyKey
```

**Explicit példányosítás** — akkor hasznos, ha az adott itemre külön névvel kell hivatkozni:

```dsl
oldKey as RustyKey
give oldKey
```

---

## 2.3. `actors`

Az `actors` blokk a játék szereplőit definiálja, például NPC-ket vagy ellenfeleket.

Minden actor:

- rendelkezik névvel
- kötelező `description` mezővel rendelkezik
- opcionálisan származhat egy másik actorból az `is` kulcsszó segítségével
- tartalmazhat tetszőleges további mezőket

Az actorok elsődleges célja, hogy kontextust adjanak a narrációnak és a jelenetekben szereplő entitásoknak.

Az actorok példányosítása ugyanazt a logikát követi, mint a nem stackelhető itemeké. A háttérben actor példányok léteznek, és szükség esetén ezek név szerint is példányosíthatók.

### Példa

```dsl
actors {
  actor CityGuard {
    description: "A disciplined city guard"
    faction: "City Watch"
    suspicion: 2
  }

  actor GateCaptain is CityGuard {
    description: "The captain of the gate guards"
    suspicion: 4
  }
}
```

### Explicit actor példányosítás

Ez akkor hasznos, ha egy konkrét actorra külön névvel kell hivatkozni a játék logikájában vagy a narrációban:

```dsl
guard1 as CityGuard
captain as GateCaptain
```

---

## 2.4. `vars`

A `vars` blokk változókat definiál.

A változók a játék állapotának tárolására szolgálnak, például:

- életerő
- gyanú szint
- számlálók
- logikai jelzők
- haladási állapotok

### Példa

```dsl
vars {
  hp: 10
  suspicion: 0
  hasKey: false
}
```

---

## 2.5. `start`

A `start` blokk a játék kezdeti állapotát definiálja.

Ebben adhatók meg például:

- a kezdő itemek
- a kezdő mennyiségek stackelhető itemekből
- nem stackelhető itemek kezdeti példányai
- actor példányok
- egyéb induló állapotmódosítások

A `start` blokk célja, hogy egy helyre gyűjtse a játék indulásakor érvényes kezdeti beállításokat.

A nem stackelhető itemek átadásakor a rendszer a háttérben példányt hoz létre. Szükség esetén külön névvel ellátott példányok is használhatók.

### Példa — stackelhető és nem stackelhető itemek

```dsl
start {
  give 2 HealingPotion
  give RustyKey
}
```

### Példa — explicit item és actor példányok

```dsl
start {
  oldKey as RustyKey
  guard1 as CityGuard

  give oldKey
}
```

---

## 2.6. `rules`

A `rules` blokk újrahasznosítható játékszabályokat definiál.

Egy rule:

- rendelkezik névvel
- rendelkezhet paraméterekkel
- opcionálisan tartalmazhat `requires` blokkot
- kötelezően tartalmaz `success` blokkot
- kötelezően tartalmaz `fail` blokkot

A rule egy újrahasznosítható műveletként működik. Nem függvény, tehát nincs visszatérési értéke.

### Példa — paraméter nélküli rule

```dsl
rules {
  rule HealIfNeeded chance 70 {
    requires {
      has HealingPotion and hp is less than 6
    }
    success {
      consume 1 HealingPotion
      add HealingPotion.healAmount to hp
      narrate "You recover some strength."
    }
    fail {
      narrate "You cannot heal right now."
    }
  }
}
```

### Példa — paraméteres rule

```dsl
rules {
  rule GiveReward(rewardItem, amount) chance 100 {
    success {
      give amount rewardItem
      narrate "You received a reward."
    }
    fail {
      narrate "You received nothing."
    }
  }
}
```

### Rule meghívása

```dsl
use HealIfNeeded
use GiveReward(HealingPotion, 2)
```

---

## 2.7. Nyelvi konvenciók

A DSL a programozási operátorok helyett olvashatóbb, domain-specifikus megfogalmazást használ.

> A nyelvben a sorvégi pontosvesszők nem használatosak.

### Logikai kapcsolatok

- `and`
- `or`

### Megléti vizsgálat

- `has itemId`
- `dont have itemId`

### Összehasonlítások

| Kifejezés         | Jelentés      |
|-------------------|---------------|
| `is less than`    | kisebb mint   |
| `is greater than` | nagyobb mint  |
| `is equal to`     | egyenlő       |
| `is not equal to` | nem egyenlő   |
| `is at least`     | legalább      |
| `is at most`      | legfeljebb    |

### Példák feltételekre

```
has HealingPotion
dont have RustyKey
hp is less than 6
gold is greater than 10
state is equal to "alert"
state is not equal to "alert"
HealingPotion.count is at least 2
has HealingPotion and hp is less than 6
```

### Műveletek

| Művelet               | Példa                                          |
|-----------------------|------------------------------------------------|
| `give`                | `give 1 HealingPotion`                         |
| `consume`             | `consume 1 HealingPotion`                      |
| `add ... to ...`      | `add 4 to hp`                                  |
| `subtract ... from ...` | `subtract 2 from hp`                         |
| `multiply ... by ...` | `multiply gold by 2`                           |
| `divide ... by ...`   | `divide gold by 2`                             |
| `narrate`             | `narrate "The old lock clicks open."`          |
| `actorId says`        | `GateGuard says "No one enters without permission."` |
| `go to`                | `go to courtyard`                               |

---

# 3. A `world` blokk

A `world` blokk tartalmazza a játék konkrét definícióját.

Minden RPG DSL fájl pontosan egy `world` blokkot tartalmaz.  
Ebben helyezkednek el a játék jelenetei, valamint az azok közötti átmeneteket és interakciókat meghatározó elemek.

A `world` blokk a globális top-level definíciókra épül, például:

- itemekre
- actorokra
- globális változókra
- szabályokra
- kezdő állapotra

---

## 3.1. A `world` blokk szerepe

A `world` blokk írja le azt a bejárható világot, amelyben a játékos mozog.

Ez tartalmazza:

- a jeleneteket
- a jelenetekhez tartozó leírásokat
- az elérhető akciókat
- a jeleneteken belüli belépési és kilépési logikát
- a jelenetek közötti átmeneteket

### Példa

```dsl
world {
  scenes {
    scene gate {
      description: "Two guards block the gate at dusk."

      intents {
        intent talk {
          description: "Speak to the guards"
        }
      }

      on talk chance 60 {
        success {
          narrate "The guards listen."
        }
        fail {
          narrate "They ignore you."
        }
      }
    }
  }
}
```

---

## 3.2. A `world` blokk felépítése

A `world` blokk jelenleg egy `scenes` blokkot tartalmaz.

```dsl
world {
  scenes {
    ...
  }
}
```

A `scenes` blokk a játék összes `scene`-jét tartalmazza.

---

## 3.3. A `scenes` blokk

A `scenes` blokk `scene` deklarációkat tartalmaz.

Minden scene egy helyszínt, állapotot vagy narratív szituációt reprezentál, ahol a játékos valamilyen döntést vagy akciót hajthat végre.

Egy scene tipikusan a következő elemekből áll:

- név
- `description`
- opcionális `present`
- opcionális `on enter`
- opcionális `on exit`
- opcionális `intents`
- 0 vagy több `on` blokk
- opcionális `end scene` jelölés

---

## 3.4. `scene`

A `scene` a játékvilág egy konkrét jelenetét írja le.

Ez lehet például:

- egy helyszín
- egy találkozás
- egy konfliktushelyzet
- egy átvezető állapot
- egy befejező jelenet

Minden scene rendelkezik névvel és kötelező `description` mezővel.

### Példa

```dsl
scene alley {
  description: "A narrow alley behind the gate."
}
```

---

## 3.5. `description`

A `description` mező írja le a scene alaphelyzetét.

Ez a leírás kontextust ad:

- a játékos számára
- az AI narráció számára
- a scene hangulatának és helyzetének értelmezéséhez

### Példa

```dsl
scene courtyard {
  description: "A quiet courtyard lit by pale lanterns."
}
```

---

## 3.6. `present`

A `present` blokk felsorolja azokat az actorokat, amelyek az adott scene-ben jelen vannak.

Ez a blokk segít meghatározni, hogy:

- kik vannak jelen a jelenetben
- kik szólalhatnak meg
- kikre lehet hivatkozni a scene logikájában
- kik adnak narratív kontextust az AI számára

A `present` blokkban actor típusok és explicit actor példányok is szerepelhetnek.

### Példa

```dsl
scene gate {
  description: "Two guards block the gate at dusk."

  present {
    CityGuard
    guard1
    captain
  }
}
```

> Ha egy actor nincs explicit példányként megnevezve, a rendszer a háttérben létrehozhat egy implicit példányt.

A `present` blokkban szereplő actorok használhatják a `says` szerkezetet. Ez lehetővé teszi, hogy a scene-ben jelen lévő szereplők közvetlen megszólalásai elkülönüljenek a sima narrációtól.

### Példa — beszélő actor a jelenetben

```dsl
scene gate {
  description: "Two guards block the gate at dusk."

  present {
    guard1
  }

  on enter {
    guard1 says "State your business."
  }
}
```

---

## 3.7. `on enter`

Az `on enter` blokk a scene-be való belépéskor fut le.

Ez használható például:

- belépési narrációra
- állapotmódosításra
- automatikus eseményekre
- belépéskori szabályok alkalmazására

### Példa

```dsl
scene gate {
  description: "Two guards block the gate at dusk."

  on enter {
    narrate "The guards turn toward you as you approach."
    add 1 to suspicion
  }
}
```

---

## 3.8. `on exit`

Az `on exit` blokk a scene elhagyásakor fut le.

Ez használható például:

- kilépési narrációra
- állapotmentésre
- változók módosítására
- következmények rögzítésére

### Példa

```dsl
scene alley {
  description: "A narrow alley behind the gate."

  on exit {
    narrate "You leave the alley behind."
  }
}
```

---

## 3.9. `intents`

Az `intents` blokk a scene-ben elérhető játékosi szándékokat írja le.

Az intentek elsődleges szerepe, hogy megadják:

- milyen típusú cselekvés próbálható meg
- milyen narratív szándékkal történik az akció
- milyen kontextust kapjon az AI az akció értelmezéséhez

Minden intent rendelkezik névvel és `description` mezővel.

### Példa

```dsl
intents {
  intent persuade_guard {
    description: "Talk your way past the guards"
  }

  intent bribe_guard {
    description: "Offer coins in exchange for passage"
  }
}
```

---

## 3.10. `on <intent> chance <n>`

Az `on` blokk definiálja, hogy egy adott intent hogyan oldódik fel az adott scene-ben.

Ez a nyelv egyik központi eleme.

Az `on` blokk tartalmazza:

- a cél intent nevét
- egy sikeresélyt
- opcionális `requires` blokkot
- kötelező `success` blokkot
- kötelező `fail` blokkot

A `chance` értéke százalékos sikeresélyt jelent.

### Példa

```dsl
scene gate {
  description: "Two guards block the gate at dusk."

  intents {
    intent talk {
      description: "Speak to the guards"
    }
  }

  on talk chance 60 {
    success {
      narrate "The guards listen."
    }
    fail {
      narrate "They ignore you."
    }
  }
}
```

---

## 3.11. `requires`

A `requires` blokk egy előfeltételt ír le.

Ha a feltétel teljesül, az adott intent vagy rule a `success` ág felé folytatódhat.  
Ha a feltétel nem teljesül, a végrehajtás a `fail` blokkba kerül.

A feltételek a DSL nyelvi konvencióit használják.

### Példa

```dsl
requires {
  has HealingPotion and hp is less than 6
}
```

---

## 3.12. `success`

A `success` blokk azokat a műveleteket tartalmazza, amelyek akkor hajtódnak végre, ha az adott akció vagy szabály sikeres.

### Példa

```dsl
success {
  narrate "The door opens."
  go to courtyard
}
```

---

## 3.13. `fail`

A `fail` blokk azokat a műveleteket tartalmazza, amelyek akkor hajtódnak végre, ha az adott akció vagy szabály sikertelen, vagy az előfeltétele nem teljesül.

### Példa

```dsl
fail {
  narrate "The guard refuses to move."
  add 1 to suspicion
}
```

---

## 3.14. Actor megszólalások a `says` szerkezettel

Az `actorId says "..."` forma arra szolgál, hogy egy actor közvetlenül megszólaljon a narrációban.

A `says` szerkezet actorokra használható, és egy beszédaktust jelöl — ilyenkor a megadott actor mondja ki az idézett szöveget.

A `says` használható:

- explicit actor példánnyal
- implicit actor példánnyal, ha az adott actor jelen van a scene-ben

A `says` bal oldalán actorra kell hivatkozni. Ez lehet egy actor példány neve, vagy egy actor típus neve.

### Példa — explicit actor példány

```dsl
guard1 says "No one enters without permission."
```

### Példa — implicit actor megszólalás

```dsl
CityGuard says "Move along."
```

Ebben az esetben a rendszer a háttérben egy megfelelő actor példányhoz köti a megszólalást.

### Kapcsolat a `narrate` művelettel

A `narrate` és a `says` eltérő szerepet tölt be:

- `narrate "..."` → leíró vagy elbeszélő szöveg
- `actorId says "..."` → egy actor konkrét megszólalása

```dsl
narrate "The guard steps closer."
guard1 says "State your business."
```

---

## 3.15. `end scene`

Az `end scene` jelölés azt mutatja, hogy az adott scene lezáró jelenet.

Ez olyan jelenetnél használható, amely a játék befejezését jelenti, és amely után nincs további folytatás.

### Példa

```dsl
scene ending {
  description: "The kingdom falls silent around you."
  end scene
}
```

---

## 3.16. Összetett példa

```dsl
world {
  scenes {
    scene gate {
      description: "Two guards block the gate at dusk."

      present {
        guard1
        captain
      }

      on enter {
        narrate "The guards watch you carefully."
        guard1 says "No one enters without permission."
      }

      intents {
        intent talk {
          description: "Speak to the guards"
        }

        intent bribe {
          description: "Offer coins for passage"
        }
      }

      on talk chance 60 {
        success {
          captain says "We may hear you out."
        }
        fail {
          narrate "The guards ignore you."
        }
      }

      on bribe chance 75 {
        requires {
          gold is at least 5
        }
        success {
          subtract 5 from gold
          captain says "Be quick about it."
          go to courtyard
        }
        fail {
          guard1 says "You insult us."
          add 1 to suspicion
        }
      }

      on exit {
        narrate "You pass beyond the gate."
      }
    }

    scene ending {
      description: "You disappear into the dark city."
      end scene
    }
  }
}
```
