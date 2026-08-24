# Inventory Costing And Stock Count Design

## Goal

Complete the inventory flow from ingredient receipts through recipe consumption, theoretical stock, physical counts, variance, waste cost, and period reporting. Product availability remains derived from ingredient balances and recipes; no fixed menu quantity is stored.

## Decisions

- One logical warehouse.
- Moving weighted-average cost after each approved receipt.
- Purchase units convert to `G`, `ML`, or `PIECE` at receipt approval.
- Fresh/high-value items count daily; dry goods and packaging count weekly.
- Receipts and stock counts use `DRAFT` then immutable `APPROVED` state.
- Approved mutations and ledger rows commit atomically.
- `CONSUME`, `WASTE`, and stock-count adjustments snapshot cost so historical reports never change after later receipts.
- Supplier name and invoice number are receipt snapshots; no supplier module.

## Flow

`RECEIPT → INVENTORY → RECIPE → ORDER CONSUMPTION → THEORETICAL STOCK → PHYSICAL COUNT → VARIANCE → COST REPORT`.

## Cost Rules

`newAverageCost = (oldOnHand * oldAverageCost + receiptBaseQuantity * receiptBaseUnitCost) / (oldOnHand + receiptBaseQuantity)`.

`receiptBaseQuantity = purchaseQuantity * conversionFactor` and `receiptBaseUnitCost = lineTotal / receiptBaseQuantity`.

`varianceQuantity = actualQuantity - theoreticalQuantity`. Negative variance is stock-count loss. Known damage or expiry is recorded as `WASTE`, not counted again as stock-count loss.

`totalLossCost = wasteCost + stockCountLossCost`; `lossRate = totalLossCost / consumptionCost * 100` when consumption cost is positive.

## Safety

- Use `DECIMAL(19,4)` and Java `BigDecimal`; never floating-point arithmetic.
- Draft documents do not mutate inventory.
- Approved documents cannot be edited or deleted.
- Approval locks documents and inventory items in stable ID order.
- Approval rejects stale theoretical quantity/cost with HTTP 409.
- Physical quantity cannot be below reserved quantity.
- Retained database execution requires verified backup, disposable validation, and separate approval.

## Out Of Scope

Lots, expiry dates, FEFO/FIFO, supplier master data, accounts payable, multiple warehouses, prepared batches, and demand forecasting.
