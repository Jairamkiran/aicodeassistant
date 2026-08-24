import { createTheme, type Theme } from '@mui/material/styles';

/**
 * Two themes sharing one palette seed. The color-mode toggle (see
 * {@link ColorModeProvider}) swaps between them and persists the choice.
 */
function buildTheme(mode: 'light' | 'dark'): Theme {
  return createTheme({
    palette: {
      mode,
      primary: { main: '#1976d2' },
      secondary: { main: '#7c4dff' },
      ...(mode === 'dark'
        ? { background: { default: '#0e1116', paper: '#161b22' } }
        : { background: { default: '#f6f8fa', paper: '#ffffff' } }),
    },
    shape: { borderRadius: 8 },
    typography: {
      fontFamily:
        '"Inter", "Segoe UI", Roboto, -apple-system, BlinkMacSystemFont, sans-serif',
      h1: { fontSize: '1.9rem', fontWeight: 700 },
      h2: { fontSize: '1.5rem', fontWeight: 700 },
      h3: { fontSize: '1.2rem', fontWeight: 600 },
    },
    components: {
      MuiButton: { defaultProps: { disableElevation: true } },
      MuiPaper: { defaultProps: { variant: 'outlined' } },
    },
  });
}

export const lightTheme = buildTheme('light');
export const darkTheme = buildTheme('dark');
