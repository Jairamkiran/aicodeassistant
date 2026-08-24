import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatusChip } from './StatusChip';

describe('StatusChip', () => {
  it('renders a human label for each repository status', () => {
    render(<StatusChip status="READY" />);
    expect(screen.getByText('Ready')).toBeInTheDocument();
  });

  it('renders the failed label', () => {
    render(<StatusChip status="FAILED" />);
    expect(screen.getByText('Failed')).toBeInTheDocument();
  });
});
