import { Router } from "express";
import { z } from "zod";
import bcrypt from "bcryptjs";
import {
  AchievementCriterion,
  BodyFocus,
  ExperienceLevel,
  LibraryExerciseDifficulty,
  Role,
  Sex,
  TrainingGoal,
} from "@prisma/client";
import type { Prisma } from "@prisma/client";
import { prisma } from "../lib/prisma.js";
import { exerciseForApiResponse } from "../lib/exerciseGif.js";
import { ErrorCodes, handleRouteError, sendApiError } from "../lib/errors.js";
import { validateBody, validateParams } from "../middleware/validate.js";

const userIdParam = z.object({ id: z.string().min(1) });

const profileAdminSchema = z.object({
  heightCm: z.coerce.number().int().min(100).max(250).optional(),
  weightKg: z.coerce.number().min(30).max(300).optional(),
  age: z.coerce.number().int().min(10).max(120).optional(),
  sex: z.nativeEnum(Sex).nullable().optional(),
  experienceLevel: z.nativeEnum(ExperienceLevel).optional(),
  trainingGoal: z.nativeEnum(TrainingGoal).optional(),
  gymAccess: z.coerce.boolean().optional(),
  surveyCompletedAt: z.union([z.coerce.date(), z.null()]).optional(),
});

const patchUserSchema = z.object({
  displayName: z.string().trim().max(80).optional(),
  role: z.nativeEnum(Role).optional(),
  premiumUntil: z.union([z.coerce.date(), z.null()]).optional(),
  email: z.string().trim().email().max(320).optional(),
  password: z.string().min(8).max(128).optional(),
  profile: profileAdminSchema.optional(),
});

const createUserSchema = z.object({
  email: z.string().trim().email().max(320),
  password: z.string().min(8).max(128),
  role: z.nativeEnum(Role).optional(),
  displayName: z.string().trim().max(80).optional(),
});

const programCreateSchema = z.object({
  title: z.string().min(1).max(200),
  description: z.string().max(2000).optional().nullable(),
  coverImageUrl: z.union([z.string().url().max(2000), z.literal(""), z.null()]).optional(),
  isPremium: z.boolean().optional(),
  assignmentKey: z.string().min(1).max(64).optional().nullable(),
  sortOrder: z.number().int().optional(),
});

const programUpdateSchema = programCreateSchema.partial();

const bodyFocusEnum = z.enum([
  "LEGS",
  "ARMS",
  "CHEST",
  "BACK",
  "SHOULDERS",
  "CORE",
  "FULL_BODY",
  "CARDIO",
  "WEIGHT_LOSS",
]);

const dayCreateSchema = z.object({
  dayIndex: z.number().int().min(1).max(31),
  title: z.string().max(200).optional().nullable(),
  bodyFocus: bodyFocusEnum.optional(),
});

const dayUpdateSchema = z.object({
  dayIndex: z.number().int().min(1).max(31).optional(),
  title: z.string().max(200).optional().nullable(),
  bodyFocus: bodyFocusEnum.optional(),
});

const exerciseCreateSchema = z.object({
  orderIndex: z.number().int().min(1).max(100),
  name: z.string().min(1).max(200),
  sets: z.number().int().min(1).max(50),
  reps: z.number().int().min(1).max(200),
  restSeconds: z.number().int().min(0).max(600).optional(),
  exerciseSeconds: z.number().int().min(0).max(3600).optional(),
  gifUrl: z.union([z.string().url().max(2000), z.literal(""), z.null()]).optional(),
  imageUrl: z.union([z.string().url().max(2000), z.literal(""), z.null()]).optional(),
  libraryExerciseId: z.string().min(1).optional().nullable(),
});

const exerciseUpdateSchema = exerciseCreateSchema.partial();

const exerciseLibraryCreateSchema = z.object({
  name: z.string().min(1).max(200),
  gifUrl: z.string().url().max(2000),
  difficulty: z.nativeEnum(LibraryExerciseDifficulty).optional(),
  bodyFocuses: z.array(bodyFocusEnum).min(1).max(12),
  sortOrder: z.number().int().optional(),
});

const exerciseLibraryUpdateSchema = exerciseLibraryCreateSchema.partial().refine((d) => Object.keys(d).length > 0, {
  message: "Укажите хотя бы одно поле",
});

const exerciseFromLibrarySchema = z.object({
  libraryExerciseId: z.string().min(1),
  orderIndex: z.number().int().min(1).max(100),
  sets: z.number().int().min(1).max(50).optional(),
  reps: z.number().int().min(1).max(200).optional(),
  restSeconds: z.number().int().min(0).max(600).optional(),
  exerciseSeconds: z.number().int().min(0).max(3600).optional(),
});

const achievementIdParam = z.object({ id: z.string().min(1) });

const achievementCreateSchema = z
  .object({
    title: z.string().min(1).max(200),
    description: z.string().min(1).max(2000),
    criterion: z.nativeEnum(AchievementCriterion),
    criterionThreshold: z.number().int().min(1).max(100_000).optional().nullable(),
    sortOrder: z.number().int().optional(),
    isActive: z.boolean().optional(),
  })
  .superRefine((data, ctx) => {
    if (data.criterion !== AchievementCriterion.SURVEY_DONE) {
      if (data.criterionThreshold == null || data.criterionThreshold < 1) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Для этого условия укажите criterionThreshold (целое ≥ 1)",
          path: ["criterionThreshold"],
        });
      }
    }
  });

const achievementPatchSchema = z.object({
  title: z.string().min(1).max(200).optional(),
  description: z.string().min(1).max(2000).optional(),
  criterion: z.nativeEnum(AchievementCriterion).optional(),
  criterionThreshold: z.number().int().min(1).max(100_000).optional().nullable(),
  sortOrder: z.number().int().optional(),
  isActive: z.boolean().optional(),
});

const programIdParam = z.object({ programId: z.string().min(1) });
const dayIdParam = z.object({ dayId: z.string().min(1) });
const exerciseIdParam = z.object({ exerciseId: z.string().min(1) });
const libraryIdParam = z.object({ id: z.string().min(1) });

export function createAdminRouter() {
  const r = Router();

  r.get("/achievements", async (_req, res) => {
    try {
      const achievements = await prisma.achievementDefinition.findMany({
        orderBy: [{ sortOrder: "asc" }, { createdAt: "asc" }],
      });
      return res.json({ achievements });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.post("/achievements", validateBody(achievementCreateSchema), async (req, res) => {
    try {
      const body = req.body as z.infer<typeof achievementCreateSchema>;
      const criterionThreshold =
        body.criterion === AchievementCriterion.SURVEY_DONE ? null : body.criterionThreshold!;
      const row = await prisma.achievementDefinition.create({
        data: {
          title: body.title,
          description: body.description,
          criterion: body.criterion,
          criterionThreshold,
          sortOrder: body.sortOrder ?? 0,
          isActive: body.isActive ?? true,
        },
      });
      return res.status(201).json({ achievement: row });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.patch(
    "/achievements/:id",
    validateParams(achievementIdParam),
    validateBody(achievementPatchSchema),
    async (req, res) => {
      try {
        const { id } = req.params as z.infer<typeof achievementIdParam>;
        const body = req.body as z.infer<typeof achievementPatchSchema>;
        const existing = await prisma.achievementDefinition.findUnique({ where: { id } });
        if (!existing) {
          return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Achievement not found");
        }
        const criterion = body.criterion ?? existing.criterion;
        const threshold =
          body.criterionThreshold !== undefined ? body.criterionThreshold : existing.criterionThreshold;
        if (criterion !== AchievementCriterion.SURVEY_DONE && (threshold == null || threshold < 1)) {
          return sendApiError(
            res,
            400,
            ErrorCodes.VALIDATION_ERROR,
            "Для выбранного условия нужен числовой порог ≥ 1",
          );
        }
        const row = await prisma.achievementDefinition.update({
          where: { id },
          data: {
            ...(body.title !== undefined ? { title: body.title } : {}),
            ...(body.description !== undefined ? { description: body.description } : {}),
            ...(body.criterion !== undefined ? { criterion: body.criterion } : {}),
            ...(body.criterionThreshold !== undefined || body.criterion !== undefined
              ? {
                  criterionThreshold: criterion === AchievementCriterion.SURVEY_DONE ? null : threshold,
                }
              : {}),
            ...(body.sortOrder !== undefined ? { sortOrder: body.sortOrder } : {}),
            ...(body.isActive !== undefined ? { isActive: body.isActive } : {}),
          },
        });
        return res.json({ achievement: row });
      } catch (e) {
        return handleRouteError(res, e);
      }
    },
  );

  r.post("/users", validateBody(createUserSchema), async (req, res) => {
    try {
      const body = req.body as z.infer<typeof createUserSchema>;
      const exists = await prisma.user.findUnique({ where: { email: body.email } });
      if (exists) {
        return sendApiError(res, 409, ErrorCodes.CONFLICT, "Email already registered");
      }
      const passwordHash = await bcrypt.hash(body.password, 10);
      const user = await prisma.user.create({
        data: {
          email: body.email,
          passwordHash,
          role: body.role ?? Role.USER,
          ...(body.displayName !== undefined && body.displayName.trim().length > 0
            ? { displayName: body.displayName.trim() }
            : {}),
        },
        select: {
          id: true,
          email: true,
          displayName: true,
          role: true,
          premiumUntil: true,
          createdAt: true,
        },
      });
      return res.status(201).json({ user });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.get("/users", async (_req, res) => {
    try {
      const users = await prisma.user.findMany({
        orderBy: { createdAt: "desc" },
        select: {
          id: true,
          email: true,
          displayName: true,
          role: true,
          premiumUntil: true,
          createdAt: true,
          profile: {
            select: {
              heightCm: true,
              weightKg: true,
              age: true,
              sex: true,
              experienceLevel: true,
              trainingGoal: true,
              gymAccess: true,
              surveyCompletedAt: true,
            },
          },
        },
      });
      return res.json({ users });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.patch(
    "/users/:id",
    validateParams(userIdParam),
    validateBody(patchUserSchema),
    async (req, res) => {
      try {
        const { id } = req.params as z.infer<typeof userIdParam>;
        const body = req.body as z.infer<typeof patchUserSchema>;
        if (body.email !== undefined) {
          const clash = await prisma.user.findFirst({
            where: { email: body.email, NOT: { id } },
          });
          if (clash) {
            return sendApiError(res, 409, ErrorCodes.CONFLICT, "Email already in use");
          }
        }

        const data: {
          displayName?: string | null;
          role?: Role;
          premiumUntil?: Date | null;
          email?: string;
          passwordHash?: string;
        } = {};
        if (body.displayName !== undefined) {
          const d = body.displayName.trim();
          data.displayName = d.length > 0 ? d : null;
        }
        if (body.role !== undefined) data.role = body.role;
        if (body.premiumUntil !== undefined) data.premiumUntil = body.premiumUntil;
        if (body.email !== undefined) data.email = body.email;
        if (body.password !== undefined) {
          data.passwordHash = await bcrypt.hash(body.password, 10);
        }

        const user = await prisma.user.update({
          where: { id },
          data,
          select: {
            id: true,
            email: true,
            displayName: true,
            role: true,
            premiumUntil: true,
            createdAt: true,
          },
        });

        if (body.profile !== undefined) {
          const p = body.profile;
          const hasField = Object.values(p).some((v) => v !== undefined);
          if (hasField) {
            await prisma.profile.upsert({
              where: { userId: id },
              create: {
                userId: id,
                heightCm: p.heightCm ?? 170,
                weightKg: p.weightKg ?? 70,
                age: p.age ?? 25,
                sex: p.sex ?? null,
                experienceLevel: p.experienceLevel ?? ExperienceLevel.BEGINNER,
                trainingGoal: p.trainingGoal ?? TrainingGoal.GENERAL_FITNESS,
                gymAccess: p.gymAccess ?? false,
                surveyCompletedAt: p.surveyCompletedAt ?? null,
              },
              update: {
                ...(p.heightCm !== undefined ? { heightCm: p.heightCm } : {}),
                ...(p.weightKg !== undefined ? { weightKg: p.weightKg } : {}),
                ...(p.age !== undefined ? { age: p.age } : {}),
                ...(p.sex !== undefined ? { sex: p.sex } : {}),
                ...(p.experienceLevel !== undefined ? { experienceLevel: p.experienceLevel } : {}),
                ...(p.trainingGoal !== undefined ? { trainingGoal: p.trainingGoal } : {}),
                ...(p.gymAccess !== undefined ? { gymAccess: p.gymAccess } : {}),
                ...(p.surveyCompletedAt !== undefined ? { surveyCompletedAt: p.surveyCompletedAt } : {}),
              },
            });
          }
        }

        const userOut = await prisma.user.findUnique({
          where: { id },
          select: {
            id: true,
            email: true,
            displayName: true,
            role: true,
            premiumUntil: true,
            createdAt: true,
            profile: {
              select: {
                heightCm: true,
                weightKg: true,
                age: true,
                sex: true,
                experienceLevel: true,
                trainingGoal: true,
                gymAccess: true,
                surveyCompletedAt: true,
              },
            },
          },
        });
        return res.json({ user: userOut });
      } catch (e) {
        return handleRouteError(res, e);
      }
    }
  );

  r.get("/exercise-library", async (_req, res) => {
    try {
      const items = await prisma.exerciseLibrary.findMany({
        orderBy: [{ sortOrder: "asc" }, { name: "asc" }],
      });
      return res.json({ items });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.post("/exercise-library", validateBody(exerciseLibraryCreateSchema), async (req, res) => {
    try {
      const body = req.body as z.infer<typeof exerciseLibraryCreateSchema>;
      const row = await prisma.exerciseLibrary.create({
        data: {
          name: body.name,
          gifUrl: body.gifUrl,
          difficulty: body.difficulty ?? LibraryExerciseDifficulty.MEDIUM,
          bodyFocuses: body.bodyFocuses as unknown as Prisma.JsonArray,
          sortOrder: body.sortOrder ?? 0,
        },
      });
      return res.status(201).json({ item: row });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.put(
    "/exercise-library/:id",
    validateParams(libraryIdParam),
    validateBody(exerciseLibraryUpdateSchema),
    async (req, res) => {
      try {
        const { id } = req.params as z.infer<typeof libraryIdParam>;
        const body = req.body as z.infer<typeof exerciseLibraryUpdateSchema>;
        const row = await prisma.exerciseLibrary.update({
          where: { id },
          data: {
            ...(body.name !== undefined ? { name: body.name } : {}),
            ...(body.gifUrl !== undefined ? { gifUrl: body.gifUrl } : {}),
            ...(body.difficulty !== undefined ? { difficulty: body.difficulty } : {}),
            ...(body.bodyFocuses !== undefined
              ? { bodyFocuses: body.bodyFocuses as unknown as Prisma.JsonArray }
              : {}),
            ...(body.sortOrder !== undefined ? { sortOrder: body.sortOrder } : {}),
          },
        });
        return res.json({ item: row });
      } catch {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Exercise library entry not found");
      }
    },
  );

  r.delete("/exercise-library/:id", validateParams(libraryIdParam), async (req, res) => {
    try {
      const { id } = req.params as z.infer<typeof libraryIdParam>;
      await prisma.exerciseLibrary.delete({ where: { id } });
      return res.status(204).end();
    } catch {
      return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Exercise library entry not found");
    }
  });

  r.get("/programs", async (_req, res) => {
    try {
      const programs = await prisma.program.findMany({
        orderBy: { sortOrder: "asc" },
        include: {
          days: {
            orderBy: { dayIndex: "asc" },
            include: {
              exercises: {
                orderBy: { orderIndex: "asc" },
                include: { libraryExercise: { select: { gifUrl: true } } },
              },
            },
          },
        },
      });
      const programsOut = programs.map((p) => ({
        ...p,
        days: p.days.map((d) => ({
          ...d,
          exercises: d.exercises.map(exerciseForApiResponse),
        })),
      }));
      return res.json({ programs: programsOut });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.post("/programs", validateBody(programCreateSchema), async (req, res) => {
    try {
      const body = req.body as z.infer<typeof programCreateSchema>;
      const program = await prisma.program.create({
        data: {
          title: body.title,
          description: body.description ?? undefined,
          coverImageUrl:
            body.coverImageUrl === "" || body.coverImageUrl === null ? null : body.coverImageUrl,
          isPremium: body.isPremium ?? false,
          assignmentKey: body.assignmentKey ?? undefined,
          sortOrder: body.sortOrder ?? 0,
          days: {
            create: {
              dayIndex: 1,
              title: null,
              bodyFocus: BodyFocus.FULL_BODY,
            },
          },
        },
        include: {
          days: {
            orderBy: { dayIndex: "asc" },
            include: {
              exercises: {
                orderBy: { orderIndex: "asc" },
                include: { libraryExercise: { select: { gifUrl: true } } },
              },
            },
          },
        },
      });
      const programOut = {
        ...program,
        days: program.days.map((d) => ({
          ...d,
          exercises: d.exercises.map(exerciseForApiResponse),
        })),
      };
      return res.status(201).json({ program: programOut });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.put(
    "/programs/:id",
    validateParams(userIdParam),
    validateBody(programUpdateSchema),
    async (req, res) => {
      try {
        const { id } = req.params as z.infer<typeof userIdParam>;
        const body = req.body as z.infer<typeof programUpdateSchema>;
        const program = await prisma.program.update({
          where: { id },
          data: {
            ...(body.title !== undefined ? { title: body.title } : {}),
            ...(body.description !== undefined ? { description: body.description } : {}),
            ...(body.coverImageUrl !== undefined
              ? {
                  coverImageUrl:
                    body.coverImageUrl === "" || body.coverImageUrl === null
                      ? null
                      : body.coverImageUrl,
                }
              : {}),
            ...(body.isPremium !== undefined ? { isPremium: body.isPremium } : {}),
            ...(body.assignmentKey !== undefined ? { assignmentKey: body.assignmentKey } : {}),
            ...(body.sortOrder !== undefined ? { sortOrder: body.sortOrder } : {}),
          },
        });
        return res.json({ program });
      } catch {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Program not found");
      }
    }
  );

  r.delete("/programs/:id", validateParams(userIdParam), async (req, res) => {
    try {
      const { id } = req.params as z.infer<typeof userIdParam>;
      await prisma.program.delete({ where: { id } });
      return res.status(204).end();
    } catch {
      return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Program not found");
    }
  });

  r.post(
    "/programs/:programId/days",
    validateParams(programIdParam),
    validateBody(dayCreateSchema),
    async (req, res) => {
      try {
        const { programId } = req.params as z.infer<typeof programIdParam>;
        const body = req.body as z.infer<typeof dayCreateSchema>;
        const program = await prisma.program.findUnique({ where: { id: programId } });
        if (!program) {
          return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Program not found");
        }
        const dayCount = await prisma.programDay.count({ where: { programId } });
        if (dayCount >= 1) {
          return sendApiError(
            res,
            400,
            ErrorCodes.VALIDATION_ERROR,
            "В программе уже одна тренировка; добавление второго дня отключено.",
          );
        }
        const day = await prisma.programDay.create({
          data: {
            programId,
            dayIndex: body.dayIndex,
            title: body.title ?? undefined,
            ...(body.bodyFocus !== undefined ? { bodyFocus: body.bodyFocus as never } : {}),
          },
        });
        return res.status(201).json({ day });
      } catch (e) {
        return handleRouteError(res, e);
      }
    }
  );

  r.put(
    "/days/:dayId",
    validateParams(dayIdParam),
    validateBody(dayUpdateSchema),
    async (req, res) => {
      try {
        const { dayId } = req.params as z.infer<typeof dayIdParam>;
        const body = req.body as z.infer<typeof dayUpdateSchema>;
        const day = await prisma.programDay.update({
          where: { id: dayId },
          data: {
            ...(body.dayIndex !== undefined ? { dayIndex: body.dayIndex } : {}),
            ...(body.title !== undefined ? { title: body.title } : {}),
            ...(body.bodyFocus !== undefined ? { bodyFocus: body.bodyFocus as never } : {}),
          },
        });
        return res.json({ day });
      } catch {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Day not found");
      }
    }
  );

  r.delete("/days/:dayId", validateParams(dayIdParam), async (req, res) => {
    try {
      const { dayId } = req.params as z.infer<typeof dayIdParam>;
      await prisma.programDay.delete({ where: { id: dayId } });
      return res.status(204).end();
    } catch {
      return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Day not found");
    }
  });

  r.post(
    "/days/:dayId/exercises",
    validateParams(dayIdParam),
    validateBody(exerciseCreateSchema),
    async (req, res) => {
      try {
        const { dayId } = req.params as z.infer<typeof dayIdParam>;
        const body = req.body as z.infer<typeof exerciseCreateSchema>;
        const day = await prisma.programDay.findUnique({ where: { id: dayId } });
        if (!day) {
          return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Day not found");
        }
        let gifUrl: string | undefined =
          body.gifUrl === "" || body.gifUrl === null || body.gifUrl === undefined ? undefined : body.gifUrl;
        if (body.libraryExerciseId) {
          const lib = await prisma.exerciseLibrary.findUnique({ where: { id: body.libraryExerciseId } });
          gifUrl = lib?.gifUrl ?? undefined;
        }
        const exercise = await prisma.exercise.create({
          data: {
            programDayId: dayId,
            orderIndex: body.orderIndex,
            name: body.name,
            sets: body.sets,
            reps: body.reps,
            restSeconds: body.restSeconds ?? 60,
            exerciseSeconds: body.exerciseSeconds ?? 0,
            gifUrl,
            imageUrl: body.imageUrl === "" || body.imageUrl === null ? undefined : body.imageUrl,
            ...(body.libraryExerciseId ? { libraryExerciseId: body.libraryExerciseId } : {}),
          },
          include: { libraryExercise: { select: { gifUrl: true } } },
        });
        return res.status(201).json({ exercise: exerciseForApiResponse(exercise) });
      } catch (e) {
        return handleRouteError(res, e);
      }
    }
  );

  r.post(
    "/days/:dayId/exercises/from-library",
    validateParams(dayIdParam),
    validateBody(exerciseFromLibrarySchema),
    async (req, res) => {
      try {
        const { dayId } = req.params as z.infer<typeof dayIdParam>;
        const body = req.body as z.infer<typeof exerciseFromLibrarySchema>;
        const day = await prisma.programDay.findUnique({ where: { id: dayId } });
        if (!day) {
          return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Day not found");
        }
        const lib = await prisma.exerciseLibrary.findUnique({ where: { id: body.libraryExerciseId } });
        if (!lib) {
          return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Library exercise not found");
        }
        const exercise = await prisma.exercise.create({
          data: {
            programDayId: dayId,
            orderIndex: body.orderIndex,
            name: lib.name,
            sets: body.sets ?? 3,
            reps: body.reps ?? 12,
            restSeconds: body.restSeconds ?? 60,
            exerciseSeconds: body.exerciseSeconds ?? 0,
            gifUrl: lib.gifUrl,
            libraryExerciseId: lib.id,
          },
          include: { libraryExercise: { select: { gifUrl: true } } },
        });
        return res.status(201).json({ exercise: exerciseForApiResponse(exercise) });
      } catch (e) {
        return handleRouteError(res, e);
      }
    },
  );

  r.put(
    "/exercises/:exerciseId",
    validateParams(exerciseIdParam),
    validateBody(exerciseUpdateSchema),
    async (req, res) => {
      try {
        const { exerciseId } = req.params as z.infer<typeof exerciseIdParam>;
        const body = req.body as z.infer<typeof exerciseUpdateSchema>;
        const existing = await prisma.exercise.findUnique({
          where: { id: exerciseId },
          include: { libraryExercise: { select: { gifUrl: true } } },
        });
        if (!existing) {
          return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Exercise not found");
        }

        const nextLibId =
          body.libraryExerciseId === undefined ? existing.libraryExerciseId : body.libraryExerciseId;

        let gifUrlPatch: string | null | undefined;
        if (nextLibId) {
          const lib = await prisma.exerciseLibrary.findUnique({ where: { id: nextLibId } });
          gifUrlPatch = lib?.gifUrl ?? null;
        } else if (body.gifUrl !== undefined) {
          gifUrlPatch = body.gifUrl === "" || body.gifUrl === null ? null : body.gifUrl;
        }

        const exercise = await prisma.exercise.update({
          where: { id: exerciseId },
          data: {
            ...(body.orderIndex !== undefined ? { orderIndex: body.orderIndex } : {}),
            ...(body.name !== undefined ? { name: body.name } : {}),
            ...(body.sets !== undefined ? { sets: body.sets } : {}),
            ...(body.reps !== undefined ? { reps: body.reps } : {}),
            ...(body.restSeconds !== undefined ? { restSeconds: body.restSeconds } : {}),
            ...(body.exerciseSeconds !== undefined ? { exerciseSeconds: body.exerciseSeconds } : {}),
            ...(gifUrlPatch !== undefined ? { gifUrl: gifUrlPatch } : {}),
            ...(body.imageUrl !== undefined
              ? {
                  imageUrl:
                    body.imageUrl === "" || body.imageUrl === null ? null : body.imageUrl,
                }
              : {}),
            ...(body.libraryExerciseId !== undefined
              ? { libraryExerciseId: body.libraryExerciseId }
              : {}),
          },
          include: { libraryExercise: { select: { gifUrl: true } } },
        });
        return res.json({ exercise: exerciseForApiResponse(exercise) });
      } catch {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Exercise not found");
      }
    }
  );

  r.delete("/exercises/:exerciseId", validateParams(exerciseIdParam), async (req, res) => {
    try {
      const { exerciseId } = req.params as z.infer<typeof exerciseIdParam>;
      await prisma.exercise.delete({ where: { id: exerciseId } });
      return res.status(204).end();
    } catch {
      return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Exercise not found");
    }
  });

  return r;
}
