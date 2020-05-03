# kotlinx-adb-fastboot
更新一段时间就太监了。
# how to use this shit?
```kotlin
fun main(){
  val (isSuccess,message) = adb shell {"echo hello motherfucker"}
  println(message)
  if (isSuccess){
    keepGoing
  }
}
  
```
## or do like this
``` kotlin
fun main(){
  
  fun doSthWithResult(cr:CommandResult,dosth:()->Unit){
    if (rr.isSuccesss){
      dosth()
    }
    ……
  }
  
  doSthWithResult(adb reboot bootloader){
    doSthWithResult(fastboot flash pair("boot_a","path/to/boot")){
      println("yeeeeeeaaah") 
     }
  }
}

```
