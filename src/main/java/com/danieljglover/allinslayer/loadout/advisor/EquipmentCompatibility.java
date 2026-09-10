package com.danieljglover.allinslayer.loadout.advisor;

import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot.ItemStats;
import java.util.Locale;

/** Static ammunition rules, checked against the Wiki ammunition and weapon tables. */
final class EquipmentCompatibility
{
	private EquipmentCompatibility()
	{
	}

	static boolean needsAmmo(ItemStats weapon)
	{
		if (weapon == null)
		{
			return false;
		}
		String name = lower(weapon.getName());
		return !selfSupplied(name) && (name.contains("bow") || name.contains("ballista")
			|| name.contains("atlatl") || name.contains("salamander") || name.contains("swamp lizard")
			|| name.contains("seercull"));
	}

	static boolean compatible(ItemStats weapon, ItemStats ammo)
	{
		if (ammo == null)
		{
			return !needsAmmo(weapon);
		}
		String ammunition = lower(ammo.getName());
		if (weapon == null || !needsAmmo(weapon))
		{
			return ammunition.contains("blessing");
		}
		String name = lower(weapon.getName());
		if (name.contains("karil"))
		{
			return ammunition.contains("bolt rack");
		}
		if (name.contains("sunlight crossbow"))
		{
			return ammunition.contains("antler bolt");
		}
		if (name.contains("hunters'") || name.contains("hunter's"))
		{
			return ammunition.contains("kebbit bolt");
		}
		if (name.contains("crossbow"))
		{
			if (name.equals("crossbow") || name.contains("phoenix"))
			{
				return ammunition.contains("bronze bolt") && !ammunition.contains("unf");
			}
			if (name.contains("dorgeshuun") && ammunition.contains("bone bolt"))
			{
				return true;
			}
			int tier = boltTier(ammunition);
			return tier > 0 && tier <= crossbowTier(name)
				&& !(name.contains("dorgeshuun") && ammunition.contains("silver"));
		}
		if (name.contains("ballista"))
		{
			return ammunition.contains("javelin") && !ammunition.contains("morrigan");
		}
		if (name.contains("atlatl"))
		{
			return ammunition.contains("atlatl dart");
		}
		if (name.contains("swamp lizard"))
		{
			return ammunition.equals("guam tar");
		}
		if (name.contains("salamander"))
		{
			return name.contains("orange") && ammunition.equals("marrentill tar")
				|| name.contains("red") && ammunition.equals("tarromin tar")
				|| name.contains("black") && ammunition.equals("harralander tar")
				|| name.contains("tecu") && ammunition.equals("irit tar");
		}
		if (name.contains("training bow"))
		{
			return ammunition.contains("training arrow");
		}
		if (name.contains("ogre bow"))
		{
			if (ammunition.contains("ogre arrow"))
			{
				return true;
			}
			return ammunition.contains("brutal") && (name.contains("comp")
				|| materialTier(ammunition) <= 4 && materialTier(ammunition) > 0);
		}
		int tier = arrowTier(ammunition);
		return tier > 0 && tier <= bowTier(name);
	}

	static boolean chargeBearing(ItemStats weapon)
	{
		if (weapon == null)
		{
			return false;
		}
		String name = lower(weapon.getName());
		return selfSupplied(name) || name.contains("trident") || name.contains("sanguinesti")
			|| name.contains("tumeken") || name.contains("sceptre") || name.contains("warped sceptre")
			|| name.contains("arclight") || name.contains("scythe") || name.contains("venator")
			|| name.contains("chainmace");
	}

	private static boolean selfSupplied(String name)
	{
		return name.contains("crystal bow") || name.contains("faerdhinen")
			|| name.contains("craw's bow") || name.contains("webweaver") || name.contains("starter bow")
			|| name.contains("blowpipe");
	}

	private static int crossbowTier(String name)
	{
		if (name.contains("dragon") || name.contains("armadyl") || name.contains("zaryte"))
		{
			return 8;
		}
		if (name.contains("rune"))
		{
			return 7;
		}
		if (name.contains("dorgeshuun"))
		{
			return 3;
		}
		if (name.contains("blurite"))
		{
			return 2;
		}
		if (name.contains("adamant"))
		{
			return 6;
		}
		if (name.contains("mithril"))
		{
			return 5;
		}
		if (name.contains("steel"))
		{
			return 4;
		}
		if (name.contains("iron"))
		{
			return 3;
		}
		return name.equals("crossbow") || name.contains("phoenix") || name.contains("bronze") ? 1 : 0;
	}

	private static int boltTier(String name)
	{
		if (!name.contains("bolt") || name.contains("unfinished") || name.contains("unf")
			|| name.contains("tips") || name.contains("antler") || name.contains("kebbit"))
		{
			return 0;
		}
		if (name.contains("dragon bolts"))
		{
			return 8;
		}
		if (name.contains("runite") || name.contains("dragonstone") || name.contains("onyx")
			|| name.contains("broad"))
		{
			return 7;
		}
		if (name.contains("adamant") || name.contains("ruby") || name.contains("diamond"))
		{
			return 6;
		}
		if (name.contains("mithril") || name.contains("sapphire") || name.contains("emerald"))
		{
			return 5;
		}
		if (name.contains("steel") || name.contains("topaz"))
		{
			return 4;
		}
		if (name.contains("iron") || name.contains("pearl") || name.contains("silver"))
		{
			return 3;
		}
		if (name.contains("blurite") || name.contains("jade"))
		{
			return 2;
		}
		return name.contains("bronze") || name.contains("opal") ? 1 : 0;
	}

	private static int bowTier(String name)
	{
		if (name.contains("dark bow") || name.contains("twisted") || name.contains("3rd age")
			|| name.contains("venator") || name.contains("scorching"))
		{
			return 8;
		}
		if (name.contains("magic") || name.contains("seercull") || name.contains("bone shortbow"))
		{
			return 7;
		}
		if (name.contains("yew"))
		{
			return 6;
		}
		if (name.contains("maple"))
		{
			return 5;
		}
		if (name.contains("willow"))
		{
			return 4;
		}
		if (name.contains("oak"))
		{
			return 3;
		}
		return name.equals("shortbow") || name.equals("longbow") || name.equals("rain bow")
			|| name.contains("cursed goblin") ? 2 : 0;
	}

	private static int arrowTier(String name)
	{
		if (!name.contains("arrow") || name.contains("head") || name.contains("shaft")
			|| name.contains("fire") || name.contains("training") || name.contains("ogre"))
		{
			return 0;
		}
		if (name.contains("broad"))
		{
			return 7;
		}
		if (name.contains("ice arrow"))
		{
			return 6;
		}
		return materialTier(name);
	}

	private static int materialTier(String name)
	{
		String[] materials = {"bronze", "iron", "steel", "mithril", "adamant", "rune", "amethyst", "dragon"};
		for (int index = materials.length - 1; index >= 0; index--)
		{
			if (name.contains(materials[index]))
			{
				return index + 1;
			}
		}
		return 0;
	}

	private static String lower(String value)
	{
		return value.toLowerCase(Locale.ROOT);
	}
}
