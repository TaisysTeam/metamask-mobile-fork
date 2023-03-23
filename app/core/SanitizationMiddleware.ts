'use strict';

import { JsonRpcMiddleware, JsonRpcRequest } from "json-rpc-engine";
import { addHexPrefix } from 'ethereumjs-util';

interface SanitizationMiddlewareOptions { }

interface TransactionParamsLike {
  from: string
  to: string,
  value: string,
  data: string,
  gas: string,
  gasPrice: string,
  nonce: string,
  fromBlock: string,
  toBlock: string,
  address: string,
  topics: string[],
  [k: string]: string | string[],
}

// we use this to clean any custom params from the txParams
var permittedKeys = [
  'from',
  'to',
  'value',
  'data',
  'gas',
  'gasPrice',
  'nonce',
  'fromBlock',
  'toBlock',
  'address',
  'topics',
];

function cloneTxParams(txParams: TransactionParamsLike | any) {
  const sanitized = permittedKeys.reduce(function(copy, permitted: keyof TransactionParamsLike) {
    if (permitted in txParams) {
      if (Array.isArray(txParams[permitted])) {
        copy[permitted] = (txParams[permitted] as string[])
          .map(function(item) {
            return sanitize(item)
          });
      } else {
        copy[permitted] = sanitize(txParams[permitted]);
      }
    }
    return copy;
  }, {} as TransactionParamsLike);

  return sanitized;
}

function sanitize(value: any) {
  switch (value) {
    case 'latest':
      return value;
    case 'pending':
      return value;
    case 'earliest':
      return value;
    default:
      if (typeof value === 'string') {
        return addHexPrefix(value.toLowerCase());
      } else {
        return value;
      }
  }
}
export default function createSanitizationMiddleware(opts: SanitizationMiddlewareOptions = {}): JsonRpcMiddleware<unknown, unknown> {
  return (req, _, next) => {
    const params = req.params as Array<unknown>;
    const txParams = params[0];

    if (typeof txParams === 'object' && !Array.isArray(txParams) && txParams !== null) {
      var sanitized = cloneTxParams(txParams);
      console.log(sanitized);
      (req as JsonRpcRequest<any>).params[0] = sanitized;
    }

    next();
  }
}
