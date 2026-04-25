CREATE TABLE IF NOT EXISTS loan_repayments (
    id UUID PRIMARY KEY,
    loan_reference VARCHAR(50) NOT NULL,
    installment_number INTEGER NOT NULL,
    due_amount DECIMAL(19,2) NOT NULL,
    due_date DATE NOT NULL,
    paid_amount DECIMAL(19,2),
    paid_date DATE,
    status VARCHAR(20) NOT NULL,
    transaction_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (loan_reference) REFERENCES loans(loan_reference)
);

CREATE INDEX idx_repayments_loan_reference ON loan_repayments(loan_reference);
CREATE INDEX idx_repayments_status ON loan_repayments(status);
CREATE INDEX idx_repayments_due_date ON loan_repayments(due_date);
