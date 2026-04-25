INSERT INTO loan_products (product_type, product_name, default_interest_rate, max_amount, min_amount, max_tenure_months, min_tenure_months)
VALUES
    ('PERSONAL', 'Personal Loan', 12.5, 50000, 1000, 60, 12),
    ('MORTGAGE', 'Mortgage Loan', 5.5, 1000000, 50000, 360, 60),
    ('AUTO', 'Auto Loan', 8.0, 75000, 5000, 72, 12),
    ('BUSINESS', 'Business Loan', 10.0, 500000, 10000, 120, 12)
ON CONFLICT (product_type) DO UPDATE SET
    product_name = EXCLUDED.product_name,
    default_interest_rate = EXCLUDED.default_interest_rate,
    max_amount = EXCLUDED.max_amount,
    min_amount = EXCLUDED.min_amount,
    max_tenure_months = EXCLUDED.max_tenure_months,
    min_tenure_months = EXCLUDED.min_tenure_months;
