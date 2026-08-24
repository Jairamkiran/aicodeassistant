import { useCallback, useEffect, useRef, useState } from 'react';
import { streamChatMessage } from '../api/chatStream';
import type { Citation } from '../api/types';
import type { ChatMessageModel } from '../components/ChatMessage';

/**
 * Owns the live message list for a chat session: seeds from history, appends the
 * user turn, then streams the assistant turn token-by-token and attaches
 * citations when they arrive.
 */
export function useChatStream(sessionId: string | null) {
  const [messages, setMessages] = useState<ChatMessageModel[]>([]);
  const [streaming, setStreaming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const cancelRef = useRef<(() => void) | null>(null);
  // Remembers the last question so a failed stream can be retried (error recovery).
  const lastQuestionRef = useRef<string | null>(null);

  // Reset when switching sessions and cancel any in-flight stream.
  useEffect(() => {
    cancelRef.current?.();
    cancelRef.current = null;
    setMessages([]);
    setStreaming(false);
    setError(null);
    lastQuestionRef.current = null;
  }, [sessionId]);

  useEffect(() => () => cancelRef.current?.(), []);

  const seed = useCallback((history: ChatMessageModel[]) => {
    setMessages(history);
  }, []);

  const send = useCallback(
    (text: string) => {
      if (!sessionId || streaming) return;
      setError(null);
      lastQuestionRef.current = text;

      const userId = `local-user-${messages.length}`;
      const assistantId = `local-assistant-${messages.length}`;
      setMessages((prev) => [
        ...prev,
        { id: userId, role: 'USER', content: text, citations: [] },
        { id: assistantId, role: 'ASSISTANT', content: '', citations: [], streaming: true },
      ]);
      setStreaming(true);

      const patchAssistant = (patch: (m: ChatMessageModel) => ChatMessageModel) => {
        setMessages((prev) => prev.map((m) => (m.id === assistantId ? patch(m) : m)));
      };

      cancelRef.current = streamChatMessage(sessionId, text, {
        onToken: (token) => patchAssistant((m) => ({ ...m, content: m.content + token })),
        onCitations: (citations: Citation[]) => patchAssistant((m) => ({ ...m, citations })),
        onDone: () => {
          patchAssistant((m) => ({ ...m, streaming: false }));
          setStreaming(false);
          cancelRef.current = null;
        },
        onError: (err) => {
          patchAssistant((m) => ({ ...m, streaming: false }));
          setStreaming(false);
          setError(err.message);
          cancelRef.current = null;
        },
      });
    },
    [sessionId, streaming, messages.length],
  );

  const stop = useCallback(() => {
    cancelRef.current?.();
    cancelRef.current = null;
    setStreaming(false);
    setMessages((prev) => prev.map((m) => (m.streaming ? { ...m, streaming: false } : m)));
  }, []);

  /**
   * Re-sends the last question after a failed stream: drops the failed user +
   * assistant pair, then sends again. No-op if there is nothing to retry.
   */
  const retryLast = useCallback(() => {
    const question = lastQuestionRef.current;
    if (!question || streaming) return;
    setMessages((prev) => {
      // Remove the trailing assistant turn and its user turn from the failed attempt.
      const trimmed = [...prev];
      if (trimmed.at(-1)?.role === 'ASSISTANT') trimmed.pop();
      if (trimmed.at(-1)?.role === 'USER') trimmed.pop();
      return trimmed;
    });
    setError(null);
    send(question);
  }, [send, streaming]);

  return { messages, streaming, error, seed, send, stop, retryLast };
}
