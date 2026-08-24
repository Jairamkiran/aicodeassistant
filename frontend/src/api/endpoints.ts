import { api } from './client';
import type {
  ApiKeySummary,
  AuthTokens,
  ChatSession,
  CodeReview,
  CreatedApiKey,
  Notification,
  Organization,
  Repository,
  SearchResult,
  UserProfile,
} from './types';

/** Thin, typed wrappers over the backend REST endpoints, grouped by context. */

export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthTokens>('/auth/login', { email, password }, { anonymous: true }),
  register: (email: string, password: string, displayName: string) =>
    api.post<AuthTokens>('/auth/register', { email, password, displayName }, { anonymous: true }),
  logout: () => api.post<void>('/auth/logout'),
  me: () => api.get<UserProfile>('/users/me'),
};

export const orgApi = {
  list: () => api.get<Organization[]>('/organizations'),
};

/** A GitHub repository available to import (from the picker). */
export interface GitHubRepo {
  externalId: string;
  owner: string;
  name: string;
  fullName: string;
  defaultBranch: string;
  isPrivate: boolean;
}

export const repositoryApi = {
  list: (organizationId: string) =>
    api.get<Repository[]>(`/repositories?organizationId=${encodeURIComponent(organizationId)}`),
  get: (id: string) => api.get<Repository>(`/repositories/${id}`),
  listGitHub: () => api.get<GitHubRepo[]>('/repositories/github'),
  importFromGitHub: (organizationId: string, owner: string, name: string) =>
    api.post<Repository>('/repositories/import', { organizationId, owner, name }),
  reindex: (id: string) => api.post<Repository>(`/repositories/${id}/reindex`),
  remove: (id: string) => api.delete<void>(`/repositories/${id}`),
};

export const searchApi = {
  search: (
    organizationId: string,
    query: string,
    repositoryId: string | null,
    language: string | null,
  ) =>
    api.post<SearchResult[]>('/search', {
      organizationId,
      query,
      repositoryId: repositoryId ?? undefined,
      language: language ?? undefined,
    }),
};

export const chatApi = {
  listSessions: (organizationId: string) =>
    api.get<ChatSession[]>(
      `/chat/sessions?organizationId=${encodeURIComponent(organizationId)}`,
    ),
  createSession: (organizationId: string, repositoryId: string, title: string) =>
    api.post<ChatSession>('/chat/sessions', { organizationId, repositoryId, title }),
  getSession: (id: string) => api.get<ChatSession>(`/chat/sessions/${id}`),
  renameSession: (id: string, title: string) =>
    api.patch<ChatSession>(`/chat/sessions/${id}`, { title }),
  deleteSession: (id: string) => api.delete<void>(`/chat/sessions/${id}`),
};

export const codeReviewApi = {
  review: (organizationId: string, repositoryId: string, focus: string) =>
    api.post<CodeReview>('/code-reviews', { organizationId, repositoryId, focus }),
};

export const notificationApi = {
  list: (limit = 20) => api.get<Notification[]>(`/notifications?limit=${limit}`),
  unreadCount: () => api.get<{ unread: number }>('/notifications/unread-count'),
  markRead: (id: string) => api.post<void>(`/notifications/${id}/read`),
};

export const apiKeyApi = {
  list: () => api.get<ApiKeySummary[]>('/api-keys'),
  create: (name: string, scopes: string[]) =>
    api.post<CreatedApiKey>('/api-keys', { name, scopes }),
  revoke: (id: string) => api.delete<void>(`/api-keys/${id}`),
};
