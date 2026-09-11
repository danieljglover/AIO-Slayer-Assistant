package com.danieljglover.allinslayer.model.advisor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.EqualsAndHashCode;

/** An immutable client-thread capture; the recommendation engine never reads the client. */
@Getter
@EqualsAndHashCode
public final class PlayerSnapshot
{
	private final Map<Integer, Integer> owned;
	private final Map<Integer, Integer> carried;
	private final Map<Integer, ItemStats> items;
	private final Map<String, Integer> levels;
	private final Set<String> confirmedRequirements;
	private final boolean loggedIn;
	private final long bankSeenAt;
	private final Spellbook spellbook;
	private final AccountProgress accountProgress;
	private final Map<Integer, ItemValue> itemValues;
	private final Map<Integer, Integer> riskCarried;
	private final DeathContext deathContext;
	private final PreparationSnapshot preparation;

	public PlayerSnapshot(Map<Integer, Integer> owned, Map<Integer, Integer> carried,
		Map<Integer, ItemStats> items, Map<String, Integer> levels,
		Set<String> confirmedRequirements, boolean loggedIn, long bankSeenAt)
	{
		this(owned, carried, items, levels, confirmedRequirements, loggedIn, bankSeenAt, null);
	}

	public PlayerSnapshot(Map<Integer, Integer> owned, Map<Integer, Integer> carried,
		Map<Integer, ItemStats> items, Map<String, Integer> levels,
		Set<String> confirmedRequirements, boolean loggedIn, long bankSeenAt, Spellbook spellbook)
	{
		this(owned, carried, items, levels, confirmedRequirements, loggedIn, bankSeenAt, spellbook, null);
	}

	public PlayerSnapshot(Map<Integer, Integer> owned, Map<Integer, Integer> carried,
		Map<Integer, ItemStats> items, Map<String, Integer> levels,
		Set<String> confirmedRequirements, boolean loggedIn, long bankSeenAt, Spellbook spellbook,
		AccountProgress accountProgress)
	{
		this(owned, carried, items, levels, confirmedRequirements, loggedIn, bankSeenAt,
			spellbook, accountProgress, null, carried, null);
	}

	public PlayerSnapshot(Map<Integer, Integer> owned, Map<Integer, Integer> carried,
		Map<Integer, ItemStats> items, Map<String, Integer> levels,
		Set<String> confirmedRequirements, boolean loggedIn, long bankSeenAt, Spellbook spellbook,
		AccountProgress accountProgress, Map<Integer, ItemValue> itemValues,
		Map<Integer, Integer> riskCarried, DeathContext deathContext)
	{
		this(owned, carried, items, levels, confirmedRequirements, loggedIn, bankSeenAt, spellbook,
			accountProgress, itemValues, riskCarried, deathContext, null);
	}

	public PlayerSnapshot(Map<Integer, Integer> owned, Map<Integer, Integer> carried,
		Map<Integer, ItemStats> items, Map<String, Integer> levels,
		Set<String> confirmedRequirements, boolean loggedIn, long bankSeenAt, Spellbook spellbook,
		AccountProgress accountProgress, Map<Integer, ItemValue> itemValues,
		Map<Integer, Integer> riskCarried, DeathContext deathContext, PreparationSnapshot preparation)
	{
		this.owned = quantities(owned);
		this.carried = quantities(carried);
		this.items = copy(items);
		this.levels = copy(levels);
		this.confirmedRequirements = Collections.unmodifiableSet(new LinkedHashSet<>(
			confirmedRequirements == null ? Collections.emptySet() : confirmedRequirements));
		this.loggedIn = loggedIn;
		this.bankSeenAt = Math.max(0, bankSeenAt);
		this.spellbook = loggedIn ? spellbook : null;
		this.accountProgress = loggedIn && accountProgress != null ? accountProgress : new AccountProgress(null);
		this.itemValues = copy(itemValues);
		this.riskCarried = loggedIn ? quantities(riskCarried) : Collections.emptyMap();
		this.deathContext = loggedIn && deathContext != null ? deathContext : DeathContext.unknown();
		this.preparation = loggedIn && preparation != null ? preparation : PreparationSnapshot.unknown();
	}

	@Getter
	public enum Spellbook
	{
		STANDARD("Standard"),
		ANCIENT("Ancient Magicks"),
		LUNAR("Lunar"),
		ARCEUUS("Arceuus");

		private final String displayName;

		Spellbook(String displayName)
		{
			this.displayName = displayName;
		}
	}

	private static Map<Integer, Integer> quantities(Map<Integer, Integer> input)
	{
		Map<Integer, Integer> result = new LinkedHashMap<>();
		if (input != null)
		{
			input.forEach((id, quantity) ->
			{
				if (id != null && id > 0 && quantity != null && quantity > 0)
				{
					result.put(id, quantity);
				}
			});
		}
		return Collections.unmodifiableMap(result);
	}

	private static <K, V> Map<K, V> copy(Map<K, V> input)
	{
		return Collections.unmodifiableMap(new LinkedHashMap<>(
			input == null ? Collections.emptyMap() : input));
	}

	@Getter
	@EqualsAndHashCode
	public static final class ItemStats
	{
		private final int id;
		private final String name;
		private final String slot;
		private final String style;
		private final boolean usable;
		private final boolean twoHanded;
		private final boolean stackable;
		private final boolean food;
		private final double attack;
		private final double strength;
		private final double defence;
		private final double prayer;
		private final double rangedAttack;
		private final double rangedStrength;
		private final double magicAttack;
		private final double magicDamage;

		public ItemStats(int id, String name, String slot, String style, boolean usable,
			boolean twoHanded, boolean stackable, double attack, double strength,
			double defence, double prayer)
		{
			this(id, name, slot, style, usable, twoHanded, stackable,
				"MELEE".equals(style) || "ANY".equals(style) ? attack : 0,
				"MELEE".equals(style) || "ANY".equals(style) ? strength : 0, defence, prayer,
				"RANGED".equals(style) ? attack : 0, "RANGED".equals(style) ? strength : 0,
				"MAGIC".equals(style) ? attack : 0, "MAGIC".equals(style) ? strength : 0);
		}

		public ItemStats(int id, String name, String slot, String style, boolean usable,
			boolean twoHanded, boolean stackable, double meleeAttack, double meleeStrength,
			double defence, double prayer, double rangedAttack, double rangedStrength,
			double magicAttack, double magicDamage)
		{
			this(id, name, slot, style, usable, twoHanded, stackable, meleeAttack, meleeStrength,
				defence, prayer, rangedAttack, rangedStrength, magicAttack, magicDamage, false);
		}

		public ItemStats(int id, String name, String slot, String style, boolean usable,
			boolean twoHanded, boolean stackable, double meleeAttack, double meleeStrength,
			double defence, double prayer, double rangedAttack, double rangedStrength,
			double magicAttack, double magicDamage, boolean food)
		{
			this.id = id;
			this.name = name == null ? "Item " + id : name;
			this.slot = slot == null ? "" : slot;
			this.style = style == null ? "ANY" : style;
			this.usable = usable;
			this.twoHanded = twoHanded;
			this.stackable = stackable;
			this.food = food;
			this.attack = finite(meleeAttack);
			this.strength = finite(meleeStrength);
			this.defence = finite(defence);
			this.prayer = finite(prayer);
			this.rangedAttack = finite(rangedAttack);
			this.rangedStrength = finite(rangedStrength);
			this.magicAttack = finite(magicAttack);
			this.magicDamage = finite(magicDamage);
		}

		public double attackFor(String combatStyle)
		{
			return "RANGED".equalsIgnoreCase(combatStyle) ? rangedAttack
				: "MAGIC".equalsIgnoreCase(combatStyle) ? magicAttack : attack;
		}

		public double strengthFor(String combatStyle)
		{
			return "RANGED".equalsIgnoreCase(combatStyle) ? rangedStrength
				: "MAGIC".equalsIgnoreCase(combatStyle) ? magicDamage : strength;
		}

		private static double finite(double value)
		{
			return Double.isFinite(value) ? value : 0;
		}
	}
}
