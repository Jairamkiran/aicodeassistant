import { useState } from 'react';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { Box, Button, Divider, LinearProgress, Paper, Stack, Typography } from '@mui/material';
import ChatIcon from '@mui/icons-material/Chat';
import RefreshIcon from '@mui/icons-material/Refresh';
import DeleteIcon from '@mui/icons-material/DeleteOutline';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { repositoryApi } from '../api/endpoints';
import { StatusChip } from '../components/StatusChip';
import { ErrorState, LoadingState } from '../components/FeedbackStates';
import { ConfirmDialog } from '../components/ConfirmDialog';

export function RepositoryDetailPage() {
  const { repositoryId = '' } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [confirmDelete, setConfirmDelete] = useState(false);

  // While importing/indexing, poll the repository so its status updates live.
  const repoQuery = useQuery({
    queryKey: ['repository', repositoryId],
    queryFn: () => repositoryApi.get(repositoryId),
    enabled: Boolean(repositoryId),
    refetchInterval: (q) => (q.state.data?.status === 'IMPORTING' ? 2500 : false),
  });

  const reindex = useMutation({
    mutationFn: () => repositoryApi.reindex(repositoryId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['repository', repositoryId] }),
  });

  const remove = useMutation({
    mutationFn: () => repositoryApi.remove(repositoryId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repositories'] });
      navigate('/repositories');
    },
  });

  if (repoQuery.isLoading) return <LoadingState label="Loading repository…" />;
  if (repoQuery.isError)
    return <ErrorState error={repoQuery.error} onRetry={() => repoQuery.refetch()} />;
  if (!repoQuery.data) return null;

  const repo = repoQuery.data;
  const status = repo.status;
  const inProgress = status === 'IMPORTING';

  return (
    <Box>
      <Button component={RouterLink} to="/repositories" size="small" sx={{ mb: 2 }}>
        ← All repositories
      </Button>

      <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={2}>
        <Box>
          <Typography variant="h1">{repo.name}</Typography>
          <Typography color="text.secondary">
            {repo.owner}/{repo.name}
          </Typography>
        </Box>
        <StatusChip status={status} />
      </Stack>

      <Stack direction="row" spacing={1} sx={{ mt: 2 }} flexWrap="wrap" useFlexGap>
        <Button
          variant="contained"
          startIcon={<ChatIcon />}
          component={RouterLink}
          to={`/chat?repositoryId=${repo.id}`}
          disabled={status !== 'READY'}
        >
          Chat over this repo
        </Button>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={() => reindex.mutate()}
          disabled={inProgress || reindex.isPending}
        >
          {reindex.isPending ? 'Starting…' : 'Re-index'}
        </Button>
        <Button
          variant="outlined"
          color="error"
          startIcon={<DeleteIcon />}
          onClick={() => setConfirmDelete(true)}
        >
          Delete
        </Button>
      </Stack>

      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="h3" gutterBottom>
          Indexing status
        </Typography>
        {inProgress && (
          <Box sx={{ my: 2 }}>
            <LinearProgress aria-label="Indexing in progress" />
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Cloning, chunking and embedding the repository…
            </Typography>
          </Box>
        )}
        {status === 'READY' && (
          <Typography color="success.main">
            Indexed and ready for search and chat. Last updated{' '}
            {new Date(repo.updatedAt).toLocaleString()}.
          </Typography>
        )}
        {status === 'FAILED' && (
          <ErrorState
            title="Indexing failed"
            error={new Error(repo.statusDetail ?? 'Unknown error.')}
            onRetry={() => reindex.mutate()}
          />
        )}

        <Divider sx={{ my: 2 }} />
        <Stack spacing={0.5}>
          <Detail label="Default branch" value={repo.defaultBranch} />
          <Detail label="Provider" value={repo.provider} />
          <Detail label="Clone URL" value={repo.cloneUrl} />
          <Detail label="Visibility" value={repo.isPrivate ? 'Private' : 'Public'} />
        </Stack>
      </Paper>

      <ConfirmDialog
        open={confirmDelete}
        title="Delete repository?"
        message={`This removes "${repo.name}" and all indexed code. This cannot be undone.`}
        confirmLabel="Delete"
        destructive
        loading={remove.isPending}
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => remove.mutate()}
      />
    </Box>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <Stack direction="row" spacing={1}>
      <Typography variant="body2" color="text.secondary" sx={{ minWidth: 120 }}>
        {label}
      </Typography>
      <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>
        {value}
      </Typography>
    </Stack>
  );
}
