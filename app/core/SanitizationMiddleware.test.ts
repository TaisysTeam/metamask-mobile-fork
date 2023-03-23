'use strict';

import createSanitizationMiddleware from './SanitizationMiddleware';

function deepFreeze(object: any) {
  // Retrieve the property names defined on object
  const propNames = Reflect.ownKeys(object);

  // Freeze properties before freezing self
  for (const name of propNames) {
    const value = object[name];

    if ((value && typeof value === "object") || typeof value === "function") {
      deepFreeze(value);
    }
  }

  return Object.freeze(object);
}

describe('createSanitizationMiddleware', () => {

  it.only('does nothing when the method is not transaction-like', () => {
    const sanitizationMiddleware = createSanitizationMiddleware();
    const nonTransactionLike = [
      ['foo'],
      [123],
      [{ not: 'a', transation: 'i promise' }],
      [{ not: 'a', transation: 'i promise' }, 123],
    ] as any[];

    nonTransactionLike.forEach((params) => {
      const req = {
        jsonrpc: '2.0' as '2.0',
        id: '1',
        method: 'any',
        params,
      };

      sanitizationMiddleware(
        req,
        { jsonrpc: '2.0', id: 'any' },
        () => { },
        () => { }
      );

      expect(req.params[0]).toBe(params[0]);
    });
  });

  it('does nothing when the method is transaction-like, but already has 0x prefixed values', () => {
    const sanitizationMiddleware = createSanitizationMiddleware();
    const testTxLike = {
      jsonrpc: '2.0' as '2.0',
      id: '1',
      method: 'any',
      params: deepFreeze([
        {
          from: '0x123',
          to: '0x123',
          value: '0x123',
          data: '0x123',
        },
        'latest'
      ])
    };

    expect(() => {
      sanitizationMiddleware(
        testTxLike,
        { jsonrpc: '2.0', id: 'any' },
        () => { },
        () => { }
      )
    }).not.toThrow();
  });

  it('does nothing when the method is transaction-like, but the block tag is already 0x prefixed', () => {
    const sanitizationMiddleware = createSanitizationMiddleware();
    const testTxLike = {
      jsonrpc: '2.0' as '2.0',
      id: '1',
      method: 'any',
      params: deepFreeze([
        {
          from: '123',
          to: '0x123',
          value: '0x123',
          data: '0x123',
        },
        '0x123'
      ])
    };

    expect(() => {
      sanitizationMiddleware(
        testTxLike,
        { jsonrpc: '2.0', id: 'any' },
        () => { },
        () => { }
      )
    }).not.toThrow();
  });

  it('hex prefixes any tx param that is missing one', () => {
    const sanitizationMiddleware = createSanitizationMiddleware();
    const testTxLike = {
      jsonrpc: '2.0' as '2.0',
      id: '1',
      method: 'any',
      params: deepFreeze([
        {
          from: '123',
          to: '0x123',
          value: '0x123',
          data: '0x123',
        },
        '0x123'
      ])
    } as any;

    sanitizationMiddleware(
      testTxLike,
      { jsonrpc: '2.0', id: 'any' },
      () => { },
      () => { }
    )
    expect(testTxLike.method).toBe('123')
  });
});
