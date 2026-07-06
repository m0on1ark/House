UPDATE users SET building_id = NULL;
DELETE FROM buildings;

ALTER TABLE buildings
    DROP CONSTRAINT uc_f05edb12c96f4d61256dfd84c;

ALTER TABLE buildings
    DROP COLUMN teryt,
    DROP COLUMN simc,
    DROP COLUMN ulic;

ALTER TABLE buildings
    ADD COLUMN osm_id BIGINT NOT NULL,
    ADD COLUMN lat DOUBLE PRECISION,
    ADD COLUMN lon DOUBLE PRECISION;

ALTER TABLE buildings
    ADD CONSTRAINT uq_buildings_osm_id UNIQUE (osm_id);
