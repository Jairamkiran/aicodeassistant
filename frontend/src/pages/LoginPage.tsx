import { useState, type FormEvent } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Container,
  Link,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useAuth } from '../auth/AuthProvider';
import { ApiError } from '../api/problem';

interface LocationState {
  from?: { pathname: string };
}

export function LoginPage() {
  const { status, login, register } = useAuth();
  const location = useLocation();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (status === 'authenticated') {
    const to = (location.state as LocationState)?.from?.pathname ?? '/repositories';
    return <Navigate to={to} replace />;
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      if (mode === 'login') {
        await login(email, password);
      } else {
        await register(email, password, displayName);
      }
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : 'Unable to reach the server. Please try again.',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Container maxWidth="sm" sx={{ display: 'flex', minHeight: '100vh', alignItems: 'center' }}>
      <Paper sx={{ p: 4, width: '100%' }}>
        <Typography variant="h1" gutterBottom>
          {mode === 'login' ? 'Sign in' : 'Create your account'}
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 3 }}>
          AI Software Engineering Assistant
        </Typography>

        <Box component="form" onSubmit={handleSubmit} noValidate>
          <Stack spacing={2}>
            {error && <Alert severity="error">{error}</Alert>}
            {mode === 'register' && (
              <TextField
                label="Display name"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                required
                autoComplete="name"
                fullWidth
              />
            )}
            <TextField
              label="Email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              fullWidth
            />
            <TextField
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              fullWidth
            />
            <Button type="submit" variant="contained" size="large" disabled={submitting}>
              {submitting ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
            </Button>
          </Stack>
        </Box>

        <Typography variant="body2" sx={{ mt: 3 }}>
          {mode === 'login' ? "Don't have an account? " : 'Already have an account? '}
          <Link
            component="button"
            type="button"
            onClick={() => {
              setMode(mode === 'login' ? 'register' : 'login');
              setError(null);
            }}
          >
            {mode === 'login' ? 'Register' : 'Sign in'}
          </Link>
        </Typography>
      </Paper>
    </Container>
  );
}
