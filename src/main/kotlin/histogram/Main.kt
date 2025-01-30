package histogram

import common.*
import common.ConsoleColors.*
import common.RUtils.analyseHistogram
import common.StreamHelper.StreamType.WARMING_TRAINING_TEST
import common.StreamHelper.streamHeader
import common.StreamHelper.TOTAL_NUMBER_OF_SAMPLES
import common.StreamHelper.TARGET_MAX
import common.StreamHelper.TARGET_MIN
import common.StreamHelper.TRAINING_PHASE_LENGTH
import common.StreamHelper.WARMING_PHASE_LENGTH
import common.StreamHelper.loadStreaam
import common.StreamHelper.streamCounter
import common.StreamHelper.streamType
import common.StreamHelper.testPhaseStream
import common.StreamHelper.trainingPhaseStream
import common.StreamHelper.warmingphaseStream
import de.unknownreality.dataframe.DataRow
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import moa.classifiers.AbstractClassifier
import java.lang.Thread.sleep
import java.util.*
import kotlin.system.exitProcess

class Main {
    companion object {
        private val TAG: String = Main::class.java.simpleName
        private const val WARMING_PHASE_TAG = "__WARMING__"
        private const val TRAINING_PHASE_TAG = "__TRAINING__"
        private const val TEST_PHASE_TAG = "__TEST__"
        private const val DEBUG = true
        private var compositeDisposable = CompositeDisposable()
        private var numberOfRun = 10
        var currentLearnerType = LearnerType.AMRulesRegressor

        @JvmStatic
        fun main_1(args: Array<String>) {
            (0..3).forEach { _ ->  //number of Models
                println("##########   $currentLearnerType    ############")
                (1..numberOfRun).forEach {
                    if (common.DEBUG) {
                        println("************************* $it-th Experiment   ***************************")
                    }
                    Main().learnAndPredictionProcess(it)
                    println("************************* Resetting Stream ***************************")
                    compositeDisposable.clear()
                    StreamHelper.streamReset()
                }
                currentLearnerType = currentLearnerType.next()
            }
        }
        @JvmStatic
        fun main(args: Array<String>) {
            currentLearnerType = LearnerType.FIMTDD
            numberOfRun=10
            (1..numberOfRun).forEach {
                if (common.DEBUG) {
                    println("************************* $it-th Experiment   ***************************")
                }
                Main().learnAndPredictionProcess(it)
                println("************************* Resetting Stream ***************************")
                compositeDisposable.clear()
                StreamHelper.streamReset()
            }
        }
    }

    fun learnAndPredictionProcess(i: Int) {
        var numberOfSeenInstances = 0
        var numberOfSeenInstancesTMP = 0
        var lock=1
        var numberOfRepetition = 1

        streamType = WARMING_TRAINING_TEST

        loadStreaam()
        sleep(200)

        // Create learners
        val learnerBase = Learner(currentLearnerType, streamHeader!!).learner
        val learnerHistogramUS = learnerBase.copy() as AbstractClassifier
        val learnerHistogramOS = learnerBase.copy() as AbstractClassifier

        // Create Discretizer and initialize it
        val numberOfBinsinLayer1: Double = TOTAL_NUMBER_OF_SAMPLES/100
        val discretizer = PIDiscretizeV2(numberOfBinsinLayer1.toInt(), 50, 0.3, TARGET_MIN - EPSILON, TARGET_MAX + EPSILON, MethodForHistogram.Equalwidth, 100)
        discretizer.initialize()

        // Warm-up phase
        val warmingPhase = warmingphaseStream.doOnNext { instance ->
            instance.deleteAttributeAt(instance.lastIndex())
            discretizer.trainOnValue(instance.classValue())
            numberOfSeenInstances++
        }
            .doOnComplete {
            println("$YELLOW$WARMING_PHASE_TAG numberOfSeenInstances => $numberOfSeenInstances$RESET")
        }
            .subscribe({},  {
            println("$RED_BOLD_BRIGHT$WARMING_PHASE_TAG ${it.message} $RESET")
        },                  {
            numberOfSeenInstances = 0
            println("$YELLOW$WARMING_PHASE_TAG Completed!$RESET")
        })
        compositeDisposable.add(warmingPhase)
        discretizer.layer2.ignoreUpdate=false

        // Training phase
        val trainingPhase = trainingPhaseStream.doOnNext { instance ->
            instance.deleteAttributeAt(instance.lastIndex())
            runBlocking {
                val job: Job = GlobalScope.launch(Dispatchers.Default) {
                    coroutineScope {
                        launch {
                            //////////// Base Model /////////
                            learnerBase.trainOnInstanceImpl(instance)
                        }
                        launch {
                            //////////// UNDER_Sampling Model /////////
                            val classValue = instance.classValue()
                            discretizer.trainOnValue(classValue)
                            val probability = discretizer.layer1.getProb(classValue)
                            if ( Random().nextDouble() < probability) {
                                if ( classValue<1190 || classValue>1410 || (classValue>1260 && classValue<1340)) {numberOfSeenInstancesTMP++}
                                numberOfSeenInstances++
                                (0..numberOfRepetition).forEach { _ ->
                                    learnerHistogramUS.trainOnInstanceImpl(instance)
                                }

                                }
                            }
                        launch {
                            //////////// OVER_Sampling Model /////////
                            val classValue = instance.classValue()
                            discretizer.trainOnValue(classValue)
                            var probability = discretizer.layer1.getProb(classValue)
                            var probabilityOld: Double
                            var i:Int=1
                            learnerHistogramOS.trainOnInstanceImpl(instance)
                            while ( Random().nextDouble() < probability) {
                                learnerHistogramOS.trainOnInstanceImpl(instance)
                                probabilityOld=probability
                                probability /= ALPHA
                                if(i++> MAX_OVER) {
//                                    println("$RED_BOLD_BRIGHT $TRAINING_PHASE_TAG Error! probability_old ==> $probabilityOld$RESET")
//                                    println("$RED_BOLD_BRIGHT $TRAINING_PHASE_TAG Error! probability ==> $probability$RESET")
//                                    exitProcess(1)
                                    break;
                                }
                            }
                        }
                    }
                }
                job.join()
            }
        }
            .doOnComplete {
                println("$GREEN$TRAINING_PHASE_TAG numberOfSeenRareCasesEQw => /$numberOfSeenInstancesTMP/$numberOfSeenInstances/${streamCounter - WARMING_PHASE_LENGTH}$RESET")
            }.subscribe({}, {}, {
                numberOfSeenInstances = 0
                numberOfSeenInstancesTMP=0
                println("$GREEN$TRAINING_PHASE_TAG Completed!$RESET")
            })
        compositeDisposable.add(trainingPhase)

        val testPhase = testPhaseStream.doOnNext { instance ->
            val index = streamCounter - (WARMING_PHASE_LENGTH+TRAINING_PHASE_LENGTH) - 1
            val dataRow = DataRow(dataframeHist, index)
            val relevancyValue = instance.value(instance.lastIndex())
            instance.deleteAttributeAt(instance.lastIndex())
            var predictBase: Double
            var predictHistUS: Double
            var predictHistOS: Double
            runBlocking {
                val job: Job = GlobalScope.launch(Dispatchers.Default) {
                    coroutineScope {
                        launch {
                            predictBase = learnerBase.getVotesForInstance(instance)[0]
                            learnerBase.trainOnInstanceImpl(instance)
                            dataRow.set(ROW_NUMBER, streamCounter)
                            dataRow.set(RELEVANCY, String.format("%.4f", relevancyValue).toDouble())
                            dataRow.set(TRUE_VALUE, instance.classValue())
                            dataRow.set(PREDICTION_BASE, String.format("%.2f", predictBase).toDouble())
                        }
                        launch {
                            //////////// UNDER_Sampling Model /////////
                            val classValue = instance.classValue()
//                            discretizer.trainOnValue(classValue)
                            predictHistUS = learnerHistogramUS.getVotesForInstance(instance)[0]
                            dataRow.set(PREDICTION_HIST_US, String.format("%.2f", predictHistUS).toDouble())
                            val probability = discretizer.layer1.getProb(classValue)
                            dataRow.set(PROB_HIST, String.format("%.2f", probability).toDouble())
                            if ( Random().nextDouble() < probability) {
                                if ( classValue<1190 || classValue>1410 || (classValue>1260 && classValue<1340)) {numberOfSeenInstancesTMP++}
                                numberOfSeenInstances++
                                (0..numberOfRepetition).forEach { _ ->
                                    learnerHistogramUS.trainOnInstanceImpl(instance)
                                }
                            }
                        }

                        launch {
                            //////////// OVER_Sampling Model /////////
                            val classValue = instance.classValue()
//                            discretizer.trainOnValue(classValue)
                            predictHistOS = learnerHistogramOS.getVotesForInstance(instance)[0]
                            dataRow.set(PREDICTION_HIST_OS, String.format("%.2f", predictHistOS).toDouble())
                            var probability = discretizer.layer1.getProb(classValue)
                            learnerHistogramOS.trainOnInstanceImpl(instance)
                            var probabilityOld: Double
                            var numberOfTraining=1
                            while ( Random().nextDouble() < probability) {
                                learnerHistogramOS.trainOnInstanceImpl(instance)
                                probabilityOld=probability
                                probability /= ALPHA
                                if(numberOfTraining++> MAX_OVER) {
//                                    println("$RED_BOLD_BRIGHT $TEST_PHASE_TAG Error! probability_old ==> $probabilityOld$RESET")
//                                    println("$RED_BOLD_BRIGHT $TEST_PHASE_TAG Error! probability ==> $probability$RESET")
//                                    exitProcess(1)
                                    break;
                                }
                            }
                            dataRow.set(K_HIST, numberOfTraining)
                        }
                    }
                }
                job.join()
            }
            dataframeHist.append(dataRow)
        }
            .doOnComplete {
            println("$BLUE$TEST_PHASE_TAG numberOfSeenRareCasesEQw => /$numberOfSeenInstancesTMP/$numberOfSeenInstances/${streamCounter - (TRAINING_PHASE_LENGTH + WARMING_PHASE_LENGTH)}$RESET")
            dataframeHist.saveAsCSV("./Results/df.csv")
            analyseHistogram()
//            exitProcess(101)
            dataframeHist.clear()
        }.subscribe({}, {
            println("$RED_BOLD_BRIGHT$TEST_PHASE_TAG ${it.message} $RESET")
            exitProcess(1)
        }, {
                numberOfSeenInstances = 0
                println("$BLUE$TEST_PHASE_TAG Completed!$RESET")
        })
        compositeDisposable.add(testPhase)
    }
}



