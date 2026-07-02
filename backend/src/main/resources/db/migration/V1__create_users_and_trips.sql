CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    preferred_transport VARCHAR(40),
    budget_preference VARCHAR(40),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE trips (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    destination VARCHAR(120) NOT NULL,
    budget BIGINT NOT NULL CHECK (budget > 0),
    days INTEGER NOT NULL CHECK (days BETWEEN 1 AND 30),
    people INTEGER NOT NULL CHECK (people BETWEEN 1 AND 50),
    travel_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE trip_interests (
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    interest VARCHAR(50) NOT NULL,
    PRIMARY KEY (trip_id, interest)
);

CREATE INDEX idx_trips_user_created_at ON trips(user_id, created_at DESC);
