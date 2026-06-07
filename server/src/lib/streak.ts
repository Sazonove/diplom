function startOfUtcDay(d: Date): Date {
  return new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()));
}

function addUtcDays(d: Date, days: number): Date {
  const x = new Date(d);
  x.setUTCDate(x.getUTCDate() + days);
  return x;
}

function daysEqual(a: Date, b: Date): boolean {
  return startOfUtcDay(a).getTime() === startOfUtcDay(b).getTime();
}

function dayBefore(d: Date): Date {
  return addUtcDays(startOfUtcDay(d), -1);
}

/**
 * Streak: consecutive calendar days (UTC) with ≥1 completed workout.
 * Active if the most recent workout day is today or yesterday.
 */
export function computeStreak(completedAts: Date[], now: Date = new Date()): number {
  if (completedAts.length === 0) return 0;
  const uniq = [...new Set(completedAts.map((d) => startOfUtcDay(d).getTime()))]
    .sort((a, b) => b - a)
    .map((t) => new Date(t));

  const today = startOfUtcDay(now);
  const yesterday = addUtcDays(today, -1);
  const last = uniq[0];
  if (!daysEqual(last, today) && !daysEqual(last, yesterday)) return 0;

  let streak = 1;
  let expectedPrev = dayBefore(last);
  for (let i = 1; i < uniq.length; i++) {
    const d = uniq[i];
    if (daysEqual(d, expectedPrev)) {
      streak += 1;
      expectedPrev = dayBefore(d);
    } else if (startOfUtcDay(d).getTime() < expectedPrev.getTime()) {
      break;
    }
  }
  return streak;
}

/**
 * Longest run of consecutive calendar days (UTC), each day with ≥1 workout.
 */
export function computeMaxStreak(completedAts: Date[]): number {
  if (completedAts.length === 0) return 0;
  const uniq = [...new Set(completedAts.map((d) => startOfUtcDay(d).getTime()))].sort((a, b) => a - b);
  let maxRun = 1;
  let run = 1;
  for (let i = 1; i < uniq.length; i++) {
    const prev = new Date(uniq[i - 1]!);
    const expectedNext = addUtcDays(prev, 1).getTime();
    if (uniq[i] === expectedNext) {
      run += 1;
      maxRun = Math.max(maxRun, run);
    } else {
      run = 1;
    }
  }
  return maxRun;
}
