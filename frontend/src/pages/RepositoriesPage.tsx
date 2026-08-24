import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  Stack,
  Typography,
} from '@mui/material';
import Grid from '@mui/material/Grid2';
import AddIcon from '@mui/icons-material/Add';
import SourceIcon from '@mui/icons-material/Source';
import { useQuery } from '@tanstack/react-query';
import { repositoryApi } from '../api/endpoints';
import { useOrg } from '../auth/OrgProvider';
import { StatusChip } from '../components/StatusChip';
import { EmptyState, ErrorState, LoadingState } from '../components/FeedbackStates';
import { ImportRepositoryDialog } from '../components/ImportRepositoryDialog';

export function RepositoriesPage() {
  const { activeOrg } = useOrg();
  const [importOpen, setImportOpen] = useState(false);

  const query = useQuery({
    queryKey: ['repositories', activeOrg?.id],
    queryFn: () => repositoryApi.list(activeOrg!.id),
    enabled: Boolean(activeOrg),
    refetchInterval: (q) => {
      const repos = q.state.data ?? [];
      const active = repos.some((r) => r.status === 'IMPORTING');
      return active ? 3000 : false;
    },
  });

  if (!activeOrg) {
    return <LoadingState label="Loading organization…" />;
  }

  return (
    <Box>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 3 }}>
        <Typography variant="h1">Repositories</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setImportOpen(true)}>
          Import repository
        </Button>
      </Stack>

      {query.isLoading && <LoadingState label="Loading repositories…" />}
      {query.isError && <ErrorState error={query.error} onRetry={() => query.refetch()} />}

      {query.isSuccess && query.data.length === 0 && (
        <EmptyState
          icon={<SourceIcon />}
          title="No repositories yet"
          description="Import a repository to index its code and start chatting over it."
          action={
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setImportOpen(true)}>
              Import repository
            </Button>
          }
        />
      )}

      {query.isSuccess && query.data.length > 0 && (
        <Grid container spacing={2}>
          {query.data.map((repo) => (
            <Grid key={repo.id} size={{ xs: 12, sm: 6, md: 4 }}>
              <Card>
                <CardActionArea component={RouterLink} to={`/repositories/${repo.id}`}>
                  <CardContent>
                    <Stack
                      direction="row"
                      justifyContent="space-between"
                      alignItems="flex-start"
                      spacing={1}
                    >
                      <Typography variant="h3" noWrap title={`${repo.owner}/${repo.name}`}>
                        {repo.name}
                      </Typography>
                      <StatusChip status={repo.status} />
                    </Stack>
                    <Typography variant="body2" color="text.secondary" noWrap>
                      {repo.owner}/{repo.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {repo.defaultBranch}
                    </Typography>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <ImportRepositoryDialog
        open={importOpen}
        organizationId={activeOrg.id}
        onClose={() => setImportOpen(false)}
        onImported={() => setImportOpen(false)}
      />
    </Box>
  );
}
