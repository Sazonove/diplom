import type { Exercise } from "@prisma/client";

/** Упражнение с опциональной связью на реестр (только gifUrl из шаблона). */
export type ExerciseWithLibraryGif = Exercise & {
  libraryExercise?: { gifUrl: string } | null;
};

/**
 * Для ответа API: при привязке к реестру GIF всегда из шаблона; иначе — своё поле или запасной вариант из join.
 */
export function exerciseForApiResponse(ex: ExerciseWithLibraryGif): Exercise {
  const { libraryExercise, ...rest } = ex;
  const linked = !!rest.libraryExerciseId;
  const gifUrl = linked
    ? (libraryExercise?.gifUrl ?? null)
    : (rest.gifUrl ?? libraryExercise?.gifUrl ?? null);
  return {
    ...rest,
    gifUrl,
  };
}
