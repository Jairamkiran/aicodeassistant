import { Chip, type ChipProps } from '@mui/material';
import type { RepositoryStatus } from '../api/types';

const STATUS_COLOR: Record<RepositoryStatus, ChipProps['color']> = {
  REGISTERED: 'default',
  IMPORTING: 'warning',
  READY: 'success',
  FAILED: 'error',
};

const STATUS_LABEL: Record<RepositoryStatus, string> = {
  REGISTERED: 'Registered',
  IMPORTING: 'Indexing',
  READY: 'Ready',
  FAILED: 'Failed',
};

export function StatusChip({ status }: { status: RepositoryStatus }) {
  return (
    <Chip
      size="small"
      color={STATUS_COLOR[status]}
      label={STATUS_LABEL[status]}
      variant={status === 'READY' ? 'filled' : 'outlined'}
    />
  );
}
