-- Insert rows only if there is no existing row with same product_id and idempotency_key
INSERT INTO inventories (inventory_id, product_id, product_name, idempotency_key, available_quantity, reserved_quantity, status, created_at, updated_at)
SELECT '2f1e6a7a-9c6b-4c2f-8e1b-6d8c3a9f5a11', '9f1c2e6a-3b4d-4b5c-8d9e-0a1b2c3d4e11', 'Acme Widget Small', 'd4f3a2b1-9c8e-4f7a-b6c5-1a2b3c4d5e11', 120, 0, 'AVAILABLE', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventories WHERE product_id = '9f1c2e6a-3b4d-4b5c-8d9e-0a1b2c3d4e11' AND idempotency_key = 'd4f3a2b1-9c8e-4f7a-b6c5-1a2b3c4d5e11');

INSERT INTO inventories (inventory_id, product_id, product_name, idempotency_key, available_quantity, reserved_quantity, status, created_at, updated_at)
SELECT '4b6c8d9e-1f2a-4b3c-9d8e-0f1a2b3c4d33', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c22', 'Omega Gadget Pro', 'e5a4b3c2-8d7f-4e6a-a5b4-2c3d4e5f6a22', 10, 0, 'AVAILABLE', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventories WHERE product_id = '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c22' AND idempotency_key = 'e5a4b3c2-8d7f-4e6a-a5b4-2c3d4e5f6a22');

INSERT INTO inventories (inventory_id, product_id, product_name, idempotency_key, available_quantity, reserved_quantity, status, created_at, updated_at)
SELECT '6d8e0f1a-3c4d-5e6f-9a0b-2c3d4e5f6a55', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d33', 'Delta Thing 500', 'f6b5c4d3-7e6f-4d8a-c3b2-3d4e5f6a7b33', 300, 0, 'AVAILABLE', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventories WHERE product_id = '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d33' AND idempotency_key = 'f6b5c4d3-7e6f-4d8a-c3b2-3d4e5f6a7b33');

INSERT INTO inventories (inventory_id, product_id, product_name, idempotency_key, available_quantity, reserved_quantity, status, created_at, updated_at)
SELECT '8f0a2b3c-5e6f-7a8b-1c2d-4e5f6a7b8c77', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e44', 'Sigma Component', 'a7c6d5e4-6f5a-4c9b-b2a1-4e5f6a7b8c44', 60, 0, 'AVAILABLE', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventories WHERE product_id = '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e44' AND idempotency_key = 'a7c6d5e4-6f5a-4c9b-b2a1-4e5f6a7b8c44');

INSERT INTO inventories (inventory_id, product_id, product_name, idempotency_key, available_quantity, reserved_quantity, status, created_at, updated_at)
SELECT 'a1bc4d5e-7a8b-9c0d-3e4f-6a7b8c9d0e99', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f55', 'Epsilon Accessory', 'b8d7e6f5-5a4b-4b0c-a192-5f6a7b8c9d55', 18, 0, 'AVAILABLE', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventories WHERE product_id = '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f55' AND idempotency_key = 'b8d7e6f5-5a4b-4b0c-a192-5f6a7b8c9d55');
