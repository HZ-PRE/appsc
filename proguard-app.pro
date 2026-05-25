-dontshrink
-dontoptimize
-dontpreverify
-ignorewarnings
-useuniqueclassmembernames

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Record,Exceptions
-keep class com.sync.sc.ScFXApp { *; }
-keep @org.springframework.boot.autoconfigure.SpringBootApplication class * { *; }
-keep @org.springframework.stereotype.Controller class * { *; }
-keep @org.springframework.web.bind.annotation.RestController class * { *; }
-keep @org.springframework.stereotype.Service class * { *; }
-keep @org.springframework.stereotype.Component class * { *; }
-keep @org.springframework.context.annotation.Configuration class * { *; }
-keep class com.sync.sc.entity.** { *; }
-keep class com.sync.sc.config.** { *; }
-keepclassmembers class * {
    @org.springframework.beans.factory.annotation.Autowired *;
    @jakarta.annotation.Resource *;
    @org.springframework.beans.factory.annotation.Value *;
    @org.springframework.web.bind.annotation.* *;
}
-keepclassmembers class * {
    public <init>();
    public void set*(...);
    public *** get*();
    public boolean is*();
}
