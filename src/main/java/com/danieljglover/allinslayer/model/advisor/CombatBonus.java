package com.danieljglover.allinslayer.model.advisor;

/** Item identity comes from reviewed source IDs, never display-name or variation matching. */
public enum CombatBonus
{
	NONE,
	SLAYER,
	SLAYER_IMBUED,
	SALVE,
	SALVE_ENCHANTED,
	SALVE_IMBUED,
	SALVE_ENCHANTED_IMBUED;

	public boolean isSlayer()
	{
		return this == SLAYER || this == SLAYER_IMBUED;
	}

	public boolean isSalve()
	{
		return this != NONE && !isSlayer();
	}

	public double multiplier(String style)
	{
		if (this == NONE)
		{
			return 1;
		}
		if ("MELEE".equalsIgnoreCase(style))
		{
			return this == SALVE_ENCHANTED || this == SALVE_ENCHANTED_IMBUED ? 1.20 : 7.0 / 6.0;
		}
		if ("RANGED".equalsIgnoreCase(style) || "MAGIC".equalsIgnoreCase(style))
		{
			if (this == SALVE_ENCHANTED_IMBUED)
			{
				return 1.20;
			}
			if (this == SALVE_IMBUED)
			{
				return "RANGED".equalsIgnoreCase(style) ? 7.0 / 6.0 : 1.15;
			}
			if (this == SLAYER_IMBUED)
			{
				return 1.15;
			}
		}
		return 1;
	}
}
