package com.danieljglover.allinslayer.model.advisor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Current account observations; unknown is distinct from an unmet requirement. */
@Getter
@EqualsAndHashCode
public final class AccountProgress
{
	public enum State { MET, UNMET, UNKNOWN }

	private final Map<String, State> requirements;
	private final Map<String, String> configuredRequirements;

	public AccountProgress(Map<String, State> requirements)
	{
		this(requirements, Collections.emptyMap());
	}

	public AccountProgress(Map<String, State> requirements, Map<String, String> configuredRequirements)
	{
		Map<String, State> normalized = new LinkedHashMap<>();
		if (requirements != null)
		{
			requirements.forEach((name, state) -> normalized.put(key(name), state == null ? State.UNKNOWN : state));
		}
		this.requirements = Collections.unmodifiableMap(normalized);
		Map<String, String> sources = new LinkedHashMap<>();
		configuredRequirements.forEach((name, source) -> sources.put(key(name), source));
		this.configuredRequirements = Collections.unmodifiableMap(sources);
	}

	public static String key(String text)
	{
		return text == null ? "" : text.trim().toLowerCase(Locale.ROOT)
			.replace('\u2019', '\'').replaceAll("\\s+", " ");
	}
}
