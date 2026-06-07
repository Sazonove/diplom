import type { NextFunction, Request, Response } from "express";
import type { ZodSchema } from "zod";
import { ZodError } from "zod";
import { ErrorCodes, sendApiError } from "../lib/errors.js";

export function validateBody<T>(schema: ZodSchema<T>) {
  return (req: Request, res: Response, next: NextFunction) => {
    try {
      req.body = schema.parse(req.body);
      next();
    } catch (e) {
      if (e instanceof ZodError) {
        return sendApiError(res, 400, ErrorCodes.VALIDATION_ERROR, "Invalid request body", e.flatten());
      }
      throw e;
    }
  };
}

export function validateParams<T>(schema: ZodSchema<T>) {
  return (req: Request, res: Response, next: NextFunction) => {
    try {
      req.params = schema.parse(req.params) as Request["params"];
      next();
    } catch (e) {
      if (e instanceof ZodError) {
        return sendApiError(res, 400, ErrorCodes.VALIDATION_ERROR, "Invalid route params", e.flatten());
      }
      throw e;
    }
  };
}
