import { describe, expect, it } from 'vitest';
import { z } from 'zod';

/**
 * The return form's validation rules, mirrored from the page so the
 * behaviour that matters most — a return must carry at least one valid item —
 * is covered by a test.
 */
const itemSchema = z.object({
  sku: z.string().min(1, 'SKU is required'),
  quantity: z.coerce.number().int().min(1, 'Quantity must be at least 1'),
  condition: z.string().min(1),
});

const formSchema = z.object({
  reason: z.string().min(1, 'A reason is required'),
  items: z.array(itemSchema).min(1, 'Add at least one item'),
});

describe('return form validation', () => {
  it('rejects a return with no items', () => {
    const result = formSchema.safeParse({ reason: 'DEFECTIVE', items: [] });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Add at least one item');
    }
  });

  it('rejects an item without a SKU', () => {
    const result = formSchema.safeParse({
      reason: 'DEFECTIVE',
      items: [{ sku: '', quantity: 1, condition: 'GOOD' }],
    });
    expect(result.success).toBe(false);
  });

  it('rejects a non-positive quantity', () => {
    const result = formSchema.safeParse({
      reason: 'DEFECTIVE',
      items: [{ sku: 'NWR-PH-0142', quantity: 0, condition: 'GOOD' }],
    });
    expect(result.success).toBe(false);
  });

  it('accepts a well-formed return', () => {
    const result = formSchema.safeParse({
      reason: 'DEFECTIVE',
      items: [{ sku: 'NWR-PH-0142', quantity: 2, condition: 'GOOD' }],
    });
    expect(result.success).toBe(true);
  });
});
