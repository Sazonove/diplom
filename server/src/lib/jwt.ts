import jwt, { type SignOptions } from "jsonwebtoken";
import type { Role } from "@prisma/client";

export type JwtPayload = {
  sub: string;
  email: string;
  role: Role;
};

export function signToken(payload: JwtPayload, secret: string, expiresIn: string) {
  const options = { expiresIn } as SignOptions;
  return jwt.sign(payload, secret, options);
}

export function verifyToken(token: string, secret: string): JwtPayload {
  const decoded = jwt.verify(token, secret);
  if (typeof decoded !== "object" || decoded === null) throw new Error("Invalid token");
  const { sub, email, role } = decoded as Record<string, unknown>;
  if (typeof sub !== "string" || typeof email !== "string" || typeof role !== "string") {
    throw new Error("Invalid token payload");
  }
  return { sub, email, role: role as Role };
}
