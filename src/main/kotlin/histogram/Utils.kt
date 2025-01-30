package histogram

import com.yahoo.labs.samoa.instances.Instance
import common.SingletonDataframe
import de.unknownreality.dataframe.DataFrame


enum class MethodForHistogram {
    Equalwidth, EqualFrequency
}

enum class Normalization {
    STANDARD, PERCENTAGE, CONTRIBUTION
}

inline val dataframeHist: DataFrame
    get() = SingletonDataframe.getInstanceForHistogramMethod()

fun Instance.lastIndex() = this.numAttributes() - 1

/////////////// Based on Layer2
//fun PIDiscretizeV2.prob(instance: Instance): Double  {
//    val k = layer2.getIntervalIndexNumber(instance.classValue())
//    return layer2.normalProbabilityDistributionScaled[k]
//}


fun PIDiscretizeV2.trainOnInstanceImpl(instance: Instance) = trainOnValue(instance.classValue())


