/**
 * Запуск из папки server: npm run db:check
 * Проверяет, что .env читается и Prisma может подключиться к PostgreSQL.
 */
import { config } from "dotenv";
import path from "path";
import { fileURLToPath } from "url";
import { PrismaClient } from "@prisma/client";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const envPath = path.join(__dirname, "..", ".env");
config({ path: envPath });

const raw = process.env.DATABASE_URL?.trim();
if (!raw) {
  console.error(
    "DATABASE_URL пустой или не найден.\n" +
      `  Ожидается файл: ${envPath}\n` +
      "  Скопируйте .env.example → .env и задайте строку подключения.\n" +
      "  Запускайте команды из каталога server."
  );
  process.exit(1);
}

let parsed: URL;
try {
  parsed = new URL(raw);
} catch {
  console.error(
    "DATABASE_URL не является корректным URL. Пример:\n  postgresql://diplom:diplom@127.0.0.1:5433/diplom\n  (порт 5433 — Docker в этом проекте; 5432 часто занят локальным PostgreSQL на Windows)"
  );
  process.exit(1);
}

const user = parsed.username || "(пусто)";
const host = parsed.hostname;
const port = parsed.port || "5432";
const db = parsed.pathname.replace(/^\//, "") || "(пусто)";

console.log("Чтение .env:", envPath);
console.log(`Параметры: user=${user}, host=${host}, port=${port}, database=${db}`);

const prisma = new PrismaClient();
try {
  await prisma.$connect();
  console.log("Prisma: подключение к PostgreSQL успешно.");
} catch (e) {
  console.error("\n--- Ошибка подключения Prisma ---\n", e);
  console.error(`
Что сделать:
  1) Docker: в корне репозитория выполните  docker compose up -d
  2) Совпадение логина/пароля с docker-compose.yml (сейчас user=diplom, password=diplom, db=diplom)
  3) Если пароль в compose меняли ПОСЛЕ первого запуска — старый пароль остался в томе. Сброс данных:
       docker compose down -v
       docker compose up -d
     затем снова: npx prisma migrate deploy && npm run db:seed
  4) Убедитесь, что порт как в docker-compose (сейчас снаружи 5433, не 5432):
       DATABASE_URL=postgresql://diplom:diplom@127.0.0.1:5433/diplom
  5) Сохраните .env как UTF-8 без BOM (в Блокноте «Сохранить как» → кодировка UTF-8).
`);
  process.exit(1);
} finally {
  await prisma.$disconnect();
}
