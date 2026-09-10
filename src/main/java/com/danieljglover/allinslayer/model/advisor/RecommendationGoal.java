package com.danieljglover.allinslayer.model.advisor;

public enum RecommendationGoal
{
	SLAYER_XP("Slayer XP"),
	PROFIT("Profit"),
	LOW_EFFORT("Low effort");

	private final String label;

	RecommendationGoal(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
