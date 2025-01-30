// This is the code that implement the paper
package chebyshev


import com.github.javacliparser.IntOption
import common.*
import common.StreamHelper.TARGET_MEDIAN
import common.StreamHelper.extremeSides
import common.StreamHelper.loadStreaam
import common.StreamHelper.streamCounter
import common.StreamHelper.streamHeader
import common.StreamHelper.streamType
import common.StreamHelper.testPhaseStream
import common.StreamHelper.trainingPhaseStream
import de.unknownreality.dataframe.DataRow
import histogram.lastIndex
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import moa.classifiers.core.driftdetection.ChangeDetector
import moa.classifiers.rules.AMRulesRegressor
import moa.classifiers.rules.core.anomalydetection.AnomalyDetector
import moa.options.ClassOption
import java.lang.Thread.sleep
import java.util.*
import kotlin.reflect.typeOf
import kotlin.system.exitProcess


class LearnFromNewData {
    companion object {
        private val TAG: String = LearnFromNewData::class.java.simpleName
        private var compositeDisposable = CompositeDisposable()

        @JvmStatic
        fun main(args: Array<String>) {
            LearnFromNewData().learnAndPredictionProcess()
        }
    }

    fun learnAndPredictionProcess() {
        var numberOfSeenFrequentCase = 0
        var numberOfSeenRareCases = 0
        var numberOfSeenExamplesInRemainingPhase = 0

        streamType = StreamHelper.StreamType.TRAIN_TEST
        loadStreaam()
        sleep(200)
        val learnerBase= AMRulesRegressor()

        learnerBase.changeDetector= ClassOption(
            "changeDetector",
            'H',
            "Change Detector.",
            ChangeDetector::class.java,
            "PageHinkleyDM -d 0.05 -l 3.4028234663852886E38")
        learnerBase.anomalyDetector= ClassOption(
            "anomalyDetector",
            'A',
            "Anomaly Detector.",
            AnomalyDetector::class.java,
            "OddsRatioScore -t -10.0 -p CantellisInequality")
        learnerBase.gracePeriodOption= IntOption(
            "gracePeriod",
            'g',
            "Hoeffding Bound Parameter. The number of instances a leaf should observe between split attempts.",
            20, 1, 2147483647);
        learnerBase.modelContext = streamHeader
        learnerBase.prepareForUse()
        val learnerChebyshevUnder = learnerBase.copy() as AMRulesRegressor
        val learnerChebyshevOver = learnerBase.copy() as AMRulesRegressor





        val tRAINING = trainingPhaseStream.doOnNext { instance ->
            val phiValue = instance.value(instance.lastIndex())
            instance.deleteAttributeAt(instance.lastIndex())
            learnerBase.trainOnInstanceImpl(instance)
            val chebyshevProbability = getChebyshevProb(instance)
            val chebyshevK = getChebyshevK(instance)
            if (instance.shouldBeConsidered() && Random().nextDouble() > chebyshevProbability) {
                numberOfSeenRareCases++
                learnerChebyshevUnder.trainOnInstanceImpl(instance)
            } else if (numberOfSeenFrequentCase < numberOfSeenRareCases && instance.shouldBeConsidered() && Random().nextDouble() < 0.15) {
                numberOfSeenFrequentCase++
                learnerChebyshevUnder.trainOnInstanceImpl(instance)
            }
            if (instance.shouldBeConsidered()) {
                (0..chebyshevK).forEach { _ ->
                    learnerChebyshevOver.trainOnInstanceImpl(instance)
                }
            }

        }.subscribe({}, {
            println("Error:: Message is: ${it.message}. Below is the detail.")
            it.printStackTrace()
            exitProcess(1)
        }, {
//            dataframeCheby.saveAsCSV("./Results/df.csv")
            if (DEBUG) println("warmingPhase Finished. Number of instances seen in this phase: $numberOfSeenFrequentCase / $numberOfSeenRareCases / $streamCounter")
        })
        compositeDisposable.add(tRAINING)


        val tEST = testPhaseStream.doOnNext { instance ->

            runBlocking {
                val job: Job = GlobalScope.launch(Dispatchers.Default) {
                    coroutineScope {
                        launch {
                            val basePrediction = learnerBase.getVotesForInstance(instance)
                            learnerBase.trainOnInstanceImpl(instance)
                        }
                        launch {
                            val underSamplingPrediction = learnerChebyshevUnder.getVotesForInstance(instance)
                            val chebyshevProbability = getChebyshevProb(instance)

                            if (instance.shouldBeConsidered() && Random().nextDouble() > chebyshevProbability) {
                                numberOfSeenRareCases++
                                numberOfSeenExamplesInRemainingPhase++
                                learnerChebyshevUnder.trainOnInstanceImpl(instance)
                            } else if (numberOfSeenFrequentCase < numberOfSeenRareCases && instance.shouldBeConsidered() && Random().nextDouble() < 0.15) {
                                numberOfSeenFrequentCase++
                                learnerChebyshevUnder.trainOnInstanceImpl(instance)
                            }

                        }
                        launch {
                            val overSamplingPrediction = learnerChebyshevOver.getVotesForInstance(instance)
                            val chebyshevK = getChebyshevK(instance)

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
        }.doOnComplete {
            dataframeCheby.saveAsCSV("./Results/df.csv")
            println("remainingPhase Finished. Number of instances seen in this phase: $numberOfSeenExamplesInRemainingPhase")
        }
            .subscribe({}, {
                it.printStackTrace()

            }, {
                val m = when (extremeSides) {
                    StreamHelper.ExtremeType.RIGHT -> {
                        RUtils.analyse("r", TARGET_MEDIAN)
                    }
                    StreamHelper.ExtremeType.LEFT -> {
                        RUtils.analyse("l", TARGET_MEDIAN)
                    }
                    StreamHelper.ExtremeType.BOTH_SIDE -> {
                        RUtils.analyse("b", TARGET_MEDIAN)
                    }
                }
                println(m)

            })
        compositeDisposable.add(tEST)
    }

}
