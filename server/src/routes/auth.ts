import { Router } from "express";
import bcrypt from "bcryptjs";
import { z } from "zod";
import { prisma } from "../lib/prisma.js";
import { signToken } from "../lib/jwt.js";
import { ErrorCodes, handleRouteError, sendApiError } from "../lib/errors.js";
import { validateBody } from "../middleware/validate.js";

const registerSchema = z.object({
  email: z.string().trim().email().max(320),
  password: z.string().min(8).max(128),
});

const loginSchema = z.object({
  email: z.string().trim().email(),
  password: z.string().min(1),
});

export function createAuthRouter(jwtSecret: string, jwtExpiresIn: string) {
  const r = Router();

  r.post("/register", validateBody(registerSchema), async (req, res) => {
    try {
      const { email, password } = req.body as z.infer<typeof registerSchema>;
      const exists = await prisma.user.findUnique({ where: { email } });
      if (exists) {
        return sendApiError(res, 409, ErrorCodes.CONFLICT, "Email already registered");
      }
      const passwordHash = await bcrypt.hash(password, 10);
      const user = await prisma.user.create({
        data: { email, passwordHash },
      });
      const token = signToken({ sub: user.id, email: user.email, role: user.role }, jwtSecret, jwtExpiresIn);
      return res.status(201).json({
        token,
        user: { id: user.id, email: user.email, role: user.role },
      });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  r.post("/login", validateBody(loginSchema), async (req, res) => {
    try {
      const { email, password } = req.body as z.infer<typeof loginSchema>;
      const user = await prisma.user.findUnique({ where: { email } });
      if (!user) {
        return sendApiError(res, 401, ErrorCodes.UNAUTHORIZED, "Invalid credentials");
      }
      const ok = await bcrypt.compare(password, user.passwordHash);
      if (!ok) {
        return sendApiError(res, 401, ErrorCodes.UNAUTHORIZED, "Invalid credentials");
      }
      const token = signToken({ sub: user.id, email: user.email, role: user.role }, jwtSecret, jwtExpiresIn);
      return res.json({
        token,
        user: { id: user.id, email: user.email, role: user.role },
      });
    } catch (e) {
      return handleRouteError(res, e);
    }
  });

  return r;
}
