import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { useQuery } from '@tanstack/react-query';
import { orgApi } from '../api/endpoints';
import type { Organization } from '../api/types';
import { useAuth } from './AuthProvider';

interface OrgContextValue {
  organizations: Organization[];
  activeOrg: Organization | null;
  setActiveOrg: (id: string) => void;
  isLoading: boolean;
}

const OrgContext = createContext<OrgContextValue | undefined>(undefined);

const STORAGE_KEY = 'aica.active-org';

export function OrgProvider({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  const [activeOrgId, setActiveOrgId] = useState<string | null>(
    () => localStorage.getItem(STORAGE_KEY),
  );

  const { data: organizations = [], isLoading } = useQuery({
    queryKey: ['organizations'],
    queryFn: orgApi.list,
    enabled: status === 'authenticated',
  });

  // Default the active org to the first one once the list arrives, unless the
  // persisted choice still exists.
  useEffect(() => {
    if (organizations.length === 0) return;
    const exists = activeOrgId && organizations.some((o) => o.id === activeOrgId);
    if (!exists) {
      setActiveOrgId(organizations[0].id);
    }
  }, [organizations, activeOrgId]);

  const setActiveOrg = useCallback((id: string) => {
    localStorage.setItem(STORAGE_KEY, id);
    setActiveOrgId(id);
  }, []);

  const activeOrg = useMemo(
    () => organizations.find((o) => o.id === activeOrgId) ?? null,
    [organizations, activeOrgId],
  );

  const value = useMemo(
    () => ({ organizations, activeOrg, setActiveOrg, isLoading }),
    [organizations, activeOrg, setActiveOrg, isLoading],
  );

  return <OrgContext.Provider value={value}>{children}</OrgContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components -- hook co-located with its provider by convention
export function useOrg(): OrgContextValue {
  const ctx = useContext(OrgContext);
  if (!ctx) {
    throw new Error('useOrg must be used within an OrgProvider');
  }
  return ctx;
}
