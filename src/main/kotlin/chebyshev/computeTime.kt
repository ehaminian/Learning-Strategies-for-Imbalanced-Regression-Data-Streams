// Time calculation
package chebyshev

import common.*
import common.StreamHelper.loadStreaam
import common.StreamHelper.streamHeader
import common.StreamHelper.streamType
import common.StreamHelper.trainingPhaseStream
import common.LearnerType
import common.Learner
import histogram.lastIndex
import moa.classifiers.AbstractClassifier
import java.lang.Thread.sleep
import java.util.Random
import io.reactivex.disposables.CompositeDisposable
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis


class ComputeTime {
    companion object {
        private var compositeDisposable = CompositeDisposable()
        var timeUnder:Long=0L
        var timeOver:Long=0L
        var timeBase:Long=0L
        var currentLearnerType = LearnerType.PERCEPTRON

        @JvmStatic
        fun main(args: Array<String>) {
            currentLearnerType = LearnerType.PERCEPTRON
            ComputeTime().learnAndPredictionProcess()


        }
    }


    fun learnAndPredictionProcess() {
        var numberOfSeenFrequentCase = 0
        var numberOfSeenRareCases = 0

        streamType = StreamHelper.StreamType.TRAIN_TEST
        loadStreaam()
        sleep(200)

        val learnerBase = Learner(currentLearnerType, streamHeader!!).learner
        val learnerChebyshevUnder = learnerBase.copy() as AbstractClassifier
        val learnerChebyshevOver = learnerBase.copy() as AbstractClassifier

        val d1 = trainingPhaseStream.doOnNext { instance ->
            instance.deleteAttributeAt(instance.lastIndex())
            learnerBase.trainOnInstanceImpl(instance)
            timeUnder += measureTimeMillis {
                val chebyshevProbability = getChebyshevProb(instance)
                if (instance.shouldBeConsidered() && Random().nextDouble() > chebyshevProbability) {
                    numberOfSeenRareCases++
                    learnerChebyshevUnder.trainOnInstanceImpl(instance)
                } else if (numberOfSeenFrequentCase < numberOfSeenRareCases && instance.shouldBeConsidered() && Random().nextDouble() < 0.15) {
                    numberOfSeenFrequentCase++
                    learnerChebyshevUnder.trainOnInstanceImpl(instance)
                }
            }

            timeOver += measureTimeMillis {
                val chebyshevK = getChebyshevK(instance)
                if (instance.shouldBeConsidered()) {
                    (0..chebyshevK).forEach { _ ->
                        learnerChebyshevOver.trainOnInstanceImpl(instance)
                    }
                }
            }

            timeBase += measureTimeMillis {
                learnerBase.trainOnInstanceImpl(instance)
            }



        }.subscribe({}, {
            println("Error:: Message is: ${it.message}. Below is the detail.")
            it.printStackTrace()
            exitProcess(0)
        }, {
            println("timeUnder=$timeUnder,   timeOver=$timeOver,     timeBase=$timeBase")
            println("numberOfSeenRareCases=$numberOfSeenRareCases")
        })
        compositeDisposable.add(d1)
    }

}
