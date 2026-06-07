import type { Response } from "express";
import { Prisma } from "@prisma/client";
import { ZodError } from "zod";

export const ErrorCodes = {
  VALIDATION_ERROR: "VALIDATION_ERROR",
  UNAUTHORIZED: "UNAUTHORIZED",
  FORBIDDEN: "FORBIDDEN",
  NOT_FOUND: "NOT_FOUND",
  CONFLICT: "CONFLICT",
  INTERNAL: "INTERNAL",
} as const;

export type ErrorCode = (typeof ErrorCodes)[keyof typeof ErrorCodes];

export function sendApiError(
  res: Response,
  status: number,
  code: ErrorCode,
  message: string,
  details?: unknown
) {
  const body: { error: { code: string; message: string; details?: unknown } } = {
    error: { code, message },
  };
  if (details !== undefined) body.error.details = details;
  return res.status(status).json(body);
}

export function handleRouteError(res: Response, err: unknown) {
  if (err instanceof ZodError) {
    return sendApiError(res, 400, ErrorCodes.VALIDATION_ERROR, "Invalid request body", err.flatten());
  }

  if (err instanceof Prisma.PrismaClientInitializationError) {
    console.error(err);
    return sendApiError(
      res,
      503,
      ErrorCodes.INTERNAL,
      "Database unavailable. Start PostgreSQL (docker compose up -d) and check DATABASE_URL in server/.env"
    );
  }

  if (err instanceof Prisma.PrismaClientKnownRequestError) {
    console.error(err);
    if (err.code === "P2021" || err.code === "P2010" || err.code === "P2025") {
      return sendApiError(
        res,
        503,
        ErrorCodes.INTERNAL,
        "Database schema missing or out of date. Run: npx prisma migrate deploy && npm run db:seed"
      );
    }
    if (err.code === "P1001") {
      return sendApiError(
        res,
        503,
        ErrorCodes.INTERNAL,
        "Cannot reach database server. Check DATABASE_URL and that PostgreSQL is running."
      );
    }
  }

  console.error(err);
  const expose =
    process.env.NODE_ENV !== "production" || process.env.SHOW_ERROR_MESSAGE === "1";
  const message =
    expose && err instanceof Error && err.message
      ? err.message
      : "Internal server error";
  return sendApiError(res, 500, ErrorCodes.INTERNAL, message);
}
