initPrompt {
  "A small dark fantasy scene. The tone is tense and quiet. The narration should feel atmospheric and uncertain."
}

items {
  item RustyKey {
    description: "An old iron key covered in rust"
    rarity: "Common"
  }

  stackable item HealingPotion {
    description: "A weak healing tonic"
    healAmount: 3
    rarity: "Common"
  }
}

actors {
  actor GateGuard {
    description: "A tired but alert guard"
    faction: "Watch"
    suspicion: 1
  }
}

vars {
  hp: 5
  suspicion: 0
  gold: 2
}

start {
  guard1 as GateGuard
  give 1 HealingPotion
}

rules {
  rule HealIfNeeded chance 100 {
    requires {
      has HealingPotion and hp is less than 6
    }
    success {
      consume 1 HealingPotion
      add HealingPotion.healAmount to hp
      narrate "You quietly drink the potion and feel a little stronger."
    }
    fail {
      narrate "You cannot heal right now."
    }
  }
}

world {
  scenes {
    scene gate {
      description: "A wooden gate blocks the road into the village."

      present {
        guard1
      }

      on enter {
        narrate "Cold wind moves through the empty road."
        guard1 says "State your business."
      }

      intents {
        intent talk {
          description: "Try to convince the guard to let you pass"
        }

        intent heal {
          description: "Drink your potion before speaking"
        }
      }

      on heal chance 100 {
        success {
          use HealIfNeeded
        }
        fail {
          narrate "Nothing happens."
        }
      }

      on talk chance 60 {
        success {
          guard1 says "Very well. You may enter."
          go to village
        }
        fail {
          guard1 says "No. Not tonight."
          add 1 to suspicion
        }
      }
    }

    scene village {
      description: "The village is silent, lit by weak lanterns."
      end scene
    }
  }
}