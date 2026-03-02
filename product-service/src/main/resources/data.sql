INSERT INTO public.products (product_id, created_at, updated_at, idempotency_key, amount, currency, product_name, version) VALUES
('9f1c2e6a-3b4d-4b5c-8d9e-0a1b2c3d4e11', now(), now(), 'd4f3a2b1-9c8e-4f7a-b6c5-1a2b3c4d5e11', 199900, 'KRW', 'Acme Widget Small', 1),
('1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c22', now(), now(), 'e5a4b3c2-8d7f-4e6a-a5b4-2c3d4e5f6a22', 499900, 'KRW', 'Omega Gadget Pro', 1),
('2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d33', now(), now(), 'f6b5c4d3-7e6f-4d8a-c3b2-3d4e5f6a7b33', 125000, 'KRW', 'Delta Thing 500', 0),
('3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e44', now(), now(), 'a7c6d5e4-6f5a-4c9b-b2a1-4e5f6a7b8c44', 75000, 'KRW', 'Sigma Component', 0),
('4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f55', now(), now(), 'b8d7e6f5-5a4b-4b0c-a192-5f6a7b8c9d55', 299900, 'KRW', 'Epsilon Accessory', 1)
ON CONFLICT (product_name, idempotency_key) DO NOTHING;