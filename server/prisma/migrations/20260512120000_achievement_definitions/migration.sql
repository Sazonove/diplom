-- CreateEnum
CREATE TYPE "AchievementCriterion" AS ENUM ('WORKOUT_COUNT', 'MAX_STREAK_DAYS', 'SURVEY_DONE', 'WEIGHT_LOG_COUNT');

-- CreateTable
CREATE TABLE "AchievementDefinition" (
    "id" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "description" TEXT NOT NULL,
    "criterion" "AchievementCriterion" NOT NULL,
    "criterionThreshold" INTEGER,
    "sortOrder" INTEGER NOT NULL DEFAULT 0,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "AchievementDefinition_pkey" PRIMARY KEY ("id")
);

-- Seed (initial set; editable via admin)
INSERT INTO "AchievementDefinition" ("id", "title", "description", "criterion", "criterionThreshold", "sortOrder", "isActive", "createdAt", "updatedAt")
VALUES
  ('ach_seed_first_workout', 'Первый шаг', 'Завершите первую тренировку', 'WORKOUT_COUNT', 1, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('ach_seed_ten', 'Десять тренировок', 'Завершите 10 тренировок', 'WORKOUT_COUNT', 10, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('ach_seed_twentyfive', 'Марафонец', 'Завершите 25 тренировок', 'WORKOUT_COUNT', 25, 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('ach_seed_streak7', 'Неделя без перерыва', '7 дней подряд с тренировкой', 'MAX_STREAK_DAYS', 7, 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('ach_seed_streak14', 'Две недели огня', '14 дней подряд с тренировкой', 'MAX_STREAK_DAYS', 14, 4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('ach_seed_survey', 'Знаю себя', 'Заполните анкету', 'SURVEY_DONE', NULL, 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('ach_seed_weight5', 'Контроль веса', 'Сделайте 5 записей веса', 'WEIGHT_LOG_COUNT', 5, 6, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
