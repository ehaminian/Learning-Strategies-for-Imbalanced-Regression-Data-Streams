package common

import chebyshev.Main
import com.yahoo.labs.samoa.instances.Instance
import com.yahoo.labs.samoa.instances.InstancesHeader
import common.ConsoleColors.PURPLE
import common.ConsoleColors.RESET
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception

object StreamHelper {
    private val TAG = StreamHelper::class.java.simpleName

    ////////////////////////// DataSet ////////////////////////////////
    private const val datasetName: String = "18_"
    val relevancyIndex: Boolean? = true //can be null or an index number
    private const val replicationNumber=1
    ///////////////////////////////////////////////////////////////////

    private var datasetPath: String = getDatasetPaths(BASE_PATH).first { it.contains(datasetName) }
    val extremeSides=ExtremeType.BOTH_SIDE  //BOTH, RIGHT and LEFT


    var TOTAL_NUMBER_OF_SAMPLES = 0.0
    var WARMING_PHASE_LENGTH = 2000 // will be set on getCachedInstancesStream function
    var TRAINING_PHASE_LENGTH = 2000 * replicationNumber // will be set on getCachedInstancesStream function
    var TARGET_MIN = 0.0
    var TARGET_MAX = 0.0
    var TARGET_MEAN = 0.0
    var TARGET_STDDEV = 0.0
    var TARGET_MEDIAN = 0.0

    enum class StreamType {
        ALL, TRAIN_TEST, WARMING_TRAINING_TEST
    }
    enum class ExtremeType {
        BOTH_SIDE, LEFT, RIGHT
    }


    var streamType: StreamType = StreamType.ALL
    var streamHeader: InstancesHeader? = null
    var allStream: Observable<Instance>? = null

    var warmingphaseStream: Observable<Instance> = Observable.empty()
    var trainingPhaseStream: Observable<Instance> = Observable.empty()
    var testPhaseStream: Observable<Instance> = Observable.empty()



    val all = getDatasetPaths(BASE_PATH).also {
        println(it)
    }

    var stream = getCachedInstancesStream(datasetPath,replicationNumber)



    fun streamReset() {
        stream = getCachedInstancesStream(datasetPath,replicationNumber)
        streamCounter = 0
    }

    var streamCounter = 0
    fun loadStreaam() {
        GlobalScope.launch(Dispatchers.Default) {
            when (streamType) {
                StreamType.ALL -> {
                    if (DEBUG) println("$PURPLE$TAG StreamType.ALL has been selected$RESET")
                    allStream = Observable.create { emitter ->
                        try {
                            var inst: Instance
                            while (stream.hasMoreInstances()) {
                                inst = stream.nextInstance().data
                                emitter.onNext(inst)
                                ++streamCounter
                            }
                            emitter.onComplete()
                        } catch (e: Exception) {
                            emitter.onError(e)
                        }

                    }
                }
                StreamType.TRAIN_TEST -> {
                    if (DEBUG) println(" $PURPLE $TAG :=> StreamType.TRAIN_TEST has been selected$RESET")
                    trainingPhaseStream = Observable.create<Instance> { emitter ->
                        try {
                            var inst: Instance
                            while (stream.hasMoreInstances()) {
                                inst = stream.nextInstance().data
                                emitter.onNext(inst)
                                if (++streamCounter > TRAINING_PHASE_LENGTH ) {
                                    break
                                }
                            }
                            emitter.onComplete()
                            if (DEBUG) println(" $PURPLE $TAG :=> trainingPhaseStream Completed$RESET")
                        } catch (e: Exception) {
                            emitter.onError(e)
                        }
                    }.doOnComplete {
                        testPhaseStream = Observable.create { emitter ->
                            try {
                                var inst: Instance
                                while (stream.hasMoreInstances()) {
                                    inst = stream.nextInstance().data
                                    emitter.onNext(inst)
                                    ++streamCounter
                                }
                                emitter.onComplete()
                                if (DEBUG) println(" $PURPLE $TAG :=> testPhaseStream Completed$RESET")
                            } catch (e: Exception) {
                                emitter.onError(e)
                            }

                        }
                    }
                }
                StreamType.WARMING_TRAINING_TEST->{
                    if (DEBUG) println("$PURPLE$TAG StreamType.WARMING_TRAINING_TEST has been selected$RESET")
                    warmingphaseStream = Observable.create<Instance> { emitter ->
                        try {
                            var inst: Instance
                            while (stream.hasMoreInstances()) {
                                inst = stream.nextInstance().data
                                emitter.onNext(inst)
                                if (++streamCounter > WARMING_PHASE_LENGTH) {
                                    break
                                }
                            }
                            emitter.onComplete()
                            if (DEBUG) println("$PURPLE$TAG warmingPhaseStream Completed$RESET")
                        } catch (e: Exception) {
                            emitter.onError(e)
                        }
                    }.doOnComplete {
                        trainingPhaseStream = Observable.create<Instance?> { emitter ->
                            try {
                                var inst: Instance
                                while (stream.hasMoreInstances()) {
                                    inst = stream.nextInstance().data
                                    emitter.onNext(inst)
                                    if (++streamCounter > TRAINING_PHASE_LENGTH + WARMING_PHASE_LENGTH) {
                                        break
                                    }
                                }
                                emitter.onComplete()
                                if (DEBUG) println("$PURPLE$TAG trainingPhaseStream Completed$RESET")
                            } catch (e: Exception) {
                                emitter.onError(e)
                            }
                        }.doOnComplete {
                            testPhaseStream = Observable.create { emitter ->
                                try {
                                    var inst: Instance
                                    while (stream.hasMoreInstances()) {
                                        inst = stream.nextInstance().data
                                        emitter.onNext(inst)
                                        ++streamCounter
                                    }
                                    emitter.onComplete()
                                    if (DEBUG) println("$PURPLE$TAG testPhaseStream Completed$RESET")
                                } catch (e: Exception) {
                                    emitter.onError(e)
                                }

                            }
                        }
                    }
                }
            }
        }
    }
}