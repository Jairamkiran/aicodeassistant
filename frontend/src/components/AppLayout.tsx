import { useState } from 'react';
import { Link as RouterLink, Outlet, useLocation } from 'react-router-dom';
import {
  AppBar,
  Box,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import MenuIcon from '@mui/icons-material/Menu';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import LightModeIcon from '@mui/icons-material/LightMode';
import LogoutIcon from '@mui/icons-material/Logout';
import SourceIcon from '@mui/icons-material/Source';
import SearchIcon from '@mui/icons-material/Search';
import RateReviewIcon from '@mui/icons-material/RateReview';
import ChatIcon from '@mui/icons-material/Chat';
import SettingsIcon from '@mui/icons-material/Settings';
import { useColorMode } from '../theme/ColorModeProvider';
import { useAuth } from '../auth/AuthProvider';
import { OrgSwitcher } from './OrgSwitcher';
import { NotificationBell } from './NotificationBell';

const DRAWER_WIDTH = 240;

const NAV_ITEMS = [
  { to: '/repositories', label: 'Repositories', icon: <SourceIcon /> },
  { to: '/search', label: 'Search', icon: <SearchIcon /> },
  { to: '/code-review', label: 'Code review', icon: <RateReviewIcon /> },
  { to: '/chat', label: 'Chat', icon: <ChatIcon /> },
  { to: '/settings', label: 'Settings', icon: <SettingsIcon /> },
];

export function AppLayout() {
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const { mode, toggle } = useColorMode();
  const { user, logout } = useAuth();
  const location = useLocation();

  const drawer = (
    <Box role="navigation" aria-label="Primary">
      <Toolbar>
        <Typography variant="h3" noWrap component="div">
          AI Code Assistant
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {NAV_ITEMS.map((item) => {
          const selected = location.pathname.startsWith(item.to);
          return (
            <ListItemButton
              key={item.to}
              component={RouterLink}
              to={item.to}
              selected={selected}
              onClick={() => setMobileOpen(false)}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} />
            </ListItemButton>
          );
        })}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Box
        component="a"
        href="#main-content"
        sx={{
          position: 'absolute',
          left: -9999,
          top: 8,
          zIndex: (t) => t.zIndex.tooltip + 1,
          p: 1,
          bgcolor: 'background.paper',
          borderRadius: 1,
          '&:focus': { left: 8 },
        }}
      >
        Skip to main content
      </Box>
      <AppBar
        position="fixed"
        color="default"
        sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}
        elevation={0}
        variant="outlined"
      >
        <Toolbar sx={{ gap: 1 }}>
          {!isDesktop && (
            <IconButton
              edge="start"
              aria-label="Open navigation menu"
              onClick={() => setMobileOpen((o) => !o)}
            >
              <MenuIcon />
            </IconButton>
          )}
          <Box sx={{ flexGrow: 1 }} />
          <OrgSwitcher />
          <NotificationBell />
          <Tooltip title={mode === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}>
            <IconButton onClick={toggle} aria-label="Toggle color mode">
              {mode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
            </IconButton>
          </Tooltip>
          <Tooltip title={user ? `Sign out ${user.displayName}` : 'Sign out'}>
            <IconButton onClick={logout} aria-label="Sign out">
              <LogoutIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        <Drawer
          variant={isDesktop ? 'permanent' : 'temporary'}
          open={isDesktop || mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH },
          }}
        >
          {drawer}
        </Drawer>
      </Box>

      <Box
        component="main"
        id="main-content"
        tabIndex={-1}
        sx={{
          flexGrow: 1,
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          p: { xs: 2, md: 3 },
          outline: 'none',
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
