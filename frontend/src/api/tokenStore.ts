/**
 * In-memory access-token holder. The access token is deliberately NOT persisted
 * to localStorage (XSS exfiltration risk); it lives only in memory and is
 * re-minted from the HttpOnly refresh cookie on page load via /auth/refresh.
 *
 * A subscriber hook lets React components react to sign-in / sign-out without a
 * global state library.
 */
type Listener = (token: string | null) => void;

let accessToken: string | null = null;
const listeners = new Set<Listener>();

export const tokenStore = {
  get(): string | null {
    return accessToken;
  },
  set(token: string | null): void {
    accessToken = token;
    for (const listener of listeners) {
      listener(token);
    }
  },
  clear(): void {
    tokenStore.set(null);
  },
  subscribe(listener: Listener): () => void {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};
