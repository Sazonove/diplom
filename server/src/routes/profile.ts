import { Router } from "express";
import type { ExperienceLevel, Sex, TrainingGoal } from "@prisma/client";
import type { Multer } from "multer";
import multer from "multer";
import { z } from "zod";
import { prisma } from "../lib/prisma.js";
import { resolveAssignmentKey } from "../lib/programAssignment.js";
import { buildForYouSummary, GOAL_LABEL_RU } from "../lib/forYou.js";
import { buildAchievementsFromDefinitions } from "../lib/achievements.js";
import { computeMaxStreak } from "../lib/streak.js";
import { ErrorCodes, handleRouteError, sendApiError } from "../lib/errors.js";
import { validateBody } from "../middleware/validate.js";
import type { AuthedRequest } from "../middleware/auth.js";

const surveySchema = z.object({
  heightCm: z.number().int().min(100).max(250),
  weightKg: z.number().min(30).max(250),
  age: z.number().int().min(10).max(99),
  sex: z.enum(["MALE", "FEMALE", "OTHER"]).optional(),
  experienceLevel: z.enum(["BEGINNER", "INTERMEDIATE", "ADVANCED"]),
  trainingGoal: z.enum(["WEIGHT_LOSS", "MUSCLE_GAIN", "MAINTENANCE", "ENDURANCE", "GENERAL_FITNESS"]),
  gymAccess: z.boolean(),
});

const weightBodySchema = z.object({
  weightKg: z.number().min(30).max(250),
  recordedAt: z.coerce.date().optional(),
});

const patchTrainingGoalSchema = z.object({
  trainingGoal: z.enum(["WEIGHT_LOSS", "MUSCLE_GAIN", "MAINTENANCE", "ENDURANCE", "GENERAL_FITNESS"]),
});

const patchProfileSchema = z
  .object({
    displayName: z.string().trim().max(80).optional(),
    heightCm: z.number().int().min(100).max(250).optional(),
    weightKg: z.number().min(30).max(250).optional(),
    age: z.number().int().min(10).max(99).optional(),
    sex: z.enum(["MALE", "FEMALE", "OTHER"]).nullable().optional(),
    experienceLevel: z.enum(["BEGINNER", "INTERMEDIATE", "ADVANCED"]).optional(),
    trainingGoal: z.enum(["WEIGHT_LOSS", "MUSCLE_GAIN", "MAINTENANCE", "ENDURANCE", "GENERAL_FITNESS"]).optional(),
    gymAccess: z.boolean().optional(),
  })
  .refine((data) => Object.values(data).some((v) => v !== undefined), {
    message: "Укажите хотя бы одно поле",
  });

type ProfileRouterOpts = {
  avatarMulter: Multer;
};

export function createProfileRouter(opts: ProfileRouterOpts) {
  const r = Router();
  const uploadAvatar = opts.avatarMulter.single("avatar");

  r.get("/me", async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const [user, workoutDates, workoutCount, weightLogCount, achievementDefs] = await Promise.all([
        prisma.user.findUnique({
          where: { id: sub },
          include: { profile: true, assignment: { include: { program: true } } },
        }),
        prisma.workoutSession.findMany({
          where: { userId: sub },
          select: { completedAt: true },
          orderBy: { completedAt: "asc" },
        }),
        prisma.workoutSession.count({ where: { userId: sub } }),
        prisma.weightLog.count({ where: { userId: sub } }),
        prisma.achievementDefinition.findMany({
          where: { isActive: true },
          orderBy: [{ sortOrder: "asc" }, { createdAt: "asc" }],
          select: {
            id: true,
            title: true,
            description: true,
            criterion: true,
            criterionThreshold: true,
          },
        }),
      ]);
      if (!user) {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "User not found");
      }

      const now = new Date();
      const hasPremium = !!user.premiumUntil && user.premiumUntil > now;
      const completedAts = workoutDates.map((w) => w.completedAt);
      const maxStreak = computeMaxStreak(completedAts);
      const surveyDone = !!user.profile?.surveyCompletedAt;
      const achievements = buildAchievementsFromDefinitions(achievementDefs, {
        workoutCount,
        maxStreak,
        surveyDone,
        weightLogCount,
      });

      return res.json({
        user: {
          id: user.id,
          email: user.email,
          displayName: user.displayName,
          role: user.role,
          premiumUntil: user.premiumUntil,
          hasPremium,
          avatarUrl: user.avatarUrl,
        },
        profile: user.profile,
        assignment: user.assignment
          ? {
              programId: user.assignment.programId,
              programTitle: user.assignment.program.title,
              assignedAt: user.assignment.assignedAt,
            }
          : null,
        achievements,
      });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.post("/avatar", (req, res, next) => {
    uploadAvatar(req, res, (err: unknown) => {
      if (err instanceof multer.MulterError) {
        if (err.code === "LIMIT_FILE_SIZE") {
          return sendApiError(res, 413, ErrorCodes.VALIDATION_ERROR, "Файл слишком большой (макс. 2 МБ)");
        }
        return sendApiError(res, 400, ErrorCodes.VALIDATION_ERROR, "Не удалось загрузить файл");
      }
      if (err) return handleRouteError(res, err);
      next();
    });
  }, async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const file = req.file;
      if (!file) {
        return sendApiError(
          res,
          400,
          ErrorCodes.VALIDATION_ERROR,
          "Загрузите изображение в формате JPEG, PNG или WebP",
        );
      }
      const publicPath = `/uploads/avatars/${file.filename}`;
      await prisma.user.update({
        where: { id: sub },
        data: { avatarUrl: publicPath },
      });
      return res.json({ avatarUrl: publicPath });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.put("/survey", validateBody(surveySchema), async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const body = req.body as z.infer<typeof surveySchema>;

      const key = resolveAssignmentKey({
        gymAccess: body.gymAccess,
        experienceLevel: body.experienceLevel as ExperienceLevel,
        trainingGoal: body.trainingGoal as TrainingGoal,
      });

      const program = await prisma.program.findUnique({ where: { assignmentKey: key } });
      if (!program) {
        return res.status(500).json({
          error: { code: "INTERNAL", message: `Program for key ${key} not found` },
        });
      }

      const surveyCompletedAt = new Date();

      await prisma.$transaction(async (tx) => {
        await tx.profile.upsert({
          where: { userId: sub },
          create: {
            userId: sub,
            heightCm: body.heightCm,
            weightKg: body.weightKg,
            age: body.age,
            sex: (body.sex ?? undefined) as Sex | undefined,
            experienceLevel: body.experienceLevel as ExperienceLevel,
            trainingGoal: body.trainingGoal as TrainingGoal,
            gymAccess: body.gymAccess,
            surveyCompletedAt,
          },
          update: {
            heightCm: body.heightCm,
            weightKg: body.weightKg,
            age: body.age,
            sex: body.sex ?? null,
            experienceLevel: body.experienceLevel as ExperienceLevel,
            trainingGoal: body.trainingGoal as TrainingGoal,
            gymAccess: body.gymAccess,
            surveyCompletedAt,
          },
        });

        await tx.weightLog.create({
          data: {
            userId: sub,
            weightKg: body.weightKg,
            recordedAt: surveyCompletedAt,
          },
        });

        await tx.userProgramAssignment.upsert({
          where: { userId: sub },
          create: { userId: sub, programId: program.id },
          update: { programId: program.id, assignedAt: new Date() },
        });
      });

      return res.json({
        assignedProgramId: program.id,
        assignmentKey: key,
        surveyCompletedAt,
      });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.get("/weight-history", async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const items = await prisma.weightLog.findMany({
        where: { userId: sub },
        orderBy: { recordedAt: "asc" },
        take: 400,
        select: { id: true, weightKg: true, recordedAt: true },
      });
      return res.json({ items });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.post("/weight", validateBody(weightBodySchema), async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const body = req.body as z.infer<typeof weightBodySchema>;
      const profile = await prisma.profile.findUnique({ where: { userId: sub } });
      if (!profile?.surveyCompletedAt) {
        return sendApiError(res, 400, ErrorCodes.VALIDATION_ERROR, "Complete onboarding survey first");
      }
      const recordedAt = body.recordedAt ?? new Date();
      await prisma.$transaction([
        prisma.profile.update({
          where: { userId: sub },
          data: { weightKg: body.weightKg },
        }),
        prisma.weightLog.create({
          data: { userId: sub, weightKg: body.weightKg, recordedAt },
        }),
      ]);
      return res.status(201).json({ ok: true });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.patch("/", validateBody(patchProfileSchema), async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const body = req.body as z.infer<typeof patchProfileSchema>;
      const profile = await prisma.profile.findUnique({ where: { userId: sub } });
      if (!profile?.surveyCompletedAt) {
        return sendApiError(res, 400, ErrorCodes.VALIDATION_ERROR, "Сначала завершите анкету при входе");
      }

      const merged = {
        gymAccess: body.gymAccess ?? profile.gymAccess,
        experienceLevel: (body.experienceLevel ?? profile.experienceLevel) as ExperienceLevel,
        trainingGoal: (body.trainingGoal ?? profile.trainingGoal) as TrainingGoal,
      };
      const key = resolveAssignmentKey({
        gymAccess: merged.gymAccess,
        experienceLevel: merged.experienceLevel,
        trainingGoal: merged.trainingGoal,
      });
      const program = await prisma.program.findUnique({ where: { assignmentKey: key } });
      if (!program) {
        return res.status(500).json({
          error: { code: "INTERNAL", message: `Program for key ${key} not found` },
        });
      }

      const data: {
        heightCm?: number;
        weightKg?: number;
        age?: number;
        sex?: Sex | null;
        experienceLevel?: ExperienceLevel;
        trainingGoal?: TrainingGoal;
        gymAccess?: boolean;
      } = {};
      if (body.heightCm !== undefined) data.heightCm = body.heightCm;
      if (body.weightKg !== undefined) data.weightKg = body.weightKg;
      if (body.age !== undefined) data.age = body.age;
      if (body.sex !== undefined) data.sex = body.sex;
      if (body.experienceLevel !== undefined) data.experienceLevel = body.experienceLevel as ExperienceLevel;
      if (body.trainingGoal !== undefined) data.trainingGoal = body.trainingGoal as TrainingGoal;
      if (body.gymAccess !== undefined) data.gymAccess = body.gymAccess;

      const weightChanged =
        body.weightKg !== undefined && Math.abs(body.weightKg - profile.weightKg) > 1e-6;

      await prisma.$transaction(async (tx) => {
        if (Object.keys(data).length > 0) {
          await tx.profile.update({
            where: { userId: sub },
            data,
          });
        }
        if (body.displayName !== undefined) {
          const trimmed = body.displayName.trim();
          await tx.user.update({
            where: { id: sub },
            data: { displayName: trimmed.length > 0 ? trimmed : null },
          });
        }
        if (weightChanged && body.weightKg !== undefined) {
          await tx.weightLog.create({
            data: {
              userId: sub,
              weightKg: body.weightKg,
              recordedAt: new Date(),
            },
          });
        }
        await tx.userProgramAssignment.upsert({
          where: { userId: sub },
          create: { userId: sub, programId: program.id },
          update: { programId: program.id, assignedAt: new Date() },
        });
      });

      return res.json({
        ok: true,
        assignedProgramId: program.id,
        assignmentKey: key,
      });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.patch("/training-goal", validateBody(patchTrainingGoalSchema), async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const body = req.body as z.infer<typeof patchTrainingGoalSchema>;
      const profile = await prisma.profile.findUnique({ where: { userId: sub } });
      if (!profile?.surveyCompletedAt) {
        return sendApiError(res, 400, ErrorCodes.VALIDATION_ERROR, "Complete onboarding survey first");
      }

      const key = resolveAssignmentKey({
        gymAccess: profile.gymAccess,
        experienceLevel: profile.experienceLevel,
        trainingGoal: body.trainingGoal as TrainingGoal,
      });
      const program = await prisma.program.findUnique({ where: { assignmentKey: key } });
      if (!program) {
        return res.status(500).json({
          error: { code: "INTERNAL", message: `Program for key ${key} not found` },
        });
      }

      await prisma.$transaction([
        prisma.profile.update({
          where: { userId: sub },
          data: { trainingGoal: body.trainingGoal as TrainingGoal },
        }),
        prisma.userProgramAssignment.upsert({
          where: { userId: sub },
          create: { userId: sub, programId: program.id },
          update: { programId: program.id, assignedAt: new Date() },
        }),
      ]);

      return res.json({
        trainingGoal: body.trainingGoal,
        assignedProgramId: program.id,
        assignmentKey: key,
      });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.get("/for-you", async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const user = await prisma.user.findUnique({
        where: { id: sub },
        include: { profile: true, assignment: { include: { program: true } } },
      });
      if (!user) {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "User not found");
      }
      if (!user.profile || !user.profile.surveyCompletedAt) {
        return res.json({
          needsSurvey: true,
          message: "Завершите анкету после входа",
        });
      }

      const now = new Date();
      const hasPremium = !!user.premiumUntil && user.premiumUntil > now;
      const programs = await prisma.program.findMany({
        orderBy: { sortOrder: "asc" },
        select: { id: true, title: true, description: true, isPremium: true, coverImageUrl: true },
      });
      const { summary, tips } = buildForYouSummary(user.profile);
      const primaryId = user.assignment?.programId ?? null;
      const primaryTitle = user.assignment?.program.title ?? null;

      type Rec = {
        id: string;
        title: string;
        description: string | null;
        coverImageUrl: string | null;
        locked: boolean;
        isPrimary: boolean;
      };

      const recommendedPrograms: Rec[] = [];
      const seen = new Set<string>();
      const pushRec = (id: string, isPrimary: boolean) => {
        const p = programs.find((x) => x.id === id);
        if (!p || seen.has(p.id)) return;
        seen.add(p.id);
        recommendedPrograms.push({
          id: p.id,
          title: p.title,
          description: p.description,
          coverImageUrl: p.coverImageUrl,
          locked: p.isPremium && !hasPremium,
          isPrimary,
        });
      };

      if (primaryId) pushRec(primaryId, true);
      for (const p of programs) {
        if (recommendedPrograms.length >= 5) break;
        pushRec(p.id, false);
      }

      const otherPrograms = programs
        .filter((p) => p.id !== primaryId)
        .map((p) => ({
          id: p.id,
          title: p.title,
          description: p.description,
          coverImageUrl: p.coverImageUrl,
          locked: p.isPremium && !hasPremium,
        }));

      return res.json({
        needsSurvey: false,
        trainingGoal: user.profile.trainingGoal,
        goalLabel: GOAL_LABEL_RU[user.profile.trainingGoal],
        summary,
        tips,
        primaryProgramId: primaryId,
        primaryProgramTitle: primaryTitle,
        recommendedPrograms,
        otherPrograms,
      });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  return r;
}
