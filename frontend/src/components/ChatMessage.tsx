import { Box, Paper, Stack, Typography } from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import type { Citation, ChatTurnRole } from '../api/types';
import { CitationChip } from './CitationChip';

export interface ChatMessageModel {
  id: string;
  role: ChatTurnRole;
  content: string;
  citations: Citation[];
  /** True while the assistant answer is still streaming in. */
  streaming?: boolean;
}

interface Props {
  message: ChatMessageModel;
  onCitationClick: (citation: Citation) => void;
}

export function ChatMessage({ message, onCitationClick }: Props) {
  const isUser = message.role === 'USER';
  return (
    <Stack direction="row" spacing={1.5} sx={{ flexDirection: isUser ? 'row-reverse' : 'row' }}>
      <Box
        aria-hidden
        sx={{
          mt: 0.5,
          color: isUser ? 'secondary.main' : 'primary.main',
          '& svg': { fontSize: 28 },
        }}
      >
        {isUser ? <PersonIcon /> : <SmartToyIcon />}
      </Box>
      <Paper
        sx={{
          p: 2,
          maxWidth: '80%',
          bgcolor: isUser ? 'action.hover' : 'background.paper',
        }}
      >
        <Typography
          component="div"
          sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}
          aria-label={isUser ? 'Your message' : 'Assistant message'}
        >
          {message.content}
          {message.streaming && <Box component="span" className="stream-caret" aria-hidden />}
        </Typography>

        {message.citations.length > 0 && (
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1.5 }}>
            {message.citations.map((citation) => (
              <CitationChip
                key={citation.index}
                citation={citation}
                onClick={() => onCitationClick(citation)}
              />
            ))}
          </Stack>
        )}
      </Paper>
    </Stack>
  );
}
