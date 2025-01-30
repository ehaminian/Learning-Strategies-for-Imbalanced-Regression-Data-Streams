package chebyshev


import common.*
import common.RUtils.savePicture
import common.StreamHelper.TARGET_MEDIAN
import common.StreamHelper.TRAINING_PHASE_LENGTH
import common.StreamHelper.extremeSides
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
import histogram.lastIndex
import java.lang.Thread.sleep
import io.reactivex.disposables.CompositeDisposable
import kotlin.system.exitProcess


class InfoGen {
    companion object {
        private val TAG: String = InfoGen::class.java.simpleName
        private var compositeDisposable = CompositeDisposable()
        private const val numberOfRun=10
        val currentLearnerType= LearnerType.TargetMean


        @JvmStatic
        fun main(args: Array<String>) {
            (1..numberOfRun).forEach {
                if(DEBUG){println("************************* $it-th Experiment   ***************************")}
                InfoGen().learnAndPredictionProcess(it)
                when(it){
                    numberOfRun->{}
                    else->{
                        if(DEBUG){println("************************* Resetting Stream ***************************")}
                        compositeDisposable.clear()
                        streamReset()
                    }
                }
            }
        }
    }


    fun learnAndPredictionProcess(i:Int){
        var numberOfSeenExamples=0
        var numberOfSeenExamplesInWarmingPhase=0
        streamType = StreamHelper.StreamType.TRAIN_TEST
        loadStreaam()
        sleep(200)

        val learnerInfoGen = Learner(currentLearnerType, streamHeader!!).learner

        val d1=trainingPhaseStream.doOnNext { instance ->

            when(currentLearnerType){
                LearnerType.AMRulesRegressor->{
                    instance.setValue(instance.lastIndex(),0.0)
                }
                else->{
                    instance.deleteAttributeAt(instance.lastIndex())
                }
            }
            learnerInfoGen.trainOnInstanceImpl(instance)
        }.subscribe({}, {
            println("Error:: Message is: ${it.message}. Below is the detail.")
            it.printStackTrace()
            exitProcess(0)
        }, {
            if (DEBUG) println("warmingPhase Finished. Number of instances seen in this phase: $numberOfSeenExamplesInWarmingPhase / $streamCounter")
        })
        compositeDisposable.add(d1)


        val d2=testPhaseStream.doOnNext { instance ->

            val phiValue = instance.value(instance.lastIndex())

            when(currentLearnerType){
                LearnerType.AMRulesRegressor->{
                    instance.setValue(instance.lastIndex(),0.0)
                }
                else->{
                    instance.deleteAttributeAt(instance.lastIndex())
                }
            }

            val index=streamCounter-TRAINING_PHASE_LENGTH-1
            val dataRow: DataRow = DataRow(dataframeGeneralPurpose,index )
            dataRow.set(ROW_NUMBER, streamCounter)
            val infoGenPrediction=learnerInfoGen.getVotesForInstance(instance)
            dataRow.set(TRUE_VALUE,instance.classValue())
            dataRow.set(PHI,phiValue)
            dataRow.set(PREDICTION_1,infoGenPrediction[0])
            learnerInfoGen.trainOnInstanceImpl(instance)
            dataframeGeneralPurpose.append(dataRow)
        }.doOnComplete {
            dataframeGeneralPurpose.saveAsCSV("./Results/df.csv")
            println("remainingPhase Finished. Number of instances seen in this phase: $numberOfSeenExamples")
        }
            .subscribe({}, {
                it.printStackTrace()

            },{
                if(i==1){
                    savePicture()
                }
                val m=when(extremeSides){
                    StreamHelper.ExtremeType.RIGHT->{RUtils.analyse("r", TARGET_MEDIAN)}
                    StreamHelper.ExtremeType.LEFT->{RUtils.analyse("l", TARGET_MEDIAN)}
                    StreamHelper.ExtremeType.BOTH_SIDE->{RUtils.analyse("b", TARGET_MEDIAN)}
                }
                println(m)

            })
        compositeDisposable.add(d2)
    }

}
