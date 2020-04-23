(ns helpers.schedule-builder
  (:require
   [cheshire.core :refer [parse-string generate-string]]
   [clojure.string :as cls]))

(defn pretty-divide [upper lower]
  (format "%.2f" (float (/ upper lower))))

(defn make-row [class length rr & extra]
  (list :tr {:class class}
    (vec
      (concat
       (list :th {:scope "row" :class "first"}
             (cls/capitalize class) ": " length [:br] "RR: " rr)
        (when extra (list [:br] (cls/join " " extra)))))))

(defprotocol ScheduleType
  (render [self row? small divisor])
  (length [self]))

(defrecord Playlist [name length]
  ScheduleType
  (render [self row? small divisor] (if row?
    (let [render (render self false small divisor)
          len (int (Math/floor (/ (first render) divisor)))]
      [len (vec (concat (make-row "playlist" length (pretty-divide length small)) (second render)))])
    (let [len (int (Math/floor (/ length divisor)))]
      [len (list
        [:td {:colspan len}
          [:span {:class "name"} name]
          [:br]
          [:span {:class "count"} "Count: " length]])])))
  (length [self] length))

(defrecord Merge [playlists]
  ScheduleType
  (render [self row? small divisor]
    (if row?
      (let [render (map #(render % false small divisor) playlists)
            len (reduce + (map first render))]
        [len (vec
          (concat
            (make-row "merge" (length self) (pretty-divide (length self) small))
            (map second render)))])
      (let [render (map #(render % false small divisor) playlists)]
        [(reduce + (map first render)) (vec (reduce concat (map second render)))])))
  (length [self] (reduce + (map #(length %) playlists))))

(defrecord Multi [playlist step]
  ScheduleType
  (render [self row? small divisor]
    (if row?
      (let [render (render playlist false small (* step divisor))
            len (first render)]
        [len (vec
          (concat
            (make-row "multi" (length self) (pretty-divide (length self) small) "Step:" step)
          (second render)))])
      (let [render (render playlist false small (* step divisor))]
        [(first render)
        (concat
          (list
            [:td {:class "first"}
              "Multi: " (length self) [:br]
              "Step: " step [:br]
              "RR: " (pretty-divide (length self) small)])
              (second render))])))
  (length [self] (float (/ (length playlist) step))))

(defrecord Complex [playlists]
  ScheduleType
  (render [self row? small divisor]
    (if row?
      (let [render (map #(render % true (/ small (count playlists)) divisor)
                        playlists)]
        [(length self) (vec
              (concat
               (make-row "complex"
                         (length self)
                         (pretty-divide
                          (length self)
                          small))
               (list [:td {:colspan (length self)}
                      [:table (map second render)]])))])
      (let [render (map #(render % true (/ small (count playlists)) divisor)
                        playlists)]
        [(length self)
         (list [:td {:colspan (apply max (map length playlists))} [:table (map second render)]])])))
  (length [self] 
                   (* (apply max (map length playlists))
                    (count playlists))))

(defprotocol Schedule
  (parses? [self])
  (parsed [self])
  (string [self])
  (valid? [self]))

(defn make-schedule-string [in-string validity]
  (try
    (let [p (parse-string in-string true)
          v (validity in-string)]
      (reify Schedule
        (parses? [_] true)
        (parsed [_] p)
        (string [_] in-string)
        (valid? [_] v)))
    (catch Exception e
      (reify Schedule
        (parses? [_] false)
        (parsed [_] nil)
        (string [_] in-string)
        (valid? [_] {:status "invalid" :messages [(.getMessage e)]})))))

(defn make-schedule-map [map validity]
  (let [g (generate-string map {:pretty true})
        v (validity g)]
    (reify Schedule
      (parses? [_] true)
      (parsed [_] map)
      (string [_] g)
      (valid? [_] v))))
