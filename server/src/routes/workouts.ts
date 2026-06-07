import { Router } from "express";
import { z } from "zod";
import { prisma } from "../lib/prisma.js";
import { BODY_FOCUS_LABEL_RU, BODY_FOCUS_ORDER } from "../lib/bodyFocusLabels.js";
import { computeStreak } from "../lib/streak.js";
import { ErrorCodes, handleRouteError, sendApiError } from "../lib/errors.js";
import { validateBody } from "../middleware/validate.js";
import type { AuthedRequest } from "../middleware/auth.js";

const completeSchema = z.object({
  programId: z.string().min(1),
  programDayId: z.string().min(1),
  durationSeconds: z.number().int().min(0).max(86400),
  startedAt: z.coerce.date().optional(),
  completedAt: z.coerce.date().optional(),
});

export function createWorkoutsRouter() {
  const r = Router();

  /** Все тренировочные дни доступных программ, сгруппированные по зоне (ноги, руки, пресс и т.д.) */
  r.get("/catalog", async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const user = await prisma.user.findUnique({ where: { id: sub } });
      const hasPremium = !!user?.premiumUntil && user.premiumUntil > new Date();

      const programs = await prisma.program.findMany({
        orderBy: { sortOrder: "asc" },
        include: {
          days: {
            orderBy: { dayIndex: "asc" },
            include: { _count: { select: { exercises: true } } },
          },
        },
      });

      type Item = {
        programId: string;
        programTitle: string;
        programDayId: string;
        dayIndex: number;
        dayTitle: string | null;
        /** Название тренировки для списка (без «дня») */
        workoutTitle: string;
        bodyFocus: string;
        coverImageUrl: string | null;
        exerciseCount: number;
        locked: boolean;
      };

      const byFocus = new Map<string, Item[]>();
      for (const p of programs) {
        const locked = p.isPremium && !hasPremium;
        for (const d of p.days) {
          const workoutTitle =
            (d.title && String(d.title).trim()) ||
            p.title;
          const item: Item = {
            programId: p.id,
            programTitle: p.title,
            programDayId: d.id,
            dayIndex: d.dayIndex,
            dayTitle: d.title,
            workoutTitle,
            bodyFocus: d.bodyFocus,
            coverImageUrl: p.coverImageUrl,
            exerciseCount: d._count.exercises,
            locked,
          };
          const list = byFocus.get(d.bodyFocus) ?? [];
          list.push(item);
          byFocus.set(d.bodyFocus, list);
        }
      }

      const groups = BODY_FOCUS_ORDER.filter((k) => (byFocus.get(k)?.length ?? 0) > 0).map((bodyFocus) => ({
        bodyFocus,
        label: BODY_FOCUS_LABEL_RU[bodyFocus],
        items: byFocus.get(bodyFocus) ?? [],
      }));

      return res.json({ groups });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.post("/complete", validateBody(completeSchema), async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const body = req.body as z.infer<typeof completeSchema>;
      const completedAt = body.completedAt ?? new Date();

      const day = await prisma.programDay.findFirst({
        where: { id: body.programDayId, programId: body.programId },
      });
      if (!day) {
        return sendApiError(res, 400, ErrorCodes.VALIDATION_ERROR, "Invalid program day for this program");
      }

      const user = await prisma.user.findUnique({ where: { id: sub } });
      const hasPremium = !!user?.premiumUntil && user.premiumUntil > new Date();
      const program = await prisma.program.findUnique({ where: { id: body.programId } });
      if (!program) {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Program not found");
      }
      if (program.isPremium && !hasPremium) {
        return sendApiError(res, 403, ErrorCodes.FORBIDDEN, "Premium subscription required");
      }

      const session = await prisma.workoutSession.create({
        data: {
          userId: sub,
          programId: body.programId,
          programDayId: body.programDayId,
          durationSeconds: body.durationSeconds,
          startedAt: body.startedAt ?? null,
          completedAt,
        },
      });

      return res.status(201).json({ session });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.get("/history", async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const items = await prisma.workoutSession.findMany({
        where: { userId: sub },
        orderBy: { completedAt: "desc" },
        take: 100,
        include: {
          program: { select: { id: true, title: true } },
          programDay: { select: { id: true, dayIndex: true, title: true } },
        },
      });
      return res.json({ items });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.get("/streak", async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const sessions = await prisma.workoutSession.findMany({
        where: { userId: sub },
        select: { completedAt: true },
      });
      const streak = computeStreak(sessions.map((s) => s.completedAt));
      return res.json({ streak });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  return r;
}
