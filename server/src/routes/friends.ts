import { Router } from "express";
import { z } from "zod";
import { prisma } from "../lib/prisma.js";
import { buildAchievementsFromDefinitions } from "../lib/achievements.js";
import { computeMaxStreak } from "../lib/streak.js";
import { ErrorCodes, handleRouteError, sendApiError } from "../lib/errors.js";
import { validateBody } from "../middleware/validate.js";
import type { AuthedRequest } from "../middleware/auth.js";

function sortedPair(a: string, b: string): { low: string; high: string } {
  return a < b ? { low: a, high: b } : { low: b, high: a };
}

function displayName(user: { displayName: string | null; email: string }): string {
  const dn = user.displayName?.trim();
  if (dn) return dn;
  const local = user.email.split("@")[0] ?? user.email;
  return local || user.email;
}

async function assertFriendship(viewerId: string, otherId: string): Promise<boolean> {
  const { low, high } = sortedPair(viewerId, otherId);
  const f = await prisma.friendship.findUnique({
    where: {
      lowerUserId_higherUserId: { lowerUserId: low, higherUserId: high },
    },
  });
  return !!f;
}

const sendRequestSchema = z
  .object({
    email: z.string().trim().email().optional(),
    toUserId: z.string().min(1).optional(),
  })
  .refine((d) => !!(d.email ?? d.toUserId), { message: "Укажите email или пользователя" });

export function createFriendsRouter() {
  const r = Router();

  /** Список друзей */
  r.get("/", async (req, res) => {
    try {
      const { sub } = (req as unknown as AuthedRequest).user;
      const rows = await prisma.friendship.findMany({
        where: {
          OR: [{ lowerUserId: sub }, { higherUserId: sub }],
        },
        include: {
          lowerUser: { select: { id: true, email: true, displayName: true, avatarUrl: true } },
          higherUser: { select: { id: true, email: true, displayName: true, avatarUrl: true } },
        },
      });
      const friends = rows.map((row) => {
        const u = row.lowerUserId === sub ? row.higherUser : row.lowerUser;
        return {
          id: u.id,
          email: u.email,
          displayName: displayName(u),
          avatarUrl: u.avatarUrl,
        };
      });
      return res.json({ friends });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  /** Поиск пользователя по email (точное совпадение, без учёта регистра) */
  r.get("/search", async (req, res) => {
    try {
      const { sub } = (req as unknown as AuthedRequest).user;
      const raw = typeof req.query.email === "string" ? req.query.email.trim() : "";
      if (!raw) {
        return sendApiError(res, 400, ErrorCodes.VALIDATION_ERROR, "Укажите email в параметре email");
      }
      const emailNorm = raw.toLowerCase();
      const user = await prisma.user.findFirst({
        where: { email: { equals: emailNorm, mode: "insensitive" } },
        select: { id: true, email: true, displayName: true, avatarUrl: true },
      });
      if (!user) {
        return res.json({
          user: null as null,
          relationship: "NOT_FOUND" as const,
        });
      }
      if (user.id === sub) {
        return res.json({
          user: {
            id: user.id,
            email: user.email,
            displayName: displayName(user),
            avatarUrl: user.avatarUrl,
          },
          relationship: "SELF" as const,
        });
      }
      const { low, high } = sortedPair(sub, user.id);
      const friendshipRow = await prisma.friendship.findUnique({
        where: { lowerUserId_higherUserId: { lowerUserId: low, higherUserId: high } },
      });
      if (friendshipRow) {
        return res.json({
          user: {
            id: user.id,
            email: user.email,
            displayName: displayName(user),
            avatarUrl: user.avatarUrl,
          },
          relationship: "FRIEND" as const,
        });
      }
      const out = await prisma.friendRequest.findUnique({
        where: { fromUserId_toUserId: { fromUserId: sub, toUserId: user.id } },
      });
      if (out) {
        return res.json({
          user: {
            id: user.id,
            email: user.email,
            displayName: displayName(user),
            avatarUrl: user.avatarUrl,
          },
          relationship: "OUTGOING_PENDING" as const,
        });
      }
      const inc = await prisma.friendRequest.findUnique({
        where: { fromUserId_toUserId: { fromUserId: user.id, toUserId: sub } },
      });
      if (inc) {
        return res.json({
          user: {
            id: user.id,
            email: user.email,
            displayName: displayName(user),
            avatarUrl: user.avatarUrl,
          },
          relationship: "INCOMING_PENDING" as const,
        });
      }
      return res.json({
        user: {
          id: user.id,
          email: user.email,
          displayName: displayName(user),
          avatarUrl: user.avatarUrl,
        },
        relationship: "NONE" as const,
      });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  /** Заявки ко мне */
  r.get("/requests/incoming", async (req, res) => {
    try {
      const { sub } = (req as unknown as AuthedRequest).user;
      const rows = await prisma.friendRequest.findMany({
        where: { toUserId: sub },
        orderBy: { createdAt: "desc" },
        include: {
          fromUser: { select: { id: true, email: true, displayName: true, avatarUrl: true } },
        },
      });
      return res.json({
        requests: rows.map((row) => ({
          id: row.id,
          createdAt: row.createdAt.toISOString(),
          from: {
            id: row.fromUser.id,
            email: row.fromUser.email,
            displayName: displayName(row.fromUser),
            avatarUrl: row.fromUser.avatarUrl,
          },
        })),
      });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  /** Отправить заявку */
  r.post("/requests", validateBody(sendRequestSchema), async (req, res) => {
    try {
      const { sub } = (req as unknown as AuthedRequest).user;
      const body = req.body as z.infer<typeof sendRequestSchema>;

      let targetId = body.toUserId ?? null;
      if (!targetId && body.email) {
        const q = body.email.trim().toLowerCase();
        const u = await prisma.user.findFirst({
          where: { email: { equals: q, mode: "insensitive" } },
          select: { id: true },
        });
        targetId = u?.id ?? null;
      }
      if (!targetId) {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Пользователь не найден");
      }
      if (targetId === sub) {
        return sendApiError(res, 400, ErrorCodes.VALIDATION_ERROR, "Нельзя отправить заявку самому себе");
      }

      const { low, high } = sortedPair(sub, targetId);
      const existingFriend = await prisma.friendship.findUnique({
        where: { lowerUserId_higherUserId: { lowerUserId: low, higherUserId: high } },
      });
      if (existingFriend) {
        return sendApiError(res, 409, ErrorCodes.CONFLICT, "Уже в друзьях");
      }

      const reverse = await prisma.friendRequest.findUnique({
        where: { fromUserId_toUserId: { fromUserId: targetId, toUserId: sub } },
      });
      if (reverse) {
        await prisma.$transaction(async (tx) => {
          const { low, high } = sortedPair(sub, targetId);
          await tx.friendship.create({
            data: { lowerUserId: low, higherUserId: high },
          });
          await tx.friendRequest.deleteMany({
            where: {
              OR: [
                { fromUserId: sub, toUserId: targetId },
                { fromUserId: targetId, toUserId: sub },
              ],
            },
          });
        });
        return res.status(201).json({ ok: true, becameFriends: true });
      }

      const outgoing = await prisma.friendRequest.findUnique({
        where: { fromUserId_toUserId: { fromUserId: sub, toUserId: targetId } },
      });
      if (outgoing) {
        return sendApiError(res, 409, ErrorCodes.CONFLICT, "Заявка уже отправлена");
      }

      await prisma.friendRequest.create({
        data: { fromUserId: sub, toUserId: targetId },
      });
      return res.status(201).json({ ok: true, becameFriends: false });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.post("/requests/:requestId/accept", async (req, res) => {
    try {
      const { sub } = (req as unknown as AuthedRequest).user;
      const requestId = req.params.requestId;
      const row = await prisma.friendRequest.findUnique({
        where: { id: requestId },
      });
      if (!row || row.toUserId !== sub) {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Заявка не найдена");
      }
      await prisma.$transaction(async (tx) => {
        const { low, high } = sortedPair(row.fromUserId, row.toUserId);
        await tx.friendship.create({
          data: { lowerUserId: low, higherUserId: high },
        });
        await tx.friendRequest.deleteMany({
          where: {
            OR: [
              { fromUserId: row.fromUserId, toUserId: row.toUserId },
              { fromUserId: row.toUserId, toUserId: row.fromUserId },
            ],
          },
        });
      });
      return res.json({ ok: true });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.post("/requests/:requestId/decline", async (req, res) => {
    try {
      const { sub } = (req as unknown as AuthedRequest).user;
      const requestId = req.params.requestId;
      const row = await prisma.friendRequest.findUnique({
        where: { id: requestId },
      });
      if (!row || row.toUserId !== sub) {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Заявка не найдена");
      }
      await prisma.friendRequest.delete({ where: { id: requestId } });
      return res.json({ ok: true });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  /** Профиль друга: достижения и статистика */
  r.get("/profile/:userId", async (req, res) => {
    try {
      const { sub } = (req as unknown as AuthedRequest).user;
      const userId = req.params.userId;
      if (!(await assertFriendship(sub, userId))) {
        return sendApiError(res, 403, ErrorCodes.FORBIDDEN, "Доступно только друзьям");
      }

      const user = await prisma.user.findUnique({
        where: { id: userId },
        select: { id: true, email: true, displayName: true, avatarUrl: true },
      });
      if (!user) {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "Пользователь не найден");
      }

      const [workoutDates, workoutCount, weightLogCount, achievementDefs] = await Promise.all([
        prisma.workoutSession.findMany({
          where: { userId },
          select: { completedAt: true },
          orderBy: { completedAt: "asc" },
        }),
        prisma.workoutSession.count({ where: { userId } }),
        prisma.weightLog.count({ where: { userId } }),
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

      const completedAts = workoutDates.map((w) => w.completedAt);
      const maxStreak = computeMaxStreak(completedAts);
      const surveyDone = !!(await prisma.profile.findUnique({
        where: { userId },
        select: { surveyCompletedAt: true },
      }))?.surveyCompletedAt;

      const achievements = buildAchievementsFromDefinitions(achievementDefs, {
        workoutCount,
        maxStreak,
        surveyDone: !!surveyDone,
        weightLogCount,
      });

      return res.json({
        user: {
          id: user.id,
          email: user.email,
          displayName: displayName(user),
          avatarUrl: user.avatarUrl,
        },
        workoutCount,
        maxStreakDays: maxStreak,
        achievements,
      });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  /** Топ друзей (включая текущего пользователя) */
  r.get("/leaderboard", async (req, res) => {
    try {
      const { sub } = (req as unknown as AuthedRequest).user;
      const by = req.query.by === "maxStreak" ? "maxStreak" : "workouts";

      const selfUser = await prisma.user.findUnique({
        where: { id: sub },
        select: { id: true, email: true, displayName: true, avatarUrl: true },
      });
      if (!selfUser) {
        return sendApiError(res, 404, ErrorCodes.NOT_FOUND, "User not found");
      }

      const rows = await prisma.friendship.findMany({
        where: {
          OR: [{ lowerUserId: sub }, { higherUserId: sub }],
        },
        include: {
          lowerUser: { select: { id: true, email: true, displayName: true, avatarUrl: true } },
          higherUser: { select: { id: true, email: true, displayName: true, avatarUrl: true } },
        },
      });

      const friendIds = rows.map((row) => (row.lowerUserId === sub ? row.higherUser.id : row.lowerUser.id));
      const allIds = [...new Set([sub, ...friendIds])];

      const sessions = await prisma.workoutSession.findMany({
        where: { userId: { in: allIds } },
        select: { userId: true, completedAt: true },
      });

      const byUser = new Map<string, Date[]>();
      for (const s of sessions) {
        const arr = byUser.get(s.userId) ?? [];
        arr.push(s.completedAt);
        byUser.set(s.userId, arr);
      }

      type Item = {
        user: { id: string; email: string; displayName: string; avatarUrl: string | null };
        workoutCount: number;
        maxStreakDays: number;
        isMe: boolean;
      };

      const friendUserById = new Map<
        string,
        { id: string; email: string; displayName: string | null; avatarUrl: string | null }
      >();
      for (const row of rows) {
        const u = row.lowerUserId === sub ? row.higherUser : row.lowerUser;
        friendUserById.set(u.id, u);
      }

      const items: Item[] = allIds.map((id) => {
        const u = id === sub ? selfUser : friendUserById.get(id)!;
        const dates = byUser.get(id) ?? [];
        const workoutCount = dates.length;
        const maxStreakDays = computeMaxStreak(dates);
        return {
          user: {
            id: u.id,
            email: u.email,
            displayName: displayName(u),
            avatarUrl: u.avatarUrl,
          },
          workoutCount,
          maxStreakDays,
          isMe: id === sub,
        };
      });

      if (by === "maxStreak") {
        items.sort((a, b) => b.maxStreakDays - a.maxStreakDays || b.workoutCount - a.workoutCount);
      } else {
        items.sort((a, b) => b.workoutCount - a.workoutCount || b.maxStreakDays - a.maxStreakDays);
      }

      return res.json({ sortBy: by, items });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  /** Последние тренировки друзей */
  r.get("/feed", async (req, res) => {
    try {
      const { sub } = (req as unknown as AuthedRequest).user;
      const rows = await prisma.friendship.findMany({
        where: {
          OR: [{ lowerUserId: sub }, { higherUserId: sub }],
        },
        include: {
          lowerUser: { select: { id: true, email: true, displayName: true } },
          higherUser: { select: { id: true, email: true, displayName: true } },
        },
      });

      const friendIds = rows.map((row) => (row.lowerUserId === sub ? row.higherUser.id : row.lowerUser.id));
      if (friendIds.length === 0) {
        return res.json({ items: [] });
      }

      const friendMetaFeed = new Map<string, { id: string; email: string; displayName: string | null }>();
      for (const row of rows) {
        const u = row.lowerUserId === sub ? row.higherUser : row.lowerUser;
        friendMetaFeed.set(u.id, u);
      }

      const sessions = await prisma.workoutSession.findMany({
        where: { userId: { in: friendIds } },
        orderBy: { completedAt: "desc" },
        take: 40,
        include: {
          program: { select: { title: true } },
          programDay: { select: { title: true, dayIndex: true } },
        },
      });

      const items = sessions.map((s) => {
        const fri = friendMetaFeed.get(s.userId);
        if (!fri) {
          return null;
        }
        const dayPart = s.programDay.title?.trim()
          ? s.programDay.title
          : `День ${s.programDay.dayIndex}`;
        const workoutTitle = `${s.program.title} · ${dayPart}`;
        return {
          friendId: s.userId,
          friendDisplayName: displayName(fri),
          workoutTitle,
          completedAt: s.completedAt.toISOString(),
        };
      }).filter((x): x is NonNullable<typeof x> => x != null);

      return res.json({ items });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  return r;
}
