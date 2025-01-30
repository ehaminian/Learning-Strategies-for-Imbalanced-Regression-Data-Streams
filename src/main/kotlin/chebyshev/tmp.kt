package chebyshev

import common.SingletonDataframe
import de.unknownreality.dataframe.DataFrame
import de.unknownreality.dataframe.DataRow
import io.reactivex.disposables.CompositeDisposable
import kotlin.system.exitProcess

class tmp {
    companion object {



        @JvmStatic
        fun main(args: Array<String>) {

            (0..100000).forEach {
                val dataframeCheby1=SingletonDataframe.getInstanceForChebyshevMethod()
                try {
                    val dataRow: DataRow = DataRow(dataframeCheby1, it.toInt())
                    dataframeCheby1.append(dataRow)
                }
                catch (e: Exception){
                    println(it)
                    println(e.message)
                    print(e.stackTrace.toString())
                    exitProcess(1)
                }

            }
        }
    }
}