(ns schedule.schedule-types
	(:require [schedule.primes :refer [lcmv]]))

(defrecord Item [name index])

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

(defn frame [schedule]
	(let [playlists (:playlists schedule)]
	(* (count playlists) (lcmv (map length playlists)))))


(defmulti make-sched-type-from-json :type)

(defn make-schedule-from-json [name playlists]
	(map playlists make-sched-type-from-json))

(defmethod make-sched-type-from-json "playlist" [item]
	(if (nil? (:name item))
		(throw (ex-info (str "Invalid playlist missing name: " item)
			{:type :missing-name, :cause :validity})))
	(if (nil? (:length item))
		(throw (ex-info (str "Invalid playlist missing length: " item)
			{:type :missing-length, :cause :validity})))
	(->Playlist (:name item) (:length item)))

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
	(->Merge (map make-sched-type-from-json (:playlists item))))
