import type { JsonRpcRequest, PendingJsonRpcResponse } from 'json-rpc-engine';
import {
  TransactionController,
  WalletDevice,
} from '@metamask/transaction-controller';
import { isObject, hasProperty } from '@metamask/utils';
import { ethErrors } from 'eth-json-rpc-errors';

/**
 * Handle a `eth_sendTransaction` request.
 *
 * @param args - Named arguments.
 * @param args.hostname - The hostname associated with this request.
 * @param args.req - The JSON-RPC request.
 * @param args.res - The JSON-RPC response.
 * @param args.sendTransaction - A function that requests approval for the given transaction, then
 * signs the transaction and broadcasts it.
 * @param args.validateAccountAndChainId - A function that validates the account and chain ID
 * used in the transaction.
 */
async function eth_sendTransaction({
  hostname,
  req,
  res,
  sendTransaction,
  validateAccountAndChainId,
}: {
  hostname: string;
  req: JsonRpcRequest<unknown> & { method: 'eth_sendTransaction' };
  res: PendingJsonRpcResponse<unknown>;
  sendTransaction: TransactionController['addTransaction'];
  validateAccountAndChainId: (args: {
    from: string;
    chainId?: number;
  }) => Promise<void>;
}) {
  if (
    !Array.isArray(req.params) &&
    !(isObject(req.params) && hasProperty(req.params, 0))
  ) {
    throw ethErrors.rpc.invalidParams({
      message: `Invalid parameters: expected an array`,
    });
  }
  const transactionParameters = req.params[0];
  if (!isObject(transactionParameters)) {
    throw ethErrors.rpc.invalidParams({
      message: `Invalid parameters: expected the first parameter to be an object`,
    });
  }
  await validateAccountAndChainId({
    from: req.params[0].from,
    chainId: req.params[0].chainId,
  });

  const hash = await (
    await sendTransaction(req.params[0], hostname, WalletDevice.MM_MOBILE)
  ).result;

  res.result = hash;
}

export default eth_sendTransaction;
