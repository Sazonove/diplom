import "dotenv/config";
import { createApp } from "./app.js";
import { prisma } from "./lib/prisma.js";

const PORT = Number(process.env.PORT ?? 3000);
const jwtSecret = process.env.JWT_SECRET;
if (!jwtSecret || jwtSecret.length < 16) {
  console.error(
    "JWT_SECRET в server/.env обязателен и должен быть не короче 16 символов.\n" +
      "Это секрет подписи на сервере, а не токен из ответа /api/auth/login.\n" +
      "Если процесс сразу завершается, в браузере при логине будет «Failed to fetch».\n"
  );
  process.exit(1);
}
const jwtExpiresIn = process.env.JWT_EXPIRES_IN ?? "7d";

async function main() {
  try {
    await prisma.$connect();
    console.log("PostgreSQL: connection OK");
  } catch (e) {
    console.error(
      "\nНе удалось подключиться к PostgreSQL. Запустите диагностику:\n" +
        "  cd server && npm run db:check\n\n" +
        "Типичные шаги:\n" +
        "  1) В корне репозитория: docker compose up -d\n" +
        "  2) В папке server: файл .env с DATABASE_URL (см. .env.example)\n" +
        "  3) Если меняли пароль в compose после первого запуска: docker compose down -v && docker compose up -d\n" +
        "  4) В .env порт 5433 (Docker), не 5432 — на Windows 5432 часто занят другим PostgreSQL\n\n" +
        "Детали Prisma:",
      e
    );
    process.exit(1);
  }

  const app = createApp({ jwtSecret: jwtSecret!, jwtExpiresIn });
  app.listen(PORT, () => {
    console.log(`Server listening on http://localhost:${PORT}`);
  });
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
