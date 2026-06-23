import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { CallableRequest, HttpsError, onCall } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";

initializeApp();

const threedlookApiToken = defineSecret("THREEDLOOK_API_TOKEN");
const devTerraDevId = defineSecret("DEV_TERRA_DEV_ID");
const devTerraApiKey = defineSecret("DEV_TERRA_API_KEY");
const prodTerraDevId = defineSecret("PROD_TERRA_DEV_ID");
const prodTerraApiKey = defineSecret("PROD_TERRA_API_KEY");

const region = "us-central1";
const terraBaseUrl = "https://api.tryterra.co/v2";
const threeDLookBaseUrl = "https://backend.fitxpress.3dlook.me/api/1.0";
const terraSuccessRedirectUrl = "fitxpress://terra/success";
const terraFailureRedirectUrl = "fitxpress://terra/failure";

// App Check rollout plan:
// Add `enforceAppCheck: true` to these callable options only after both Android and iOS
// are registered, tested, and enforcement is enabled gradually in Firebase Console.
const callableOptions = {
  region,
  cors: true,
  maxInstances: 20,
};

const devTerraSecretOptions = {
  ...callableOptions,
  secrets: [devTerraDevId, devTerraApiKey],
};

const prodTerraSecretOptions = {
  ...callableOptions,
  secrets: [prodTerraDevId, prodTerraApiKey],
};

const threeDLookSecretOptions = {
  ...callableOptions,
  secrets: [threedlookApiToken],
  timeoutSeconds: 120,
  memory: "512MiB" as const,
};

type TerraEnvironment = "dev" | "prod";

type TerraSecrets = {
  environment: TerraEnvironment;
  devId: ReturnType<typeof defineSecret>;
  apiKey: ReturnType<typeof defineSecret>;
};

function terraHandlers(secrets: TerraSecrets) {
  return {
    generateAuthToken: async (request: CallableRequest) => {
      requireAuth(request.auth?.uid);

      const response = await terraFetch(secrets, "/auth/generateAuthToken", {
        method: "POST",
        body: "{}",
        headers: { "Content-Type": "application/json" },
      });
      const json = await parseJsonResponse(response, `Terra ${secrets.environment} generateAuthToken`);
      const token = readString(json, "token");
      if (!token) {
        throw new HttpsError("internal", "Terra did not return an auth token");
      }
      return { token };
    },

    authenticateUser: async (request: CallableRequest) => {
      const uid = requireAuth(request.auth?.uid);
      const resource = requireString(request.data?.resource, "resource");

      const url = new URL(`${terraBaseUrl}/auth/authenticateUser`);
      url.searchParams.set("resource", resource);
      url.searchParams.set("reference_id", uid);
      url.searchParams.set("auth_success_redirect_url", terraSuccessRedirectUrl);
      url.searchParams.set("auth_failure_redirect_url", terraFailureRedirectUrl);
      url.searchParams.set("language", "en");

      const response = await terraFetch(secrets, url, {
        method: "POST",
        body: "{}",
        headers: { "Content-Type": "application/json" },
      });
      const json = await parseJsonResponse(response, `Terra ${secrets.environment} authenticateUser`);
      const authUrl = readString(json, "auth_url");
      if (!authUrl) {
        throw new HttpsError("internal", "Terra did not return an auth URL");
      }
      return {
        authUrl,
        userId: readString(json, "user_id"),
      };
    },

    generateWidgetSession: async (request: CallableRequest) => {
      const uid = requireAuth(request.auth?.uid);
      const providers = requireString(request.data?.providers, "providers");

      const response = await terraFetch(secrets, "/auth/generateWidgetSession", {
        method: "POST",
        body: JSON.stringify({
          reference_id: uid,
          auth_success_redirect_url: terraSuccessRedirectUrl,
          auth_failure_redirect_url: terraFailureRedirectUrl,
          providers,
          language: "en",
        }),
        headers: { "Content-Type": "application/json" },
      });
      const json = await parseJsonResponse(response, `Terra ${secrets.environment} generateWidgetSession`);
      const url = readString(json, "url");
      if (!url) {
        throw new HttpsError("internal", "Terra did not return a widget URL");
      }
      return { url };
    },

    getDaily: async (request: CallableRequest) => {
      const uid = requireAuth(request.auth?.uid);
      const terraUserId = requireString(request.data?.terraUserId, "terraUserId");
      await requireTerraConnection(uid, terraUserId, secrets.environment);

      const url = terraDataUrl("/daily", request.data);
      const response = await terraFetch(secrets, url, { method: "GET" });
      const json = await parseJsonResponse(response, `Terra ${secrets.environment} daily`);
      return projectByDetail(json, request.data);
    },

    getSleep: async (request: CallableRequest) => {
      const uid = requireAuth(request.auth?.uid);
      const terraUserId = requireString(request.data?.terraUserId, "terraUserId");
      await requireTerraConnection(uid, terraUserId, secrets.environment);

      const url = terraDataUrl("/sleep", request.data);
      const response = await terraFetch(secrets, url, { method: "GET" });
      const json = await parseJsonResponse(response, `Terra ${secrets.environment} sleep`);
      return projectByDetail(json, request.data);
    },

    getUserInfo: async (request: CallableRequest) => {
      const uid = requireAuth(request.auth?.uid);
      const terraUserId = requireString(request.data?.terraUserId, "terraUserId");

      const url = new URL(`${terraBaseUrl}/userInfo`);
      url.searchParams.set("user_id", terraUserId);
      const response = await terraFetch(secrets, url, { method: "GET" });
      const json = await parseJsonResponse(response, `Terra ${secrets.environment} userInfo`);
      requireOwnedByCaller(json, uid);
      return json;
    },

    deauthenticateUser: async (request: CallableRequest) => {
      const uid = requireAuth(request.auth?.uid);
      const terraUserId = requireString(request.data?.terraUserId, "terraUserId");
      await requireTerraConnection(uid, terraUserId, secrets.environment);

      const url = new URL(`${terraBaseUrl}/auth/deauthenticateUser`);
      url.searchParams.set("user_id", terraUserId);
      const response = await terraFetch(secrets, url, { method: "DELETE" });
      if (!response.ok && response.status !== 404) {
        await throwHttpError(response, `Terra ${secrets.environment} deauthenticateUser`);
      }
      return { ok: true };
    },
  };
}

const devTerra = terraHandlers({
  environment: "dev",
  devId: devTerraDevId,
  apiKey: devTerraApiKey,
});

const prodTerra = terraHandlers({
  environment: "prod",
  devId: prodTerraDevId,
  apiKey: prodTerraApiKey,
});

export const terraDevGenerateAuthToken = onCall(devTerraSecretOptions, devTerra.generateAuthToken);
export const terraDevAuthenticateUser = onCall(devTerraSecretOptions, devTerra.authenticateUser);
export const terraDevGenerateWidgetSession = onCall(devTerraSecretOptions, devTerra.generateWidgetSession);
export const terraDevGetDaily = onCall(devTerraSecretOptions, devTerra.getDaily);
export const terraDevGetSleep = onCall(devTerraSecretOptions, devTerra.getSleep);
export const terraDevGetUserInfo = onCall(devTerraSecretOptions, devTerra.getUserInfo);
export const terraDevDeauthenticateUser = onCall(devTerraSecretOptions, devTerra.deauthenticateUser);

export const terraProdGenerateAuthToken = onCall(prodTerraSecretOptions, prodTerra.generateAuthToken);
export const terraProdAuthenticateUser = onCall(prodTerraSecretOptions, prodTerra.authenticateUser);
export const terraProdGenerateWidgetSession = onCall(prodTerraSecretOptions, prodTerra.generateWidgetSession);
export const terraProdGetDaily = onCall(prodTerraSecretOptions, prodTerra.getDaily);
export const terraProdGetSleep = onCall(prodTerraSecretOptions, prodTerra.getSleep);
export const terraProdGetUserInfo = onCall(prodTerraSecretOptions, prodTerra.getUserInfo);
export const terraProdDeauthenticateUser = onCall(prodTerraSecretOptions, prodTerra.deauthenticateUser);

export const threeDLookCreateMeasurement = onCall(threeDLookSecretOptions, async (request) => {
  const uid = requireAuth(request.auth?.uid);
  const frontPath = requireTempScanPath(uid, request.data?.frontPath, "frontPath");
  const sidePath = requireTempScanPath(uid, request.data?.sidePath, "sidePath");
  const heightCm = requireNumber(request.data?.heightCm, "heightCm");
  const gender = requireString(request.data?.gender, "gender");
  const age = requireNumber(request.data?.age, "age");
  const weightKg = optionalNumber(request.data?.weightKg, "weightKg");

  const bucket = getStorage().bucket();
  const frontFile = bucket.file(frontPath);
  const sideFile = bucket.file(sidePath);

  try {
    const [[frontBytes], [sideBytes]] = await Promise.all([
      frontFile.download(),
      sideFile.download(),
    ]);

    const form = new FormData();
    form.append("front_photo", jpegBlob(frontBytes), "front.jpg");
    form.append("side_photo", jpegBlob(sideBytes), "side.jpg");
    form.append("height", Math.round(heightCm).toString());
    form.append("gender", gender);
    form.append("age", Math.round(age).toString());
    if (weightKg !== undefined) {
      form.append("weight", Math.round(weightKg).toString());
    }

    const response = await threeDLookFetch("/measurements/", {
      method: "POST",
      body: form,
    });
    return parseJsonResponse(response, "3DLook createMeasurement");
  } finally {
    await Promise.allSettled([frontFile.delete(), sideFile.delete()]);
  }
});

export const threeDLookGetMeasurement = onCall(threeDLookSecretOptions, async (request) => {
  requireAuth(request.auth?.uid);
  const id = requireString(request.data?.id, "id");
  const response = await threeDLookFetch(`/measurements/${encodeURIComponent(id)}/`, {
    method: "GET",
  });
  return parseJsonResponse(response, "3DLook getMeasurement");
});

export const threeDLookCreateBodyProgress = onCall(threeDLookSecretOptions, async (request) => {
  requireAuth(request.auth?.uid);
  const measurementBeforeId = requireString(request.data?.measurementBeforeId, "measurementBeforeId");
  const measurementAfterId = requireString(request.data?.measurementAfterId, "measurementAfterId");
  const response = await threeDLookFetch("/body_progress/", {
    method: "POST",
    body: JSON.stringify({
      measurement_before_id: measurementBeforeId,
      measurement_after_id: measurementAfterId,
    }),
    headers: { "Content-Type": "application/json" },
  });
  return parseJsonResponse(response, "3DLook createBodyProgress");
});

export const threeDLookGetBodyProgress = onCall(threeDLookSecretOptions, async (request) => {
  requireAuth(request.auth?.uid);
  const id = requireString(request.data?.id, "id");
  const response = await threeDLookFetch(`/body_progress/${encodeURIComponent(id)}/`, {
    method: "GET",
  });
  return parseJsonResponse(response, "3DLook getBodyProgress");
});

function terraDataUrl(path: "/daily" | "/sleep", data: unknown): URL {
  const payload = asRecord(data);
  const terraUserId = requireString(payload.terraUserId, "terraUserId");
  const startDate = requireIsoDate(payload.startDate, "startDate");
  const endDate = requireIsoDate(payload.endDate, "endDate");

  const url = new URL(`${terraBaseUrl}${path}`);
  url.searchParams.set("user_id", terraUserId);
  url.searchParams.set("start_date", startDate);
  url.searchParams.set("end_date", endDate);
  url.searchParams.set("to_webhook", "false");
  url.searchParams.set("with_samples", readDetail(data) === "none" ? "false" : "true");
  return url;
}

async function terraFetch(secrets: TerraSecrets, input: string | URL, init: RequestInit): Promise<Response> {
  const url = input instanceof URL ? input : new URL(`${terraBaseUrl}${input}`);
  const headers = new Headers(init.headers);
  headers.set("dev-id", secrets.devId.value());
  headers.set("x-api-key", secrets.apiKey.value());
  headers.set("Accept", "application/json");
  return fetch(url, { ...init, headers });
}

async function threeDLookFetch(path: string, init: RequestInit): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set("Authorization", `Token ${threedlookApiToken.value()}`);
  return fetch(`${threeDLookBaseUrl}${path}`, { ...init, headers });
}

function jpegBlob(buffer: Buffer): Blob {
  return new Blob([new Uint8Array(buffer)], { type: "image/jpeg" });
}

async function parseJsonResponse(response: Response, label: string): Promise<unknown> {
  const text = await response.text();
  if (!response.ok) {
    throwProviderError(response.status, label, text);
  }
  try {
    return text.length > 0 ? JSON.parse(text) : {};
  } catch (error) {
    logger.error(`${label} returned invalid JSON`, { error });
    throw new HttpsError("internal", `${label} returned invalid JSON`);
  }
}

async function throwHttpError(response: Response, label: string): Promise<never> {
  throwProviderError(response.status, label, await response.text());
}

function throwProviderError(status: number, label: string, body: string): never {
  logger.warn(`${label} failed`, {
    status,
    body: body.slice(0, 1000),
  });
  const code = status === 401 || status === 403 ? "permission-denied" : "internal";
  throw new HttpsError(code, `${label} failed with HTTP ${status}`);
}

function requireAuth(uid: string | undefined): string {
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign in required");
  }
  return uid;
}

async function requireTerraConnection(
  uid: string,
  terraUserId: string,
  environment: TerraEnvironment,
): Promise<void> {
  const snapshot = await getFirestore()
    .collection("users")
    .doc(uid)
    .collection("settings")
    .doc("userSettings")
    .collection("healthConnections")
    .doc(terraUserId)
    .get();

  const storedEnvironment = snapshot.get("environment");
  if (
    !snapshot.exists ||
    snapshot.get("active") !== true ||
    (typeof storedEnvironment === "string" && storedEnvironment !== environment)
  ) {
    throw new HttpsError("permission-denied", "Terra connection is not active for this user");
  }
}

function requireOwnedByCaller(userInfo: unknown, uid: string): void {
  const user = asRecord(asRecord(userInfo).user);
  if (readString(user, "reference_id") !== uid) {
    throw new HttpsError("permission-denied", "Terra user does not belong to this account");
  }
}

function requireTempScanPath(uid: string, value: unknown, field: string): string {
  const path = requireString(value, field);
  const prefix = `tmpScans/${uid}/`;
  if (!path.startsWith(prefix) || path.includes("..")) {
    throw new HttpsError("invalid-argument", `${field} must be under ${prefix}`);
  }
  return path;
}

function requireString(value: unknown, field: string): string {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new HttpsError("invalid-argument", `${field} is required`);
  }
  return value.trim();
}

function readString(value: unknown, field: string): string | undefined {
  const record = asRecord(value);
  const item = record[field];
  return typeof item === "string" && item.length > 0 ? item : undefined;
}

function requireNumber(value: unknown, field: string): number {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new HttpsError("invalid-argument", `${field} must be a number`);
  }
  return value;
}

function optionalNumber(value: unknown, field: string): number | undefined {
  if (value === undefined || value === null) {
    return undefined;
  }
  return requireNumber(value, field);
}

function requireIsoDate(value: unknown, field: string): string {
  const date = requireString(value, field);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    throw new HttpsError("invalid-argument", `${field} must be YYYY-MM-DD`);
  }
  return date;
}

type TerraDetail = "none" | "stages" | "full";

// Sleep-stage hypnogram is small and the only sample array an overview legitimately needs; every
// other "*_samples" array is high-frequency time-series (HR/HRV/MET) that bloats range payloads.
const KEEP_SAMPLE_KEY = "hypnogram_samples";

function readDetail(data: unknown): TerraDetail {
  const value = asRecord(data).detail;
  // Default "full" keeps the pre-update app (which doesn't send `detail`) on its existing behavior.
  return value === "none" || value === "stages" || value === "full" ? value : "full";
}

function projectByDetail(json: unknown, data: unknown): unknown {
  const detail = readDetail(data);
  if (detail === "full") {
    return json;
  }
  return stripSampleArrays(json, /* keepHypnogram */ detail === "stages");
}

function stripSampleArrays(value: unknown, keepHypnogram: boolean): unknown {
  if (Array.isArray(value)) {
    return value.map((item) => stripSampleArrays(item, keepHypnogram));
  }
  if (value !== null && typeof value === "object") {
    const result: Record<string, unknown> = {};
    for (const [key, item] of Object.entries(value as Record<string, unknown>)) {
      const isSampleArray = Array.isArray(item) && /sample/i.test(key);
      if (isSampleArray && !(keepHypnogram && key === KEEP_SAMPLE_KEY)) {
        continue;
      }
      result[key] = stripSampleArrays(item, keepHypnogram);
    }
    return result;
  }
  return value;
}

function asRecord(value: unknown): Record<string, unknown> {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    return {};
  }
  return value as Record<string, unknown>;
}
