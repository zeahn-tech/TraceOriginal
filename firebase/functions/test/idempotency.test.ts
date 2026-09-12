import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import type { createFakeFirestore } from './mocks/firestore';

vi.mock('firebase-admin', async () => {
  const { createFakeFirestore } = await import('./mocks/firestore');
  const fake = createFakeFirestore();
  return { initializeApp: vi.fn(), firestore: fake.firestore, __fake: fake };
});

let fake: ReturnType<typeof createFakeFirestore>;
let withIdempotency: typeof import('../src/lib/idempotency').withIdempotency;

beforeAll(async () => {
  const admin = (await import('firebase-admin')) as unknown as { __fake: ReturnType<typeof createFakeFirestore> };
  fake = admin.__fake;
  ({ withIdempotency } = await import('../src/lib/idempotency'));
});

beforeEach(() => fake._reset());

describe('withIdempotency', () => {
  it('rejects a missing/too-short requestId outright — no silent fallback to "just run it anyway"', async () => {
    await expect(withIdempotency('', async () => 'result')).rejects.toMatchObject({ code: 'invalid-argument' });
    await expect(withIdempotency('short', async () => 'result')).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('runs the command exactly once and returns its result on first call', async () => {
    const fn = vi.fn(async () => 'the-result');
    const result = await withIdempotency('a-valid-request-id-1', fn);
    expect(result).toBe('the-result');
    expect(fn).toHaveBeenCalledTimes(1);
  });

  it('a second call with the SAME key never re-runs the command — returns the cached result instead', async () => {
    const fn = vi.fn(async () => 'the-result');
    await withIdempotency('a-valid-request-id-2', fn);
    const second = await withIdempotency('a-valid-request-id-2', fn);
    expect(second).toBe('the-result');
    expect(fn).toHaveBeenCalledTimes(1); // NOT 2 — this is the whole point
  });

  it('a DIFFERENT key always runs the command again — keys are not globally sticky', async () => {
    const fn = vi.fn(async () => 'result');
    await withIdempotency('a-valid-request-id-3a', fn);
    await withIdempotency('a-valid-request-id-3b', fn);
    expect(fn).toHaveBeenCalledTimes(2);
  });

  it('if the command throws, the failure is recorded, and retrying the SAME key re-throws rather than silently retrying forever', async () => {
    const fn = vi.fn(async () => { throw new Error('boom'); });
    await expect(withIdempotency('a-valid-request-id-4', fn)).rejects.toThrow('boom');
    await expect(withIdempotency('a-valid-request-id-4', fn)).rejects.toMatchObject({ code: 'aborted' });
    expect(fn).toHaveBeenCalledTimes(1); // the second call did NOT re-invoke the failed command
  });

  it('a genuinely new attempt after a failure needs a NEW key — this is intentional, not a bug', async () => {
    const failingOnce = vi.fn(async () => { throw new Error('transient failure'); });
    await expect(withIdempotency('a-valid-request-id-5', failingOnce)).rejects.toThrow();
    const succeeding = vi.fn(async () => 'recovered');
    const result = await withIdempotency('a-valid-request-id-5-retry', succeeding);
    expect(result).toBe('recovered');
  });
});
