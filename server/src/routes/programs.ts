import { Router } from "express";
import { z } from "zod";
import { exerciseForApiResponse } from "../lib/exerciseGif.js";
import { prisma } from "../lib/prisma.js";
import { ErrorCodes, handleRouteError, sendApiError } from "../lib/errors.js";
import { validateParams } from "../middleware/validate.js";
import type { AuthedRequest } from "../middleware/auth.js";

const idParam = z.object({ id: z.string().min(1) });

export function createProgramsRouter() {
  const r = Router();

  async function userHasPremium(userId: string): Promise<boolean> {
    const u = await prisma.user.findUnique({ where: { id: userId } });
    if (!u?.premiumUntil) return false;
    return u.premiumUntil > new Date();
  }

  r.get("/", async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const hasPremium = await userHasPremium(sub);
      const programs = await prisma.program.findMany({
        orderBy: { sortOrder: "asc" },
        select: {
          id: true,
          title: true,
          description: true,
          coverImageUrl: true,
          isPremium: true,
          sortOrder: true,
          _count: { select: { days: true } },
        },
      });
      return res.json({
        programs: programs.map((p) => ({
          id: p.id,
          title: p.title,
          description: p.description,
          coverImageUrl: p.coverImageUrl,
          isPremium: p.isPremium,
          sortOrder: p.sortOrder,
          dayCount: p._count.days,
          locked: p.isPremium && !hasPremium,
        })),
      });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.get("/:id", validateParams(idParam), async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const { id } = req.params as z.infer<typeof idParam>;
      const hasPremium = await userHasPremium(sub);

      const program = await prisma.program.findUnique({
        where: { id },
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
      if (!program) {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Program not found");
      }

      if (program.isPremium && !hasPremium) {
        return sendApiError(res, 403, ErrorCodes.FORBIDDEN, "Premium subscription required");
      }

      const programOut = {
        ...program,
        days: program.days.map((d) => ({
          ...d,
          exercises: d.exercises.map(exerciseForApiResponse),
        })),
      };

      return res.json({ program: programOut });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  return r;
}
