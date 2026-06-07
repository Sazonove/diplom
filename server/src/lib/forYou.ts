import type { Profile, TrainingGoal } from "@prisma/client";

export const GOAL_LABEL_RU: Record<TrainingGoal, string> = {
  WEIGHT_LOSS: "Похудение",
  MUSCLE_GAIN: "Набор массы",
  MAINTENANCE: "Поддержание формы",
  ENDURANCE: "Выносливость",
  GENERAL_FITNESS: "Общая активность",
};

export function buildForYouSummary(profile: Profile): { summary: string; tips: string[] } {
  const goal = profile.trainingGoal;
  const gym = profile.gymAccess ? "с доступом в зал" : "домашних тренировок";
  const level =
    profile.experienceLevel === "BEGINNER"
      ? "начинающий уровень"
      : profile.experienceLevel === "INTERMEDIATE"
        ? "средний уровень"
        : "продвинутый уровень";

  const summary = `Подобранный план: цель «${GOAL_LABEL_RU[goal]}», ${level}, акцент на ${gym}.`;

  const tips: string[] = [];
  switch (goal) {
    case "WEIGHT_LOSS":
      tips.push("Держите умеренный дефицит калорий и регулярность важнее интенсивности.");
      tips.push("Добавляйте шаги в течение дня между тренировками.");
      break;
    case "MUSCLE_GAIN":
      tips.push("Прогрессируйте нагрузку (вес/повторы) от недели к неделе.");
      tips.push("Спите не меньше 7 часов — восстановление критично для роста.");
      break;
    case "MAINTENANCE":
      tips.push("Поддерживайте стабильный режим: 2–4 тренировки в неделю.");
      break;
    case "ENDURANCE":
      tips.push("Чередуйте дни силы и лёгкого кардио.");
      tips.push("Следите за пульсом и временем отдыха между подходами.");
      break;
    default:
      tips.push("Сочетайте силовые и лёгкое кардио для общего здоровья.");
  }
  if (!profile.gymAccess) {
    tips.push("Дома используйте вес тела и доступный инвентарь; техника важнее веса.");
  } else {
    tips.push("В зале фиксируйте рабочие веса в заметках после каждой тренировки.");
  }
  return { summary, tips };
}
