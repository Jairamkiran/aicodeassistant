import { useState } from 'react';
import {
  Box,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Menu,
  MenuItem,
  Typography,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import type { ChatSession } from '../api/types';

interface Props {
  sessions: ChatSession[];
  activeSessionId: string | null;
  onSelect: (id: string) => void;
  onRename: (session: ChatSession) => void;
  onDelete: (session: ChatSession) => void;
}

export function SessionList({ sessions, activeSessionId, onSelect, onRename, onDelete }: Props) {
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
  const [menuSession, setMenuSession] = useState<ChatSession | null>(null);

  const openMenu = (event: React.MouseEvent<HTMLElement>, session: ChatSession) => {
    event.stopPropagation();
    setMenuAnchor(event.currentTarget);
    setMenuSession(session);
  };

  const closeMenu = () => {
    setMenuAnchor(null);
    setMenuSession(null);
  };

  if (sessions.length === 0) {
    return (
      <Box sx={{ p: 2 }}>
        <Typography variant="body2" color="text.secondary">
          No conversations yet.
        </Typography>
      </Box>
    );
  }

  return (
    <>
      <List dense aria-label="Conversations">
        {sessions.map((session) => (
          <ListItem
            key={session.id}
            disablePadding
            secondaryAction={
              <IconButton
                edge="end"
                size="small"
                aria-label={`Options for ${session.title}`}
                onClick={(e) => openMenu(e, session)}
              >
                <MoreVertIcon fontSize="small" />
              </IconButton>
            }
          >
            <ListItemButton
              selected={session.id === activeSessionId}
              onClick={() => onSelect(session.id)}
            >
              <ListItemText
                primary={session.title}
                primaryTypographyProps={{ noWrap: true }}
                secondary={new Date(session.updatedAt).toLocaleDateString()}
              />
            </ListItemButton>
          </ListItem>
        ))}
      </List>

      <Menu anchorEl={menuAnchor} open={Boolean(menuAnchor)} onClose={closeMenu}>
        <MenuItem
          onClick={() => {
            if (menuSession) onRename(menuSession);
            closeMenu();
          }}
        >
          Rename
        </MenuItem>
        <MenuItem
          onClick={() => {
            if (menuSession) onDelete(menuSession);
            closeMenu();
          }}
        >
          Delete
        </MenuItem>
      </Menu>
    </>
  );
}
