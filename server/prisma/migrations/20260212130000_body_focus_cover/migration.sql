-- CreateEnum
CREATE TYPE "BodyFocus" AS ENUM ('LEGS', 'ARMS', 'CHEST', 'BACK', 'SHOULDERS', 'CORE', 'FULL_BODY', 'CARDIO');

-- AlterTable
ALTER TABLE "Program" ADD COLUMN "coverImageUrl" TEXT;

-- AlterTable
ALTER TABLE "ProgramDay" ADD COLUMN "bodyFocus" "BodyFocus" NOT NULL DEFAULT 'FULL_BODY';
