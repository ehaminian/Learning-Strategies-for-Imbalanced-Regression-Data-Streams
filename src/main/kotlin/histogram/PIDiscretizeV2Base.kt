package histogram

import kotlin.properties.Delegates

open class PIDiscretizeV2Base() {
    companion object{
        val TAG:String=PIDiscretizeV2Base::class.java.simpleName
    }
    protected var nunOfObservedSamples by Delegates.notNull<Int>() //Number of observed values
    protected var layer2UpdateFrequeny by Delegates.notNull<Int>()
    protected var histogramMethod by Delegates.notNull<MethodForHistogram>()
    protected var numBreaksInLayer1 by Delegates.notNull<Int>() //number of breaks in layer1
    protected var numBinsL2 by Delegates.notNull<Int>() //number of breaks in layer2
    protected var alfa by Delegates.notNull<Double>() //Threshold for Split an interval
    protected var min by Delegates.notNull<Double>()
    protected var max by Delegates.notNull<Double>()
    var shouldLayer2BeUpdated = false
    val selectedHistogramMethod get() = histogramMethod

    init {
        alfa = 0.75
        numBreaksInLayer1 = 200
        numBinsL2 = 3
        min = 0.0
        max = 1.0
        histogramMethod = MethodForHistogram.Equalwidth
        layer2UpdateFrequeny = 200
    }

    constructor(
        numberOfBinsInLayer1: Int,
        numberOfBinsInLayer2: Int,
        alfa: Double,
        min: Double,
        max: Double,
        histogramMethod: MethodForHistogram,
        layer2UpdateFrequency: Int
    ) : this() {
        this.alfa = alfa
        this.numBreaksInLayer1 = numberOfBinsInLayer1
        this.numBinsL2 = numberOfBinsInLayer2
        this.min = min
        this.max = max
        this.histogramMethod = histogramMethod
        this.layer2UpdateFrequeny = layer2UpdateFrequency
    }

    fun initialize() {
        nunOfObservedSamples = 0
    }
}

