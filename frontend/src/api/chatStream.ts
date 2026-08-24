import { tokenStore } from './tokenStore';
import { api } from './client';
import type { Citation } from './types';

/** Events emitted by the RAG chat SSE endpoint, mapped to callbacks. */
export interface ChatStreamHandlers {
  onToken: (text: string) => void;
  onCitations: (citations: Citation[]) => void;
  onDone: () => void;
  onError: (error: Error) => void;
}

interface ParsedEvent {
  event: string;
  data: string;
}

/**
 * Streams an assistant answer over Server-Sent Events. Uses fetch + a manual
 * SSE parser (rather than EventSource) because we must send an Authorization
 * header and a POST body, neither of which EventSource supports.
 *
 * Returns an abort function the caller can invoke to cancel the stream.
 */
export function streamChatMessage(
  sessionId: string,
  message: string,
  handlers: ChatStreamHandlers,
): () => void {
  const controller = new AbortController();

  void (async () => {
    let attemptedRefresh = false;

    const open = async (token: string | null): Promise<Response> =>
      fetch(`${api.baseUrl}/chat/sessions/${sessionId}/messages`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ message }),
        signal: controller.signal,
      });

    try {
      let response = await open(tokenStore.get());
      if (response.status === 401 && !attemptedRefresh) {
        attemptedRefresh = true;
        const refreshed = await api.refresh();
        if (refreshed) {
          response = await open(refreshed);
        }
      }

      if (!response.ok || !response.body) {
        throw new Error(`Chat stream failed (${response.status})`);
      }

      await consume(response.body, handlers);
      handlers.onDone();
    } catch (err) {
      if (controller.signal.aborted) {
        // Caller-initiated cancellation is not an error.
        return;
      }
      handlers.onError(err instanceof Error ? err : new Error('Chat stream failed'));
    }
  })();

  return () => controller.abort();
}

async function consume(body: ReadableStream<Uint8Array>, handlers: ChatStreamHandlers): Promise<void> {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    // SSE frames are separated by a blank line.
    let boundary = buffer.indexOf('\n\n');
    while (boundary !== -1) {
      const frame = buffer.slice(0, boundary);
      buffer = buffer.slice(boundary + 2);
      dispatch(parseFrame(frame), handlers);
      boundary = buffer.indexOf('\n\n');
    }
  }
}

function parseFrame(frame: string): ParsedEvent {
  let event = 'message';
  const dataLines: string[] = [];
  for (const rawLine of frame.split('\n')) {
    const line = rawLine.replace(/\r$/, '');
    if (line.startsWith('event:')) {
      event = line.slice('event:'.length).trim();
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).replace(/^ /, ''));
    }
  }
  return { event, data: dataLines.join('\n') };
}

function dispatch({ event, data }: ParsedEvent, handlers: ChatStreamHandlers): void {
  switch (event) {
    case 'token':
      handlers.onToken(data);
      break;
    case 'citations':
      try {
        handlers.onCitations(JSON.parse(data) as Citation[]);
      } catch {
        // A malformed citations frame should not abort the whole stream.
      }
      break;
    case 'done':
      // Terminal marker; onDone fires when the body closes.
      break;
    default:
      break;
  }
}
