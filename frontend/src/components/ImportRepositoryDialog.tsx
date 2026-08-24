import { useState } from 'react';
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { repositoryApi, type GitHubRepo } from '../api/endpoints';
import type { Repository } from '../api/types';
import { ApiError } from '../api/problem';

interface Props {
  open: boolean;
  organizationId: string;
  onClose: () => void;
  onImported: (repo: Repository) => void;
}

export function ImportRepositoryDialog({ open, organizationId, onClose, onImported }: Props) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<GitHubRepo | null>(null);

  // Load the caller's linkable GitHub repositories only while the dialog is open.
  const repos = useQuery({
    queryKey: ['github-repos'],
    queryFn: repositoryApi.listGitHub,
    enabled: open,
  });

  const mutation = useMutation({
    mutationFn: () =>
      repositoryApi.importFromGitHub(organizationId, selected!.owner, selected!.name),
    onSuccess: (repo) => {
      queryClient.invalidateQueries({ queryKey: ['repositories', organizationId] });
      reset();
      onImported(repo);
    },
  });

  const reset = () => {
    setSelected(null);
    mutation.reset();
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  const loadError =
    repos.error instanceof ApiError
      ? repos.error.message
      : repos.error
        ? 'Could not load your GitHub repositories. Have you linked GitHub?'
        : null;

  const importError =
    mutation.error instanceof ApiError
      ? mutation.error.message
      : mutation.error
        ? 'Import failed. Please try again.'
        : null;

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
      <Box
        component="form"
        onSubmit={(e) => {
          e.preventDefault();
          if (selected) mutation.mutate();
        }}
      >
        <DialogTitle>Import a repository</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {loadError && <Alert severity="warning">{loadError}</Alert>}
            {importError && <Alert severity="error">{importError}</Alert>}
            <Autocomplete<GitHubRepo>
              options={repos.data ?? []}
              loading={repos.isLoading}
              getOptionLabel={(o) => o.fullName}
              value={selected}
              onChange={(_, value) => setSelected(value)}
              isOptionEqualToValue={(a, b) => a.externalId === b.externalId}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="GitHub repository"
                  placeholder="Search your repositories"
                  required
                />
              )}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={!selected || mutation.isPending}>
            {mutation.isPending ? 'Importing…' : 'Import'}
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}
