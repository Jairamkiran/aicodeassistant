/**
 * RFC 9457 "problem+json" representation returned by the backend's
 * global exception handler. All fields are optional per the spec.
 */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  /** Backend adds a correlation id to aid support; surfaced in error UIs. */
  correlationId?: string;
  /** Field-level validation errors, when present. */
  errors?: Record<string, string>;
}

/** Error thrown by the API client for any non-2xx response. */
export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetail;

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail || problem.title || `Request failed (${status})`);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
  }

  /** True for auth failures the UI should treat as "session expired". */
  get isUnauthorized(): boolean {
    return this.status === 401;
  }

  get isForbidden(): boolean {
    return this.status === 403;
  }

  get isNotFound(): boolean {
    return this.status === 404;
  }
}

export async function parseProblem(response: Response): Promise<ProblemDetail> {
  try {
    const body = (await response.json()) as ProblemDetail;
    return { status: response.status, ...body };
  } catch {
    return { status: response.status, title: response.statusText || 'Request failed' };
  }
}
