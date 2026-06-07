import fs from "fs";
import multer, { type Multer } from "multer";
import path from "path";
import { randomUUID } from "crypto";

export function ensureDir(dir: string) {
  fs.mkdirSync(dir, { recursive: true });
}

export function createAvatarMulter(uploadDir: string): Multer {
  ensureDir(uploadDir);
  return multer({
    storage: multer.diskStorage({
      destination: (_req, _file, cb) => cb(null, uploadDir),
      filename: (_req, file, cb) => {
        const ext = path.extname(file.originalname || "").toLowerCase();
        const allowed = [".jpg", ".jpeg", ".png", ".webp"];
        const safe = allowed.includes(ext) ? ext : ".jpg";
        cb(null, `${randomUUID()}${safe}`);
      },
    }),
    limits: { fileSize: 2 * 1024 * 1024 },
    fileFilter: (_req, file, cb) => {
      const ok = /^image\/(jpeg|png|webp)$/i.test(file.mimetype);
      cb(null, ok);
    },
  });
}
