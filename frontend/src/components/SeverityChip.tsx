import { Chip, type ChipProps } from '@mui/material';
import type { ReviewSeverity } from '../api/types';

const SEVERITY_COLOR: Record<ReviewSeverity, ChipProps['color']> = {
  CRITICAL: 'error',
  HIGH: 'error',
  MEDIUM: 'warning',
  LOW: 'info',
  INFO: 'default',
};

export function SeverityChip({ severity }: { severity: ReviewSeverity }) {
  return (
    <Chip
      size="small"
      color={SEVERITY_COLOR[severity]}
      label={severity}
      variant={severity === 'CRITICAL' || severity === 'HIGH' ? 'filled' : 'outlined'}
    />
  );
}
