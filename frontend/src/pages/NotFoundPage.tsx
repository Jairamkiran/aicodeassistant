import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, Container, Typography } from '@mui/material';

export function NotFoundPage() {
  return (
    <Container maxWidth="sm">
      <Box sx={{ textAlign: 'center', py: 10 }}>
        <Typography variant="h1" gutterBottom>
          404
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 3 }}>
          The page you are looking for does not exist.
        </Typography>
        <Button component={RouterLink} to="/" variant="contained">
          Go home
        </Button>
      </Box>
    </Container>
  );
}
