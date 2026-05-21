initPrompt {
  "This world is a decaying frontier city called Ashenfall, built around buried ruins and governed by fear, commerce, and old superstition.
  The tone is dark, investigative, oppressive, and morally ambiguous.
  Responses should emphasize consequence, scarcity, tension between factions, and the unsettling sense that ancient forces are still watching."
}

items {
  item Weapon {
    description: "A basic weapon"
    damage: 1
    rarity: "Common"
  }

  item RitualItem {
    description: "A base item used in forbidden rites"
    danger: 1
    rarity: "Uncommon"
  }

  item RustKnife is Weapon {
    description: "A chipped iron knife"
    damage: 2
    rarity: "Common"
  }

  item GuardSpear is Weapon {
    description: "A long spear used by city guards"
    damage: 4
    rarity: "Common"
  }

  item FineSword is Weapon {
    description: "A polished officer's sword"
    damage: 6
    rarity: "Rare"
  }

  item ShrineCharm {
    description: "A small charm blessed against evil"
    ward: 2
    rarity: "Uncommon"
  }

  item SmugglerSeal {
    description: "A carved token that grants passage through illicit networks"
    rank: 1
    rarity: "Rare"
  }

  item ArchiveKey {
    description: "A brass key marked with the city archive crest"
    tier: 1
    rarity: "Rare"
  }

  item CellarKey {
    description: "A black iron key for hidden doors"
    tier: 1
    rarity: "Uncommon"
  }

  item MoonRelic is RitualItem {
    description: "A cold stone disk engraved with lunar geometry"
    danger: 5
    rarity: "Legendary"
  }

  item EmberRelic is RitualItem {
    description: "A cracked ember core that pulses with buried heat"
    danger: 4
    rarity: "Legendary"
  }

  item LedgerPage {
    description: "A page removed from a financial ledger"
    importance: 2
    rarity: "Uncommon"
  }

  item WatchBadge {
    description: "A bronze insignia of the city watch"
    authority: 2
    rarity: "Common"
  }

  item NobleLetter {
    description: "A sealed letter bearing aristocratic wax"
    influence: 3
    rarity: "Rare"
  }

  item SewerMap {
    description: "A partial map of forgotten tunnels"
    utility: 3
    rarity: "Uncommon"
  }

  stackable item GoldCoin {
    description: "Currency used across Ashenfall"
    value: 1
    rarity: "Common"
  }

  stackable item HealingPotion {
    description: "A restorative tonic"
    healAmount: 4
    rarity: "Common"
  }

  stackable item SmokeBomb {
    description: "A clay sphere that bursts into smoke"
    escapePower: 2
    rarity: "Uncommon"
  }

  stackable item EvidenceNote {
    description: "A note, testimony, or clue relevant to hidden crimes"
    weight: 1
    rarity: "Common"
  }

  stackable item RitualDust {
    description: "A glittering dust used in occult rites"
    danger: 2
    rarity: "Rare"
  }

  stackable item Ration {
    description: "A preserved food bundle"
    nutrition: 1
    rarity: "Common"
  }
}

actors {
  actor CityGuard {
    description: "A member of the city watch"
    faction: "Watch"
    suspicion: 2
    rank: 1
  }

  actor GateCaptain is CityGuard {
    description: "Commander of the northern gate"
    suspicion: 4
    rank: 3
  }

  actor WatchInspector is CityGuard {
    description: "An investigator tasked with internal discipline"
    suspicion: 3
    rank: 4
  }

  actor Smuggler {
    description: "A broker of goods and secrets"
    faction: "Black Market"
    trust: 1
    threat: 2
  }

  actor Priest {
    description: "A guardian of public ritual and private doubt"
    faction: "Shrine"
    trust: 2
    fear: 1
  }

  actor Noble {
    description: "A well-dressed figure from the upper quarter"
    faction: "Nobility"
    influence: 4
    cruelty: 2
  }

  actor Scholar {
    description: "A keeper of hidden and forbidden knowledge"
    faction: "Archive"
    knowledge: 5
    caution: 3
  }

  actor Beggar {
    description: "A forgotten witness of the streets"
    faction: "Streets"
    insight: 3
    desperation: 4
  }

  actor Cultist {
    description: "A hidden servant of ancient forces"
    faction: "Ash Moon"
    zeal: 4
    danger: 4
  }

  actor Ferryman {
    description: "A silent guide across the black canal"
    faction: "Canal"
    trust: 1
  }

  actor Merchant {
    description: "A trader whose loyalty follows profit"
    faction: "Market"
    greed: 3
  }
}

vars {
  hp: 10
  suspicion: 0
  faith: 0
  trust: 0
  fear: 0
  corruption: 0
  evidence: 0
  goldDebt: 0
  ritualKnowledge: 0
  archiveAccess: false
  sewerUnlocked: false
  shrineAligned: false
  smugglersKnown: false
  watchAllied: false
  nobleBlackmailed: false
  relicRecovered: false
  emberRecovered: false
  moonRecovered: false
  canalOpen: false
  curfewBroken: false
  cultAlert: false
  districtState: "tense"
  endingState: "none"
}

start {
  captain as GateCaptain
  inspector as WatchInspector
  priest1 as Priest
  smuggler1 as Smuggler
  beggar1 as Beggar
  scholar1 as Scholar
  noble1 as Noble
  ferryman1 as Ferryman
  merchant1 as Merchant
  cultist1 as Cultist
  cultist2 as Cultist

  give 8 GoldCoin
  give 2 HealingPotion
  give 1 Ration
  give RustKnife
}

rules {
  rule HealIfNeeded chance 100 {
    requires {
      has HealingPotion and hp is less than 10
    }
    success {
      consume 1 HealingPotion
      add HealingPotion.healAmount to hp
      narrate "You steady yourself with bitter medicine."
    }
    fail {
      narrate "You cannot heal right now."
    }
  }

  rule SpendCoins(amount) chance 100 {
    requires {
      GoldCoin.count is at least amount
    }
    success {
      consume amount GoldCoin
      narrate "Coins change hands in the dark."
    }
    fail {
      narrate "You do not have enough coins."
    }
  }

  rule GainEvidence chance 100 {
    success {
      give 1 EvidenceNote
      add 1 to evidence
      narrate "You secure another piece of the truth."
    }
    fail {
      narrate "You fail to gather anything useful."
    }
  }

  rule IncreaseSuspicion(amount) chance 100 {
    success {
      add amount to suspicion
      narrate "Eyes linger on you longer than before."
    }
    fail {
      narrate "Nothing changes."
    }
  }

  rule LowerSuspicion(amount) chance 100 {
    success {
      subtract amount from suspicion
      narrate "For the moment, attention drifts elsewhere."
    }
    fail {
      narrate "Nothing changes."
    }
  }

  rule PrayAtShrine chance 100 {
    success {
      add 1 to faith
      narrate "You kneel in the old silence and offer a careful prayer."
      random 30 {
        narrate "The candle flame bends though no wind is present."
      }
    }
    fail {
      narrate "The words die in your throat."
    }
  }

  rule DiscoverSmugglerNetwork chance 100 {
    success {
      add 1 to trust
      narrate "You begin to understand how contraband moves through Ashenfall."
    }
    fail {
      narrate "You fail to connect the threads."
    }
  }

  rule GainArchiveAccess chance 100 {
    success {
      narrate "You obtain a path into restricted records."
    }
    fail {
      narrate "The archives remain closed to you."
    }
  }

  rule LearnRitual chance 100 {
    success {
      add 1 to ritualKnowledge
      narrate "You uncover another fragment of forgotten rite and warning."
    }
    fail {
      narrate "The meaning escapes you."
    }
  }

  rule BreakCurfew chance 100 {
    success {
      add 1 to suspicion
      narrate "You move through districts where you should not be seen."
    }
    fail {
      narrate "You remain within the law."
    }
  }

  rule RecoverMoonRelic chance 100 {
    success {
      give MoonRelic
      narrate "The relic is colder than stone should be."
    }
    fail {
      narrate "The relic remains beyond your reach."
    }
  }

  rule RecoverEmberRelic chance 100 {
    success {
      give EmberRelic
      narrate "Heat pulses through the relic like a hidden heart."
    }
    fail {
      narrate "You fail to recover the ember relic."
    }
  }

  rule OpenSewerPath chance 100 {
    success {
      narrate "A hidden route into the undercity becomes available."
    }
    fail {
      narrate "The way remains shut."
    }
  }

  rule FeedBeggar chance 100 {
    requires {
      has Ration
    }
    success {
      consume 1 Ration
      add 1 to trust
      narrate "The beggar accepts the ration. Kindness is rare here. He will remember."
    }
    fail {
      narrate "Mercy cannot be promised on an empty hand."
    }
  }

  rule BribeWatch chance 100 {
    requires {
      GoldCoin.count is at least 4
    }
    success {
      consume 4 GoldCoin
      add 1 to corruption
      narrate "The watchman pretends not to notice you."
    }
    fail {
      narrate "Your attempt fails before it begins."
    }
  }

  rule ExposeNoble chance 100 {
    requires {
      evidence is at least 4
    }
    success {
      narrate "You gather enough proof to corner the noble house."
    }
    fail {
      narrate "You still lack the leverage to strike."
    }
  }

  rule JoinWatch chance 100 {
    success {
      add 1 to trust
      narrate "The watch begins to treat you as an asset rather than a problem."
    }
    fail {
      narrate "The offer collapses."
    }
  }

  rule InvokeWard chance 100 {
    requires {
      has ShrineCharm
    }
    success {
      subtract 1 from fear
      narrate "The charm steadies the air around you."
    }
    fail {
      narrate "You have no ward against what surrounds you."
    }
  }
}

world {
  scenes {
    scene north_gate {
      description: "The northern gate rises above the road in iron and black timber, guarded under lantern light."

      present {
        captain
      }

      on enter {
        narrate "Rainwater collects in the ruts before the gate."
        captain says "Ashenfall closes early. Speak clearly."
      }

      intents {
        intent persuade_entry {
          description: "Talk your way into the city"
        }

        intent bribe_entry {
          description: "Offer coins for quiet entry"
        }

        intent sneak_away {
          description: "Avoid the gate and slip toward the side streets"
        }
      }

      on persuade_entry chance 65 {
        success {
          captain says "You may pass, but trouble follows those who arrive late."
          go to market_square
        }
        fail {
          captain says "Not tonight."
          use IncreaseSuspicion(1)
        }
      }

      on bribe_entry chance 75 {
        requires {
          GoldCoin.count is at least 3
        }
        success {
          consume 3 GoldCoin
          add 1 to corruption
          captain says "I did not see you."
          go to market_square
        }
        fail {
          captain says "You insult this office."
          use IncreaseSuspicion(2)
        }
      }

      on sneak_away chance 100 {
        success {
          narrate "You leave the road and disappear into the outer alleys."
          go to outer_alley
        }
        fail {
          narrate "You remain before the gate."
        }
      }
    }

    scene market_square {
      description: "The market square is cramped with shuttered stalls, gutter water, and people who speak only when safe."

      present {
        beggar1
        merchant1
      }

      on enter {
        narrate "Lanterns burn low over abandoned trade."
        random 30 {
          merchant1 says "Business dies when fear grows."
        }
      }

      intents {
        intent speak_beggar {
          description: "Speak with the beggar by the dry fountain"
        }

        intent feed_beggar {
          description: "Offer food to the beggar"
        }

        intent inspect_stalls {
          description: "Search the market for traces of illegal trade"
        }

        intent visit_shrine {
          description: "Go to the shrine quarter"
        }

        intent go_archive {
          description: "Head toward the city archive"
        }

        intent heal {
          description: "Use a potion if wounded"
        }
      }

      on speak_beggar chance 70 {
        success {
          beggar1 says "The city guard and the smugglers both bury their sins underground."
          beggar1 says "The cooper's lane hides one door. The archive hides another."
          add 1 to trust
          add 1 to evidence
        }
        fail {
          beggar1 says "I have no words for empty pockets."
        }
      }

      on feed_beggar chance 100 {
        success {
          use FeedBeggar
        }
        fail {
          beggar1 says "You do not feed me."
        }
      }

      on inspect_stalls chance 60 {
        success {
          narrate "You find coded shipment marks burned beneath a merchant table."
          use GainEvidence
          use DiscoverSmugglerNetwork
        }
        fail {
          narrate "You find only spoiled grain and damp wood."
        }
      }

      on visit_shrine chance 100 {
        success {
          go to shrine_yard
        }
        fail {
          narrate "You stay in the square."
        }
      }

      on go_archive chance 100 {
        success {
          go to archive_steps
        }
        fail {
          narrate "You do not leave the square."
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

    scene shrine_yard {
      description: "A narrow stone yard surrounds the old shrine, where wax smoke and damp prayer cling to the air."

      present {
        priest1
      }

      on enter {
        narrate "Bells do not ring here anymore."
        priest1 says "Some still kneel. Fewer still listen."
      }

      intents {
        intent pray {
          description: "Offer a prayer at the worn altar"
        }

        intent ask_priest {
          description: "Ask the priest about unrest and corruption"
        }

        intent request_charm {
          description: "Ask for a protective charm"
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
          priest1 says "The city suffers not only from greed but from old hunger."
          priest1 says "If ruins wake below us, wealth and law alike will kneel."
          use LearnRitual
          add 1 to faith
        }
        fail {
          priest1 says "I cannot safely speak of that."
        }
      }

      on request_charm chance 70 {
        requires {
          faith is at least 1
        }
        success {
          give ShrineCharm
          priest1 says "Carry this. It may not save you, but it may warn you."
        }
        fail {
          priest1 says "You ask for blessing without devotion."
        }
      }

      on leave_shrine chance 100 {
        success {
          go to market_square
        }
        fail {
          narrate "You remain by the shrine."
        }
      }
    }

    scene archive_steps {
      description: "Cold stairs rise toward the city archive, where records are guarded as tightly as treasure."

      present {
        scholar1
      }

      on enter {
        narrate "Dust gathers even on the outer stone, as if memory itself is unwelcome."
        scholar1 says "Records exist, but permission does not."
      }

      intents {
        intent persuade_scholar {
          description: "Convince the scholar to help you"
        }

        intent bribe_scholar {
          description: "Offer coins for access"
        }

        intent show_interest {
          description: "Discuss the buried ruins beneath the city"
        }

        intent enter_archive {
          description: "Attempt to enter the archive"
        }

        intent return_square {
          description: "Go back to the market square"
        }
      }

      on persuade_scholar chance 60 {
        success {
          scholar1 says "Curiosity is dangerous, but ignorance is worse."
          give ArchiveKey
          use GainArchiveAccess
        }
        fail {
          scholar1 says "You are not prepared for what lies in those records."
        }
      }

      on bribe_scholar chance 70 {
        requires {
          GoldCoin.count is at least 4
        }
        success {
          consume 4 GoldCoin
          scholar1 says "I was never here."
          give ArchiveKey
          use GainArchiveAccess
        }
        fail {
          scholar1 says "You lack either funds or subtlety."
        }
      }

      on show_interest chance 75 {
        success {
          scholar1 says "The ruins below Ashenfall predate our oldest histories."
          scholar1 says "Two relics are named in damaged records: moon and ember."
          use LearnRitual
          add 1 to trust
        }
        fail {
          scholar1 says "You ask the wrong questions too loudly."
          use IncreaseSuspicion(1)
        }
      }

      on enter_archive chance 80 {
        requires {
          has ArchiveKey or archiveAccess is equal to true
        }
        success {
          go to archive_hall
        }
        fail {
          narrate "The doors remain closed."
        }
      }

      on return_square chance 100 {
        success {
          go to market_square
        }
        fail {
          narrate "You stay on the archive steps."
        }
      }
    }

    scene archive_hall {
      description: "Tall shelves and chained cabinets fill the archive hall, where official lies and forbidden truths share dust."

      present {
        scholar1
      }

      on enter {
        narrate "The room smells of leather, mildew, and controlled panic."
      }

      intents {
        intent search_watch_records {
          description: "Look for evidence of corruption in the watch"
        }

        intent search_ruin_maps {
          description: "Search for maps of the undercity"
        }

        intent search_noble_accounts {
          description: "Search for links to noble financing"
        }

        intent leave_archive {
          description: "Leave the archive"
        }
      }

      on search_watch_records chance 70 {
        success {
          narrate "You find irregular patrol notes and unsigned confiscation logs."
          use GainEvidence
          use GainEvidence
        }
        fail {
          narrate "The files are incomplete or deliberately removed."
        }
      }

      on search_ruin_maps chance 75 {
        success {
          narrate "Hidden among old engineering plans is a marked tunnel grid."
          give SewerMap
          use OpenSewerPath
        }
        fail {
          narrate "The maps are too damaged to use."
        }
      }

      on search_noble_accounts chance 65 {
        success {
          narrate "A set of coded payments links the upper quarter to illegal excavation."
          give NobleLetter
          use GainEvidence
          use ExposeNoble
        }
        fail {
          narrate "You fail to connect names to numbers."
        }
      }

      on leave_archive chance 100 {
        success {
          go to archive_steps
        }
        fail {
          narrate "You remain among the shelves."
        }
      }
    }

    scene outer_alley {
      description: "A cramped lane outside the central district, lined with boarded doors and wet stone."

      present {
        smuggler1
      }

      on enter {
        narrate "The city sounds different here, as though official order ends one street behind you."
        random 40 {
          smuggler1 says "People who drift into this lane usually need something."
        }
      }

      intents {
        intent talk_smuggler {
          description: "Speak with the smuggler in the shadows"
        }

        intent buy_smoke {
          description: "Purchase smoke bombs"
        }

        intent ask_routes {
          description: "Ask about hidden movement through the city"
        }

        intent return_market {
          description: "Go to the market square"
        }
      }

      on talk_smuggler chance 75 {
        success {
          smuggler1 says "The watch takes bribes. The nobles take relics. We take risks."
          use DiscoverSmugglerNetwork
        }
        fail {
          smuggler1 says "I do not know you."
        }
      }

      on buy_smoke chance 80 {
        requires {
          GoldCoin.count is at least 2
        }
        success {
          consume 2 GoldCoin
          give 2 SmokeBomb
          smuggler1 says "Use them when law comes too close."
        }
        fail {
          smuggler1 says "Come back with coin."
        }
      }

      on ask_routes chance 70 {
        success {
          smuggler1 says "The sewers remember paths the city forgot."
          smuggler1 says "Find a map or a ferryman."
          use DiscoverSmugglerNetwork
        }
        fail {
          smuggler1 says "You are asking for routes without paying for trust."
        }
      }

      on return_market chance 100 {
        success {
          go to market_square
        }
        fail {
          narrate "You remain in the alley."
        }
      }
    }

    scene canal_dock {
      description: "A black canal cuts beneath the district, where a ferryman waits beside a half-rotten boat."

      present {
        ferryman1
      }

      on enter {
        narrate "The water reflects almost no light."
        ferryman1 says "I carry cargo, secrets, and the occasional fool."
      }

      intents {
        intent pay_passage {
          description: "Pay for passage to the lower district"
        }

        intent ask_ferryman {
          description: "Ask what moves along the canal at night"
        }

        intent return_alley {
          description: "Leave the dock"
        }
      }

      on pay_passage chance 85 {
        requires {
          GoldCoin.count is at least 2
        }
        success {
          consume 2 GoldCoin
          ferryman1 says "Keep your hands inside the boat and your prayers inside your teeth."
          go to lower_district
        }
        fail {
          ferryman1 says "No coin, no crossing."
        }
      }

      on ask_ferryman chance 70 {
        success {
          ferryman1 says "Crates. Corpses. Men with badges. Men without names."
          use GainEvidence
        }
        fail {
          ferryman1 says "I row. I do not remember."
        }
      }

      on return_alley chance 100 {
        success {
          go to outer_alley
        }
        fail {
          narrate "You remain at the dock."
        }
      }
    }

    scene lower_district {
      description: "Below the central wards, the lower district folds into tunnels, cellars, and forgotten masonry."

      on enter {
        narrate "The air turns colder and older as you descend."
        use BreakCurfew
      }

      intents {
        intent search_cellar {
          description: "Search the smuggling cellar"
        }

        intent enter_sewer {
          description: "Use maps or clues to enter the sewer path"
        }

        intent inspect_guard_cache {
          description: "Inspect signs of hidden watch activity"
        }

        intent leave_lower {
          description: "Return to the canal dock"
        }
      }

      on search_cellar chance 75 {
        success {
          go to smuggler_cellar
        }
        fail {
          narrate "The door remains hidden among old stone and clutter."
        }
      }

      on enter_sewer chance 80 {
        requires {
          has SewerMap or sewerUnlocked is equal to true
        }
        success {
          go to sewer_gate
        }
        fail {
          narrate "You cannot find a stable route below."
        }
      }

      on inspect_guard_cache chance 65 {
        success {
          narrate "You uncover contraband and sealed watch documentation in the same crate."
          give WatchBadge
          use GainEvidence
          use GainEvidence
        }
        fail {
          narrate "The trail is too well hidden."
        }
      }

      on leave_lower chance 100 {
        success {
          go to canal_dock
        }
        fail {
          narrate "You remain below."
        }
      }
    }

    scene smuggler_cellar {
      description: "A broad cellar hides ledgers, illicit goods, and symbols scratched into stone far older than the room above."

      present {
        smuggler1
      }

      on enter {
        narrate "Trade and ritual meet here in uneasy compromise."
      }

      intents {
        intent inspect_ledgers {
          description: "Study the smuggling ledgers"
        }

        intent confront_smuggler {
          description: "Demand answers from the smuggler"
        }

        intent search_symbols {
          description: "Examine the ancient symbols on the stone"
        }

        intent leave_cellar {
          description: "Return to the lower district"
        }
      }

      on inspect_ledgers chance 80 {
        success {
          narrate "Routes, names, and payments tie multiple factions together."
          give LedgerPage
          use GainEvidence
          use GainEvidence
        }
        fail {
          narrate "Too many pages are missing."
        }
      }

      on confront_smuggler chance 65 {
        success {
          smuggler1 says "The nobles fund digs. The watch hides the dead. The cult takes what the ruins wake."
          add 1 to trust
          add 1 to evidence
        }
        fail {
          smuggler1 says "You push too hard."
          use IncreaseSuspicion(1)
        }
      }

      on search_symbols chance 70 {
        success {
          narrate "The marks describe a moon chamber below the water line."
          use LearnRitual
          use GainEvidence
        }
        fail {
          narrate "The symbols remain half-legible and deeply unsettling."
          add 1 to fear
        }
      }

      on leave_cellar chance 100 {
        success {
          go to lower_district
        }
        fail {
          narrate "You remain in the cellar."
        }
      }
    }

    scene sewer_gate {
      description: "A rusted iron gate separates the city sewer from older tunnels cut into black stone."

      on enter {
        narrate "The masonry changes here. This place was old before Ashenfall had a name."
        random 30 {
          narrate "You hear chanting far below the sound of water."
        }
      }

      intents {
        intent force_gate {
          description: "Force the old gate open"
        }

        intent use_key {
          description: "Try a hidden key on the lock"
        }

        intent follow_chant {
          description: "Track the distant sound deeper below"
        }

        intent retreat {
          description: "Return to the lower district"
        }
      }

      on force_gate chance 55 {
        success {
          narrate "The gate tears free with a scream of metal."
          add 1 to suspicion
          go to ruin_threshold
        }
        fail {
          narrate "The gate refuses to yield."
        }
      }

      on use_key chance 70 {
        requires {
          has CellarKey or has ArchiveKey
        }
        success {
          narrate "The lock clicks with reluctant age."
          go to ruin_threshold
        }
        fail {
          narrate "You have no suitable key."
        }
      }

      on follow_chant chance 60 {
        success {
          go to cult_chamber
        }
        fail {
          narrate "The tunnels distort sound and direction."
        }
      }

      on retreat chance 100 {
        success {
          go to lower_district
        }
        fail {
          narrate "You hesitate at the threshold."
        }
      }
    }

    scene ruin_threshold {
      description: "Broken columns and geometric carvings mark the threshold of the buried ruins beneath the city."

      on enter {
        narrate "The air feels still in the way of sealed tombs and sleeping temples."
        add 1 to fear
      }

      intents {
        intent inspect_carvings {
          description: "Study the carvings for clues"
        }

        intent seek_moon_path {
          description: "Search for the moon relic chamber"
        }

        intent seek_ember_path {
          description: "Search for the ember relic chamber"
        }

        intent withdraw {
          description: "Leave the ruins"
        }
      }

      on inspect_carvings chance 75 {
        success {
          narrate "The carvings describe twin bindings: one of cold order, one of devouring heat."
          use LearnRitual
          use GainEvidence
        }
        fail {
          narrate "The symbols swim before your eyes without forming meaning."
        }
      }

      on seek_moon_path chance 70 {
        success {
          go to moon_chamber
        }
        fail {
          narrate "You fail to locate the moonward route."
        }
      }

      on seek_ember_path chance 70 {
        success {
          go to ember_chamber
        }
        fail {
          narrate "Collapsed stone blocks the way."
        }
      }

      on withdraw chance 100 {
        success {
          go to sewer_gate
        }
        fail {
          narrate "You remain among the ruins."
        }
      }
    }

    scene moon_chamber {
      description: "A circular chamber of pale stone reflects dim light in impossible ways, centered around a black altar."

      present {
        cultist1
      }

      on enter {
        narrate "The chamber is silent except for your breathing and someone else's."
        cultist1 says "You are late to the moon's witness."
      }

      intents {
        intent negotiate {
          description: "Try to talk with the cultist"
        }

        intent steal_relic {
          description: "Seize the relic by force or speed"
        }

        intent invoke_ward {
          description: "Use a shrine charm for protection"
        }

        intent flee {
          description: "Retreat from the chamber"
        }
      }

      on negotiate chance 55 {
        success {
          cultist1 says "The moon relic binds memory. Without it, the city forgets what it seals."
          add 1 to ritualKnowledge
          add 1 to fear
        }
        fail {
          cultist1 says "Words are for the unawakened."
        }
      }

      on steal_relic chance 65 {
        success {
          use RecoverMoonRelic
          go to relic_decision
        }
        fail {
          narrate "The cultist intercepts you, and the chamber erupts in movement."
          add 2 to fear
        }
      }

      on invoke_ward chance 100 {
        success {
          use InvokeWard
        }
        fail {
          narrate "Nothing shields you."
        }
      }

      on flee chance 100 {
        success {
          go to ruin_threshold
        }
        fail {
          narrate "You cannot move."
        }
      }
    }

    scene ember_chamber {
      description: "A cracked chamber glows with buried heat, its central pedestal wrapped in chains and ash."

      present {
        cultist2
      }

      on enter {
        narrate "Every breath tastes faintly of smoke and old iron."
        cultist2 says "The ember was never meant for hands like yours."
      }

      intents {
        intent challenge_cultist {
          description: "Confront the cultist directly"
        }

        intent take_ember {
          description: "Attempt to recover the ember relic"
        }

        intent study_chains {
          description: "Inspect the chains and bindings"
        }

        intent retreat_ember {
          description: "Retreat from the chamber"
        }
      }

      on challenge_cultist chance 60 {
        success {
          cultist2 says "Then hear this: the nobles wanted power, the watch wanted silence, and we wanted awakening."
          add 1 to evidence
          add 1 to fear
        }
        fail {
          cultist2 says "You understand nothing."
        }
      }

      on take_ember chance 65 {
        success {
          use RecoverEmberRelic
          go to relic_decision
        }
        fail {
          narrate "The heat surges violently and forces you back."
          add 2 to fear
        }
      }

      on study_chains chance 70 {
        success {
          narrate "The chains were not meant to imprison the relic. They were meant to protect the city from it."
          use LearnRitual
          use GainEvidence
        }
        fail {
          narrate "You cannot safely approach the bindings."
        }
      }

      on retreat_ember chance 100 {
        success {
          go to ruin_threshold
        }
        fail {
          narrate "You remain before the burning pedestal."
        }
      }
    }

    scene cult_chamber {
      description: "A hidden assembly chamber lies beneath the city, where masked figures once gathered around cracked lunar mosaics."

      on enter {
        narrate "The chamber is abandoned, but not long abandoned."
      }

      intents {
        intent search_remains {
          description: "Search the chamber for clues and records"
        }

        intent inspect_mosaic {
          description: "Study the ritual mosaic"
        }

        intent leave_cult {
          description: "Return to the sewer gate"
        }
      }

      on search_remains chance 75 {
        success {
          narrate "You find discarded robes, ritual ash, and coded schedules."
          give 1 RitualDust
          use GainEvidence
          use GainEvidence
        }
        fail {
          narrate "Most of the chamber has already been stripped."
        }
      }

      on inspect_mosaic chance 80 {
        success {
          narrate "The mosaic reveals that one relic seals memory while the other amplifies hunger."
          use LearnRitual
          add 1 to fear
        }
        fail {
          narrate "The broken pattern resists interpretation."
        }
      }

      on leave_cult chance 100 {
        success {
          go to sewer_gate
        }
        fail {
          narrate "You remain in the chamber."
        }
      }
    }

    scene upper_quarter {
      description: "The upper quarter is clean, guarded, and unnaturally quiet, as if wealth itself suppresses sound."

      present {
        noble1
        inspector
      }

      on enter {
        narrate "Stone facades and curtained windows suggest a city eager to hide where its money comes from."
      }

      intents {
        intent confront_noble {
          description: "Confront the noble with gathered evidence"
        }

        intent speak_inspector {
          description: "Bring evidence to the watch inspector"
        }

        intent infiltrate_manor {
          description: "Attempt to enter the noble manor"
        }

        intent leave_upper {
          description: "Leave the upper quarter"
        }
      }

      on confront_noble chance 70 {
        requires {
          evidence is at least 4
        }
        success {
          noble1 says "You do not understand the cost of keeping this city alive."
          use ExposeNoble
          add 1 to corruption
        }
        fail {
          noble1 says "Accusation without proof is suicide."
        }
      }

      on speak_inspector chance 75 {
        requires {
          evidence is at least 3
        }
        success {
          inspector says "If even half of this is true, the rot reaches high."
          use JoinWatch
        }
        fail {
          inspector says "Bring me more than rumor."
        }
      }

      on infiltrate_manor chance 60 {
        success {
          narrate "You slip through a servants' side passage into hidden record rooms."
          use GainEvidence
          use GainEvidence
        }
        fail {
          narrate "The manor guards force you back."
          use IncreaseSuspicion(2)
        }
      }

      on leave_upper chance 100 {
        success {
          go to decision_hall
        }
        fail {
          narrate "You remain in the upper quarter."
        }
      }
    }

    scene decision_hall {
      description: "With evidence, allies, and perhaps relics in hand, you must decide what Ashenfall will become."

      on enter {
        narrate "All paths now demand sacrifice."
      }

      intents {
        intent side_with_watch {
          description: "Give the evidence to the watch and attempt official reform"
        }

        intent side_with_smugglers {
          description: "Use the city's corruption for personal leverage and shadow control"
        }

        intent side_with_shrine {
          description: "Trust the shrine to contain what the city uncovered"
        }

        intent use_relics {
          description: "Take control of the relics yourself"
        }

        intent abandon_city {
          description: "Leave Ashenfall behind"
        }
      }

      on side_with_watch chance 75 {
        requires {
          watchAllied is equal to true and evidence is at least 4
        }
        success {
          go to ending_watch
        }
        fail {
          narrate "The watch will not move without stronger alliance or proof."
        }
      }

      on side_with_smugglers chance 75 {
        requires {
          smugglersKnown is equal to true and evidence is at least 3
        }
        success {
          go to ending_smuggler
        }
        fail {
          narrate "The underworld does not trust you enough yet."
        }
      }

      on side_with_shrine chance 80 {
        requires {
          faith is at least 2 and ritualKnowledge is at least 2
        }
        success {
          go to ending_shrine
        }
        fail {
          narrate "You lack either faith or understanding."
        }
      }

      on use_relics chance 70 {
        requires {
          relicRecovered is equal to true and ritualKnowledge is at least 3
        }
        success {
          go to ending_relic
        }
        fail {
          narrate "Power without knowledge is only another form of death."
        }
      }

      on abandon_city chance 100 {
        success {
          go to ending_departure
        }
        fail {
          narrate "You cannot yet leave."
        }
      }
    }

    scene relic_decision {
      description: "Holding an ancient relic changes every remaining choice before you."

      intents {
        intent take_to_upper {
          description: "Carry the relic into the upper quarter confrontation"
        }

        intent hide_relic {
          description: "Hide the relic and continue with evidence alone"
        }

        intent bring_to_shrine {
          description: "Take the relic to the shrine first"
        }
      }

      on take_to_upper chance 100 {
        success {
          go to upper_quarter
        }
        fail {
          narrate "You hesitate."
        }
      }

      on hide_relic chance 100 {
        success {
          narrate "You conceal the relic beneath old wrappings and keep moving."
          go to upper_quarter
        }
        fail {
          narrate "You fail to hide it."
        }
      }

      on bring_to_shrine chance 100 {
        success {
          go to shrine_revelation
        }
        fail {
          narrate "You remain uncertain."
        }
      }
    }

    scene shrine_revelation {
      description: "Back at the shrine, the relic reveals truths the city buried on purpose."

      present {
        priest1
      }

      on enter {
        narrate "The relic reacts to the candles with a pulse of cold or heat."
        priest1 says "So it is true. They found them again."
      }

      intents {
        intent ask_meaning {
          description: "Ask what the relic truly is"
        }

        intent seal_relic {
          description: "Ask the shrine to contain the relic"
        }

        intent leave_for_final {
          description: "Proceed to the final decision"
        }
      }

      on ask_meaning chance 85 {
        success {
          priest1 says "These were never treasures. They were anchors."
          priest1 says "Without them, memory breaks or hunger spreads."
          use LearnRitual
          add 1 to faith
        }
        fail {
          priest1 says "I do not know enough to answer safely."
        }
      }

      on seal_relic chance 75 {
        requires {
          faith is at least 2
        }
        success {
          priest1 says "Then let the burden pass from ambition into custody."
        }
        fail {
          priest1 says "You are not ready to entrust this."
        }
      }

      on leave_for_final chance 100 {
        success {
          go to decision_hall
        }
        fail {
          narrate "You remain in the shrine."
        }
      }
    }

    scene ending_watch {
      description: "The evidence reaches the inspector, arrests begin, and Ashenfall lurches toward painful reform."

      present {
        inspector
      }

      on enter {
        narrate "The city does not become clean, but it becomes less hidden."
        inspector says "Justice came late. Still, it came."
      }

      end scene
    }

    scene ending_smuggler {
      description: "With the right secrets in the right hands, the underworld becomes the true government of Ashenfall."

      present {
        smuggler1
      }

      on enter {
        narrate "Order survives only as a surface performance."
        smuggler1 says "Cities are always ruled by what they refuse to admit."
      }

      end scene
    }

    scene ending_shrine {
      description: "The relics are sealed, the rites are restored, and the city is spared at the cost of silence and sacrifice."

      present {
        priest1
      }

      on enter {
        narrate "The shrine keeps its vigil, and the city forgets just enough to keep breathing."
        priest1 says "Some truths must be guarded more carefully than laws."
      }

      end scene
    }

    scene ending_relic {
      description: "You claim the buried power beneath Ashenfall and become something the city will obey, fear, or worship."

      on enter {
        narrate "The city changes shape around your will."
        random 50 {
          narrate "Whether this is salvation or ruin depends on who survives to name it."
        }
      }

      end scene
    }

    scene ending_departure {
      description: "You leave Ashenfall behind with knowledge, guilt, and the certainty that the city will endure in one broken form or another."

      on enter {
        narrate "At dawn the road looks almost innocent."
      }

      end scene
    }
  }
}