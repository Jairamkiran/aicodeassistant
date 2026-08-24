import { Dialog, DialogContent, DialogTitle, IconButton, Typography } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { useQuery } from '@tanstack/react-query';
import { searchApi } from '../api/endpoints';
import type { Citation } from '../api/types';
import { CodeViewer } from './CodeViewer';
import { ErrorState, LoadingState } from './FeedbackStates';
import { useOrg } from '../auth/OrgProvider';

interface Props {
  citation: Citation | null;
  onClose: () => void;
}

/**
 * Opens a cited file span in a Monaco viewer. The cited content is located by
 * re-querying search scoped to the citation's file path, then highlighting the
 * cited line range — reusing the retrieval path rather than adding a new
 * raw-file endpoint.
 */
export function CitationViewerDialog({ citation, onClose }: Props) {
  const { activeOrg } = useOrg();

  const query = useQuery({
    queryKey: ['citation', citation?.repositoryId, citation?.filePath, citation?.startLine],
    queryFn: () =>
      searchApi.search(activeOrg!.id, citation!.filePath, citation!.repositoryId, null),
    enabled: Boolean(citation && activeOrg),
  });

  const match =
    query.data?.find(
      (r) => r.filePath === citation?.filePath && r.startLine <= (citation?.startLine ?? 0),
    ) ?? query.data?.[0];

  return (
    <Dialog open={Boolean(citation)} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ pr: 6, fontFamily: 'monospace', fontSize: '0.95rem' }}>
        {citation ? `${citation.filePath}:${citation.startLine}-${citation.endLine}` : ''}
        <IconButton
          onClick={onClose}
          aria-label="Close"
          sx={{ position: 'absolute', right: 8, top: 8 }}
        >
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <DialogContent dividers>
        {query.isLoading && <LoadingState label="Loading cited code…" />}
        {query.isError && <ErrorState error={query.error} onRetry={() => query.refetch()} />}
        {query.isSuccess && !match && (
          <Typography color="text.secondary">The cited content is no longer available.</Typography>
        )}
        {match && citation && (
          <CodeViewer
            content={match.snippet}
            language={match.language}
            highlightStart={Math.max(1, citation.startLine - match.startLine + 1)}
            highlightEnd={Math.max(1, citation.endLine - match.startLine + 1)}
            height={420}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}
