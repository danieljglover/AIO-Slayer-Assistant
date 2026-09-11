package com.danieljglover.allinslayer.model.advisor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class FoodOverride
{
	private static final FoodOverride DISABLED = new FoodOverride(false, null);
	private final boolean enabled;
	private final List<String> preferences;

	public FoodOverride(boolean enabled, String foods)
	{
		this.enabled = enabled;
		List<String> parsed = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		if (foods != null)
		{
			for (String food : foods.split("[,;\\r\\n]+"))
			{
				String name = food.trim().replaceAll("\\s+", " ");
				if (!name.isEmpty() && seen.add(name.toLowerCase(Locale.ROOT)))
				{
					parsed.add(name);
					if (parsed.size() == 28) { break; }
				}
			}
		}
		preferences = Collections.unmodifiableList(parsed);
	}

	public static FoodOverride disabled()
	{
		return DISABLED;
	}
}
