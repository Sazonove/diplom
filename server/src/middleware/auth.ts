import type { NextFunction, Request, Response } from "express";
import { verifyToken } from "../lib/jwt.js";
import type { JwtPayload } from "../lib/jwt.js";
import { ErrorCodes, sendApiError } from "../lib/errors.js";

export type AuthedRequest = Request & { user: JwtPayload };

export function authMiddleware(secret: string) {
  return (req: Request, res: Response, next: NextFunction) => {
    const header = req.headers.authorization;
    if (!header?.startsWith("Bearer ")) {
      return sendApiError(res, 401, ErrorCodes.UNAUTHORIZED, "Missing or invalid Authorization header");
    }
    const token = header.slice("Bearer ".length).trim();
    try {
      const payload = verifyToken(token, secret);
      (req as AuthedRequest).user = payload;
      return next();
    } catch {
      return sendApiError(res, 401, ErrorCodes.UNAUTHORIZED, "Invalid or expired token");
    }
  };
}

export function requireAdmin(req: Request, res: Response, next: NextFunction) {
  const u = (req as AuthedRequest).user;
  if (u.role !== "ADMIN") {
    return sendApiError(res, 403, ErrorCodes.FORBIDDEN, "Admin only");
  }
  return next();
}
