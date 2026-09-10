package com.danieljglover.allinslayer.loadout.advisor;

import com.danieljglover.allinslayer.model.advisor.AccountProgress;
import com.danieljglover.allinslayer.model.advisor.AccountProgress.State;
import com.danieljglover.allinslayer.model.advisor.RecommendationRequest;
import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot.Spellbook;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;

/** One requirement decision shared by recommendations and the account panel. */
public final class RequirementEvaluator
{
	private static final String SKILLS = "attack|strength|defence|ranged|prayer|magic|runecraft(?:ing)?|"
		+ "construction|hitpoints|agility|herblore|thieving|crafting|fletching|slayer|hunter|"
		+ "mining|smithing|fishing|cooking|firemaking|woodcutting|farming|sailing|combat";
	private static final Pattern SKILL_FIRST = Pattern.compile("(?i)^(" + SKILLS
		+ ")\\s*(?:level\\s*)?(?:>=|:|=|of|at least)?\\s*(\\d{1,3})\\+?$");
	private static final Pattern LEVEL_FIRST = Pattern.compile("(?i)^(?:level\\s*)?(\\d{1,3})"
		+ "\\+?\\s+(" + SKILLS + ")(?:\\s+level)?$");
	private static final Pattern SKILL_PURPOSE = Pattern.compile("(?i)^((?:level\\s*)?\\d{1,3}\\+?\\s+(?:"
		+ SKILLS + ")|(?:" + SKILLS + ")\\s+(?:level\\s*)?\\d{1,3}\\+?)"
		+ "\\s+(?:(?:is\\s+)?required\\s+)?(?:for|to|before)\\s+(.+)$");
	private static final Pattern EXTRA_CONDITION = Pattern.compile(
		"(?i)[0-9;,]|\\b(and|or|plus|with|without|unless|if|recommended|optional|boost|partial|quest|unlock)\\b");
	private static final Pattern BOOSTED_SKILL = Pattern.compile("(?i)^(\\d{1,3})\\s+(" + SKILLS
		+ ")\\s+with\\s+(.+?)\\s+boost$");
	private static final Pattern DIARY_OR_BOSS = Pattern.compile(
		"^(Medium|Hard) Wilderness Diary completed or (Vet'ion|Callisto|Venenatis) boss assignment$", Pattern.CASE_INSENSITIVE);
	private final RecommendationRequest request;
	private final String planningMonsterId;

	public RequirementEvaluator(RecommendationRequest request)
	{
		this(request, request.getMonsterId());
	}

	public RequirementEvaluator(RecommendationRequest request, String planningMonsterId)
	{
		this.request = request;
		this.planningMonsterId = planningMonsterId;
	}

	/** Assignment alternatives come from live state or an explicit preview, never a saved confirmation. */
	public static String confirmationRequirement(String requirement)
	{
		Matcher matcher = DIARY_OR_BOSS.matcher(requirement.trim());
		return matcher.matches() ? matcher.group(1) + " Wilderness Diary completed" : requirement;
	}

	@Getter
	public static final class Assessment
	{
		private final State state;
		private final String detail;
		private final boolean manual;
		private final boolean assumed;

		private Assessment(State state, String detail, boolean manual)
		{
			this(state, detail, manual, false);
		}

		private Assessment(State state, String detail, boolean manual, boolean assumed)
		{
			this.state = state;
			this.detail = detail;
			this.manual = manual;
			this.assumed = assumed;
		}

		public boolean canConfirm()
		{
			return state == State.UNKNOWN && manual;
		}
	}

	public Assessment assess(String requirement)
	{
		String text = requirement == null ? "" : requirement.trim();
		if (text.isEmpty())
		{
			return known(State.MET, "No requirement.");
		}
		Assessment result = automatic(text);
		String configured = request.getPlayer().getAccountProgress().getConfiguredRequirements().get(AccountProgress.key(text));
		if (result.canConfirm() && request.getPlayer().isLoggedIn() && configured != null)
		{
			return new Assessment(State.MET, configured, false);
		}
		if (result.canConfirm() && request.getPlayer().isLoggedIn()
			&& request.getPlayer().getConfirmedRequirements().stream()
				.anyMatch(value -> AccountProgress.key(value).equals(AccountProgress.key(text))))
		{
			return new Assessment(State.MET, "Manually confirmed for this account.", true);
		}
		return result;
	}

	public boolean isMet(String requirement)
	{
		return assess(requirement).getState() == State.MET;
	}

	void checkAll(List<String> requirements, List<String> blockers)
	{
		if (requirements != null)
		{
			for (String requirement : requirements)
			{
				String failure = failure(requirement);
				if (failure != null) { add(blockers, failure); }
			}
		}
	}

	String failure(String requirement)
	{
		Assessment assessment = assess(requirement);
		if (assessment.getState() == State.MET) { return null; }
		if (assessment.canConfirm()) { return "Confirm requirement: " + requirement; }
		return (assessment.getState() == State.UNMET ? "Requirement not met: " : "Cannot verify requirement: ")
			+ requirement + " (" + assessment.getDetail() + ")";
	}

	private Assessment automatic(String text)
	{
		String lower = AccountProgress.key(text);
		if (lower.equals("none") || lower.equals("none.") || lower.equals("no requirements"))
		{
			return known(State.MET, "No additional requirement.");
		}
		Matcher diaryOrBoss = DIARY_OR_BOSS.matcher(text);
		if (diaryOrBoss.matches())
		{
			Assessment diary = assess(confirmationRequirement(text));
			if (diary.getState() == State.MET) { return diary; }
			String bossId = diaryOrBoss.group(2).equalsIgnoreCase("Vet'ion") ? "vet-ion"
				: diaryOrBoss.group(2).toLowerCase(Locale.ROOT);
			if (request.isTaskPreview() && "boss".equals(request.getTaskId())
				&& bossId.equals(previewBossAssignment()))
			{
				return assumed("On-task preview: assumes the matching " + diaryOrBoss.group(2)
					+ " boss assignment for this access alternative; diary completion is not assumed.");
			}
			if (request.getPlayer().isLoggedIn() && request.isActiveTask() && request.getRemaining() > 0
				&& "boss".equals(request.getTaskId()) && request.getAllowedMonsterIds().contains(bossId))
			{
				return known(State.MET, "The matching boss assignment grants this access alternative.");
			}
			return diary;
		}
		if (lower.equals("task only") || lower.equals("task-only")
			|| lower.equals("on a slayer task") || lower.equals("active slayer task"))
		{
			if (request.isTaskPreview())
			{
				return assumed("On-task preview: assumes the selected Slayer assignment for task-only access; the detected task is unchanged.");
			}
			return known(!request.getPlayer().isLoggedIn() ? State.UNKNOWN
				: request.isActiveTask() && request.getRemaining() > 0 ? State.MET : State.UNMET,
				"Requires an active matching Slayer task.");
		}
		// Whole-name lookup precedes expression splitting: quest names and diary regions
		// can themselves contain 'and', commas, or '&'.
		State observed = request.getPlayer().getAccountProgress().getRequirements().get(lower);
		if (observed == null && lower.endsWith(" (partial)"))
		{
			String completed = lower.substring(0, lower.length() - " (partial)".length());
			// Completion proves a partial-progress gate. An unfinished quest still needs
			// its specific stage checked; do not infer that stage from IN_PROGRESS.
			if (request.getPlayer().getAccountProgress().getRequirements().get(completed) == State.MET)
			{
				return known(State.MET, "Completed quest also satisfies the partial-progress requirement.");
			}
		}
		if (observed != null)
		{
			return known(observed, observed == State.MET ? "Verified from current account progress."
				: observed == State.UNMET ? "Not completed or unlocked on this account."
				: "Current account progress is unavailable.");
		}
		Assessment skill = skill(text);
		if (skill != null) { return skill; }
		Matcher purpose = SKILL_PURPOSE.matcher(text);
		if (purpose.matches() && !EXTRA_CONDITION.matcher(purpose.group(2)).find())
		{
			// Pure level gates may include a location/purpose. Additional conditions remain
			// explicit; a numeric fragment alone cannot verify a whole preparation sentence.
			return skill(purpose.group(1));
		}
		Matcher boosted = BOOSTED_SKILL.matcher(text);
		if (boosted.matches())
		{
			Assessment base = skill(boosted.group(1) + " " + boosted.group(2));
			if (base.getState() != State.MET) { return base; }
			return new Assessment(State.UNKNOWN, base.getDetail() + " Confirm the required "
				+ boosted.group(3) + " boost is prepared.", true);
		}
		Spellbook requiredBook = requiredSpellbook(lower);
		if (requiredBook != null)
		{
			Spellbook currentBook = request.getPlayer().getSpellbook();
			if (currentBook == null)
			{
				return known(State.UNKNOWN, "Current spellbook is unavailable; requires " + requiredBook.getDisplayName() + ".");
			}
			if (currentBook != requiredBook)
			{
				return known(State.UNMET, "Requires " + requiredBook.getDisplayName() + "; currently " + currentBook.getDisplayName() + ".");
			}
			if (lower.equals(requiredBook.getDisplayName().toLowerCase(Locale.ROOT) + " spellbook selected"))
			{
				return known(State.MET, "Current spellbook: " + currentBook.getDisplayName() + ".");
			}
		}
		// Preserve the unknown remainder of a mixed preparation requirement. Known failed
		// clauses still block it, even if the whole sentence has a saved confirmation.
		// Punctuation separates requirements: "quest; mask or helmet" requires the
		// quest whichever head item is chosen. Named facts retain their own punctuation.
		List<String> clauses = splitClauses(lower, "\\s*;\\s*|\\s*,(?!\\s*(?:or|and)\\b)\\s*");
		if (clauses.size() > 1) { return combine(clauses, true); }
		List<String> alternatives = splitClauses(lower, "\\s*,?\\s+or\\s+");
		if (alternatives.size() > 1) { return combine(alternatives, false); }
		clauses = splitClauses(lower, "\\s*,?\\s+and\\s+|\\s+&\\s+");
		if (clauses.size() > 1) { return combine(clauses, true); }
		if (lower.startsWith("quest:"))
		{
			return known(State.UNKNOWN, "This quest's current progress is unavailable.");
		}
		return new Assessment(State.UNKNOWN, request.getPlayer().isLoggedIn()
			? "This requirement has no verified client-state check; confirm it manually if completed."
			: "Log in to check current account progress.", request.getPlayer().isLoggedIn());
	}

	private List<String> splitClauses(String text, String delimiter)
	{
		List<String> clauses = new ArrayList<>();
		Matcher matcher = Pattern.compile(delimiter).matcher(text);
		int start = 0;
		while (matcher.find())
		{
			if (!insideFact(text, matcher.start(), matcher.end()))
			{
				clauses.add(text.substring(start, matcher.start()).trim());
				start = matcher.end();
			}
		}
		clauses.add(text.substring(start).trim());
		return clauses;
	}

	private boolean insideFact(String text, int start, int end)
	{
		for (String name : request.getPlayer().getAccountProgress().getRequirements().keySet())
		{
			if (name.isEmpty()) { continue; }
			for (int at = text.indexOf(name); at >= 0 && at < end; at = text.indexOf(name, at + 1))
			{
				int after = at + name.length();
				if (at <= start && after >= end
					&& (at == 0 || !Character.isLetterOrDigit(text.charAt(at - 1)))
					&& (after == text.length() || !Character.isLetterOrDigit(text.charAt(after))))
				{
					return true;
				}
			}
		}
		return false;
	}

	private Assessment combine(List<String> clauses, boolean all)
	{
		List<Assessment> results = new ArrayList<>();
		for (String clause : clauses) { results.add(assess(clause)); }
		if (all)
		{
			for (Assessment result : results)
			{
				if (result.state == State.UNMET) { return result; }
			}
			if (results.stream().allMatch(result -> result.state == State.MET))
			{
				boolean assumed = results.stream().anyMatch(result -> result.assumed);
				String detail = assumed ? results.stream().filter(result -> result.assumed)
					.map(Assessment::getDetail).distinct().collect(Collectors.joining(" "))
					+ " Other required conditions are satisfied." : "All required conditions are satisfied.";
				return new Assessment(State.MET, detail,
					results.stream().anyMatch(result -> result.manual),
					assumed);
			}
			boolean manual = results.stream().filter(result -> result.state == State.UNKNOWN)
				.allMatch(Assessment::canConfirm);
			return new Assessment(State.UNKNOWN, manual ? "Remaining conditions need manual confirmation."
				: "Some required account progress is unavailable.", manual);
		}
		for (Assessment result : results)
		{
			if (result.state == State.MET && !result.manual) { return result; }
		}
		for (Assessment result : results)
		{
			if (result.state == State.MET) { return result; }
		}
		if (results.stream().allMatch(result -> result.state == State.UNMET))
		{
			return known(State.UNMET, "None of the alternative requirements is met.");
		}
		boolean manual = results.stream().anyMatch(Assessment::canConfirm);
		return new Assessment(State.UNKNOWN, manual ? "An alternative needs manual confirmation."
			: "Account progress for an alternative is unavailable.", manual);
	}

	private Assessment skill(String text)
	{
		Matcher first = SKILL_FIRST.matcher(text);
		Matcher second = LEVEL_FIRST.matcher(text);
		String skill;
		int minimum;
		if (first.matches())
		{
			skill = first.group(1);
			minimum = Integer.parseInt(first.group(2));
		}
		else if (second.matches())
		{
			skill = second.group(2);
			minimum = Integer.parseInt(second.group(1));
		}
		else { return null; }
		skill = skill.toUpperCase(Locale.ROOT).replace("RUNECRAFTING", "RUNECRAFT");
		Integer actual = request.getPlayer().getLevels().get(skill);
		return known(actual == null ? State.UNKNOWN : actual >= minimum ? State.MET : State.UNMET,
			actual == null ? "Current " + skill + " level is unavailable; requires " + minimum + "."
				: "Current " + skill + " level " + actual + "; requires " + minimum + ".");
	}

	private static Spellbook requiredSpellbook(String requirement)
	{
		switch (requirement)
		{
			case "standard spellbook selected": return Spellbook.STANDARD;
			case "ancient magicks spellbook selected": return Spellbook.ANCIENT;
			case "lunar spellbook selected": return Spellbook.LUNAR;
			case "arceuus spellbook selected":
			case "arceuus spellbook with dark demonbane and mark of darkness selected":
			case "slayer 50; 65 magic; arceuus spellbook; ensouled bloodveld head":
			case "expert reanimation on arceuus spellbook; ensouled dagannoth head":
				return Spellbook.ARCEUUS;
			default: return null;
		}
	}

	void level(String skill, int minimum, List<String> blockers)
	{
		if (minimum > 0)
		{
			String failure = failure(skill + " >= " + minimum);
			if (failure != null) { add(blockers, failure); }
		}
	}

	private static Assessment known(State state, String detail)
	{
		return new Assessment(state, detail, false);
	}

	private static Assessment assumed(String detail)
	{
		return new Assessment(State.MET, detail, false, true);
	}

	private String previewBossAssignment()
	{
		// These concrete demi-boss variants count for their matching boss assignment.
		if ("calvar-ion".equals(planningMonsterId)) { return "vet-ion"; }
		if ("artio".equals(planningMonsterId)) { return "callisto"; }
		if ("spindel".equals(planningMonsterId)) { return "venenatis"; }
		return planningMonsterId;
	}

	private static void add(List<String> values, String value)
	{
		if (!values.contains(value)) { values.add(value); }
	}
}
