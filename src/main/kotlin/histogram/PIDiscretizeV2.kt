package histogram

import common.BETA
import common.ConsoleColors.*
import common.EPSILON
import common.numberofseensamples
import java.lang.Double.max
import java.lang.Exception
import kotlin.math.exp
import kotlin.properties.Delegates
import kotlin.system.exitProcess

class PIDiscretizeV2(
    numberOfBinsInLayer1: Int,
    numberOfBinsInLayer2: Int,
    alfa: Double,
    min: Double,
    max: Double,
    histogramMethod: MethodForHistogram,
    layer2UpdateFrequency: Int
) :
    PIDiscretizeV2Base(
        numberOfBinsInLayer1,
        numberOfBinsInLayer2,
        alfa,
        min,
        max,
        histogramMethod,
        layer2UpdateFrequency
    ) {
    companion object {
        val TAG = PIDiscretizeV2::class.java.simpleName
        const val DEBUG = false
    }

    private val l1: Layer1 = Layer1()
    private val l2: Layer2 = Layer2()

    val layer1
        get() = l1
    val layer2
        get() = l2

    inner class Layer1() {
        private var step by Delegates.notNull<Double>()
        private val firstIndex = 0
        private var breaksLst: MutableList<Double>
        private var countsLst: MutableList<Double>
        init {
            step = (max - min) / numBreaksInLayer1
            breaksLst = DoubleArray(numBreaksInLayer1+1).toMutableList()
            countsLst = DoubleArray(numBreaksInLayer1).toMutableList()
            (0 until breaksLst.size).forEach { i ->
                breaksLst[i] = min + step * i
            }
        }

        val breaks
            get() = breaksLst
        val counts
            get() = countsLst



        fun update(x: Double) {
            val k = getIntervalIndexNumber(x)
            countsLst[k] += 1.0
            nunOfObservedSamples++


//            val fraction =
//                (1.0 + countsLst[k]) / (nunOfObservedSamples + 2) //should be double. Note: Don't change 1.0 to 1
//
//            if (nunOfObservedSamples >= layer2UpdateFrequeny && fraction > alfa) {
//                if (DEBUG) println("$TAG - Split process: \"x=$x, k=$k, fraction=$fraction\". Also set shouldLayer2BeUpdateed=true")
//                shouldLayer2BeUpdated = true
//                val halfValue = countsLst[k] / 2
//                countsLst[k] = halfValue
//                when (k) {
//                    firstIndex -> {
//                        countsLst.add(firstIndex, halfValue)
//                        val newBinPoint = binsLst[firstIndex] - step
//                        binsLst.add(firstIndex, newBinPoint)
//                    }
//                    countsLst.lastIndex -> {
//                        countsLst.add(countsLst.lastIndex + 1, halfValue)
//                        val newBinPoint = binsLst[binsLst.lastIndex] + step
//                        binsLst.add(binsLst.lastIndex + 1, newBinPoint)
//
//                    }
//                    else -> {
//                        countsLst.add(k + 1, halfValue)
//                        val newBinPoint = (binsLst[k - 1] + binsLst[k]) / 2
//                        binsLst.add(k, newBinPoint)
//                    }
//                }
//                numBinsL1 += 1
//            }

        }

        fun getIntervalIndexNumber(x: Double): Int {
            var k = numBreaksInLayer1 / 2
            return when {
                x < breaksLst[firstIndex] -> {
                    min = x
                    breaksLst[firstIndex] = x
                    firstIndex
                }
                x > breaksLst[breaksLst.lastIndex] -> {
                    max = x
                    breaksLst[breaksLst.lastIndex] = x
                    breaksLst.lastIndex - 1
                }
                else -> {
                    while (k < breaksLst.lastIndex && x >= breaksLst[k]) {
                        k += 1
                    }
                    while (k > firstIndex && x < breaksLst[k]) {
                        k -= 1
                    }
                    k
                }
            }
        }

        fun showBins() {
            val m = mutableListOf<String>()
            breaks.forEach {
                m.add(String.format("%.2f", it))
            }
            println("layer1Bins with size '${breaks.size}' => $m")
        }

        fun showCounts() {
            println("layer1Counts with size '${countsLst.size}' and sum '${countsLst.sum()}' => $countsLst")
        }

        fun getProb(x: Double): Double {
            val k = getIntervalIndexNumber(x)
            val maxValue = countsLst.maxOrNull()
            val scaledCounts:MutableList<Double> = ArrayList(countsLst)

            scaledCounts.replaceAll { it / (maxValue!! + EPSILON)}
//            return  1 - scaledCounts[k]
            return  exp(-1 * BETA * scaledCounts[k])
        }
    }


    inner class Layer2() {
        private var step by Delegates.notNull<Double>()
        private var numberOfExamplesInEachBin by Delegates.notNull<Double>()
        private lateinit var binsLst: MutableList<Double>
        private lateinit var countsLst: MutableList<Double>
        private lateinit var normalizedWeightCountsLst: List<Double>
        private var binsLengthsLst = mutableListOf<Double>()
        var ignoreUpdate = false

        private lateinit var pdfScaled: List<Double>
        private lateinit var contributionMethod: List<Double>
        private lateinit var percentageMethod: List<Double>

        private fun initialize() {
            when (histogramMethod) {
                MethodForHistogram.Equalwidth -> {
                    step = (l1.breaks[l1.breaks.lastIndex - 1] - l1.breaks[1]) / (numBinsL2 - 1)
                    binsLst = DoubleArray(numBinsL2).toMutableList()
                    countsLst = DoubleArray(numBinsL2 + 1).toMutableList()
                    (0 until binsLst.size).forEach { i ->
                        binsLst[i] = l1.breaks[1] + step * i
                    }
                }
                MethodForHistogram.EqualFrequency -> {
                    numberOfExamplesInEachBin = max(l1.counts.sum() / numBinsL2, l1.counts.maxOrNull()!! + EPSILON)
                    binsLst = mutableListOf<Double>()
                    countsLst = mutableListOf<Double>()
                    l1.counts.findLast { it > numberOfExamplesInEachBin }?.let {
                        println(RED + "You need to tune the initialization parameters of the ${PIDiscretizeV2Base::class.java.simpleName} class")
                        println("Since you have counts in layer1 containing items (like '$it') greater then numberOfExamplesInEachBin=$numberOfExamplesInEachBin")
                        println("We can not proceed with these parameters$RESET")
                        exitProcess(1)
                    }
                }
            }
        }

        internal fun update() {
            initialize()
            when (histogramMethod) {
                MethodForHistogram.Equalwidth -> {
                    var front: Int = 0
                    var tail: Int = 0
                    var pointerToLayer2countsLst: Int = 0
                    binsLst.forEach { bin ->
                        front = l1.getIntervalIndexNumber(bin)
                        (tail until front).forEach { pointerToLayer1countsLst ->
                            countsLst[pointerToLayer2countsLst] += l1.counts[pointerToLayer1countsLst]
                        }
                        tail = front
                        pointerToLayer2countsLst++
                    }
                    (tail until l1.counts.lastIndex + 1).forEach { pointerToLayer1countsLst ->
                        countsLst[pointerToLayer2countsLst] += l1.counts[pointerToLayer1countsLst]
                    }
                }
                MethodForHistogram.EqualFrequency -> {
                    var indexBins: Int = 0
                    var indexCounts: Int = 0
                    var indexl1counts: Int = 0
                    val l1Counts1 = l1.counts.dropWhile { it == 0.toDouble() }
                    val l1Bins1 = l1.breaks.drop(l1.counts.lastIndex - l1Counts1.lastIndex)
                    val l1Counts = l1Counts1.dropLastWhile { it == 0.toDouble() }
                    val l1Bins = l1Bins1.dropLast(l1Counts1.lastIndex - l1Counts.lastIndex)
                    binsLst.add(indexBins++, l1Bins[0])
                    while (indexl1counts <= l1Counts.lastIndex) {
                        var s: Double = 0.toDouble()
                        while (indexl1counts <= l1Counts.lastIndex && s + l1Counts[indexl1counts] <= numberOfExamplesInEachBin) {
                            s += l1Counts[indexl1counts++]
                        }
                        countsLst.add(indexCounts++, s)
                        binsLst.add(indexBins++, l1Bins[indexl1counts])
                    }
                }
            }
            shouldLayer2BeUpdated = false
            updateNormalizedWeightCountsLst()
            if(selectedHistogramMethod==MethodForHistogram.EqualFrequency) {
                updateBinsLengthsLst()
            }
            makeScaledPdfFromHistogram()
//            makePercentage()
//            makeContribution()
        }

        val bins
            get() = when (histogramMethod) {
                MethodForHistogram.Equalwidth -> {
                    mutableListOf(binsLst[0] - step) + binsLst + mutableListOf(binsLst[binsLst.lastIndex] + step)
                }
                MethodForHistogram.EqualFrequency -> {
                    binsLst
                }
            }
        val counts
            get() = countsLst
        val normalizedWeightCounts
            get() = normalizedWeightCountsLst
        val binsLengths
            get() = binsLengthsLst
        val normalProbabilityDistributionScaled
            get() = pdfScaled
        val contributionProbabilityDistribution
            get() = contributionMethod
        val percentageProbabilityDistribution
            get() = percentageMethod

        fun getIntervalIndexNumber(x: Double): Int {
            try {
                var k = (bins.lastIndex) / 2
                return when {
                    x < bins[0] -> {
                        println(YELLOW_BOLD + "The response may not be precise enough due to the value you have chosen for 'min' in the initialization" + RESET)
                        0 // First Index of the counts
                    }
                    x > bins[bins.lastIndex] -> {
                        println(YELLOW_BOLD + "The response may not be precise enough due to the value you have chosen for 'max' in the initialization" + RESET)
                        counts.lastIndex
                    }
                    else -> {
                        while (k < bins.lastIndex && x >= bins[k]) {
                            k += 1
                        }
                        while (k > 0 && x < bins[k]) {
                            k -= 1
                        }
                        k
                    }
                }
            } catch (e: Exception) {
                println("$RED_BOLD_BRIGHT selectedHistogramMethod=$selectedHistogramMethod, Error: ${e.printStackTrace()}$RESET ")
                exitProcess(1)
            }

        }

        private fun updateNormalizedWeightCountsLst() {
            val sumCounts = counts.sum()
            val weightCountsLst = counts.map { (it * 1.0) / sumCounts }
            val maxWeightCountsLst = weightCountsLst.maxOrNull()!!
            normalizedWeightCountsLst = weightCountsLst.map { (it * 1.0) / maxWeightCountsLst }
        }

        private fun updateBinsLengthsLst() {
            binsLengthsLst = mutableListOf()
            bins.reduceIndexed { index, acc, d ->
                binsLengthsLst.add(index - 1, d - acc)
                d
            }
        }

        private fun makeScaledPdfFromHistogram() {
            pdfScaled = when (selectedHistogramMethod) {
                MethodForHistogram.Equalwidth -> {
                    val maxCounts = counts.maxOrNull()!!
                    val minCounts = counts.minOrNull()!!
                    val dominant = maxCounts - minCounts + EPSILON
                    counts.mapIndexed { _, item ->
                        ((item - minCounts) * 1.0) / dominant
                    }
                }
                MethodForHistogram.EqualFrequency -> {
                    val maxBinLength = binsLengths.maxOrNull()!!
                    val minBinLength = binsLengths.minOrNull()!!
                    val dominant = maxBinLength - minBinLength + EPSILON
                    binsLengths.mapIndexed { index, countLength ->
                        (1 - (((countLength - minBinLength) * 1.0) / dominant)) * normalizedWeightCountsLst[index]
//                        (1-(((countLength - minBinLength) * 1.0) / dominant))
                    }
                }
            }
        }

        private fun makePercentage() {
            percentageMethod = when (selectedHistogramMethod) {
                MethodForHistogram.Equalwidth -> {
                    val maxCounts = countsLst.maxOrNull()!!
                    countsLst.mapIndexed { _, item ->
                        (item * 1.0) / maxCounts
                    }
                }
                MethodForHistogram.EqualFrequency -> {
                    val maxCountLength = binsLengthsLst.maxOrNull()!!
                    binsLengthsLst.mapIndexed { index, item ->
                        (1 - ((item * 1.0) / maxCountLength)) * normalizedWeightCountsLst[index]
//                        (1-((item * 1.0) / maxCountLength))
                    }
                }
            }
        }

        private fun makeContribution() {
            contributionMethod = when (selectedHistogramMethod) {
                MethodForHistogram.Equalwidth -> {
                    val sumCounts = countsLst.sum()
                    countsLst.mapIndexed { _, item ->
                        (item * 1.0) / sumCounts
                    }
                }
                MethodForHistogram.EqualFrequency -> {
                    val sumCountLength = binsLengthsLst.sum()
                    binsLengthsLst.mapIndexed { index, item ->
                        (1 - ((item * 1.0) / sumCountLength)) * normalizedWeightCountsLst[index]
//                        (1-((item * 1.0) / sumCountLength))
                    }
                }
            }
        }

        fun showBins() {
            val m = mutableListOf<String>()
            bins.forEach {
                m.add(String.format("%.2f", it))
            }
            println("layer2Bins with size '${bins.size}' => $m")
        }

        fun showCounts() {
            println("layer2Counts with size '${countsLst.size}' and sum '${countsLst.sum()}' => $countsLst")
        }
    }

    fun trainOnValue(x: Double) {
        l1.update(x)
//        if (!l2.ignoreUpdate) {
//            if (numberofseensamples > 0 && numberofseensamples % layer2UpdateFrequeny == 0) {
//                if (DEBUG) println("$TAG - Number of observed examples is: $numberofseensamples. Set shouldLayer2BeUpdateed=true")
//                shouldLayer2BeUpdated = true
//            }
//            if (shouldLayer2BeUpdated) {
//                l2.update()
//            }
//        }
        numberofseensamples++
    }
}