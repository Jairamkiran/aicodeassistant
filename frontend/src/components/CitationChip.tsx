import { Chip, Tooltip } from '@mui/material';
import type { Citation } from '../api/types';

/** A clickable [n] citation that opens the cited file span in a code viewer. */
export function CitationChip({ citation, onClick }: { citation: Citation; onClick: () => void }) {
  const label = `${citation.filePath}:${citation.startLine}`;
  return (
    <Tooltip title={label}>
      <Chip
        size="small"
        color="primary"
        variant="outlined"
        label={`[${citation.index}]`}
        onClick={onClick}
        sx={{ fontFamily: 'monospace', cursor: 'pointer' }}
      />
    </Tooltip>
  );
}
