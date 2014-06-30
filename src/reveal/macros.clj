(ns reveal.macros)

(defn indexed-postwalk
  [coll pred & [transform]]
  (let [counter (atom 0)
        transformed
        (clojure.walk/postwalk
         #(if (pred %)
            (do
              (swap! counter inc)
              (if transform
                (transform % @counter)
                %))
            %) coll)]
    (if transform
      transformed
      @counter)))

(defmacro with-counter
  "Puts a counter onto all the slides contained within the body.
  The counter is formed by calling f with n, the number of the current slide
  and t, the total number of slides"
  [f & slides]
  (let [slide? #(and (list? %) (= (first %) 'slide))
        t (indexed-postwalk slides slide?)]
    (into [] (indexed-postwalk
              slides
              slide?
              (fn [slide n]
                (concat slide (list (list f n t))))))))

;(fn [i slide]
;           `(let [~'playing? (tailrecursion.javelin/cell false)
;                  ~'seconds-remaining (tailrecursion.javelin/cell 0)]
;              ~(concat slide
;                       (list `(~'input :type "range" :min 0 :max 60 :value 0)
;                             `(~'h2 ~(str (inc i) "/" total-slides))))))
;         slides))))
