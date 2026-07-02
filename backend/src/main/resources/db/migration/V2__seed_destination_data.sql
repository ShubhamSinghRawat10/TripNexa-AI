CREATE TABLE destination_price_baseline (
    destination VARCHAR(120) PRIMARY KEY,
    avg_hotel_price BIGINT NOT NULL CHECK (avg_hotel_price > 0),
    peak_multiplier DECIMAL(3, 2) NOT NULL DEFAULT 1.40 CHECK (peak_multiplier >= 1.00)
);

CREATE TABLE destination_crowd_calendar (
    destination VARCHAR(120) NOT NULL REFERENCES destination_price_baseline(destination) ON DELETE CASCADE,
    travel_date DATE NOT NULL,
    crowd_level VARCHAR(20) NOT NULL,
    reason VARCHAR(160) NOT NULL,
    PRIMARY KEY (destination, travel_date)
);

INSERT INTO destination_price_baseline (destination, avg_hotel_price, peak_multiplier) VALUES
    ('Agra',       2800, 1.40),
    ('Ahmedabad',  2600, 1.40),
    ('Amritsar',   2500, 1.40),
    ('Andaman',    5200, 1.40),
    ('Bengaluru',  3400, 1.40),
    ('Darjeeling', 3800, 1.40),
    ('Goa',        4800, 1.40),
    ('Hyderabad',  3200, 1.40),
    ('Jaipur',     3000, 1.40),
    ('Jaisalmer',  3400, 1.40),
    ('Kochi',      3100, 1.40),
    ('Leh',        4200, 1.40),
    ('Manali',     3500, 1.40),
    ('Mumbai',     5200, 1.40),
    ('Munnar',     3600, 1.40),
    ('Rishikesh',  2900, 1.40),
    ('Shillong',   3300, 1.40),
    ('Udaipur',    3600, 1.40),
    ('Varanasi',   2700, 1.40);

-- Official 2026 all-India holidays and major festivals are precomputed as HIGH crowd dates.
-- Weekend dates are evaluated dynamically by HolidayUtil so the model keeps working beyond 2026.
INSERT INTO destination_crowd_calendar (destination, travel_date, crowd_level, reason)
SELECT destination, travel_date, 'HIGH', reason
FROM destination_price_baseline
CROSS JOIN (
    VALUES
        (DATE '2026-01-26', 'Republic Day'),
        (DATE '2026-03-04', 'Holi'),
        (DATE '2026-03-21', 'Id-ul-Fitr'),
        (DATE '2026-03-26', 'Ram Navami'),
        (DATE '2026-04-03', 'Good Friday'),
        (DATE '2026-05-01', 'Buddha Purnima'),
        (DATE '2026-06-26', 'Muharram'),
        (DATE '2026-08-15', 'Independence Day'),
        (DATE '2026-09-04', 'Janmashtami'),
        (DATE '2026-10-02', 'Gandhi Jayanti'),
        (DATE '2026-10-20', 'Dussehra'),
        (DATE '2026-11-08', 'Diwali'),
        (DATE '2026-11-24', 'Guru Nanak Jayanti'),
        (DATE '2026-12-25', 'Christmas Day')
) AS holidays(travel_date, reason);

CREATE INDEX idx_crowd_calendar_date ON destination_crowd_calendar(travel_date);
