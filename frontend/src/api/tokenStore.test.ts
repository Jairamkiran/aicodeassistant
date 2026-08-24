import { beforeEach, describe, expect, it, vi } from 'vitest';
import { tokenStore } from './tokenStore';

describe('tokenStore', () => {
  beforeEach(() => tokenStore.clear());

  it('stores and returns the access token', () => {
    tokenStore.set('abc');
    expect(tokenStore.get()).toBe('abc');
  });

  it('notifies subscribers on change and stops after unsubscribe', () => {
    const listener = vi.fn();
    const unsubscribe = tokenStore.subscribe(listener);

    tokenStore.set('one');
    expect(listener).toHaveBeenLastCalledWith('one');

    unsubscribe();
    tokenStore.set('two');
    expect(listener).toHaveBeenCalledTimes(1);
  });

  it('clear resets to null', () => {
    tokenStore.set('abc');
    tokenStore.clear();
    expect(tokenStore.get()).toBeNull();
  });
});
