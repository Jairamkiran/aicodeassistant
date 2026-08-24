import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { api } from '../api/client';
import { authApi } from '../api/endpoints';
import { tokenStore } from '../api/tokenStore';
import type { UserProfile } from '../api/types';

type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

interface AuthContextValue {
  status: AuthStatus;
  user: UserProfile | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [user, setUser] = useState<UserProfile | null>(null);

  const loadProfile = useCallback(async () => {
    const profile = await authApi.me();
    setUser(profile);
    setStatus('authenticated');
  }, []);

  // On startup, try to re-mint an access token from the refresh cookie.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const token = await api.refresh();
      if (cancelled) return;
      if (!token) {
        setStatus('anonymous');
        return;
      }
      try {
        await loadProfile();
      } catch {
        if (!cancelled) {
          tokenStore.clear();
          setStatus('anonymous');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [loadProfile]);

  const login = useCallback(
    async (email: string, password: string) => {
      const tokens = await authApi.login(email, password);
      tokenStore.set(tokens.accessToken);
      await loadProfile();
    },
    [loadProfile],
  );

  const register = useCallback(
    async (email: string, password: string, displayName: string) => {
      const tokens = await authApi.register(email, password, displayName);
      tokenStore.set(tokens.accessToken);
      await loadProfile();
    },
    [loadProfile],
  );

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      tokenStore.clear();
      setUser(null);
      setStatus('anonymous');
    }
  }, []);

  const value = useMemo(
    () => ({ status, user, login, register, logout }),
    [status, user, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components -- hook co-located with its provider by convention
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
