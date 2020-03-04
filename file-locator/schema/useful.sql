SELECT protocol,path 
FROM file_locator.urls
JOIN file_locator.hosts
ON urls.host_id = hosts.id
JOIN file_locator.protocols
ON urls.protocol_id = protocols.id
JOIN file_locator.catalog_ids
ON urls.host_id = catalog_ids.id
WHERE catalog_ids.catalog_id =?
AND hosts.host = ?
