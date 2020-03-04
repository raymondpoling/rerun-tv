(ns schedule.schedule-types
	(:require [schedule.primes :refer [lcmv]]))

(defrecord Item [name index])

(defrecord Schedule [name playlists])

(defprotocol ScheduleType
	(length [self])
	(index [self idx]))

(defrecord Playlist [name length]
	ScheduleType
	(length [self] length)
	(index [self idx] (->Item name  (mod idx length))))

(defrecord Merge [playlists]
	ScheduleType
	(length [self] (reduce + (map length playlists)))
	(index [self idx]
		(let [updated-idx (mod idx (length self))
					reduct (fn [i p]
									(let [top (first p)]
										(if (< i (length top))
											(index top i)
											(recur (- i (length top)) (rest p)))))]
			(reduct updated-idx playlists))))

(defrecord Complex [playlists]
	ScheduleType
	(length [self] (* (count playlists) (apply lcmv (map length playlists))))
	(index [self idx]
		(index (nth playlists (mod idx (count playlists)))
                                 (int (/ idx (count playlists))))))

(defrecord Multi [playlist start step]
	ScheduleType
	(length [self]
		(if (= 0 (mod (length playlist) step))
			(/ (length playlist) step)
			(length playlist)))
	(index [self idx]
		(index playlist (+ start (* step idx)))))

(defrecord Schedule [name playlists])

(defn frame [schedule idx]
	(let [playlists (:playlists schedule)]
		(doall (map #(index % idx) playlists))))


(defmulti make-sched-type-from-json :type)

(defmethod make-sched-type-from-json "playlist" [item]
	(if (nil? (:name item))
		(throw (ex-info (str "Invalid playlist missing name: " item)
			{:type :missing-name, :cause :validity})))
	(if (nil? (:length item))
		(throw (ex-info (str "Invalid playlist missing length: " item)
			{:type :missing-length, :cause :validity})))
	(assoc (->Playlist (:name item) (:length item)) :type "playlist"))

(defmethod make-sched-type-from-json "merge" [item]
	(if (nil? (:playlists item))
		(throw (ex-info (str "Invalid merge missing playlists: " item)
			{:type :missing-playlists, :cause :validity})))
	(if (not (coll? (:playlists item)))
		(throw (ex-info (str "Invalid merge playlists not a seq: " item)
				{:type :not-a-seq, :cause :validity})))
	(if (= 0 (count (:playlists item)))
		(throw (ex-info (str "Invalid merge playlists empty: " item)
			{:type :missing-playlists, :cause :validity})))
	(assoc (->Merge (doall (map make-sched-type-from-json (:playlists item)))) :type "merge"))

(defmethod make-sched-type-from-json "multi" [item]
	(if (nil? (:playlist item))
		(throw (ex-info (str "Invalid multi missing playlist: " item)
			{:type :missing-playlist, :cause :validity})))
	(if (nil? (:start item))
		(throw (ex-info (str "Invalid multi playlist no start: " item)
				{:type :no-start, :cause :validity})))
	(if (nil? (:step item))
		(throw (ex-info (str "Invalid multi playlist no step: " item)
				{:type :no-step, :cause :validity})))
	(assoc (->Multi (make-sched-type-from-json (:playlist item)) (:start item) (:step item)) :type "multi"))

(defmethod make-sched-type-from-json "complex" [item]
	(if (nil? (:playlists item))
		(throw (ex-info (str "Invalid complex missing playlists: " item)
			{:type :missing-playlists, :cause :validity})))
	(if (not (coll? (:playlists item)))
		(throw (ex-info (str "Invalid complex playlists not a seq: " item)
				{:type :not-a-seq, :cause :validity})))
	(if (= 0 (count (:playlists item)))
		(throw (ex-info (str "Invalid complex playlists empty: " item)
			{:type :missing-playlists, :cause :validity})))
	(assoc (->Complex (doall (map make-sched-type-from-json (:playlists item)))) :type "complex"))

(defn make-schedule-from-json [schedule]
	(if (nil? (:playlists schedule))
		(throw (ex-info (str "Invalid schedule missing playlists: " schedule)
			{:type :missing-playlists, :cause :validity})))
	(if (nil? (:name schedule))
		(throw (ex-info (str "Invalid schedule missing name: " schedule)
			{:type :missing-playlists, :cause :validity})))
	(if (not (coll? (:playlists schedule)))
		(throw (ex-info (str "Invalid schedule playlists not a seq: " schedule)
				{:type :not-a-seq, :cause :validity})))
	(if (= 0 (count (:playlists schedule)))
		(throw (ex-info (str "Invalid schedule playlists empty: " schedule)
			{:type :missing-playlists, :cause :validity})))
	(->Schedule (:name schedule) (doall (map make-sched-type-from-json (:playlists schedule)))))
