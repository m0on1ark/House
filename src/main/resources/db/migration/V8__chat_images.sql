ALTER TABLE deal_messages
    ADD COLUMN image      BYTEA,
    ADD COLUMN image_type VARCHAR(100);

ALTER TABLE building_messages
    ADD COLUMN image      BYTEA,
    ADD COLUMN image_type VARCHAR(100);
