CREATE TABLE
    category (
        id VARCHAR(255) PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        icon VARCHAR(255),
        color VARCHAR(255),
        budget NUMERIC(12, 2)
    );

CREATE TABLE
    account (
        id VARCHAR(255) PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        bank VARCHAR(255),
        icon VARCHAR(255),
        color VARCHAR(255),
        balance NUMERIC(12, 2)
    );

CREATE TABLE
    transaction (
        id BIGSERIAL PRIMARY KEY,
        date DATE NOT NULL,
        description VARCHAR(255) NOT NULL,
        type VARCHAR(255) NOT NULL,
        amount NUMERIC(12, 2),
        category_id VARCHAR(255) REFERENCES category (id),
        account_id VARCHAR(255) REFERENCES account (id)
    );

INSERT INTO
    category (id, name, icon, color, budget)
VALUES
    (
        'needs',
        'Needs',
        'Home',
        '#6B8CAE',
        1800.00
    ),
    (
        'invest',
        'Investments',
        'TrendingUp',
        '#7A9B76',
        600.00
    ),
    (
        'emergency',
        'Emergency Fund',
        'ShieldCheck',
        '#D4A548',
        400.00
    ),
    (
        'leisure',
        'Leisure',
        'Sparkles',
        '#C97B84',
        300.00
    ) ON CONFLICT (id) DO NOTHING;