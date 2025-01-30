package chebyshev

import com.yahoo.labs.samoa.instances.Instance
import common.SingletonDataframe
import common.StreamHelper.TARGET_MEAN
import common.StreamHelper.TARGET_STDDEV
import de.unknownreality.dataframe.DataFrame
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt


fun getChebyshevProb(ins: Instance): Double {
    val t = abs(ins.classValue() - TARGET_MEAN) / (TARGET_STDDEV)
    return if (t < 1) 1.0 else 1/(t*t)
}
fun getChebyshevK(ins: Instance): Int {
    val t = abs(ins.classValue() - TARGET_MEAN) / (TARGET_STDDEV)
    return if (t < 1) 1 else ceil(t).toInt()
}
inline val dataframeCheby: DataFrame
    get() = SingletonDataframe.getInstanceForChebyshevMethod()

inline val dataframeGeneralPurpose: DataFrame
    get() = SingletonDataframe.getInstanceForTest()

inline val dataframeStatistics: DataFrame
    get() = SingletonDataframe.getInstanceForStatistics()