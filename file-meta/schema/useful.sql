# Find by catalog id
SELECT *
FROM meta.files
JOIN meta.series
WHERE meta.files.id = meta.series.series_id
AND meta.series.catalog_prefix = %
AND meta.series.season = %
AND meta.files.episode = %;
