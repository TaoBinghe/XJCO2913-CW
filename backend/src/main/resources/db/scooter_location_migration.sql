ALTER TABLE `scooter`
    ADD COLUMN `longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'GCJ-02 longitude for map display' AFTER `location`,
    ADD COLUMN `latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'GCJ-02 latitude for map display' AFTER `longitude`;

UPDATE `scooter` SET `location` = 'Xipu Campus Library North Plaza', `longitude` = 103.981570, `latitude` = 30.768249 WHERE `scooter_code` = 'SC001';
UPDATE `scooter` SET `location` = 'Xipu Campus Library South Plaza', `longitude` = 103.981320, `latitude` = 30.767580 WHERE `scooter_code` = 'SC002';
UPDATE `scooter` SET `location` = 'Xipu Campus South Gate', `longitude` = 103.981110, `latitude` = 30.764820 WHERE `scooter_code` = 'SC003';
UPDATE `scooter` SET `location` = 'Xipu Campus East Gate', `longitude` = 103.985100, `latitude` = 30.768050 WHERE `scooter_code` = 'SC004';
UPDATE `scooter` SET `location` = 'Xipu Campus West Gate', `longitude` = 103.977850, `latitude` = 30.768200 WHERE `scooter_code` = 'SC005';
UPDATE `scooter` SET `location` = 'Xipu Campus Teaching Building 1', `longitude` = 103.982980, `latitude` = 30.769450 WHERE `scooter_code` = 'SC006';
UPDATE `scooter` SET `location` = 'Xipu Campus Teaching Building 4', `longitude` = 103.979920, `latitude` = 30.769180 WHERE `scooter_code` = 'SC007';
UPDATE `scooter` SET `location` = 'Xipu Campus Student Center', `longitude` = 103.983540, `latitude` = 30.766980 WHERE `scooter_code` = 'SC008';
UPDATE `scooter` SET `location` = 'Xipu Campus Dormitory Area A', `longitude` = 103.979180, `latitude` = 30.766540 WHERE `scooter_code` = 'SC009';
UPDATE `scooter` SET `location` = 'Xipu Campus Stadium', `longitude` = 103.984280, `latitude` = 30.770120 WHERE `scooter_code` = 'SC010';
