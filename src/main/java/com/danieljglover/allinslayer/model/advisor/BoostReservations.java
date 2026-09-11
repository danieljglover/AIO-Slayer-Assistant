package com.danieljglover.allinslayer.model.advisor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public final class BoostReservations
{
	private static final int MAX_SLOTS = 28;
	private static final Style DISABLED_STYLE = new Style(0, null);
	private static final BoostReservations DISABLED = new BoostReservations(0, null, 0, null, 0, null);

	private final Style melee;
	private final Style ranged;
	private final Style magic;

	public BoostReservations(int meleeSlots, String meleeItems, int rangedSlots,
		String rangedItems, int magicSlots, String magicItems)
	{
		melee = new Style(meleeSlots, meleeItems);
		ranged = new Style(rangedSlots, rangedItems);
		magic = new Style(magicSlots, magicItems);
	}

	public static BoostReservations disabled()
	{
		return DISABLED;
	}

	public Style forStyle(String style)
	{
		if (style == null)
		{
			return DISABLED_STYLE;
		}
		switch (style.trim().toUpperCase(Locale.ROOT))
		{
			case "MELEE": return melee;
			case "RANGED": return ranged;
			case "MAGIC": return magic;
			default: return DISABLED_STYLE;
		}
	}

	@Getter
	@EqualsAndHashCode
	public static final class Style
	{
		private final int slots;
		private final List<String> preferences;

		private Style(int slots, String items)
		{
			this.slots = Math.max(0, Math.min(MAX_SLOTS, slots));
			List<String> parsed = new ArrayList<>();
			Set<String> seen = new HashSet<>();
			if (items != null)
			{
				for (String item : items.split("[,;\\r\\n]+"))
				{
					String name = item.trim();
					if (!name.isEmpty() && seen.add(name.toLowerCase(Locale.ROOT)))
					{
						parsed.add(name);
						if (parsed.size() == MAX_SLOTS)
						{
							break;
						}
					}
				}
			}
			preferences = Collections.unmodifiableList(parsed);
		}
	}
}
