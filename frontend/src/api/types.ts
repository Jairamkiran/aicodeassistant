/** Domain types mirroring the backend REST contracts (no provider leakage). */

export interface AuthTokens {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
}

export type OrgRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';

export interface Organization {
  id: string;
  name: string;
  slug: string;
  role: OrgRole;
}

// Mirrors the backend ImportStatus state machine.
export type RepositoryStatus = 'REGISTERED' | 'IMPORTING' | 'READY' | 'FAILED';

export interface Repository {
  id: string;
  organizationId: string;
  provider: string;
  owner: string;
  name: string;
  cloneUrl: string;
  defaultBranch: string;
  isPrivate: boolean;
  status: RepositoryStatus;
  statusDetail: string | null;
  createdAt: string;
  updatedAt: string;
}

export type SearchSource = 'VECTOR' | 'LEXICAL' | 'HYBRID';

/** Mirrors the backend SearchHitView. */
export interface SearchResult {
  chunkId: string;
  repositoryId: string;
  filePath: string;
  language: string | null;
  startLine: number;
  endLine: number;
  snippet: string;
  score: number;
  source: SearchSource;
}

export interface Citation {
  index: number;
  chunkId: string;
  repositoryId: string;
  filePath: string;
  startLine: number;
  endLine: number;
}

export type ChatTurnRole = 'USER' | 'ASSISTANT';

/** Mirrors the backend SessionView.TurnView. */
export interface ChatTurn {
  seq: number;
  role: ChatTurnRole;
  content: string;
  citations: Citation[];
}

/** Mirrors the backend SessionView (turns present only when a single session is fetched). */
export interface ChatSession {
  id: string;
  organizationId: string;
  repositoryId: string | null;
  title: string;
  createdAt: string;
  updatedAt: string;
  turns: ChatTurn[];
}

export type ReviewSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';

export interface ReviewFinding {
  severity: ReviewSeverity;
  category: string;
  filePath: string;
  startLine: number;
  endLine: number;
  title: string;
  detail: string;
  recommendation: string;
}

export interface CodeReview {
  repositoryId: string;
  focus: string;
  summary: string;
  findings: ReviewFinding[];
}

export interface ApiKeySummary {
  id: string;
  name: string;
  prefix: string;
  scopes: string[];
  createdAt: string;
  expiresAt: string | null;
  lastUsedAt: string | null;
  revoked: boolean;
}

export interface CreatedApiKey extends ApiKeySummary {
  /** Full secret, returned exactly once at creation time. */
  secret: string;
}

export interface Notification {
  id: string;
  organizationId: string;
  type: string;
  title: string;
  message: string;
  resourceType: string | null;
  resourceId: string | null;
  createdAt: string;
  read: boolean;
}
