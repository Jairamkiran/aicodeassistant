import { describe, expect, it } from 'vitest';
import { ApiError, parseProblem } from './problem';

describe('ApiError', () => {
  it('classifies status codes', () => {
    expect(new ApiError(401, {}).isUnauthorized).toBe(true);
    expect(new ApiError(403, {}).isForbidden).toBe(true);
    expect(new ApiError(404, {}).isNotFound).toBe(true);
    expect(new ApiError(500, {}).isUnauthorized).toBe(false);
  });

  it('prefers detail, then title, then a generic message', () => {
    expect(new ApiError(400, { detail: 'bad input' }).message).toBe('bad input');
    expect(new ApiError(400, { title: 'Bad Request' }).message).toBe('Bad Request');
    expect(new ApiError(400, {}).message).toBe('Request failed (400)');
  });
});

describe('parseProblem', () => {
  it('parses a problem+json body and injects the status', async () => {
    const response = new Response(JSON.stringify({ title: 'Nope', detail: 'no access' }), {
      status: 403,
      headers: { 'Content-Type': 'application/problem+json' },
    });
    const problem = await parseProblem(response);
    expect(problem.status).toBe(403);
    expect(problem.detail).toBe('no access');
  });

  it('falls back to status text on a non-JSON body', async () => {
    const response = new Response('boom', { status: 502, statusText: 'Bad Gateway' });
    const problem = await parseProblem(response);
    expect(problem.status).toBe(502);
    expect(problem.title).toBe('Bad Gateway');
  });
});
