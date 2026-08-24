import { Alert, AlertTitle, Box, Button, CircularProgress, Typography } from '@mui/material';
import type { ReactNode } from 'react';
import { ApiError } from '../api/problem';

/** Centered spinner for full-panel loading. */
export function LoadingState({ label = 'Loading…' }: { label?: string }) {
  return (
    <Box
      sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2, py: 6 }}
      role="status"
      aria-live="polite"
    >
      <CircularProgress aria-hidden />
      <Typography color="text.secondary">{label}</Typography>
    </Box>
  );
}

/** Uniform error panel with an optional retry action. */
export function ErrorState({
  error,
  onRetry,
  title = 'Something went wrong',
}: {
  error: unknown;
  onRetry?: () => void;
  title?: string;
}) {
  const message =
    error instanceof ApiError
      ? error.message
      : error instanceof Error
        ? error.message
        : 'An unexpected error occurred.';
  const correlationId = error instanceof ApiError ? error.problem.correlationId : undefined;

  return (
    <Alert
      severity="error"
      action={
        onRetry ? (
          <Button color="inherit" size="small" onClick={onRetry}>
            Retry
          </Button>
        ) : undefined
      }
    >
      <AlertTitle>{title}</AlertTitle>
      {message}
      {correlationId && (
        <Typography variant="caption" component="div" sx={{ mt: 0.5, opacity: 0.8 }}>
          Reference: {correlationId}
        </Typography>
      )}
    </Alert>
  );
}

/** Friendly empty-state with an optional call to action. */
export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <Box sx={{ textAlign: 'center', py: 6, px: 2 }}>
      {icon && <Box sx={{ color: 'text.secondary', mb: 1, '& svg': { fontSize: 48 } }}>{icon}</Box>}
      <Typography variant="h3" gutterBottom>
        {title}
      </Typography>
      {description && (
        <Typography color="text.secondary" sx={{ mb: 2, maxWidth: 420, mx: 'auto' }}>
          {description}
        </Typography>
      )}
      {action}
    </Box>
  );
}
