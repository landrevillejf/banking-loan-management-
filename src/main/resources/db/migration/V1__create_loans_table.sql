CREATE TABLE IF NOT EXISTS loans (
    id UUID PRIMARY KEY,
    loan_reference VARCHAR(50) UNIQUE NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    loan_product VARCHAR(20) NOT NULL,
    principal_amount DECIMAL(19,2) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    total_interest DECIMAL(19,2) NOT NULL,
    total_repayable DECIMAL(19,2) NOT NULL,
    monthly_payment DECIMAL(19,2) NOT NULL,
    tenure_months INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    purpose TEXT,
    remaining_balance DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    disbursed_at TIMESTAMP,
    closed_at TIMESTAMP,
    approved_by VARCHAR(100),
    version INTEGER DEFAULT 0
);

CREATE INDEX idx_loans_account_number ON loans(account_number);
CREATE INDEX idx_loans_customer_id ON loans(customer_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_loan_reference ON loans(loan_reference);
