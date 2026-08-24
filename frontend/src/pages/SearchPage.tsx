import { useMemo, useState, type FormEvent } from 'react';
import {
  Box,
  Chip,
  InputAdornment,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { useMutation, useQuery } from '@tanstack/react-query';
import { repositoryApi, searchApi } from '../api/endpoints';
import { useOrg } from '../auth/OrgProvider';
import type { SearchResult } from '../api/types';
import { CodeViewer } from '../components/CodeViewer';
import { EmptyState, ErrorState, LoadingState } from '../components/FeedbackStates';

export function SearchPage() {
  const { activeOrg } = useOrg();
  const [query, setQuery] = useState('');
  const [repositoryId, setRepositoryId] = useState('');

  const repos = useQuery({
    queryKey: ['repositories', activeOrg?.id],
    queryFn: () => repositoryApi.list(activeOrg!.id),
    enabled: Boolean(activeOrg),
  });

  const search = useMutation({
    mutationFn: () =>
      searchApi.search(activeOrg!.id, query.trim(), repositoryId || null, null),
  });

  const readyRepos = useMemo(
    () => (repos.data ?? []).filter((r) => r.status === 'READY'),
    [repos.data],
  );

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (query.trim().length > 0) {
      search.mutate();
    }
  };

  if (!activeOrg) return <LoadingState label="Loading organization…" />;

  return (
    <Box>
      <Typography variant="h1" gutterBottom>
        Search
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Hybrid semantic + lexical search across your indexed code.
      </Typography>

      <Box component="form" onSubmit={handleSubmit}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <TextField
            fullWidth
            label="Search query"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="e.g. where is the JWT refresh token rotated?"
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
          />
          <TextField
            select
            label="Repository"
            value={repositoryId}
            onChange={(e) => setRepositoryId(e.target.value)}
            sx={{ minWidth: 220 }}
          >
            <MenuItem value="">All repositories</MenuItem>
            {readyRepos.map((repo) => (
              <MenuItem key={repo.id} value={repo.id}>
                {repo.name}
              </MenuItem>
            ))}
          </TextField>
        </Stack>
      </Box>

      <Box sx={{ mt: 3 }}>
        {search.isPending && <LoadingState label="Searching…" />}
        {search.isError && <ErrorState error={search.error} onRetry={() => search.mutate()} />}
        {search.isSuccess && search.data.length === 0 && (
          <EmptyState title="No matches" description="Try a different query or repository." />
        )}
        {search.isSuccess && search.data.length > 0 && (
          <Stack spacing={2}>
            {search.data.map((result, index) => (
              <SearchResultCard
                key={`${result.filePath}:${result.startLine}:${index}`}
                result={result}
              />
            ))}
          </Stack>
        )}
      </Box>
    </Box>
  );
}

function SearchResultCard({ result }: { result: SearchResult }) {
  return (
    <Paper sx={{ p: 2 }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
        <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
          {result.filePath}:{result.startLine}-{result.endLine}
        </Typography>
        <Stack direction="row" spacing={1}>
          {result.language && <Chip size="small" label={result.language} variant="outlined" />}
          <Chip size="small" label={result.source} color="primary" variant="outlined" />
        </Stack>
      </Stack>
      <CodeViewer
        content={result.snippet}
        language={result.language}
        height={Math.min(320, 24 * (result.endLine - result.startLine + 2) + 40)}
      />
    </Paper>
  );
}
