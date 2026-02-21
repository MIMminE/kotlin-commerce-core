INSERT INTO inventories (inventory_id, product_id, product_name, idempotency_key, available_quantity, reserved_quantity, status, version, created_at, updated_at) VALUES
('2f1e6a7a-9c6b-4c2f-8e1b-6d8c3a9f5a11'::uuid, '9f1c2e6a-3b4d-4b5c-8d9e-0a1b2c3d4e11'::uuid, 'Acme Widget Small', 'd4f3a2b1-9c8e-4f7a-b6c5-1a2b3c4d5e11'::uuid, 120, 12, 'AVAILABLE', 1, CURRENT_TIMESTAMP::timestamptz, CURRENT_TIMESTAMP::timestamptz),
('4b6c8d9e-1f2a-4b3c-9d8e-0f1a2b3c4d33'::uuid, '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c22'::uuid, 'Omega Gadget Pro', 'e5a4b3c2-8d7f-4e6a-a5b4-2c3d4e5f6a22'::uuid, 10, 0, 'AVAILABLE', 0, CURRENT_TIMESTAMP::timestamptz, CURRENT_TIMESTAMP::timestamptz),
('6d8e0f1a-3c4d-5e6f-9a0b-2c3d4e5f6a55'::uuid, '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d33'::uuid, 'Delta Thing 500', 'f6b5c4d3-7e6f-4d8a-c3b2-3d4e5f6a7b33'::uuid, 300, 0, 'AVAILABLE', 2, CURRENT_TIMESTAMP::timestamptz, CURRENT_TIMESTAMP::timestamptz),
('8f0a2b3c-5e6f-7a8b-1c2d-4e5f6a7b8c77'::uuid, '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e44'::uuid, 'Sigma Component', 'a7c6d5e4-6f5a-4c9b-b2a1-4e5f6a7b8c44'::uuid, 60, 0, 'AVAILABLE', 0, CURRENT_TIMESTAMP::timestamptz, CURRENT_TIMESTAMP::timestamptz),
('a1bc4d5e-7a8b-9c0d-3e4f-6a7b8c9d0e99'::uuid, '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f55'::uuid, 'Epsilon Accessory', 'b8d7e6f5-5a4b-4b0c-a192-5f6a7b8c9d55'::uuid, 18, 3, 'AVAILABLE', 1, CURRENT_TIMESTAMP::timestamptz, CURRENT_TIMESTAMP::timestamptz)
ON CONFLICT (product_id, idempotency_key) DO NOTHING;

