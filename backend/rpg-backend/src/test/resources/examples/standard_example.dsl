initPrompt {
  "This world is a grim medieval town on the edge of collapse. The tone is suspicious, intimate, and heavy with hidden motives. Narration should emphasize social tension, uncertainty, and moral compromise."
}

items {
  item RustyKey {
    description: "An old key from a cellar lock"
    rarity: "Common"
  }

  item SmugglerSeal {
    description: "A carved token used by black market couriers"
    rarity: "Rare"
  }

  item ShrineCharm {
    description: "A wooden charm blessed by a local priest"
    rarity: "Uncommon"
    faith: 2
  }

  item Weapon {
    description: "A basic weapon"
    damage: 1
    rarity: "Common"
  }

  item Knife is Weapon {
    description: "A narrow knife with a wrapped handle"
    damage: 3
    rarity: "Common"
  }

  stackable item GoldCoin {
    description: "A worn coin used in local trade"
    value: 1
    rarity: "Common"
  }

  stackable item HealingPotion {
    description: "A medicinal tonic that restores health"
    healAmount: 4
    rarity: "Common"
  }

  stackable item EvidenceNote {
    description: "A written clue that may reveal hidden crimes"
    weight: 1
    rarity: "Uncommon"
  }
}

actors {
  actor CityGuard {
    description: "A member of the town watch"
    faction: "Town Watch"
    suspicion: 2
  }

  actor GateCaptain is CityGuard {
    description: "The officer in charge of the north gate"
    suspicion: 4
  }

  actor Priest {
    description: "A weary servant of the shrine"
    faction: "Shrine"
    trust: 2
  }

  actor Smuggler {
    description: "A shadowy courier working the alleys"
    faction: "Black Market"
    trust: 1
  }

  actor Beggar {
    description: "A hungry local who sees more than people think"
    faction: "Streets"
    insight: 3
  }
}

vars {
  hp: 8
  suspicion: 0
  faith: 0
  trust: 0
  clueCount: 0
  cellarOpened: false
  shrineVisited: false
  smugglersKnown: false
  endingState: "none"
}

start {
  captain as GateCaptain
  priest1 as Priest
  beggar1 as Beggar
  smuggler1 as Smuggler

  give 5 GoldCoin
  give 1 HealingPotion
}

rules {
  rule PrayAtShrine chance 100 {
    success {
      add 1 to faith
      narrate "You bow your head and whisper a quiet prayer."
      random 40 {
        narrate "For a brief moment, the silence around you feels merciful."
      }
    }
    fail {
      narrate "Your prayer goes unanswered."
    }
  }

  rule HealIfNeeded chance 100 {
    requires {
      has HealingPotion and hp is less than 8
    }
    success {
      consume 1 HealingPotion
      add HealingPotion.healAmount to hp
      narrate "Warm relief spreads through your body."
    }
    fail {
      narrate "You cannot heal right now."
    }
  }

  rule GainEvidence chance 100 {
    success {
      give 1 EvidenceNote
      add 1 to clueCount
      narrate "You secure a piece of evidence."
    }
    fail {
      narrate "You fail to learn anything useful."
    }
  }

  rule PayCoins(amount) chance 100 {
    requires {
      GoldCoin.count is at least amount
    }
    success {
      consume amount GoldCoin
      narrate "You hand over the coins."
    }
    fail {
      narrate "You do not have enough coins."
    }
  }
}

world {
  scenes {
    scene north_gate {
      description: "A heavy timber gate marks the northern entrance to the town."

      present {
        captain
      }

      on enter {
        narrate "The walls cast long shadows over the muddy road."
        captain says "Travelers are questioned after sunset."
      }

      intents {
        intent persuade {
          description: "Try to gain entry through calm conversation"
        }

        intent bribe {
          description: "Offer money for faster passage"
        }

        intent leave {
          description: "Step away from the gate and search elsewhere"
        }
      }

      on persuade chance 60 {
        success {
          captain says "You seem harmless enough. Move along."
          go to market_square
        }
        fail {
          captain says "I do not trust your face."
          add 1 to suspicion
        }
      }

      on bribe chance 75 {
        requires {
          GoldCoin.count is at least 3
        }
        success {
          consume 3 GoldCoin
          captain says "I saw nothing. Enter quickly."
          add 1 to suspicion
          go to market_square
        }
        fail {
          captain says "Trying to bribe an officer is a poor first impression."
          add 2 to suspicion
        }
      }

      on leave chance 100 {
        success {
          narrate "You turn away from the gate and circle toward the alleys."
          go to alley
        }
        fail {
          narrate "You remain at the gate."
        }
      }
    }

    scene market_square {
      description: "A cramped square filled with shuttered stalls and suspicious lantern light."

      present {
        beggar1
      }

      on enter {
        narrate "The town feels awake in all the wrong ways."
        random 35 {
          narrate "You hear a distant argument behind a closed shop."
        }
      }

      intents {
        intent talk_beggar {
          description: "Speak with the beggar near the dry fountain"
        }

        intent visit_shrine {
          description: "Walk to the old shrine at the eastern edge"
        }

        intent search_square {
          description: "Look around the market for clues"
        }

        intent heal {
          description: "Use a healing potion if needed"
        }
      }

      on talk_beggar chance 70 {
        success {
          beggar1 says "Coin first, words second."
          use PayCoins(1)
          beggar1 says "There is movement beneath the cooper's house. Cellar door. Late at night."
          add 1 to trust
          add 1 to clueCount
          narrate "You remember the location he described."
        }
        fail {
          beggar1 says "No charity, no memory."
        }
      }

      on visit_shrine chance 100 {
        success {
          go to shrine
        }
        fail {
          narrate "You stay in the square."
        }
      }

      on search_square chance 55 {
        success {
          use GainEvidence
          narrate "A torn ledger scrap suggests illegal deliveries."
        }
        fail {
          narrate "You find only mud, broken wood, and old footprints."
        }
      }

      on heal chance 100 {
        success {
          use HealIfNeeded
        }
        fail {
          narrate "Nothing changes."
        }
      }
    }

    scene shrine {
      description: "A neglected stone shrine stands in a narrow yard behind iron fencing."

      present {
        priest1
      }

      on enter {
        narrate "Wax smoke and damp stone fill the air."
        priest1 says "Few come here unless they are desperate."
        add 1 to faith
      }

      intents {
        intent pray {
          description: "Pray in silence before the shrine"
        }

        intent ask_priest {
          description: "Ask the priest about unrest in town"
        }

        intent leave_shrine {
          description: "Return to the market square"
        }
      }

      on pray chance 100 {
        success {
          use PrayAtShrine
        }
        fail {
          narrate "You cannot focus enough to pray."
        }
      }

      on ask_priest chance 65 {
        success {
          priest1 says "The town rots from beneath, not above."
          priest1 says "If you seek the truth, look where commerce avoids sunlight."
          add 1 to trust
        }
        fail {
          priest1 says "I cannot help you."
        }
      }

      on leave_shrine chance 100 {
        success {
          go to market_square
        }
        fail {
          narrate "You remain in the shrine yard."
        }
      }
    }

    scene alley {
      description: "A damp side alley runs behind several locked homes and storage buildings."

      present {
        smuggler1
      }

      on enter {
        narrate "Water drips from broken gutters into black puddles."
        random 30 {
          smuggler1 says "You are not supposed to be here."
        }
      }

      intents {
        intent follow_clue {
          description: "Search for the cellar mentioned in whispers"
        }

        intent approach_smuggler {
          description: "Speak to the shadowy figure in the alley"
        }

        intent return_square {
          description: "Go back to the market square"
        }
      }

      on follow_clue chance 60 {
        success {
          narrate "You find a hidden cellar door beneath stacked crates."
          go to cellar
        }
        fail {
          narrate "You search the alley but find nothing certain."
        }
      }

      on approach_smuggler chance 70 {
        success {
          smuggler1 says "You know enough to be dangerous."
          smuggler1 says "Bring me proof that the watch is involved, and I may help you."
        }
        fail {
          smuggler1 says "Keep walking."
        }
      }

      on return_square chance 100 {
        success {
          go to market_square
        }
        fail {
          narrate "You stay in the alley."
        }
      }
    }

    scene cellar {
      description: "A low underground chamber holds crates, ledgers, and the smell of wet rope."

      on enter {
        narrate "You descend into darkness beneath the cooper's house."
        random 40 {
          narrate "You hear movement above, then silence."
        }
      }

      intents {
        intent inspect_crates {
          description: "Examine the marked cargo crates"
        }

        intent read_ledger {
          description: "Study the smuggling ledger"
        }

        intent escape {
          description: "Leave before someone finds you"
        }
      }

      on inspect_crates chance 75 {
        success {
          narrate "You discover contraband packed for guarded transport."
          use GainEvidence
        }
        fail {
          narrate "Most of the crates are sealed too tightly to inspect."
        }
      }

      on read_ledger chance 80 {
        success {
          narrate "The ledger names paid routes and watch patrol timings."
          add 2 to clueCount
        }
        fail {
          narrate "Moisture has ruined too many pages."
        }
      }

      on escape chance 100 {
        success {
          go to decision
        }
        fail {
          narrate "You hesitate in the dark."
        }
      }
    }

    scene decision {
      description: "With truth in hand, you must decide who will receive it."

      intents {
        intent report_to_priest {
          description: "Bring the evidence to the priest"
        }

        intent sell_to_smuggler {
          description: "Sell what you learned to the smugglers"
        }

        intent disappear {
          description: "Walk away from the town and its corruption"
        }
      }

      on report_to_priest chance 70 {
        requires {
          clueCount is at least 2
        }
        success {
          go to ending_justice
        }
        fail {
          narrate "You lack enough proof to convince anyone righteous."
        }
      }

      on sell_to_smuggler chance 75 {
        requires {
          clueCount is at least 2
        }
        success {
          give 5 GoldCoin
          go to ending_profit
        }
        fail {
          narrate "You do not have enough leverage to bargain."
        }
      }

      on disappear chance 100 {
        success {
          go to ending_escape
        }
        fail {
          narrate "You cannot bring yourself to leave yet."
        }
      }
    }

    scene ending_justice {
      description: "The truth spreads quietly, and the town begins to turn on its hidden masters."

      on enter {
        narrate "You choose conscience over safety."
      }

      end scene
    }

    scene ending_profit {
      description: "You trade truth for survival and leave heavier in coin than in spirit."

      on enter {
        narrate "The smugglers pay well, and the town remains what it was."
      }

      end scene
    }

    scene ending_escape {
      description: "You leave before dawn, carrying only memory and unease."

      on enter {
        narrate "Some places consume whoever tries to save them."
      }

      end scene
    }
  }
}