import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Divider,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SendIcon from '@mui/icons-material/Send';
import StopIcon from '@mui/icons-material/Stop';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { chatApi, repositoryApi } from '../api/endpoints';
import { useOrg } from '../auth/OrgProvider';
import type { ChatSession, Citation } from '../api/types';
import { ChatMessage, type ChatMessageModel } from '../components/ChatMessage';
import { SessionList } from '../components/SessionList';
import { CitationViewerDialog } from '../components/CitationViewerDialog';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { RenameDialog } from '../components/RenameDialog';
import { EmptyState, LoadingState } from '../components/FeedbackStates';
import { useChatStream } from '../hooks/useChatStream';

export function ChatPage() {
  const { activeOrg } = useOrg();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const [openCitation, setOpenCitation] = useState<Citation | null>(null);
  const [renameTarget, setRenameTarget] = useState<ChatSession | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ChatSession | null>(null);
  const [creatingFor, setCreatingFor] = useState('');
  const scrollRef = useRef<HTMLDivElement>(null);

  const repos = useQuery({
    queryKey: ['repositories', activeOrg?.id],
    queryFn: () => repositoryApi.list(activeOrg!.id),
    enabled: Boolean(activeOrg),
  });

  const sessions = useQuery({
    queryKey: ['chat-sessions', activeOrg?.id],
    queryFn: () => chatApi.listSessions(activeOrg!.id),
    enabled: Boolean(activeOrg),
  });

  // The single-session fetch carries the persisted turns (history).
  const history = useQuery({
    queryKey: ['chat-session', activeSessionId],
    queryFn: () => chatApi.getSession(activeSessionId!),
    enabled: Boolean(activeSessionId),
  });

  const stream = useChatStream(activeSessionId);
  const { seed } = stream;

  // Seed the live message list from persisted history when it loads.
  useEffect(() => {
    if (history.data) {
      seed(
        history.data.turns.map<ChatMessageModel>((turn) => ({
          id: `turn-${turn.seq}`,
          role: turn.role,
          content: turn.content,
          citations: turn.citations,
        })),
      );
    }
  }, [history.data, seed]);

  // Auto-scroll to the newest message.
  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [stream.messages]);

  const readyRepos = useMemo(
    () => (repos.data ?? []).filter((r) => r.status === 'READY'),
    [repos.data],
  );

  const createSession = useMutation({
    mutationFn: (repositoryId: string) => {
      const repo = readyRepos.find((r) => r.id === repositoryId);
      return chatApi.createSession(activeOrg!.id, repositoryId, `Chat · ${repo?.name ?? 'repository'}`);
    },
    onSuccess: (session) => {
      queryClient.invalidateQueries({ queryKey: ['chat-sessions', activeOrg?.id] });
      setActiveSessionId(session.id);
      setCreatingFor('');
    },
  });

  const renameSession = useMutation({
    mutationFn: ({ id, title }: { id: string; title: string }) => chatApi.renameSession(id, title),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['chat-sessions', activeOrg?.id] });
      setRenameTarget(null);
    },
  });

  const deleteSession = useMutation({
    mutationFn: (id: string) => chatApi.deleteSession(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['chat-sessions', activeOrg?.id] });
      if (activeSessionId === id) setActiveSessionId(null);
      setDeleteTarget(null);
    },
  });

  // A ?repositoryId= param (from the repo page) pre-selects the new-chat repo.
  const presetRepo = searchParams.get('repositoryId');
  useEffect(() => {
    if (presetRepo && readyRepos.some((r) => r.id === presetRepo)) {
      setCreatingFor(presetRepo);
      setSearchParams({}, { replace: true });
    }
  }, [presetRepo, readyRepos, setSearchParams]);

  const handleSend = (event: FormEvent) => {
    event.preventDefault();
    const text = draft.trim();
    if (text.length > 0 && !stream.streaming) {
      stream.send(text);
      setDraft('');
    }
  };

  if (!activeOrg) return <LoadingState label="Loading organization…" />;

  return (
    <Box sx={{ display: 'flex', gap: 2, height: 'calc(100vh - 128px)' }}>
      <Paper sx={{ width: 280, flexShrink: 0, display: { xs: 'none', md: 'flex' }, flexDirection: 'column' }}>
        <Box sx={{ p: 2 }}>
          <TextField
            select
            size="small"
            fullWidth
            label="New chat over…"
            value={creatingFor}
            onChange={(e) => setCreatingFor(e.target.value)}
            disabled={readyRepos.length === 0}
            helperText={readyRepos.length === 0 ? 'Index a repository first.' : undefined}
          >
            {readyRepos.map((repo) => (
              <MenuItem key={repo.id} value={repo.id}>
                {repo.name}
              </MenuItem>
            ))}
          </TextField>
          <Button
            fullWidth
            startIcon={<AddIcon />}
            variant="contained"
            sx={{ mt: 1 }}
            disabled={!creatingFor || createSession.isPending}
            onClick={() => createSession.mutate(creatingFor)}
          >
            New conversation
          </Button>
        </Box>
        <Divider />
        <Box sx={{ overflowY: 'auto', flexGrow: 1 }}>
          {sessions.isLoading ? (
            <LoadingState label="Loading…" />
          ) : (
            <SessionList
              sessions={sessions.data ?? []}
              activeSessionId={activeSessionId}
              onSelect={setActiveSessionId}
              onRename={setRenameTarget}
              onDelete={setDeleteTarget}
            />
          )}
        </Box>
      </Paper>

      <Paper sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        {!activeSessionId ? (
          <EmptyState
            title="Start a conversation"
            description="Pick a repository and create a new conversation to chat over its code with grounded, cited answers."
          />
        ) : (
          <>
            <Box ref={scrollRef} sx={{ flexGrow: 1, overflowY: 'auto', p: 2 }}>
              {history.isLoading ? (
                <LoadingState label="Loading conversation…" />
              ) : (
                <Stack spacing={2}>
                  {stream.messages.map((message) => (
                    <ChatMessage
                      key={message.id}
                      message={message}
                      onCitationClick={setOpenCitation}
                    />
                  ))}
                  {stream.messages.length === 0 && (
                    <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 4 }}>
                      Ask a question about this repository to begin.
                    </Typography>
                  )}
                </Stack>
              )}
            </Box>

            {stream.error && (
              <Alert
                severity="error"
                sx={{ mx: 2 }}
                action={
                  <Button color="inherit" size="small" onClick={stream.retryLast}>
                    Retry
                  </Button>
                }
              >
                {stream.error}
              </Alert>
            )}

            <Divider />
            <Box component="form" onSubmit={handleSend} sx={{ p: 2 }}>
              <Stack direction="row" spacing={1} alignItems="flex-end">
                <TextField
                  fullWidth
                  multiline
                  maxRows={5}
                  placeholder="Ask about this repository…"
                  value={draft}
                  onChange={(e) => setDraft(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      handleSend(e);
                    }
                  }}
                  aria-label="Message"
                />
                {stream.streaming ? (
                  <IconButton color="error" onClick={stream.stop} aria-label="Stop generating">
                    <StopIcon />
                  </IconButton>
                ) : (
                  <IconButton
                    color="primary"
                    type="submit"
                    disabled={draft.trim().length === 0}
                    aria-label="Send message"
                  >
                    <SendIcon />
                  </IconButton>
                )}
              </Stack>
            </Box>
          </>
        )}
      </Paper>

      <CitationViewerDialog citation={openCitation} onClose={() => setOpenCitation(null)} />
      <RenameDialog
        open={Boolean(renameTarget)}
        initialValue={renameTarget?.title ?? ''}
        title="Rename conversation"
        loading={renameSession.isPending}
        onCancel={() => setRenameTarget(null)}
        onSubmit={(title) => renameTarget && renameSession.mutate({ id: renameTarget.id, title })}
      />
      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Delete conversation?"
        message={`"${deleteTarget?.title}" and its messages will be permanently removed.`}
        confirmLabel="Delete"
        destructive
        loading={deleteSession.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteSession.mutate(deleteTarget.id)}
      />
    </Box>
  );
}
