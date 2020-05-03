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
