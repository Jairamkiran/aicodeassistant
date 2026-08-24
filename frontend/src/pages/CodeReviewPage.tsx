import { useMemo, useState, type FormEvent } from 'react';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Button,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import RateReviewIcon from '@mui/icons-material/RateReview';
import { useMutation, useQuery } from '@tanstack/react-query';
import { codeReviewApi, repositoryApi } from '../api/endpoints';
import { useOrg } from '../auth/OrgProvider';
import type { ReviewFinding } from '../api/types';
import { SeverityChip } from '../components/SeverityChip';
import { EmptyState, ErrorState, LoadingState } from '../components/FeedbackStates';

export function CodeReviewPage() {
  const { activeOrg } = useOrg();
  const [repositoryId, setRepositoryId] = useState('');
  const [focus, setFocus] = useState('');

  const repos = useQuery({
    queryKey: ['repositories', activeOrg?.id],
    queryFn: () => repositoryApi.list(activeOrg!.id),
    enabled: Boolean(activeOrg),
  });

  const review = useMutation({
    mutationFn: () => codeReviewApi.review(activeOrg!.id, repositoryId, focus.trim()),
  });

  const readyRepos = useMemo(
    () => (repos.data ?? []).filter((r) => r.status === 'READY'),
    [repos.data],
  );

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (repositoryId && focus.trim().length > 0) {
      review.mutate();
    }
  };

  if (!activeOrg) return <LoadingState label="Loading organization…" />;

  return (
    <Box>
      <Typography variant="h1" gutterBottom>
        Code review
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        AI-assisted, structured review grounded in your indexed code.
      </Typography>

      <Paper sx={{ p: 3, mb: 3 }} component="form" onSubmit={handleSubmit}>
        <Stack spacing={2}>
          <TextField
            select
            label="Repository"
            value={repositoryId}
            onChange={(e) => setRepositoryId(e.target.value)}
            disabled={readyRepos.length === 0}
            helperText={readyRepos.length === 0 ? 'Index a repository first.' : undefined}
            required
          >
            {readyRepos.map((repo) => (
              <MenuItem key={repo.id} value={repo.id}>
                {repo.name}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Review focus"
            placeholder="e.g. error handling and input validation in the auth flow"
            value={focus}
            onChange={(e) => setFocus(e.target.value)}
            multiline
            minRows={2}
            required
          />
          <Box>
            <Button
              type="submit"
              variant="contained"
              startIcon={<RateReviewIcon />}
              disabled={!repositoryId || focus.trim().length === 0 || review.isPending}
            >
              {review.isPending ? 'Reviewing…' : 'Run review'}
            </Button>
          </Box>
        </Stack>
      </Paper>

      {review.isPending && <LoadingState label="Analyzing code…" />}
      {review.isError && <ErrorState error={review.error} onRetry={() => review.mutate()} />}
      {review.isSuccess && <ReviewResult summary={review.data.summary} findings={review.data.findings} />}
    </Box>
  );
}

function ReviewResult({ summary, findings }: { summary: string; findings: ReviewFinding[] }) {
  return (
    <Box>
      {summary && (
        <Alert severity="info" sx={{ mb: 2 }}>
          {summary}
        </Alert>
      )}
      {findings.length === 0 ? (
        <EmptyState title="No findings" description="The reviewer did not flag any issues for this focus." />
      ) : (
        <Stack spacing={1}>
          {findings.map((finding, index) => (
            <Accordion key={`${finding.filePath}:${finding.startLine}:${index}`} disableGutters>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Stack direction="row" spacing={1.5} alignItems="center" sx={{ width: '100%' }}>
                  <SeverityChip severity={finding.severity} />
                  <Typography sx={{ flexGrow: 1 }}>{finding.title}</Typography>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{ fontFamily: 'monospace', display: { xs: 'none', sm: 'block' } }}
                  >
                    {finding.filePath}
                    {finding.startLine > 0 ? `:${finding.startLine}` : ''}
                  </Typography>
                </Stack>
              </AccordionSummary>
              <AccordionDetails>
                <Typography variant="overline" color="text.secondary">
                  {finding.category}
                </Typography>
                <Typography paragraph>{finding.detail}</Typography>
                {finding.recommendation && (
                  <>
                    <Typography variant="subtitle2">Recommendation</Typography>
                    <Typography color="text.secondary">{finding.recommendation}</Typography>
                  </>
                )}
              </AccordionDetails>
            </Accordion>
          ))}
        </Stack>
      )}
    </Box>
  );
}
