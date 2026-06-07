import type { ExperienceLevel, TrainingGoal } from "@prisma/client";

/** Returns Prisma Program.assignmentKey */
export function resolveAssignmentKey(input: {
  gymAccess: boolean;
  experienceLevel: ExperienceLevel;
  trainingGoal: TrainingGoal;
}): string {
  const { gymAccess, experienceLevel, trainingGoal } = input;

  if (gymAccess && experienceLevel === "ADVANCED") return "GYM_ADVANCED";

  if (gymAccess && trainingGoal === "ENDURANCE") return "GYM_INTERMEDIATE";
  if (gymAccess && trainingGoal === "WEIGHT_LOSS" && experienceLevel !== "BEGINNER") return "GYM_INTERMEDIATE";
  if (gymAccess && trainingGoal === "MUSCLE_GAIN") return "GYM_INTERMEDIATE";

  if (gymAccess) return "GYM_INTERMEDIATE";

  if (trainingGoal === "WEIGHT_LOSS" || trainingGoal === "ENDURANCE") return "HOME_BASIC";
  if (trainingGoal === "MUSCLE_GAIN" && !gymAccess) return "HOME_BASIC";

  return "HOME_BASIC";
}
