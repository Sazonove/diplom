import type { AchievementCriterion, AchievementDefinition } from "@prisma/client";

export type AchievementCtx = {
  workoutCount: number;
  maxStreak: number;
  surveyDone: boolean;
  weightLogCount: number;
};

function unlockedFor(criterion: AchievementCriterion, threshold: number | null, ctx: AchievementCtx): boolean {
  switch (criterion) {
    case "WORKOUT_COUNT":
      return threshold != null && ctx.workoutCount >= threshold;
    case "MAX_STREAK_DAYS":
      return threshold != null && ctx.maxStreak >= threshold;
    case "SURVEY_DONE":
      return ctx.surveyDone;
    case "WEIGHT_LOG_COUNT":
      return threshold != null && ctx.weightLogCount >= threshold;
    default:
      return false;
  }
}

export type AchievementRow = Pick<
  AchievementDefinition,
  "id" | "title" | "description" | "criterion" | "criterionThreshold"
>;

export function buildAchievementsFromDefinitions(
  defs: AchievementRow[],
  ctx: AchievementCtx,
): Array<{ id: string; title: string; description: string; unlocked: boolean }> {
  return defs.map((d) => ({
    id: d.id,
    title: d.title,
    description: d.description,
    unlocked: unlockedFor(d.criterion, d.criterionThreshold, ctx),
  }));
}
