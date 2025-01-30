// This is the code that implement the paper
package chebyshev


import common.*
import common.ConsoleColors.*
import common.StreamHelper.StreamType.TRAIN_TEST
import common.StreamHelper.TRAINING_PHASE_LENGTH
import common.StreamHelper.loadStreaam
import common.StreamHelper.testPhaseStream
import common.StreamHelper.streamReset
import common.StreamHelper.streamCounter
import common.StreamHelper.streamHeader
import common.StreamHelper.streamType
import common.StreamHelper.trainingPhaseStream
import de.unknownreality.dataframe.DataRow
import common.LearnerType
import common.Learner
import common.RUtils.analyseCheby
import histogram.lastIndex
import kotlinx.coroutines.*
import moa.classifiers.AbstractClassifier
import java.lang.Thread.sleep
import java.util.Random
import io.reactivex.disposables.CompositeDisposable
import kotlin.system.exitProcess


class Main {
    companion object {
        private val TAG: String = Main::class.java.simpleName
        private const val WARMING_PHASE_TAG = "__WARMING__"
        private const val TRAINING_PHASE_TAG = "__TRAINING__"
        private const val TEST_PHASE_TAG = "__TESTING__"
        private var compositeDisposable = CompositeDisposable()
        private var numberOfRun = 10
        var currentLearnerType = LearnerType.AMRulesRegressor

        @JvmStatic
        fun main1(args: Array<String>) {
            (0..3).forEach { _ ->  //number of Models
                println("##########   $currentLearnerType    ############")
                (1..numberOfRun).forEach {
                    if (DEBUG) {
                        println("************************* $it-th Experiment   ***************************")
                    }
                    Main().learnAndPredictionProcess(it)
                    println("************************* Resetting Stream ***************************")
                    compositeDisposable.clear()
                    streamReset()
                }
                currentLearnerType = currentLearnerType.next()
            }


        }


        @JvmStatic
        fun main(args: Array<String>) {
            currentLearnerType = LearnerType.FIMTDD
            numberOfRun = 10
            (1..numberOfRun).forEach {
                if (DEBUG) {
                    println("************************* $it-th Experiment   ***************************")
                }
                Main().learnAndPredictionProcess(it)
                println("************************* Resetting Stream ***************************")
                compositeDisposable.clear()
                streamReset()
            }
        }
    }

    fun learnAndPredictionProcess(i: Int) {
        var numberOfSeenFrequentCase = 0
        var numberOfSeenRareCases = 0
        var numberOfSeenRareCasesTestPhase = 0

        streamType = TRAIN_TEST
        loadStreaam()
        sleep(200)

        // Create learners
        val learnerBase = Learner(currentLearnerType, streamHeader!!).learner
        val learnerChebyshevUnder = learnerBase.copy() as AbstractClassifier
        val learnerChebyshevOver = learnerBase.copy() as AbstractClassifier

        // Training phase
        val trainingPhase = trainingPhaseStream.doOnNext { instance ->
            instance.deleteAttributeAt(instance.lastIndex())
            val shouldBeConsidered : Boolean = instance.shouldBeConsidered()
            runBlocking {
                val job: Job = GlobalScope.launch(Dispatchers.Default) {
                    coroutineScope {
                        launch {
                            // Base learner
                            learnerBase.trainOnInstanceImpl(instance)
                        }
                        launch {
                            val chebyshevProbability = getChebyshevProb(instance)
                            if (shouldBeConsidered && Random().nextDouble() > chebyshevProbability) {
                                numberOfSeenRareCases++
                                learnerChebyshevUnder.trainOnInstanceImpl(instance)
                            } else if (numberOfSeenFrequentCase < numberOfSeenRareCases && instance.shouldBeConsidered() && Random().nextDouble() < 0.15) {
                                numberOfSeenFrequentCase++
                                learnerChebyshevUnder.trainOnInstanceImpl(instance)
                            }
                            launch {
                                val chebyshevK = getChebyshevK(instance)
                                if (shouldBeConsidered) {
                                    (0..chebyshevK).forEach { _ ->
                                        learnerChebyshevOver.trainOnInstanceImpl(instance)
                                    }
                                }
                            }
                        }
                    }
                }
                job.join()
            }
        }.doOnComplete{
            println(" $GREEN $TAG :=> $TRAINING_PHASE_TAG Number of instances seen in this phase: Freq/Rare/Total :$numberOfSeenFrequentCase / $numberOfSeenRareCases / $streamCounter$RESET")
        }.subscribe({}, {
            println("Error:: Message is: ${it.message}. Below is the detail.")
            it.printStackTrace()
            exitProcess(1)
        }, {
            println(" $GREEN $TAG :=> $TRAINING_PHASE_TAG Completed!$RESET")
        })
        compositeDisposable.add(trainingPhase)


        val testPhase = testPhaseStream.doOnNext { instance ->

            val relevancyValue = instance.value(instance.lastIndex())
            instance.deleteAttributeAt(instance.lastIndex())

            val index = streamCounter - TRAINING_PHASE_LENGTH - 1
            val dataRow: DataRow = DataRow(dataframeCheby, index)
            dataRow.set(ROW_NUMBER, streamCounter)
            runBlocking {
                val job: Job = GlobalScope.launch(Dispatchers.Default) {
                    coroutineScope {
                        launch {
                            val basePrediction = learnerBase.getVotesForInstance(instance)
                            dataRow.set(TRUE_VALUE, instance.classValue())
                            dataRow.set(RELEVANCY, String.format("%.4f", relevancyValue).toDouble())
                            dataRow.set(PREDICTION_BASE, String.format("%.2f", basePrediction[0]).toDouble())
                            learnerBase.trainOnInstanceImpl(instance)
                        }
                        launch {
                            val underSamplingPrediction = learnerChebyshevUnder.getVotesForInstance(instance)
                            val chebyshevProbability = getChebyshevProb(instance)
                            dataRow.set(PREDICTION_UNDER, String.format("%.2f", underSamplingPrediction[0]).toDouble())
                            dataRow.set(CHEBYSHEV_PROBABILITY, String.format("%.2f", chebyshevProbability).toDouble())
                            if (instance.shouldBeConsidered() && Random().nextDouble() > chebyshevProbability) {
                                numberOfSeenRareCases++
                                numberOfSeenRareCasesTestPhase++
                                learnerChebyshevUnder.trainOnInstanceImpl(instance)
                            } else if (numberOfSeenFrequentCase < numberOfSeenRareCases && instance.shouldBeConsidered() && Random().nextDouble() < 0.15) {
                                numberOfSeenFrequentCase++
                                learnerChebyshevUnder.trainOnInstanceImpl(instance)
                            }

                        }
                        launch {
                            val overSamplingPrediction = learnerChebyshevOver.getVotesForInstance(instance)
                            val chebyshevK = getChebyshevK(instance)
                            dataRow.set(PREDICTION_OVER, String.format("%.2f", overSamplingPrediction[0]).toDouble())
                            dataRow.set(CHEBYSHEV_K, chebyshevK)
                            if (instance.shouldBeConsidered()) {
                                (0..chebyshevK).forEach { _ ->
                                    learnerChebyshevOver.trainOnInstanceImpl(instance)
                                }
                            }

                        }
                    }
                }
                job.join()
            }
            dataframeCheby.append(dataRow)
        }.doOnComplete {
            println(" $BLUE $TAG :=> $TEST_PHASE_TAG  Number of instances seen in this phase RareTestPhase/TotalRare/Total: $numberOfSeenRareCasesTestPhase / $numberOfSeenRareCases / $streamCounter $RESET")
            dataframeCheby.saveAsCSV("./Results/df.csv")
            val m = analyseCheby()
//            println()
//            println(m)
//            exitProcess(123)
            dataframeCheby.clear()
        }.doOnComplete {
            println(" $BLUE $TAG :=> $TEST_PHASE_TAG Number of instances seen in this phase: $numberOfSeenFrequentCase / $numberOfSeenRareCases / $streamCounter $RESET")
        }.subscribe({}, {
                println(" $RED_BOLD_BRIGHT $TAG :=> $TEST_PHASE_TAG ${it.message} $RESET")
                it.printStackTrace()
                exitProcess(1)
            }, {
                println(" $BLUE $TAG :=> $TEST_PHASE_TAG Completed!$RESET")
            })
        compositeDisposable.add(testPhase)
    }

}
