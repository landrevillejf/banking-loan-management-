CREATE TABLE IF NOT EXISTS loan_products (
    id BIGSERIAL PRIMARY KEY,
    product_type VARCHAR(20) NOT NULL UNIQUE,
    product_name VARCHAR(50) NOT NULL,
    default_interest_rate DECIMAL(5,2) NOT NULL,
    max_amount DECIMAL(19,2) NOT NULL,
    min_amount DECIMAL(19,2) NOT NULL,
    max_tenure_months INTEGER NOT NULL,
    min_tenure_months INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true
);
