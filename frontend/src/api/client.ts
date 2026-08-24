import { ApiError, parseProblem } from './problem';
import { tokenStore } from './tokenStore';

const BASE_URL = '/api/v1';

interface RequestOptions {
  method?: string;
  body?: unknown;
  /** Skip the bearer header + refresh-retry (used by the auth endpoints). */
  anonymous?: boolean;
  signal?: AbortSignal;
}

/**
 * Single in-flight refresh promise shared by all callers, so a burst of
 * concurrent 401s triggers exactly one /auth/refresh round-trip.
 */
let refreshInFlight: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const response = await fetch(`${BASE_URL}/auth/refresh`, {
          method: 'POST',
          credentials: 'include',
          headers: { Accept: 'application/json' },
        });
        if (!response.ok) {
          tokenStore.clear();
          return null;
        }
        const data = (await response.json()) as { accessToken?: string };
        const token = data.accessToken ?? null;
        tokenStore.set(token);
        return token;
      } catch {
        tokenStore.clear();
        return null;
      } finally {
        refreshInFlight = null;
      }
    })();
  }
  return refreshInFlight;
}

function buildHeaders(options: RequestOptions, token: string | null): Headers {
  const headers = new Headers({ Accept: 'application/json' });
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  if (!options.anonymous && token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  return headers;
}

async function execute(path: string, options: RequestOptions, token: string | null): Promise<Response> {
  return fetch(`${BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    credentials: 'include',
    headers: buildHeaders(options, token),
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    signal: options.signal,
  });
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  let response = await execute(path, options, tokenStore.get());

  // Transparently refresh once on a 401 for authenticated calls.
  if (response.status === 401 && !options.anonymous) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      response = await execute(path, options, refreshed);
    }
  }

  if (!response.ok) {
    throw new ApiError(response.status, await parseProblem(response));
  }

  if (response.status === 204) {
    return undefined as T;
  }
  const contentType = response.headers.get('Content-Type') ?? '';
  if (!contentType.includes('application/json')) {
    return (await response.text()) as unknown as T;
  }
  return (await response.json()) as T;
}

export const api = {
  get: <T>(path: string, signal?: AbortSignal) => request<T>(path, { signal }),
  post: <T>(path: string, body?: unknown, opts?: Partial<RequestOptions>) =>
    request<T>(path, { method: 'POST', body, ...opts }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PATCH', body }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
  /** Exposed for the auth layer to prime the token on startup. */
  refresh: refreshAccessToken,
  baseUrl: BASE_URL,
};
