USE `arrowhead`;

REVOKE ALL, GRANT OPTION FROM 'serviceinventory'@'localhost';

GRANT ALL PRIVILEGES ON `arrowhead`.`logs` TO 'serviceinventory'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'serviceinventory'@'%';

GRANT ALL PRIVILEGES ON `arrowhead`.`logs` TO 'serviceinventory'@'%';

FLUSH PRIVILEGES;