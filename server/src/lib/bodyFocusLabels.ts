export const BODY_FOCUS_ORDER = [
  "FULL_BODY",
  "LEGS",
  "ARMS",
  "CORE",
  "CHEST",
  "BACK",
  "SHOULDERS",
  "WEIGHT_LOSS",
  "CARDIO",
] as const;

export type BodyFocusCode = (typeof BODY_FOCUS_ORDER)[number];

export const BODY_FOCUS_LABEL_RU: Record<BodyFocusCode, string> = {
  LEGS: "Ноги",
  ARMS: "Руки",
  CHEST: "Грудь",
  BACK: "Спина",
  SHOULDERS: "Плечи",
  CORE: "Пресс / кор",
  FULL_BODY: "Всё тело",
  CARDIO: "Кардио",
  WEIGHT_LOSS: "Похудение",
};
