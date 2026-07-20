/**
 * Synthetic Terra data for demo accounts.
 *
 * Terra decommissioned the DUMMY provider, and its Payload Simulator only pushes
 * webhooks (this app pulls via REST GET with `to_webhook=false`), so there is no
 * Terra-native way to populate a demo account. Instead, for a small allow-list of
 * demo UIDs, `GetDaily` / `GetSleep` return deterministic generated payloads shaped
 * exactly like Terra v2 `/daily` and `/sleep` responses. Real users never hit this
 * path — it is gated on the UID allow-list below.
 *
 * The data is deterministic per calendar date (seeded from the date's epoch day) so
 * repeated pulls, caching, and the derived Recovery / Pace-of-Aging screens stay
 * stable across requests.
 */

/** UIDs that receive synthetic data instead of a live Terra pull. */
const DEMO_UIDS = new Set<string>([
  "7Wj95sC1dhe8MvXrQncaFHZ8nUu2", // demo@hexisbi.test
]);

export function isDemoUid(uid: string): boolean {
  return DEMO_UIDS.has(uid);
}

export function syntheticDailyResponse(startDate: string, endDate: string): unknown {
  return {
    status: "success",
    type: "daily",
    data: eachDate(startDate, endDate).map(dailyRecord),
  };
}

export function syntheticSleepResponse(startDate: string, endDate: string): unknown {
  return {
    status: "success",
    type: "sleep",
    data: eachDate(startDate, endDate).map(sleepRecord),
  };
}

// ---------------------------------------------------------------------------
// Record builders
// ---------------------------------------------------------------------------

function dailyRecord(date: string): unknown {
  const r = rng(epochDay(date));
  const steps = 6000 + Math.floor(r() * 7000); // 6k–13k
  const distanceMetres = Math.round(steps * (0.68 + r() * 0.1));
  const activeSeconds = 2400 + Math.floor(r() * 4200); // 40–110 min
  const activeCalories = 300 + Math.floor(r() * 450);
  const bmrCalories = 1600 + Math.floor(r() * 150);
  const vo2max = round1(44 + r() * 8); // 44–52
  return {
    metadata: {
      start_time: `${date}T00:00:00+00:00`,
      end_time: `${date}T23:59:59+00:00`,
    },
    distance_data: {
      steps,
      distance_metres: distanceMetres,
      detailed: { step_samples: hourlyStepSamples(date, steps, r) },
    },
    calories_data: {
      net_activity_calories: activeCalories,
      total_burned_calories: bmrCalories + activeCalories,
      BMR_calories: bmrCalories,
    },
    active_durations_data: { activity_seconds: activeSeconds },
    oxygen_data: { vo2max_ml_per_min_per_kg: vo2max },
  };
}

function sleepRecord(date: string): unknown {
  // Wake day is `date`; the night starts the previous evening.
  const r = rng(epochDay(date) ^ 0x9e3779b9);
  const prev = addDays(date, -1);
  const bedHour = 22 + Math.floor(r() * 2); // 22–23
  const bedMinute = Math.floor(r() * 60);
  const wakeHour = 6 + Math.floor(r() * 2); // 06–07
  const wakeMinute = Math.floor(r() * 60);

  const startTime = `${prev}T${pad(bedHour)}:${pad(bedMinute)}:00+00:00`;
  const endTime = `${date}T${pad(wakeHour)}:${pad(wakeMinute)}:00+00:00`;
  const spanSeconds = Math.max(1, (Date.parse(endTime) - Date.parse(startTime)) / 1000);

  const efficiency = round2(0.84 + r() * 0.12); // 0.84–0.96
  const asleepSeconds = Math.round(spanSeconds * efficiency);
  const restingHr = 50 + Math.floor(r() * 9); // 50–58
  const avgHr = restingHr + 3 + Math.floor(r() * 4);
  const hrvRmssd = 42 + Math.floor(r() * 28); // 42–70
  const hrvSdnn = hrvRmssd + 10 + Math.floor(r() * 15);

  return {
    metadata: { start_time: startTime, end_time: endTime, is_nap: false },
    sleep_durations_data: {
      sleep_efficiency: efficiency,
      asleep: { duration_asleep_state_seconds: asleepSeconds },
      in_bed: { duration_in_bed_seconds: Math.round(spanSeconds) },
    },
    heart_rate_data: {
      summary: {
        resting_hr_bpm: restingHr,
        avg_hr_bpm: avgHr,
        avg_hrv_rmssd: hrvRmssd,
        avg_hrv_sdnn: hrvSdnn,
      },
    },
    sleep_stage_data: buildStages(startTime, endTime, r),
  };
}

interface StagePeriod {
  start: string;
  end: string;
}

function buildStages(
  startTime: string,
  endTime: string,
  r: () => number,
): Record<string, StagePeriod[]> {
  const startMs = Date.parse(startTime);
  const endMs = Date.parse(endTime);
  const light: StagePeriod[] = [];
  const deep: StagePeriod[] = [];
  const rem: StagePeriod[] = [];
  const wake: StagePeriod[] = [];

  // Cycle light → deep → light → rem (~90 min sleep cycles) with occasional brief wakes.
  const cycle = ["light", "deep", "light", "rem"] as const;
  const minSegmentMs = 5 * 60 * 1000;
  let t = startMs;
  let i = 0;
  while (t < endMs - minSegmentMs) {
    const stage = cycle[i % cycle.length];
    const durationMs = stageDurationMs(stage, r);
    const segEnd = Math.min(t + durationMs, endMs);
    const segment: StagePeriod = { start: isoUtc(t), end: isoUtc(segEnd) };
    if (stage === "deep") deep.push(segment);
    else if (stage === "rem") rem.push(segment);
    else light.push(segment);
    t = segEnd;

    if (r() < 0.15 && t < endMs - minSegmentMs) {
      const wakeMs = (2 + Math.floor(r() * 5)) * 60 * 1000;
      const wakeEnd = Math.min(t + wakeMs, endMs);
      wake.push({ start: isoUtc(t), end: isoUtc(wakeEnd) });
      t = wakeEnd;
    }
    i++;
  }

  return {
    light_periods: light,
    deep_periods: deep,
    rem_periods: rem,
    wake_periods: wake,
  };
}

function stageDurationMs(stage: "light" | "deep" | "rem", r: () => number): number {
  switch (stage) {
    case "deep":
      return (15 + Math.floor(r() * 20)) * 60 * 1000; // 15–35 min
    case "rem":
      return (12 + Math.floor(r() * 20)) * 60 * 1000; // 12–32 min
    default:
      return (18 + Math.floor(r() * 22)) * 60 * 1000; // 18–40 min
  }
}

function hourlyStepSamples(
  date: string,
  steps: number,
  r: () => number,
): StepSample[] {
  const weights: number[] = [];
  for (let h = 0; h < 24; h++) {
    let w = 0;
    if (h >= 7 && h <= 21) w = 0.5 + r(); // waking hours
    if (h === 8 || h === 12 || h === 18) w += 1.5; // commute / lunch / evening peaks
    weights.push(w);
  }
  const total = weights.reduce((a, b) => a + b, 0);
  if (total <= 0) return [];

  const samples: StepSample[] = [];
  for (let h = 0; h < 24; h++) {
    if (weights[h] <= 0) continue;
    const hourSteps = Math.round((steps * weights[h]) / total);
    if (hourSteps <= 0) continue;
    samples.push({
      timestamp: `${date}T${pad(h)}:00:00+00:00`,
      steps: hourSteps,
    });
  }
  return samples;
}

interface StepSample {
  timestamp: string;
  steps: number;
}

// ---------------------------------------------------------------------------
// Date + RNG helpers
// ---------------------------------------------------------------------------

/** Inclusive list of `YYYY-MM-DD` dates in [start, end], never past today (UTC). */
function eachDate(start: string, end: string): string[] {
  const today = new Date().toISOString().slice(0, 10);
  const out: string[] = [];
  let cursor = Date.parse(`${start}T00:00:00Z`);
  const last = Date.parse(`${end}T00:00:00Z`);
  if (Number.isNaN(cursor) || Number.isNaN(last)) return out;
  while (cursor <= last) {
    const iso = new Date(cursor).toISOString().slice(0, 10);
    if (iso <= today) out.push(iso);
    cursor += 86_400_000;
  }
  return out;
}

function addDays(date: string, days: number): string {
  return new Date(Date.parse(`${date}T00:00:00Z`) + days * 86_400_000)
    .toISOString()
    .slice(0, 10);
}

function epochDay(date: string): number {
  return Math.floor(Date.parse(`${date}T00:00:00Z`) / 86_400_000);
}

function isoUtc(ms: number): string {
  return new Date(ms).toISOString();
}

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

function round1(n: number): number {
  return Math.round(n * 10) / 10;
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}

/** mulberry32 — small deterministic PRNG seeded per calendar date. */
function rng(seed: number): () => number {
  let state = seed >>> 0;
  return function next(): number {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4_294_967_296;
  };
}
