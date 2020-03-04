# Get a list of playists with lengths (useful for producing schedule items).
SELECT name.name AS name, count(name.name) AS length FROM name
JOIN playlist WHERE name.id = playlist.name_key
GROUP BY name.name;

# Get a single playlist with length.
SELECT name.name AS name, count(name.name) AS length FROM name
JOIN playlist WHERE name.id = playlist.name_key AND name.name = $
GROUP BY name.name;
