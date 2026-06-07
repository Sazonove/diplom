import "dotenv/config";
import {
  BodyFocus,
  ExperienceLevel,
  LibraryExerciseDifficulty,
  Prisma,
  PrismaClient,
  Role,
  Sex,
  TrainingGoal,
} from "@prisma/client";
import bcrypt from "bcryptjs";

const prisma = new PrismaClient();

const DEMO_GIF =
  "https://upload.wikimedia.org/wikipedia/commons/7/7f/Jumping_jack_exercise_animation.gif";

type LibDef = {
  name: string;
  bodyFocuses: BodyFocus[];
  difficulty: LibraryExerciseDifficulty;
};

const LIBRARY_DEFS: LibDef[] = [
  { name: "Отжимания", bodyFocuses: [BodyFocus.CHEST, BodyFocus.ARMS], difficulty: LibraryExerciseDifficulty.EASY },
  { name: "Планка", bodyFocuses: [BodyFocus.CORE], difficulty: LibraryExerciseDifficulty.MEDIUM },
  { name: "Приседания", bodyFocuses: [BodyFocus.LEGS], difficulty: LibraryExerciseDifficulty.EASY },
  { name: "Выпады", bodyFocuses: [BodyFocus.LEGS], difficulty: LibraryExerciseDifficulty.MEDIUM },
  { name: "Скручивания", bodyFocuses: [BodyFocus.CORE], difficulty: LibraryExerciseDifficulty.EASY },
  { name: "Джампинг-джеки", bodyFocuses: [BodyFocus.CARDIO, BodyFocus.WEIGHT_LOSS], difficulty: LibraryExerciseDifficulty.EASY },
  { name: "Жим штанги лёжа", bodyFocuses: [BodyFocus.CHEST, BodyFocus.ARMS], difficulty: LibraryExerciseDifficulty.HARD },
  { name: "Тяга штанги в наклоне", bodyFocuses: [BodyFocus.BACK, BodyFocus.ARMS], difficulty: LibraryExerciseDifficulty.HARD },
  { name: "Приседания со штангой", bodyFocuses: [BodyFocus.LEGS], difficulty: LibraryExerciseDifficulty.HARD },
  { name: "Сгибания ног", bodyFocuses: [BodyFocus.LEGS], difficulty: LibraryExerciseDifficulty.MEDIUM },
  { name: "Сгибания на бицепс", bodyFocuses: [BodyFocus.ARMS], difficulty: LibraryExerciseDifficulty.MEDIUM },
  { name: "Разгибания на трицепс", bodyFocuses: [BodyFocus.ARMS], difficulty: LibraryExerciseDifficulty.MEDIUM },
  { name: "Бёрпи", bodyFocuses: [BodyFocus.FULL_BODY, BodyFocus.WEIGHT_LOSS], difficulty: LibraryExerciseDifficulty.HARD },
  { name: "Горные альпинисты", bodyFocuses: [BodyFocus.CORE, BodyFocus.CARDIO], difficulty: LibraryExerciseDifficulty.MEDIUM },
  { name: "Скакалка (имитация)", bodyFocuses: [BodyFocus.CARDIO], difficulty: LibraryExerciseDifficulty.EASY },
  { name: "Велосипед (кор лёжа)", bodyFocuses: [BodyFocus.CORE], difficulty: LibraryExerciseDifficulty.EASY },
  { name: "Жим гантелей на наклонной", bodyFocuses: [BodyFocus.CHEST, BodyFocus.SHOULDERS], difficulty: LibraryExerciseDifficulty.HARD },
  { name: "Подтягивания", bodyFocuses: [BodyFocus.BACK, BodyFocus.ARMS], difficulty: LibraryExerciseDifficulty.HARD },
  { name: "Фермерская прогулка", bodyFocuses: [BodyFocus.FULL_BODY, BodyFocus.LEGS], difficulty: LibraryExerciseDifficulty.HARD },
];

type ExSpec = {
  name: string;
  orderIndex: number;
  sets: number;
  reps: number;
  restSeconds: number;
  exerciseSeconds?: number;
};

type ProgramSeed = {
  title: string;
  description: string;
  coverImageUrl: string;
  isPremium: boolean;
  assignmentKey?: string | null;
  sortOrder: number;
  bodyFocus: BodyFocus;
  dayTitle: string | null;
  exercises: ExSpec[];
};

/** Каждая программа — одна тренировка (один день). Фокус дня задаёт раздел в каталоге приложения. */
const PROGRAM_SEEDS: ProgramSeed[] = [
  {
    title: "Всё тело: утро дома",
    description: "Короткий полный цикл без инвентаря — разгон и базовые движения.",
    coverImageUrl:
      "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=1200&q=80",
    isPremium: false,
    sortOrder: 1,
    bodyFocus: BodyFocus.FULL_BODY,
    dayTitle: null,
    exercises: [
      { name: "Джампинг-джеки", orderIndex: 1, sets: 2, reps: 20, restSeconds: 30 },
      { name: "Приседания", orderIndex: 2, sets: 3, reps: 15, restSeconds: 60 },
      { name: "Отжимания", orderIndex: 3, sets: 3, reps: 12, restSeconds: 60 },
      { name: "Планка", orderIndex: 4, sets: 3, reps: 1, restSeconds: 45, exerciseSeconds: 30 },
    ],
  },
  {
    title: "Всё тело: сила и выносливость",
    description: "Интенсивнее: взрывные и устойчивые паттерны в одной сессии.",
    coverImageUrl:
      "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=1200&q=80",
    isPremium: false,
    sortOrder: 2,
    bodyFocus: BodyFocus.FULL_BODY,
    dayTitle: null,
    exercises: [
      { name: "Бёрпи", orderIndex: 1, sets: 3, reps: 8, restSeconds: 45 },
      { name: "Приседания", orderIndex: 2, sets: 4, reps: 12, restSeconds: 75 },
      { name: "Выпады", orderIndex: 3, sets: 3, reps: 10, restSeconds: 60 },
      { name: "Планка", orderIndex: 4, sets: 3, reps: 1, restSeconds: 40, exerciseSeconds: 40 },
    ],
  },
  {
    title: "Ноги: домашняя база",
    description: "Приседы и выпады без зала — удобно для старта.",
    coverImageUrl:
      "https://images.unsplash.com/photo-1434682881908-b43d0467b798?w=1200&q=80",
    isPremium: false,
    assignmentKey: "HOME_BASIC",
    sortOrder: 3,
    bodyFocus: BodyFocus.LEGS,
    dayTitle: null,
    exercises: [
      { name: "Приседания", orderIndex: 1, sets: 4, reps: 15, restSeconds: 75 },
      { name: "Выпады", orderIndex: 2, sets: 3, reps: 10, restSeconds: 60 },
      { name: "Планка", orderIndex: 3, sets: 3, reps: 1, restSeconds: 45, exerciseSeconds: 30 },
    ],
  },
  {
    title: "Ноги: зал",
    description: "Базовые движения со штангой и изоляция задней поверхности.",
    coverImageUrl:
      "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=1200&q=80",
    isPremium: false,
    sortOrder: 4,
    bodyFocus: BodyFocus.LEGS,
    dayTitle: null,
    exercises: [
      { name: "Приседания со штангой", orderIndex: 1, sets: 4, reps: 8, restSeconds: 150 },
      { name: "Сгибания ног", orderIndex: 2, sets: 3, reps: 12, restSeconds: 60 },
      { name: "Выпады", orderIndex: 3, sets: 3, reps: 10, restSeconds: 75 },
    ],
  },
  {
    title: "Руки: база без зала",
    description: "Бицепс, трицепс и грудь собственным весом.",
    coverImageUrl:
      "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=1200&q=80",
    isPremium: false,
    sortOrder: 5,
    bodyFocus: BodyFocus.ARMS,
    dayTitle: null,
    exercises: [
      { name: "Отжимания", orderIndex: 1, sets: 4, reps: 12, restSeconds: 60 },
      { name: "Сгибания на бицепс", orderIndex: 2, sets: 3, reps: 12, restSeconds: 60 },
      { name: "Разгибания на трицепс", orderIndex: 3, sets: 3, reps: 12, restSeconds: 60 },
    ],
  },
  {
    title: "Руки и спина в зале",
    description: "Тяга и изоляция рук — связка для осанки и объёма.",
    coverImageUrl:
      "https://images.unsplash.com/photo-1540497077202-7c8a3999166f?w=1200&q=80",
    isPremium: false,
    sortOrder: 6,
    bodyFocus: BodyFocus.ARMS,
    dayTitle: null,
    exercises: [
      { name: "Тяга штанги в наклоне", orderIndex: 1, sets: 4, reps: 10, restSeconds: 90 },
      { name: "Сгибания на бицепс", orderIndex: 2, sets: 3, reps: 12, restSeconds: 60 },
      { name: "Разгибания на трицепс", orderIndex: 3, sets: 3, reps: 12, restSeconds: 60 },
    ],
  },
  {
    title: "Пресс: старт",
    description: "Скручивания, велосипед и статика — мягкий вход.",
    coverImageUrl:
      "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=1200&q=80",
    isPremium: false,
    sortOrder: 7,
    bodyFocus: BodyFocus.CORE,
    dayTitle: null,
    exercises: [
      { name: "Скручивания", orderIndex: 1, sets: 3, reps: 20, restSeconds: 45 },
      { name: "Велосипед (кор лёжа)", orderIndex: 2, sets: 3, reps: 20, restSeconds: 40 },
      { name: "Планка", orderIndex: 3, sets: 3, reps: 1, restSeconds: 45, exerciseSeconds: 35 },
    ],
  },
  {
    title: "Пресс и кардио",
    description: "Динамика для кора и пульса в одной тренировке.",
    coverImageUrl:
      "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=1200&q=80",
    isPremium: false,
    sortOrder: 8,
    bodyFocus: BodyFocus.CORE,
    dayTitle: null,
    exercises: [
      { name: "Горные альпинисты", orderIndex: 1, sets: 3, reps: 24, restSeconds: 45 },
      { name: "Скручивания", orderIndex: 2, sets: 3, reps: 18, restSeconds: 40 },
      { name: "Джампинг-джеки", orderIndex: 3, sets: 3, reps: 30, restSeconds: 40 },
    ],
  },
  {
    title: "Жиросжигание: HIIT",
    description: "Короткая высокоинтенсивная сессия.",
    coverImageUrl:
      "https://images.unsplash.com/photo-1540555700479-4be289fbecef?w=1200&q=80",
    isPremium: false,
    sortOrder: 9,
    bodyFocus: BodyFocus.WEIGHT_LOSS,
    dayTitle: null,
    exercises: [
      { name: "Бёрпи", orderIndex: 1, sets: 4, reps: 10, restSeconds: 45 },
      { name: "Горные альпинисты", orderIndex: 2, sets: 3, reps: 20, restSeconds: 45 },
      { name: "Скакалка (имитация)", orderIndex: 3, sets: 3, reps: 40, restSeconds: 40 },
    ],
  },
  {
    title: "Силовой зал: база",
    description: "Назначение по анкете при доступе в зал (не премиум).",
    coverImageUrl:
      "https://images.unsplash.com/photo-1599058945522-790bde893791?w=1200&q=80",
    isPremium: false,
    assignmentKey: "GYM_INTERMEDIATE",
    sortOrder: 10,
    bodyFocus: BodyFocus.CHEST,
    dayTitle: null,
    exercises: [
      { name: "Жим штанги лёжа", orderIndex: 1, sets: 4, reps: 8, restSeconds: 120 },
      { name: "Тяга штанги в наклоне", orderIndex: 2, sets: 4, reps: 10, restSeconds: 90 },
      { name: "Сгибания на бицепс", orderIndex: 3, sets: 3, reps: 12, restSeconds: 60 },
    ],
  },
  {
    title: "Elite: полный сплит",
    description: "Продвинутая сессия на всё тело (премиум).",
    coverImageUrl:
      "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=1200&q=80",
    isPremium: true,
    assignmentKey: "GYM_ADVANCED",
    sortOrder: 11,
    bodyFocus: BodyFocus.FULL_BODY,
    dayTitle: null,
    exercises: [
      { name: "Жим гантелей на наклонной", orderIndex: 1, sets: 4, reps: 10, restSeconds: 90 },
      { name: "Подтягивания", orderIndex: 2, sets: 4, reps: 8, restSeconds: 120 },
      { name: "Фермерская прогулка", orderIndex: 3, sets: 3, reps: 20, restSeconds: 90 },
    ],
  },
];

async function main() {
  await prisma.workoutSession.deleteMany();
  await prisma.weightLog.deleteMany();
  await prisma.userProgramAssignment.deleteMany();
  await prisma.exercise.deleteMany();
  await prisma.exerciseLibrary.deleteMany();
  await prisma.programDay.deleteMany();
  await prisma.program.deleteMany();
  await prisma.profile.deleteMany();
  await prisma.user.deleteMany();

  const passwordHash = await bcrypt.hash("Admin123!", 10);
  const userHash = await bcrypt.hash("User123!", 10);

  const admin = await prisma.user.create({
    data: {
      email: "admin@diplom.local",
      passwordHash,
      role: Role.ADMIN,
      premiumUntil: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000),
    },
  });

  const demoUser = await prisma.user.create({
    data: {
      email: "user@diplom.local",
      passwordHash: userHash,
      role: Role.USER,
    },
  });

  const libByName: Record<string, { id: string; gifUrl: string }> = {};
  let sortOrder = 0;
  for (const d of LIBRARY_DEFS) {
    const row = await prisma.exerciseLibrary.create({
      data: {
        name: d.name,
        gifUrl: DEMO_GIF,
        difficulty: d.difficulty,
        bodyFocuses: d.bodyFocuses as unknown as Prisma.JsonArray,
        sortOrder: sortOrder++,
      },
    });
    libByName[d.name] = { id: row.id, gifUrl: row.gifUrl };
  }

  function exRow(spec: ExSpec) {
    const L = libByName[spec.name];
    if (!L) throw new Error(`Seed: нет в реестре: ${spec.name}`);
    return {
      orderIndex: spec.orderIndex,
      name: spec.name,
      sets: spec.sets,
      reps: spec.reps,
      restSeconds: spec.restSeconds,
      exerciseSeconds: spec.exerciseSeconds ?? 0,
      gifUrl: L.gifUrl,
      libraryExercise: { connect: { id: L.id } },
    };
  }

  const createdPrograms: { id: string; assignmentKey: string | null }[] = [];
  for (const p of PROGRAM_SEEDS) {
    const row = await prisma.program.create({
      data: {
        title: p.title,
        description: p.description,
        coverImageUrl: p.coverImageUrl,
        isPremium: p.isPremium,
        assignmentKey: p.assignmentKey ?? undefined,
        sortOrder: p.sortOrder,
        days: {
          create: {
            dayIndex: 1,
            title: p.dayTitle ?? p.title,
            bodyFocus: p.bodyFocus,
            exercises: { create: p.exercises.map(exRow) },
          },
        },
      },
    });
    createdPrograms.push({ id: row.id, assignmentKey: p.assignmentKey ?? null });
  }

  const assignProgram =
    createdPrograms.find((x) => x.assignmentKey === "HOME_BASIC")?.id ??
    createdPrograms[0]?.id;

  if (!assignProgram) throw new Error("Seed: нет программ для назначения");

  await prisma.profile.create({
    data: {
      userId: demoUser.id,
      heightCm: 175,
      weightKg: 72.5,
      age: 22,
      sex: Sex.MALE,
      experienceLevel: ExperienceLevel.BEGINNER,
      trainingGoal: TrainingGoal.GENERAL_FITNESS,
      gymAccess: false,
      surveyCompletedAt: new Date(),
    },
  });

  await prisma.userProgramAssignment.create({
    data: {
      userId: demoUser.id,
      programId: assignProgram,
    },
  });

  const t = Date.now();
  await prisma.weightLog.createMany({
    data: [
      { userId: demoUser.id, weightKg: 74.0, recordedAt: new Date(t - 21 * 86400000) },
      { userId: demoUser.id, weightKg: 73.2, recordedAt: new Date(t - 14 * 86400000) },
      { userId: demoUser.id, weightKg: 72.5, recordedAt: new Date(t) },
    ],
  });

  const imgPool = [
    "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800&q=80",
    "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800&q=80",
    "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800&q=80",
    "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=800&q=80",
    "https://images.unsplash.com/photo-1540497077202-7c8a3999166f?w=800&q=80",
    "https://images.unsplash.com/photo-1540555700479-4be289fbecef?w=800&q=80",
    "https://images.unsplash.com/photo-1576678927484-cc907957088c?w=800&q=80",
    "https://images.unsplash.com/photo-1599058945522-790bde893791?w=800&q=80",
    "https://images.unsplash.com/photo-1549060279-7e168fcee0c2?w=800&q=80",
    "https://images.unsplash.com/photo-1594736797933-d0401ba2fe65?w=800&q=80",
    "https://images.unsplash.com/photo-1598289432512-892348eab653?w=800&q=80",
    "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800&q=80",
  ];
  const exercisesAll = await prisma.exercise.findMany({ select: { id: true }, orderBy: { id: "asc" } });
  for (let i = 0; i < exercisesAll.length; i++) {
    await prisma.exercise.update({
      where: { id: exercisesAll[i].id },
      data: { imageUrl: imgPool[i % imgPool.length] },
    });
  }

  console.log("Seed OK. Admin:", admin.email, "User:", demoUser.id);
  console.log("Programs created:", createdPrograms.length);
}

main()
  .then(() => prisma.$disconnect())
  .catch(async (e) => {
    console.error(e);
    await prisma.$disconnect();
    process.exit(1);
  });
