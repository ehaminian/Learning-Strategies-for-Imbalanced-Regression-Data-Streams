package common

import java.util.*

const val DEBUG = true
const val tmpPath="C:\\Users\\Ehsan\\Desktop\\conference\\Data\\ailerons\\ailerons_0.arff"
const val BASE_PATH="Datasets\\"
const val ROW_NUMBER="RowNumber"
const val TRUE_VALUE="TrueValue"
const val PHI="phiiRon"
const val RELEVANCY="relevancyValue"
const val PREDICTION_BASE="BasePrediction"
const val PROB_HIST="histPobability"
const val K_HIST="histK"
const val PREDICTION_HIST_US="ExperimentPredictionHistUNDER"
const val PREDICTION_HIST_OS="ExperimentPredictionHistOVER"
const val CHEBYSHEV_PROBABILITY="chevPropability"
const val CHEBYSHEV_K="chevK"
const val PREDICTION_UNDER="ExperimentPredictionUNDER"
const val PREDICTION_OVER="ExperimentPredictionOVER"
const val LAST_INDEX="last"
const val PREDICTION_1="PREDICTION_1"
const val PREDICTION_2="PREDICTION_2"
const val PREDICTION_3="PREDICTION_3"
const val NUMBER_OF_SAMPLES="NUMBER OF SAMPLES"
const val N_RARE_IN_RIGHT="RIGHT RARES"
const val N_RARE_IN_LEFT="LEFT RARES"
const val NAME_OF_DATASET = "Name"
const val PERCENT_RARE_IN_RIGHT = "percent RIGHT RARES"
const val PERCENT_RARE_IN_LEFT = "percent LEFT RARES"
const val TOTAL_RARE = "Total RARES"
const val PERCENT_TOTAL = "percent Total RARES"

const val learnerno = 1
const val side = "both" //left //both //right

var choosen = 1
var israre_value = 0.2
var threshold = 0.8

var datasetnumber: String? = null
var warmingphasesamples = 5000

var classindexdatasets = intArrayOf(30, 41, 13, 9, 64, 13, 19, 11, 9, 9, 33, 39) //start from 1
var datasetnames = arrayOf(
    "2dplanes",
    "ailerons",
    "air",
    "bank8",
    "bike",
    "cpusm",
    "elevator",
    "fried",
    "house",
    "kin8nm",
    "puma32H",
    "weatherHistory"
)

//	protected static int[] numInstancesindex= {81536,44924,56991,81920,52137,49152,85430,94376,69831,44800,49530,40960,144997};
var datasetname = datasetnames[choosen]

//	protected static int numInstances=numInstancesindex[choosen];
var classindex = classindexdatasets[choosen]
var root = "C:\\Users\\Ehsan\\Desktop\\conference\\Data\\"
var workspace = root + datasetname + "\\"
var f_tail_name = ""
var path = workspace + datasetname + "_" + datasetnumber + ".arff"
var storefilename = datasetname + "_" + datasetnumber
const val debug = false

var Starget = 0.0
var numberofseensamples = 0
var _indexs: List<Int> = ArrayList()
var size = 0
var noes = 0
var base = numberofseensamples
var OurPrediction = DoubleArray(1)
var NormalPrediction = DoubleArray(1)
var OurPrediction2 = DoubleArray(1)
var NormalPrediction2 = DoubleArray(1)
var prob = 0.0
const val EPSILON = 1.0E-8
const val BETA= 1 //forget factor
const val ALPHA= 1.02 //forget factor
const val MAX_OVER= 40 //