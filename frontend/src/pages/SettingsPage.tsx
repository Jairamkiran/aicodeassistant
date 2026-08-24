import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  FormControlLabel,
  IconButton,
  Paper,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import DeleteIcon from '@mui/icons-material/DeleteOutline';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiKeyApi } from '../api/endpoints';
import type { CreatedApiKey } from '../api/types';
import { useAuth } from '../auth/AuthProvider';
import { useColorMode } from '../theme/ColorModeProvider';
import { ErrorState, LoadingState } from '../components/FeedbackStates';
import { ConfirmDialog } from '../components/ConfirmDialog';

export function SettingsPage() {
  const { user } = useAuth();
  const { mode, toggle } = useColorMode();
  const queryClient = useQueryClient();
  const [newKeyName, setNewKeyName] = useState('');
  const [createdKey, setCreatedKey] = useState<CreatedApiKey | null>(null);
  const [revokeId, setRevokeId] = useState<string | null>(null);

  const keys = useQuery({ queryKey: ['api-keys'], queryFn: apiKeyApi.list });

  const createKey = useMutation({
    mutationFn: () => apiKeyApi.create(newKeyName.trim(), ['search', 'chat']),
    onSuccess: (key) => {
      setCreatedKey(key);
      setNewKeyName('');
      queryClient.invalidateQueries({ queryKey: ['api-keys'] });
    },
  });

  const revokeKey = useMutation({
    mutationFn: (id: string) => apiKeyApi.revoke(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] });
      setRevokeId(null);
    },
  });

  return (
    <Box sx={{ maxWidth: 820 }}>
      <Typography variant="h1" gutterBottom>
        Settings
      </Typography>

      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h3" gutterBottom>
          Profile
        </Typography>
        <Stack spacing={0.5}>
          <Typography>{user?.displayName}</Typography>
          <Typography color="text.secondary">{user?.email}</Typography>
        </Stack>
      </Paper>

      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h3" gutterBottom>
          Appearance
        </Typography>
        <FormControlLabel
          control={<Switch checked={mode === 'dark'} onChange={toggle} />}
          label="Dark mode"
        />
      </Paper>

      <Paper sx={{ p: 3 }}>
        <Typography variant="h3" gutterBottom>
          API keys
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 2 }}>
          Programmatic access to search and chat. The secret is shown only once at creation.
        </Typography>

        {createdKey && (
          <Alert severity="success" sx={{ mb: 2 }} onClose={() => setCreatedKey(null)}>
            <Typography variant="body2" gutterBottom>
              Copy your new key now — it will not be shown again.
            </Typography>
            <Stack direction="row" spacing={1} alignItems="center">
              <Box component="code" sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>
                {createdKey.secret}
              </Box>
              <Tooltip title="Copy">
                <IconButton
                  size="small"
                  onClick={() => navigator.clipboard?.writeText(createdKey.secret)}
                  aria-label="Copy API key"
                >
                  <ContentCopyIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Stack>
          </Alert>
        )}

        <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
          <TextField
            size="small"
            label="Key name"
            value={newKeyName}
            onChange={(e) => setNewKeyName(e.target.value)}
            fullWidth
          />
          <Button
            variant="contained"
            onClick={() => createKey.mutate()}
            disabled={newKeyName.trim().length === 0 || createKey.isPending}
          >
            Create
          </Button>
        </Stack>

        <Divider sx={{ mb: 1 }} />

        {keys.isLoading && <LoadingState label="Loading keys…" />}
        {keys.isError && <ErrorState error={keys.error} onRetry={() => keys.refetch()} />}
        {keys.isSuccess && keys.data.length === 0 && (
          <Typography color="text.secondary">No API keys yet.</Typography>
        )}
        {keys.isSuccess && keys.data.length > 0 && (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Prefix</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {keys.data.map((key) => (
                <TableRow key={key.id}>
                  <TableCell>{key.name}</TableCell>
                  <TableCell sx={{ fontFamily: 'monospace' }}>{key.prefix}</TableCell>
                  <TableCell>
                    {key.revoked ? (
                      <Chip size="small" label="Revoked" color="error" variant="outlined" />
                    ) : (
                      <Chip size="small" label="Active" color="success" variant="outlined" />
                    )}
                  </TableCell>
                  <TableCell align="right">
                    {!key.revoked && (
                      <Tooltip title="Revoke">
                        <IconButton
                          size="small"
                          onClick={() => setRevokeId(key.id)}
                          aria-label={`Revoke ${key.name}`}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Paper>

      <ConfirmDialog
        open={Boolean(revokeId)}
        title="Revoke API key?"
        message="Any client using this key will immediately lose access."
        confirmLabel="Revoke"
        destructive
        loading={revokeKey.isPending}
        onCancel={() => setRevokeId(null)}
        onConfirm={() => revokeId && revokeKey.mutate(revokeId)}
      />
    </Box>
  );
}
