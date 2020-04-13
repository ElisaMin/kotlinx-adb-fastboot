package kotlinx.heizi.utills

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder
import kotlin.Boolean
import kotlinx.heizi.utills.CommandExecutor.run
import kotlinx.heizi.utills.PlatformTools.ADB.BootableMode.*

fun main(args: Array<String>) {
    PlatformTools.ADB shell {"su -c echo myshit"}
}
object PlatformTools{
    object ADB{
        val state:String get() {val (b,s) = adb("get-state");return if (b) s!! else "null"}
        val serialno:String get() {val (b,s) = adb("get-serialno");return if (b) s!! else "null"}
        private val adbSource  = ".\\lib\\adb.exe"
        infix fun adb(list: ArrayList<String>):CommandResult = run(arrayOf(list),false, adbSource)
        infix fun adb(string: String):CommandResult = run(string,false, adbSource)
        infix fun setVerity(isEnable:Boolean):CommandResult = this adb if (isEnable) "enable-verity" else "disable-verity"
        fun shell (list: ArrayList<String>, isRoot:Boolean):CommandResult = adb (if (isRoot) arrayListOf("su","-c").apply { addAll(list) } else list)
        infix fun root(isRoot: Boolean):CommandResult = adb(arrayListOf(if (isRoot)"root" else "unroot"))
        infix fun sideload(path:String):CommandResult = adb(arrayListOf("wait-for-sideload",path))
        infix fun remount (isReboot:Boolean) : CommandResult = adb(arrayListOf("remount"))
        infix fun pull (dirs: Pair<String,String?>):CommandResult = adb(arrayListOf("pull","${dirs.first}","${dirs.second}"))
        infix fun pull (dir: String):CommandResult = adb(arrayListOf("pull","${dir}"))
        infix fun push (dirs: Pair<String,String> ):CommandResult = adb(arrayListOf("push","${dirs.first}","${dirs.second}"))
        infix fun server (status :Boolean):CommandResult = this adb if (status)"start-server" else "kill-server"
        infix fun reconnect (status:Boolean?):CommandResult= this adb arrayListOf("reconnect", when (status) { null -> "";true ->"device"; false -> "offline" } )
        enum class BootableMode(index:Int) {
            Android(0),
            Recovery(1),
            Sideload(2),
            Bootloader(3),
            SideloadAutoReboot(4)
        }

        infix fun reboot(mode: BootableMode):CommandResult {
            return adb(arrayListOf("reboot",when(mode){
                BootableMode.Android -> ""
                BootableMode.Recovery -> "recovery"
                BootableMode.Sideload -> "sideload"
                BootableMode.Bootloader->"bootloader"
                BootableMode.SideloadAutoReboot -> "sideload-auto-reboot"
            }))
        }
        infix fun shell (shellCommand:()->String){ this adb arrayListOf("shell",shellCommand()) }
    }
}

object CommandExecutor {

    private fun getCharset(isGBK: Boolean):String = if (isGBK) "GBK" else "UTF-8"

    fun run(string: String): CommandResult = execute(listOf(string),"GBK")

    fun run(string: String,isSplit:Boolean,isGBK:Boolean): CommandResult = execute(if (isSplit) {string.split(" ")} else {arrayListOf(string)}, getCharset(isGBK))

    fun run(array: Array<ArrayList<String>>, isGBK:Boolean): CommandResult = run(array, getCharset(isGBK),null)

    fun run(arrayList: ArrayList<Pair<String,List<String>>>,charsetName: String):CommandResult{
        var resultCode = -114514
        val resultMessage = StringBuilder()
        for (it in arrayList) {
            if (!execute(ArrayList<String>().apply {add(it.first);addAll(it.second) },charsetName).apply {
                        resultMessage.append("message${if (arrayList.size == 1) "" else "\n"}")
                        resultCode = code
                    }.isSuccess){ break }
        }
        return CommandResult(resultCode,resultMessage.toString())
    }
    fun run(string: String,isGBK: Boolean,runnable: String) :CommandResult = execute(listOf(runnable,string), getCharset(isGBK))
    fun run(lists: Array<ArrayList<String>>,isGBK: Boolean,runnable: String?): CommandResult = run(lists, getCharset(isGBK),runnable)
    fun run(lists: Array<ArrayList<String>>,charsetName:String,runnable: String?): CommandResult {
        var resultCode = -114514
        val resultMessage = StringBuilder()
        val header= if (runnable == null ) { arrayListOf("cmd","/c") }else{ arrayListOf("$runnable") }

        for (list in lists) {
            var result = execute(ArrayList<String>().apply {addAll(header);addAll(list) },charsetName).apply {
                resultMessage.append("message${if (lists.size < 2) "" else "\n"}")
                resultCode = code
            }
            if (!result.isSuccess){
                break
            }
        }

        return CommandResult(resultCode,resultMessage.toString())
    }

    fun execute(list: List<String>, charsetName: String): CommandResult {
        "new command is running--------------".println()
        list.toString().println()
        var resultMessage = ""
        var resultCode = -1
        try {
            ProcessBuilder(list).redirectErrorStream(true).start().apply{
                InputStreamReader(inputStream,charsetName).apply {
                    BufferedReader(this).apply {
                        resultMessage = readText().println()
                    }.close()
                }.close()
             resultCode = exitValue()
            }.destroy()
        }catch (e:IOException){// CommandExecuter@
            if (e.message== null){ return CommandResult(114514) }
            else {
                with(e.message!!.println()){
                    when {
                        this find "error=" -> {
                            resultCode = this.split("error=")[1].split(",").apply {
                                resultMessage = when (size) {
                                    0 -> {// return@CommandExecuter CommandResult(114514)
                                        "null"
                                    }
                                    1 ->  "null"
                                    2 -> this[1]
                                    else ->{ toString().replace("[","").replace("]","") }
                                }
                            }[0].println().toInt()
                        }
                        else -> {
                            resultMessage = this
                            resultCode = 2333
                        }
                    }
                }
            }
        }catch (e:Exception){ with(e){
            resultCode = 2333
            resultMessage = message!!
            printStackTrace()
        }
        }finally {
            if (resultCode == 114514) {
                return CommandResult(114514)
            }else{
                return CommandResult(resultCode,resultMessage).also { it.println() }
            }
        }
    }
}

class CommandResult(code:Int){
    operator fun component1() = isSuccess
    operator fun component2() = message
    constructor(code:Int,message:String) : this(code) { this.message = message }
    val code = code
    var isSuccess = (code == 0)
    var message :String? = null
    fun isSuccess(string: String) : Boolean = (if (message==null)  false else  (message!! find string))
    override fun toString(): String = "Result:[$code,$message]"
    fun println() {println(toString())}
    fun equals(commandResult: CommandResult): kotlin.Boolean = ((commandResult.message == message) and (commandResult.code == code))
}

fun main() {
    CommandExecutor.execute(listOf("Sting"),"GBK")
}

// asrrayListOf(string) ++ array
//



infix fun String.find(string: String): Boolean = this match "[\\n]*.*[\\n]*.*${string}[\\n]*.*[\\n]*.*"
infix fun String.match(string: String):Boolean = this.matches(string.toRegex())
fun String.println():String = run {
    println(this)
    this }
