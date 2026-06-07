import { Router } from "express";
import { prisma } from "../lib/prisma.js";
import { handleRouteError } from "../lib/errors.js";
import type { AuthedRequest } from "../middleware/auth.js";

const DEMO_MS = 30 * 24 * 60 * 60 * 1000;

export function createPremiumRouter() {
  const r = Router();

  r.post("/demo", async (req, res) => {
    try {
      const { sub } = (req as AuthedRequest).user;
      const user = await prisma.user.findUnique({ where: { id: sub } });
      if (!user) return res.status(404).end();

      const now = new Date();
      const base = user.premiumUntil && user.premiumUntil > now ? user.premiumUntil : now;
      const premiumUntil = new Date(base.getTime() + DEMO_MS);

      await prisma.user.update({
        where: { id: sub },
        data: { premiumUntil },
      });

      return res.json({ premiumUntil, hasPremium: true });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  return r;
}
