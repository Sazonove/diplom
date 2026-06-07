-- CreateEnum
CREATE TYPE "LibraryExerciseDifficulty" AS ENUM ('EASY', 'MEDIUM', 'HARD');

-- CreateTable
CREATE TABLE "ExerciseLibrary" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "gifUrl" TEXT NOT NULL,
    "difficulty" "LibraryExerciseDifficulty" NOT NULL DEFAULT 'MEDIUM',
    "bodyFocuses" JSONB NOT NULL DEFAULT '[]',
    "sortOrder" INTEGER NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "ExerciseLibrary_pkey" PRIMARY KEY ("id")
);

-- AlterTable Exercise: add gif + library link, drop video
ALTER TABLE "Exercise" ADD COLUMN "gifUrl" TEXT;
ALTER TABLE "Exercise" ADD COLUMN "libraryExerciseId" TEXT;

UPDATE "Exercise" SET "gifUrl" = "imageUrl" WHERE "gifUrl" IS NULL AND "imageUrl" IS NOT NULL;

ALTER TABLE "Exercise" DROP COLUMN IF EXISTS "videoUrl";

-- AddForeignKey
ALTER TABLE "Exercise" ADD CONSTRAINT "Exercise_libraryExerciseId_fkey" FOREIGN KEY ("libraryExerciseId") REFERENCES "ExerciseLibrary"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- CreateIndex
CREATE INDEX "Exercise_libraryExerciseId_idx" ON "Exercise"("libraryExerciseId");
