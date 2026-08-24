import { useState } from 'react';
import {
  Badge,
  Box,
  Divider,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Popover,
  Tooltip,
  Typography,
} from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '../api/endpoints';

/**
 * Notification bell: shows the unread count (polled) and a popover list of recent
 * notifications. Opening an unread item marks it read.
 */
export function NotificationBell() {
  const queryClient = useQueryClient();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);

  const unread = useQuery({
    queryKey: ['notifications-unread'],
    queryFn: notificationApi.unreadCount,
    refetchInterval: 30_000,
  });

  const list = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationApi.list(20),
    enabled: Boolean(anchor),
  });

  const markRead = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread'] });
    },
  });

  const count = unread.data?.unread ?? 0;

  return (
    <>
      <Tooltip title="Notifications">
        <IconButton onClick={(e) => setAnchor(e.currentTarget)} aria-label="Notifications">
          <Badge badgeContent={count} color="error" max={99}>
            <NotificationsIcon />
          </Badge>
        </IconButton>
      </Tooltip>

      <Popover
        open={Boolean(anchor)}
        anchorEl={anchor}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Box sx={{ width: 360, maxWidth: '90vw' }}>
          <Typography variant="subtitle2" sx={{ p: 2, pb: 1 }}>
            Notifications
          </Typography>
          <Divider />
          {list.isSuccess && list.data.length === 0 && (
            <Typography color="text.secondary" sx={{ p: 2 }}>
              You have no notifications.
            </Typography>
          )}
          <List dense sx={{ maxHeight: 400, overflowY: 'auto' }}>
            {list.data?.map((n) => (
              <ListItemButton
                key={n.id}
                onClick={() => !n.read && markRead.mutate(n.id)}
                sx={{ bgcolor: n.read ? undefined : 'action.hover' }}
              >
                <ListItemText
                  primary={n.title}
                  secondary={n.message}
                  primaryTypographyProps={{ fontWeight: n.read ? 400 : 600 }}
                />
              </ListItemButton>
            ))}
          </List>
        </Box>
      </Popover>
    </>
  );
}
