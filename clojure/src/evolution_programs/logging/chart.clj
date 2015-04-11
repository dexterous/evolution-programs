(ns evolution-programs.logging.chart
  (:require [evolution-programs.util.population-statistic :as stat])
  (:import [javax.swing BoxLayout JFrame]
           [org.jfree.chart ChartFactory ChartPanel StandardChartTheme]
           [org.jfree.data.function Function2D]
           [org.jfree.data.general DatasetUtilities]
           [org.jfree.data.xy XYSeries XYSeriesCollection]
           [org.jfree.data.statistics BoxAndWhiskerCategoryDataset DefaultBoxAndWhiskerCategoryDataset]))

(ChartFactory/setChartTheme (StandardChartTheme. "JFree/Shadow" true))

(defmulti ^:private chart class)

(defmethod chart XYSeriesCollection [dataset]
  (doto (ChartFactory/createXYLineChart "Curve of function" "X" "Y" dataset)
    (->
      (.getXYPlot)
      (.getRenderer)
      (doto
        (.setSeriesLinesVisible 0 false)
        (.setSeriesShapesVisible 0 true)))))

(defmethod chart BoxAndWhiskerCategoryDataset [dataset]
  (ChartFactory/createBoxAndWhiskerChart "Population Analysis" "Generation" "Fitness" dataset false))

(defn- panel [chart]
  (doto (ChartPanel. chart)
    (.setMaximumDrawWidth 1600)
    (.setMouseWheelEnabled true)))

(defn- add-charts [frame datasets]
  (let [content-pane (.getContentPane frame)]
    (.setLayout content-pane (BoxLayout. content-pane BoxLayout/Y_AXIS))
    (doseq [dataset datasets]
      (.add content-pane (panel (chart dataset))))))

(defn- frame [title & datasets]
  (doto (JFrame. title)
    (add-charts datasets)
    (.setExtendedState JFrame/MAXIMIZED_BOTH)
    (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE)
    (.setVisible true)))

(defn- ->Function2D [f]
  (reify Function2D (getValue [_ x] (f x))))

(defn- sample [f min max samples]
  (doto (XYSeriesCollection. (XYSeries. "Individual"))
    (.addSeries (DatasetUtilities/sampleFunction2DToSeries (->Function2D f) min max samples "Value"))))

(defn- set-individuals [series f population]
  (doseq [[individual] population]
    (.add series individual (f individual) false)))

(defn- log-generation
  ([f generation-series analysis-dataset population generation-number]
   (doto generation-series
     (.clear)
     (set-individuals f population)
     (.fireSeriesChanged))
   (log-generation analysis-dataset population generation-number))
  ([analysis-dataset population generation-number]
   (doto analysis-dataset
     (.add (stat/fitness population) "Fitness" generation-number))
   (Thread/sleep 500)))

(defn logger
  ([]
   (let [analysis-dataset (DefaultBoxAndWhiskerCategoryDataset.)]
     (frame "Popluation Analysis" analysis-dataset)
     (partial log-generation analysis-dataset)))
  ([f min max samples]
   (let [sample-dataset (sample f min max samples)
         generation-series (.getSeries sample-dataset "Individual")
         analysis-dataset (DefaultBoxAndWhiskerCategoryDataset.)]
     (frame  "Population Analysis" sample-dataset analysis-dataset)
     (partial log-generation f generation-series analysis-dataset))))
