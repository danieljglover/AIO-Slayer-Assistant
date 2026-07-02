package com.danieljglover.allinslayer.task;

/**
 * Slayer varp/varbit ids (gameval names in comments). Defined locally so a generated-constant
 * rename across RuneLite releases cannot break the build.
 */
public final class SlayerVarbits
{
    private SlayerVarbits() {}

    public static final int SLAYER_TARGET = 395;          // VarPlayerID.SLAYER_TARGET
    public static final int SLAYER_COUNT = 394;           // VarPlayerID.SLAYER_COUNT
    public static final int SLAYER_AREA = 2096;           // VarPlayerID.SLAYER_AREA
    public static final int SLAYER_COUNT_ORIGINAL = 4258; // VarPlayerID.SLAYER_COUNT_ORIGINAL
    public static final int SLAYER_TARGET_BOSSID = 4723;  // VarbitID.SLAYER_TARGET_BOSSID

    // WA-10 (spike-player-state.md): the player-state signals. All verified against the pinned
    // 1.12.31.1 jars - 4067/4068 are additionally read live by the stock Slayer plugin.
    public static final int SLAYER_MASTER = 4067;         // VarbitID.SLAYER_MASTER (0=none, 1..9)
    public static final int SLAYER_POINTS = 4068;         // VarbitID.SLAYER_POINTS

    // Global reward-unlock varbits (Jagex gameval names; 1 = purchased).
    public static final int SLAYER_HELM_UNLOCKED = 3202;          // Malevolent masquerade
    public static final int SLAYER_RING_UNLOCKED = 3207;          // Ring bling
    public static final int SLAYER_AMMO_UNLOCKED = 3208;          // Broader Fletching
    public static final int SLAYER_UNLOCK_BOSSES = 4724;          // Like a Boss
    public static final int SLAYER_UNLOCK_SUPERIORMOBS = 5358;    // Bigger and Badder
    public static final int SLAYER_UNLOCK_STORAGE = 12442;        // Task Storage
    public static final int SLAYER_UNLOCK_WILDY_EXTRATASKS = 13636; // I Wildy More Slayer

    // Task-extension unlocks (FR-RV S2; spike-player-state.md Signal 2, SLAYER_LONGER_* gameval
    // names, all verified against the pinned 1.12.31.1 jar). One per EXTENSION unlockId in the
    // dataset; the unlock display name is in the comment. Three more spike-verified families
    // (mithril/adamant/rune dragons) have no dataset task yet -
    // add their constant only when the task (and its unlockId) is authored.
    public static final int SLAYER_LONGER_ABERRANTSPECTRES = 4747;  // Smell ya later
    public static final int SLAYER_LONGER_ABYSSALDEMONS = 4090;     // Augment my abbies
    public static final int SLAYER_LONGER_ANKOU = 4085;             // Ankou very much
    public static final int SLAYER_LONGER_AQUANITES = 19603;        // Let's Stay All Aquanite
    public static final int SLAYER_LONGER_ARAXYTES = 11022;         // More eyes than sense
    public static final int SLAYER_LONGER_AVIANSIES = 4748;         // Birds of a Feather
    public static final int SLAYER_LONGER_BASILISK = 9455;          // Basilonger
    public static final int SLAYER_LONGER_BLACKDEMONS = 4091;       // It's dark in here
    public static final int SLAYER_LONGER_BLACKDRAGONS = 4087;      // Fire & Darkness
    public static final int SLAYER_LONGER_BLOODVELD = 4746;         // Bleed me dry
    public static final int SLAYER_LONGER_CAVEHORRORS = 4750;       // Horrorific
    public static final int SLAYER_LONGER_CAVEKRAKEN = 4755;        // Krack on
    public static final int SLAYER_LONGER_DARKBEASTS = 4031;        // Need More Darkness
    public static final int SLAYER_LONGER_DUSTDEVILS = 4751;        // To Dust You Shall Return
    public static final int SLAYER_LONGER_FOSSILWYVERNS = 5733;     // Wyver-nother two
    public static final int SLAYER_UNLOCK_LONGER_FROST_DRAGONS = 15399; // I see Dragons
    public static final int SLAYER_LONGER_GARGOYLES = 4753;         // Get smashed
    public static final int SLAYER_LONGER_GREATERDEMONS = 4092;     // Greater Challenge
    public static final int SLAYER_UNLOCK_LONGER_GRYPHON = 15398;   // Gryphon and on
    public static final int SLAYER_LONGER_METALDRAGONS = 4088;      // Pedal to the Metals
    public static final int SLAYER_LONGER_NECHRYAEL = 4754;         // Nechs Please
    public static final int SLAYER_LONGER_REVENANTS = 14822;        // Revenenenenenants
    public static final int SLAYER_LONGER_SCABARITES = 5359;        // Get scabaright on it
    public static final int SLAYER_LONGER_CUSTODIANS = 17219;       // Un-restraining Order
    public static final int SLAYER_LONGER_SKELETALWYVERNS = 4752;   // Wyver-nother One
    public static final int SLAYER_LONGER_SPIRITUALGWD = 4757;      // Spiritual fervour
    public static final int SLAYER_LONGER_SUQAH = 4086;             // Suq-a-nother One
    public static final int SLAYER_LONGER_VAMPYRES = 10389;         // More at stake
    public static final int SLAYER_LONGER_WYRMS = 19602;            // Can of Wyrms

    // Helm recolour unlocks (gameval names; only the wiki-verified name->colour pairings are
    // consumed - see VarbitSlayerUnlockStateProvider).
    public static final int SLAYER_UNLOCK_HELM_BLACK = 5080;      // King Black Bonnet
    public static final int SLAYER_UNLOCK_HELM_GREEN = 5081;      // Kalphite Khat
    public static final int SLAYER_UNLOCK_HELM_RED = 5082;        // Unholy Helmet
    public static final int SLAYER_UNLOCK_HELM_PURPLE = 5631;     // Dark Mantle
    public static final int SLAYER_UNLOCK_HELM_TURQUOISE = 6096;  // Undead Head
    public static final int SLAYER_UNLOCK_HELM_HYDRA = 6570;      // Use More Head
    public static final int SLAYER_UNLOCK_HELM_TWISTED = 10104;   // Twisted Vision
}
