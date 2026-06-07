import express from "express";
import cors from "cors";
import path from "path";
import { fileURLToPath } from "url";
import type { Multer } from "multer";
import { prisma } from "./lib/prisma.js";
import { createAvatarMulter } from "./lib/avatarMulter.js";
import { authMiddleware, requireAdmin } from "./middleware/auth.js";
import { createAuthRouter } from "./routes/auth.js";
import { createProfileRouter } from "./routes/profile.js";
import { createProgramsRouter } from "./routes/programs.js";
import { createWorkoutsRouter } from "./routes/workouts.js";
import { createPremiumRouter } from "./routes/premium.js";
import { createAdminRouter } from "./routes/admin.js";
import { createFriendsRouter } from "./routes/friends.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const uploadsRoot = path.join(__dirname, "../uploads");
const avatarsDir = path.join(uploadsRoot, "avatars");
const avatarMulter: Multer = createAvatarMulter(avatarsDir);

export function createApp(config: { jwtSecret: string; jwtExpiresIn: string }) {
  const app = express();
  app.use(cors({ origin: true, credentials: true }));
  app.use(express.json({ limit: "1mb" }));
  app.use("/uploads", express.static(uploadsRoot));

  app.get("/api/health", async (_req, res) => {
    try {
      await prisma.$queryRaw`SELECT 1`;
      res.json({ status: "ok", database: "ok" });
    } catch (e) {
      console.error("GET /api/health database check failed:", e);
      res.status(503).json({
        status: "error",
        database: "down",
        error: {
          code: "DATABASE_DOWN",
          message:
            "Cannot connect to PostgreSQL. Run `docker compose up -d` from the repo root, then in server: `npx prisma migrate deploy` and check DATABASE_URL in .env",
        },
      });
    }
  });

  app.use("/api/auth", createAuthRouter(config.jwtSecret, config.jwtExpiresIn));

  const protect = authMiddleware(config.jwtSecret);
  app.use("/api/profile", protect, createProfileRouter({ avatarMulter }));
  app.use("/api/programs", protect, createProgramsRouter());
  app.use("/api/workouts", protect, createWorkoutsRouter());
  app.use("/api/premium", protect, createPremiumRouter());
  app.use("/api/admin", protect, requireAdmin, createAdminRouter());
  app.use("/api/friends", protect, createFriendsRouter());

  app.use(express.static(path.join(__dirname, "../public")));

  return app;
}
